package la.renzhen.remoting.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import la.renzhen.remoting.*;
import la.renzhen.remoting.code.RemotingAbstract;
import la.renzhen.remoting.commons.NamedThreadFactory;
import la.renzhen.remoting.netty.code.CoderProvider;
import la.renzhen.remoting.netty.code.DefaultCoderProvider;
import la.renzhen.remoting.netty.code.NettyDecoder;
import la.renzhen.remoting.netty.code.NettyEncoder;
import la.renzhen.remoting.netty.utils.NettyUtils;
import la.renzhen.remoting.protocol.RemotingCommand;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-27 20:17
 */
@Slf4j
public class NettyRemotingClient extends RemotingAbstract<Channel> implements RemotingClient<Channel> {

    //@formatter:off
    private String clientName;
    @Getter private NettyClientConfig nettyClientConfig;
    private final Bootstrap bootstrap = new Bootstrap();

    @Setter @Getter private CoderProvider coderProvider;

    private final EventLoopGroup eventLoopGroupWorker;
    private DefaultEventExecutorGroup defaultEventExecutorGroup;

    private NettyChannel channel;

    /**
     * Invoke the callback methods in this executor when process response.
     */
    private ExecutorService callbackExecutor;
    //@formatter:on

    public NettyRemotingClient(final NettyClientConfig nettyClientConfig) {
        this(UUID.randomUUID().toString(), nettyClientConfig);
    }

    public NettyRemotingClient(final String clientName, final NettyClientConfig nettyClientConfig) {
        super(nettyClientConfig);
        this.clientName = clientName;
        this.nettyClientConfig = nettyClientConfig;

        final int callThreadSize = nettyClientConfig.getCallbackThreadSize();
        if (callThreadSize > 0) {
            this.callbackExecutor = Executors.newFixedThreadPool(callThreadSize, new NamedThreadFactory("NettyClientCallback", callThreadSize));
        }
        this.eventLoopGroupWorker = new NioEventLoopGroup(1, new NamedThreadFactory("NettyClientSelector"));
    }

    @Override
    public RemotingChannel<Channel> getChannel() {
        return channel;
    }

    protected RemotingChannel<Channel> selectChannel(final String address) {
        return this.channel;
    }

    @Override
    public RemotingCommand invokeSync(RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingException {
        return this.invokeSyncHandler(getChannel(), request, timeoutMillis);
    }

    @Override
    public void invokeAsync(RemotingCommand request, long timeoutMillis, InvokeCallback invokeCallback) throws InterruptedException, RemotingException {
        this.invokeAsyncHandler(getChannel(), request, timeoutMillis, invokeCallback);
    }

    @Override
    public void invokeOneway(RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingException {
        this.invokeOnewayHandler(getChannel(), request, timeoutMillis);
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
    protected void startupTCPListener() {
        if (this.coderProvider == null) {
            this.coderProvider = new DefaultCoderProvider(nettyClientConfig);
        }

        final int workerSize = nettyClientConfig.getWorkerThreads();
        this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(workerSize, new NamedThreadFactory("NettyClientWorkerThread", workerSize));

        Bootstrap handler = this.bootstrap.group(this.eventLoopGroupWorker).channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, false)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, nettyClientConfig.getConnectTimeoutMillis())
                .option(ChannelOption.SO_SNDBUF, nettyClientConfig.getSocketSndBufSize())
                .option(ChannelOption.SO_RCVBUF, nettyClientConfig.getSocketRcvBufSize())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        //TODO ssl
                       /*if (nettyClientConfig.isUseTLS()) {
                            if (null != sslContext) {
                                pipeline.addFirst(defaultEventExecutorGroup, "sslHandler", sslContext.newHandler(ch.alloc()));
                                log.info("Prepend SSL handler");
                            } else {
                                log.warn("Connections are insecure as SSLContext is null!");
                            }
                        }*/
                        pipeline.addLast(defaultEventExecutorGroup,
                                coderProvider.encode(), coderProvider.decode(),
                                new IdleStateHandler(nettyClientConfig.getReaderIdleTimeSeconds(), nettyClientConfig.getWriterIdleTimeSeconds(),
                                        nettyClientConfig.getAllIdleTimeSeconds()),
                                new NettyConnectManageHandler(), new NettyClientHandler());
                    }
                });

        this.createAndConnectChannel();
    }

    protected void createAndConnectChannel() {
        String address = nettyClientConfig.getHost() + ":" + nettyClientConfig.getPort();
        ChannelFuture channelFuture = this.bootstrap.connect(nettyClientConfig.getHost(), nettyClientConfig.getPort());
        try {
            channelFuture.awaitUninterruptibly(nettyClientConfig.getConnectTimeoutMillis());
        } catch (Exception e) {
            throw new RemotingException(RemotingException.Type.Connect, address, e);
        }
        this.channel = new NettyChannel(channelFuture);
        log.info("Link to server {}", address);
    }

    @Override
    protected void shutdownTCPListener(boolean interrupted) {
        try {
            this.eventLoopGroupWorker.shutdownGracefully();

            if (this.defaultEventExecutorGroup != null) {
                this.defaultEventExecutorGroup.shutdownGracefully();
            }

            if (this.callbackExecutor != null) {
                this.callbackExecutor.shutdown();
            }
        } catch (Exception e) {
            log.error("close client error", e);
        }
    }

    protected void channelConnectHandler(ChannelHandlerContext ctx) {
    }

    protected void channelDisconnectHandler(ChannelHandlerContext ctx) {
    }

    protected void channelCloseHandler(ChannelHandlerContext ctx) {
    }

    class NettyClientHandler extends SimpleChannelInboundHandler<RemotingCommand> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
            final String address = NettyUtils.parseChannelRemoteAddr(ctx.channel());
            processMessageReceived(selectChannel(address), msg);
        }
    }

    class NettyConnectManageHandler extends ChannelDuplexHandler {
        @Override
        public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
            final String local = localAddress == null ? "UNKNOWN" : NettyUtils.parseSocketAddressAddr(localAddress);
            final String remote = remoteAddress == null ? "UNKNOWN" : NettyUtils.parseSocketAddressAddr(remoteAddress);
            log.info("NETTY CLIENT PIPELINE: CONNECT  {} => {}", local, remote);
            super.connect(ctx, remoteAddress, localAddress, promise);
            channelConnectHandler(ctx);
            putNettyEvent(new ChannelEvent<>(ChannelEvent.Type.CONNECT, selectChannel(remote)));
        }

        @Override
        public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            final String remoteAddress = NettyUtils.parseChannelRemoteAddr(ctx.channel());
            log.info("NETTY CLIENT PIPELINE: DISCONNECT {}", remoteAddress);
            putNettyEvent(new ChannelEvent<>(ChannelEvent.Type.CLOSE, selectChannel(remoteAddress)));
            channelDisconnectHandler(ctx);
            closeChannel(selectChannel(remoteAddress));
            super.disconnect(ctx, promise);
        }

        @Override
        public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            final String remoteAddress = NettyUtils.parseChannelRemoteAddr(ctx.channel());
            log.info("NETTY CLIENT PIPELINE: CLOSE {}", remoteAddress);
            putNettyEvent(new ChannelEvent<>(ChannelEvent.Type.CLOSE, selectChannel(remoteAddress)));
            channelCloseHandler(ctx);
            closeChannel(selectChannel(remoteAddress));
            super.close(ctx, promise);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                if (event.state().equals(IdleState.ALL_IDLE)) {
                    final String remoteAddress = NettyUtils.parseChannelRemoteAddr(ctx.channel());
                    log.warn("NETTY CLIENT PIPELINE: IDLE exception [{}]", remoteAddress);
                    putNettyEvent(new ChannelEvent<>(ChannelEvent.Type.IDLE, selectChannel(remoteAddress)));
                    closeChannel(selectChannel(remoteAddress));
                }
            }
            ctx.fireUserEventTriggered(evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            final String remoteAddress = NettyUtils.parseChannelRemoteAddr(ctx.channel());
            log.warn("NETTY CLIENT PIPELINE: exceptionCaught {}", remoteAddress);
            log.warn("NETTY CLIENT PIPELINE: exceptionCaught exception.", cause);
            closeChannel(selectChannel(remoteAddress));
            putNettyEvent(new ChannelEvent<Channel>(ChannelEvent.Type.EXCEPTION, selectChannel(remoteAddress)).setData(cause));
        }
    }

    public void closeChannel(final RemotingChannel<Channel> channel) {
        failFast(channel);
        NettyUtils.closeChannel(channel.getChannel());
    }

    @Override
    public String getUnique() {
        return this.clientName;
    }

    @Override
    public String getStartBanner() {
        return super.getStartBanner() + " .*. NettyClient .*.";
    }
}