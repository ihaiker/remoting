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
import io.netty.handler.ipfilter.RuleBasedIpFilter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import la.renzhen.remoting.*;
import la.renzhen.remoting.commons.NamedThreadFactory;
import la.renzhen.remoting.commons.Pair;
import la.renzhen.remoting.netty.coder.CoderProvider;
import la.renzhen.remoting.netty.utils.NettyUtils;
import la.renzhen.remoting.netty.utils.NiceSelector;
import la.renzhen.remoting.protocol.ClientInfoHeader;
import la.renzhen.remoting.protocol.RemotingCommand;
import la.renzhen.remoting.protocol.RemotingSysResponseCode;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.lang.reflect.Constructor;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;


public class NettyRemotingServer extends NettyRemoting implements RemotingServer<Channel> {

    //@formatter:off
    protected final String serverName;
    protected final ServerBootstrap serverBootstrap;
    protected EventLoopGroup eventLoopGroupSelector;
    protected EventLoopGroup eventLoopGroupBoss;

    protected final NettyServerConfig nettyServerConfig;

    //final ChannelGroup channels =  new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    protected final ConcurrentMap<String /* addr */, NettyChannel> channelTables = new ConcurrentHashMap<>();

    protected ExecutorService callbackExecutor;
    @Getter @Setter protected RuleBasedIpFilter ruleBasedIpFilter;
    //@formatter:on

    protected RemotingDefender<Channel> defender;

    static {
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
    }

    public NettyRemotingServer(final NettyServerConfig config) {
        this(UUID.randomUUID().toString(), config);
    }

    public NettyRemotingServer(final String serverName, final NettyServerConfig config) {
        super(config);
        setModule("RemotingServer");

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
    public void registerDefender(RemotingDefender<Channel> defender) {
        this.defender = defender;
    }

    public RemotingDefender<Channel> getDefender() {
        return defender;
    }


    protected ChannelInitializer<SocketChannel> getChannelInitializer() {
        return new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                //ipFilter
                if (getRuleBasedIpFilter() != null) {
                    pipeline.addLast(RuleBasedIpFilter.class.getSimpleName(), getRuleBasedIpFilter());
                }
                beforeInitChannel(ch);
                final CoderProvider coderProvider = getCoderProvider();
                pipeline.addLast(getEventExecutorGroup(),
                        coderProvider.encode(), coderProvider.decode(),
                        new IdleStateHandler(nettyServerConfig.getReaderIdleTimeSeconds(),
                                nettyServerConfig.getWriterIdleTimeSeconds(), nettyServerConfig.getAllIdleTimeSeconds()),
                        new NettyConnectManageHandler(),
                        new ClientInfoHandler(),
                        new NettyServerHandler());
                afterInitChannel(ch);
            }
        };
    }

    @Override
    protected void startupSocket() {
        super.startupSocket();
        ServerBootstrap childHandler = this.serverBootstrap.group(this.eventLoopGroupBoss, this.eventLoopGroupSelector)
                .channel(useEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_KEEPALIVE, false)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_SNDBUF, nettyServerConfig.getSocketSndBufSize())
                .childOption(ChannelOption.SO_RCVBUF, nettyServerConfig.getSocketRcvBufSize())
                .localAddress(getListenerAddress())
                .childHandler(getChannelInitializer());

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
    protected void shutdownSocket(boolean interrupted) {
        try {
            if (this.eventLoopGroupBoss != null) {
                this.eventLoopGroupBoss.shutdownGracefully();
            }

            if (this.eventLoopGroupSelector != null) {
                this.eventLoopGroupSelector.shutdownGracefully();
            }

            if (this.callbackExecutor != null) {
                this.callbackExecutor.shutdown();
            }

            super.shutdownSocket(interrupted);
        } catch (Exception e) {
            log.error("NettyRemotingServer shutdown exception, ", e);
        }
    }

    class ClientInfoHandler extends SimpleChannelInboundHandler<RemotingCommand> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand request) throws Exception {
            final String remoteAddr = NettyUtils.parseChannelRemoteAddr(ctx.channel());
            NettyChannel channel = (NettyChannel) getChannel(remoteAddr);
            ClientInfoHeader header = request.getCustomHeaders(ClientInfoHeader.class);
            channel.fromHeader(header);
            log.info("Client reports information: {}", header);

            RemotingCommand response = RemotingCommand.response(request);

            if (getDefender() != null) {
                if (!getDefender().checked(channel)) {
                    log.info("the client defender by {}", getDefender().getClass());
                    response.setError(RemotingSysResponseCode.REJECT, "Refuse to connect");
                }
            }

            if (response.isSuccess()) {
                ctx.pipeline().remove(this);

                ClientInfoHeader responseHeader = new ClientInfoHeader();
                responseHeader.setUnique(getUnique()).setModule(getModule()).setAttributes(NettyRemotingServer.this.getAttributes());
                response.setCustomHeaders(responseHeader);
            }

            Pair<RequestProcessor<Channel>, ExecutorService> pair = getDefaultRequestProcessor();
            if (pair != null) {
                pair.getSecond().submit(() -> {
                    channel.writeAndFlush(response);
                });
            } else {
                channel.writeAndFlush(response);
            }
        }
    }


    class NettyServerHandler extends SimpleChannelInboundHandler<RemotingCommand> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
            final String remoteAddr = NettyUtils.parseChannelRemoteAddr(ctx.channel());
            RemotingChannel<Channel> channel = getChannel(remoteAddr);
            processMessageReceived(channel, msg);
        }
    }

    protected void channelRegisteredHandler(ChannelHandlerContext ctx) {

    }
    protected void channelUnregisteredHandler(ChannelHandlerContext ctx) {

    }

    class NettyConnectManageHandler extends ChannelDuplexHandler {
        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = NettyUtils.parseChannelRemoteAddr(ctx.channel());
            log.info("NETTY SERVER PIPELINE: channelRegistered {}", remoteAddress);
            super.channelRegistered(ctx);
            NettyChannel nettyChannel = new NettyChannel(ctx);
            channelTables.put(remoteAddress, nettyChannel);
            channelRegisteredHandler(ctx);
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = NettyUtils.parseChannelRemoteAddr(ctx.channel());
            log.info("NETTY SERVER PIPELINE: channelUnregistered, the channel {}", remoteAddress);
            super.channelUnregistered(ctx);
            channelTables.remove(remoteAddress);
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
