package la.renzhen.remoting.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import la.renzhen.remoting.*;
import la.renzhen.remoting.commons.NamedThreadFactory;
import la.renzhen.remoting.netty.coder.CoderProvider;
import la.renzhen.remoting.netty.utils.NettyUtils;
import la.renzhen.remoting.protocol.RemotingCommand;
import lombok.Getter;

import java.net.SocketAddress;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-27 20:17
 */

public class NettyRemotingClient extends NettyRemoting implements RemotingClient<Channel> {

    //@formatter:off
    private String clientName;
    @Getter private NettyClientConfig nettyClientConfig;
    private final Bootstrap bootstrap = new Bootstrap();

    private final EventLoopGroup eventLoopGroupWorker;

    private volatile NettyChannel channel;

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

    protected ChannelInitializer<SocketChannel> getChannelInitializer(){
        return new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                beforeInitChannel(ch);
                ChannelPipeline pipeline = ch.pipeline();
                final CoderProvider coderProvider = getCoderProvider();
                pipeline.addLast(getEventExecutorGroup(),
                        coderProvider.encode(), coderProvider.decode(),
                        new IdleStateHandler(nettyClientConfig.getReaderIdleTimeSeconds(), nettyClientConfig.getWriterIdleTimeSeconds(),
                                nettyClientConfig.getAllIdleTimeSeconds()),
                        new NettyConnectManageHandler(), new NettyClientHandler());
                afterInitChannel(ch);
            }
        };
    }

    @Override
    protected void startupTCPListener() {
        super.startupTCPListener();

        Bootstrap handler = this.bootstrap.group(this.eventLoopGroupWorker).channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, false)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, nettyClientConfig.getConnectTimeoutMillis())
                .option(ChannelOption.SO_SNDBUF, nettyClientConfig.getSocketSndBufSize())
                .option(ChannelOption.SO_RCVBUF, nettyClientConfig.getSocketRcvBufSize())
                .handler(getChannelInitializer());

        this.connectToServer();
    }

    protected void connectToServer() {
        this.channel = connectToServer(nettyClientConfig.getHost(), nettyClientConfig.getPort());
        //TODO 自动重连
    }

    protected NettyChannel connectToServer(String host, int port) {
        String address = host + ":" + port;
        ChannelFuture channelFuture = this.bootstrap.connect(host, port);
        try {
            channelFuture.awaitUninterruptibly(nettyClientConfig.getConnectTimeoutMillis());
        } catch (Exception e) {
            throw new RemotingException(RemotingException.Type.Connect, address, e);
        }
        if (!channelFuture.channel().isActive()) {
            throw new RemotingException(RemotingException.Type.Connect, "Cannot connect to the server: " + address);
        }
        NettyChannel channel = new NettyChannel(channelFuture);
        log.info("connect to server: {}", address);
        return channel;
    }

    @Override
    protected void shutdownTCPListener(boolean interrupted) {
        try {
            this.eventLoopGroupWorker.shutdownGracefully();

            if (this.callbackExecutor != null) {
                this.callbackExecutor.shutdown();
            }

            super.shutdownTCPListener(interrupted);
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
        //NettyUtils.closeChannel(channel.getChannel());
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
