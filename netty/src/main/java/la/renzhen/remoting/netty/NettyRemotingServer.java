package la.renzhen.remoting.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import la.renzhen.remoting.*;
import la.renzhen.remoting.code.RemotingAbstract;
import la.renzhen.remoting.commons.NamedThreadFactory;
import la.renzhen.remoting.netty.code.CoderProvider;
import la.renzhen.remoting.netty.code.DefaultCoderProvider;
import la.renzhen.remoting.netty.code.NettyDecoder;
import la.renzhen.remoting.netty.code.NettyEncoder;
import la.renzhen.remoting.netty.utils.NettyUtils;
import la.renzhen.remoting.netty.utils.NiceSelector;
import la.renzhen.remoting.protocol.RemotingCommand;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

@Slf4j
public class NettyRemotingServer extends RemotingAbstract<Channel> implements RemotingServer<Channel> {

    private final String serverName;
    private final ServerBootstrap serverBootstrap;
    private DefaultEventExecutorGroup defaultEventExecutorGroup;
    private EventLoopGroup eventLoopGroupSelector;
    private EventLoopGroup eventLoopGroupBoss;

    @Setter
    @Getter
    private CoderProvider coderProvider;

    private final NettyServerConfig nettyServerConfig;

    //final ChannelGroup channels =  new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private final ConcurrentMap<String /* addr */, NettyChannel> channelTables = new ConcurrentHashMap<>();

    static {
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
    }

    private NettyServerConfig serverConfig;

    protected ExecutorService callbackExecutor;

    public NettyRemotingServer(final NettyServerConfig config) {
        this(UUID.randomUUID().toString(), config);
    }

    public NettyRemotingServer(final String serverName, final NettyServerConfig config) {
        super(config);

        this.serverName = serverName;
        this.nettyServerConfig = config;

        this.serverBootstrap = new ServerBootstrap();

        this.initEventLoopGroup();

        final int callbackExecutorSize = config.getCallbackExecutorThreads();
        if (callbackExecutorSize > 0) {
            this.callbackExecutor = Executors.newFixedThreadPool(callbackExecutorSize, new NamedThreadFactory("NettyCallbackExecutor", callbackExecutorSize));
        }
    }

    @SneakyThrows
    protected void initEventLoopGroup() {
        Class<? extends EventLoopGroup> eventLoopGroup = useEpoll() ? EpollEventLoopGroup.class : NioEventLoopGroup.class;

        Constructor<? extends EventLoopGroup> constructor = eventLoopGroup.getConstructor(Integer.TYPE, ThreadFactory.class);

        final int bossThreads = nettyServerConfig.getBossThreads();
        this.eventLoopGroupBoss = constructor.newInstance(bossThreads, new NamedThreadFactory("NettyNIOBoss", bossThreads));

        final int selectorThreads = nettyServerConfig.getSelectorThreads();
        this.eventLoopGroupSelector = constructor.newInstance(selectorThreads, new NamedThreadFactory("NettyServerNIOSelector", selectorThreads));
    }

    private boolean useEpoll() {
        return NiceSelector.isLinuxPlatform()
                && nettyServerConfig.isUseEPollNativeSelector()
                && Epoll.isAvailable();
    }

    @Override
    public RemotingChannel<Channel> getChannel(String address) {
        return channelTables.get(address);
    }

    @Override
    public RemotingCommand invokeSync(String address, RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingException {
        return invokeSyncHandler(getChannel(address), request, timeoutMillis);
    }

    @Override
    public void invokeAsync(String address, RemotingCommand request, long timeoutMillis, InvokeCallback invokeCallback) throws InterruptedException, RemotingException {
        this.invokeAsyncHandler(getChannel(address), request, timeoutMillis, invokeCallback);
    }

    @Override
    public void invokeOneway(String address, RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingException {
        this.invokeOnewayHandler(getChannel(address), request, timeoutMillis);
    }

    @Override
    protected void startupTCPListener() {
        if (this.coderProvider == null) {
            this.coderProvider = new DefaultCoderProvider(nettyServerConfig);
        }
        final int serverWorker = nettyServerConfig.getWorkerThreads();
        this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(serverWorker, new NamedThreadFactory("NettyServerCodecThread", serverWorker));

        ServerBootstrap childHandler = this.serverBootstrap.group(this.eventLoopGroupBoss, this.eventLoopGroupSelector)
                .channel(useEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_KEEPALIVE, false)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_SNDBUF, nettyServerConfig.getSocketSndBufSize())
                .childOption(ChannelOption.SO_RCVBUF, nettyServerConfig.getSocketRcvBufSize())
                .localAddress(getListenerAddress())
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                //TODO SSL
                                // .addLast(defaultEventExecutorGroup, Constants.HANDSHAKE_HANDLER_NAME, new HandshakeHandler(defaultEventExecutorGroup, null, nettyServerConfig.getTlsMode()))
                                .addLast(defaultEventExecutorGroup,
                                        coderProvider.encode(), coderProvider.decode(),
                                        new IdleStateHandler(nettyServerConfig.getReaderIdleTimeSeconds(), nettyServerConfig.getWriterIdleTimeSeconds(),
                                                nettyServerConfig.getAllIdleTimeSeconds()),
                                        new NettyConnectManageHandler(),
                                        new NettyServerHandler()
                                );
                    }
                });

        if (nettyServerConfig.isPooledByteBufAllocatorEnable()) {
            childHandler.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        }

        try {
            ChannelFuture sync = this.serverBootstrap.bind().sync();
            InetSocketAddress addr = (InetSocketAddress) sync.channel().localAddress();
            int port = addr.getPort();
            log.info("the server start " + port);
        } catch (InterruptedException e1) {
            throw new RuntimeException("this.serverBootstrap.bind().sync() InterruptedException", e1);
        }
    }

    protected SocketAddress getListenerAddress() {
        if (null == nettyServerConfig.getHost()) {
            return new InetSocketAddress(this.nettyServerConfig.getPort());
        } else {
            return new InetSocketAddress(this.nettyServerConfig.getHost(), this.nettyServerConfig.getPort());
        }
    }


    @Override
    public ExecutorService getCallbackExecutor() {
        return Optional.ofNullable(this.callbackExecutor)
                .orElseGet(this::getPublicExecutor);
    }

    public void setCallbackExecutor(ExecutorService callbackExecutor) {
        final ExecutorService oldExecutorService = this.callbackExecutor;
        this.callbackExecutor = callbackExecutor;
        if (oldExecutorService != null) {
            oldExecutorService.shutdown();
        }
    }

    @Override
    protected void shutdownTCPListener(boolean interrupted) {
        try {
            if (this.eventLoopGroupBoss != null) {
                this.eventLoopGroupBoss.shutdownGracefully();
            }

            if (this.eventLoopGroupSelector != null) {
                this.eventLoopGroupSelector.shutdownGracefully();
            }

            if (this.defaultEventExecutorGroup != null) {
                this.defaultEventExecutorGroup.shutdownGracefully();
            }
            if (this.callbackExecutor != null) {
                this.callbackExecutor.shutdown();
            }
        } catch (Exception e) {
            log.error("NettyRemotingServer shutdown exception, ", e);
        }
    }


    class NettyServerHandler extends SimpleChannelInboundHandler<RemotingCommand> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
            RemotingChannel<Channel> channel = getChannel(NettyUtils.parseChannelRemoteAddr(ctx.channel()));
            processMessageReceived(channel, msg);
        }
    }

    protected void channelRegisteredHandler(ChannelHandlerContext ctx) {
        final String remoteAddress = NettyUtils.parseChannelRemoteAddr(ctx.channel());
        NettyChannel nettyChannel = new NettyChannel(ctx);
        channelTables.put(remoteAddress, nettyChannel);
    }

    protected void channelUnregisteredHandler(ChannelHandlerContext ctx) {
        final String remoteAddress = NettyUtils.parseChannelRemoteAddr(ctx.channel());
        channelTables.remove(remoteAddress);
    }

    class NettyConnectManageHandler extends ChannelDuplexHandler {
        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = NettyUtils.parseChannelRemoteAddr(ctx.channel());
            log.info("NETTY SERVER PIPELINE: channelRegistered {}", remoteAddress);
            super.channelRegistered(ctx);
            channelRegisteredHandler(ctx);
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = NettyUtils.parseChannelRemoteAddr(ctx.channel());
            log.info("NETTY SERVER PIPELINE: channelUnregistered, the channel {}", remoteAddress);
            super.channelUnregistered(ctx);
            channelUnregisteredHandler(ctx);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = NettyUtils.parseChannelRemoteAddr(ctx.channel());
            log.info("NETTY SERVER PIPELINE: channelActive, the channel {}", remoteAddress);
            super.channelActive(ctx);

            RemotingChannel<Channel> nettyChannel = getChannel(remoteAddress);
            putNettyEvent(new ChannelEvent<>(ChannelEvent.Type.CONNECT, nettyChannel));
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = NettyUtils.parseChannelRemoteAddr(ctx.channel());
            log.info("NETTY SERVER PIPELINE: channelInactive, the channel {}", remoteAddress);
            super.channelInactive(ctx);

            RemotingChannel<Channel> nettyChannel = getChannel(remoteAddress);
            putNettyEvent(new ChannelEvent<>(ChannelEvent.Type.CLOSE, nettyChannel));
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                if (event.state().equals(IdleState.ALL_IDLE)) {
                    final String remoteAddress = NettyUtils.parseChannelRemoteAddr(ctx.channel());
                    log.warn("NETTY SERVER PIPELINE: IDLE exception [{}]", remoteAddress);

                    RemotingChannel<Channel> nettyChannel = getChannel(remoteAddress);
                    putNettyEvent(new ChannelEvent<>(ChannelEvent.Type.IDLE, nettyChannel));
                    NettyUtils.closeChannel(ctx.channel());
                }
            }
            ctx.fireUserEventTriggered(evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            final String remoteAddress = NettyUtils.parseChannelRemoteAddr(ctx.channel());
            log.warn("NETTY SERVER PIPELINE: exceptionCaught {}", remoteAddress);
            log.warn("NETTY SERVER PIPELINE: exceptionCaught exception.", cause);
            RemotingChannel<Channel> channel = getChannel(remoteAddress);
            putNettyEvent(new ChannelEvent<Channel>(ChannelEvent.Type.EXCEPTION, channel));
            NettyUtils.closeChannel(ctx.channel());
        }
    }

    @Override
    public String getUnique() {
        return this.serverName;
    }

    @Override
    public String getStartBanner() {
        return super.getStartBanner() + "  .*. NettyServer .*.";
    }
}
