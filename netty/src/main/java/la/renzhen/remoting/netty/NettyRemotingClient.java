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
import la.renzhen.remoting.protocol.ClientInfoHeader;
import la.renzhen.remoting.protocol.RemotingCommand;
import lombok.Getter;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-27 20:17
 */
@Accessors(chain = true)
public class NettyRemotingClient extends NettyRemoting implements RemotingClient<Channel> {

    //@formatter:off
    private String clientName;
    @Getter private NettyClientConfig nettyClientConfig;
    private final Bootstrap bootstrap = new Bootstrap();

    private final EventLoopGroup eventLoopGroupWorker;

    /** Invoke the callback methods in this executor when process response. */
    private ExecutorService callbackExecutor;

    @Getter private RemotingAuth auth;

    //@formatter:on

    private final Lock channelLock = new ReentrantLock();

    private final AtomicReference<String> serverAddressChoosed = new AtomicReference<String>();
    private final AtomicReference<List<String>> serverAddressSupported = new AtomicReference<List<String>>();

    private static int randomSelectServerIndex() {
        Random r = new Random();
        return Math.abs(r.nextInt() % 999) % 999;
    }

    private final AtomicInteger serverIndex = new AtomicInteger(randomSelectServerIndex());
    private final Map<String /* addr */, NettyChannel> channelTables = new HashMap<String, NettyChannel>();


    public NettyRemotingClient(@NotNull @Size(min = 1) final List<String> serverAddress, final NettyClientConfig nettyClientConfig) {
        this(UUID.randomUUID().toString(), serverAddress, nettyClientConfig);
    }

    public NettyRemotingClient(final String clientName, @NotNull @Size(min = 1) final List<String> serverAddress, final NettyClientConfig nettyClientConfig) {
        super(nettyClientConfig);
        setModule("RemotingClient");
        this.clientName = clientName;
        this.nettyClientConfig = nettyClientConfig;
        this.serverAddressSupported.set(serverAddress);

        final int callThreadSize = nettyClientConfig.getCallbackThreadSize();
        if (callThreadSize > 0) {
            this.callbackExecutor = Executors.newFixedThreadPool(callThreadSize, new NamedThreadFactory("NettyClientCallback", callThreadSize));
        }
        this.eventLoopGroupWorker = new NioEventLoopGroup(1, new NamedThreadFactory("NettyClientSelector"));
    }

    @Override
    public RemotingCommand invokeSync(RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingException {
        RemotingChannel<Channel> channel = getChannel();
        if (channel == null) {
            throw new RemotingException(RemotingException.Type.Connect, "The service you selected is disconnect.");
        }
        return this.invokeSyncHandler(channel, request, timeoutMillis);
    }

    @Override
    public void invokeAsync(RemotingCommand request, long timeoutMillis, InvokeCallback invokeCallback) throws InterruptedException, RemotingException {
        RemotingChannel<Channel> channel = getChannel();
        if (channel == null) {
            throw new RemotingException(RemotingException.Type.Connect, "The service you selected is disconnect.");
        }
        this.invokeAsyncHandler(channel, request, timeoutMillis, invokeCallback);
    }

    @Override
    public void invokeOneway(RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingException {
        RemotingChannel<Channel> channel = getChannel();
        if (channel == null) {
            throw new RemotingException(RemotingException.Type.Connect, "The service you selected is disconnect.");
        }
        this.invokeOnewayHandler(channel, request, timeoutMillis);
    }

    private RemotingChannel<Channel> checkAddressAndGetChannel(final String address) throws InterruptedException {
        if (serverAddressSupported.get().contains(address)) {
            throw new RemotingException(RemotingException.Type.NotSupportServer, "The server address provided is not supported.");
        }
        RemotingChannel<Channel> channel = selectOrCreateChannel(address);
        if (channel == null) {
            throw new RemotingException(RemotingException.Type.Connect, "The service you selected is disconnect.");
        }
        return channel;
    }

    @Override
    public RemotingCommand invokeSync(final String address, RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingException {
        RemotingChannel<Channel> channel = checkAddressAndGetChannel(address);
        return this.invokeSyncHandler(channel, request, timeoutMillis);
    }

    @Override
    public void invokeAsync(String address, RemotingCommand request, long timeoutMillis, InvokeCallback invokeCallback) throws InterruptedException, RemotingException {
        RemotingChannel<Channel> channel = checkAddressAndGetChannel(address);
        this.invokeAsyncHandler(channel, request, timeoutMillis, invokeCallback);
    }

    @Override
    public void invokeOneway(String address, RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingException {
        RemotingChannel<Channel> channel = checkAddressAndGetChannel(address);
        this.invokeOnewayHandler(channel, request, timeoutMillis);
    }

    @Override
    public ExecutorService getCallbackExecutor() {
        return Optional.ofNullable(this.callbackExecutor)
                .orElseGet(this::getPublicExecutor);
    }

    public NettyRemotingClient setCallbackExecutor(ExecutorService callbackExecutor) {
        final ExecutorService oldExecutorService = this.callbackExecutor;
        this.callbackExecutor = callbackExecutor;
        if (oldExecutorService != null) {
            oldExecutorService.shutdown();
        }
        return this;
    }

    protected ChannelInitializer<SocketChannel> getChannelInitializer() {
        return new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                beforeInitChannel(ch);
                ChannelPipeline pipeline = ch.pipeline();
                final CoderProvider coderProvider = getCoderProvider();
                pipeline.addLast(getEventExecutorGroup(), coderProvider.encode(), coderProvider.decode(),

                        new IdleStateHandler(nettyClientConfig.getReaderIdleTimeSeconds(), nettyClientConfig.getWriterIdleTimeSeconds(),
                                nettyClientConfig.getAllIdleTimeSeconds()),

                        new NettyConnectManageHandler(), new NettyClientHandler());

                afterInitChannel(ch);
            }
        };
    }

    @Override
    protected void startupSocket() {
        super.startupSocket();
        createBootstrap();

        if (!reconnect()) {
            throw new RemotingException(RemotingException.Type.Connect, "Connection server exception");
        }
    }

    /**
     * Reconnect all services。This method will not throw an exception, will return false if an exception happen.
     *
     * @return True All services are successfully connected, otherwise some connections are unsuccessful
     */
    @Override
    public boolean reconnect() {
        try {
            List<String> addressSupport = this.serverAddressSupported.get();
            if (null != addressSupport && !addressSupport.isEmpty()) {
                for (int i = 0; i < addressSupport.size(); i++) {
                    String address = addressSupport.get(i);
                    selectOrCreateChannel(address);
                }
            }
            return true;
        } catch (Exception e) {
            log.error("Reconnect service exception", e);
            return false;
        }
    }

    protected Bootstrap createBootstrap() {
        return this.bootstrap.group(this.eventLoopGroupWorker).channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, false)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, nettyClientConfig.getConnectTimeoutMillis())
                .option(ChannelOption.SO_SNDBUF, nettyClientConfig.getSocketSndBufSize())
                .option(ChannelOption.SO_RCVBUF, nettyClientConfig.getSocketRcvBufSize())
                .handler(getChannelInitializer());
    }

    @Override
    public RemotingChannel<Channel> selectOrCreateChannel(final String address) throws InterruptedException {
        if (null == address) {
            return getChannel();
        }

        RemotingChannel<Channel> channel = channelTables.get(address);
        if (channel != null && channel.isOK()) {
            return channel;
        }

        return createChannel(address);
    }


    @Override
    public RemotingChannel<Channel> getChannel() throws InterruptedException {
        String address = serverAddressChoosed.get();
        if (null != address) {
            NettyChannel channel = this.channelTables.get(address);
            if (channel != null && channel.isOK()) {
                return channel;
            }
        }
        final List<String> supportAddress = this.serverAddressSupported.get();
        if (this.channelLock.tryLock(nettyClientConfig.getConnectTimeoutMillis(), TimeUnit.MILLISECONDS)) {
            try {
                address = this.serverAddressChoosed.get();
                if (address != null) {
                    NettyChannel channel = this.channelTables.get(address);
                    if (channel != null && channel.isOK()) {
                        return channel;
                    }
                }

                if (supportAddress != null && !supportAddress.isEmpty()) {
                    for (int i = 0; i < supportAddress.size(); i++) {
                        //int index = Math.abs(this.serverIndex.incrementAndGet()) % supportAddress.size(); //fixbug: 并不是什么情况下都是大于冷 0
                        int index = this.serverIndex.incrementAndGet();
                        if (index < 0) {
                            index = 0;
                        }
                        index = index % supportAddress.size();

                        String newAddr = supportAddress.get(index);
                        this.serverAddressChoosed.set(newAddr);
                        log.info("new name server is chosen. OLD: {} , NEW: {}. serverIndex = {}", address, newAddr, serverIndex.get());
                        NettyChannel channelNew = this.createChannel(newAddr);
                        if (channelNew != null && channelNew.isOK()) {
                            return channelNew;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("create server channel exception", e);
            } finally {
                this.channelLock.unlock();
            }
        } else {
            log.warn("try to lock server, but timeout, {}ms", nettyClientConfig.getConnectTimeoutMillis());
        }
        return null;
    }

    protected NettyChannel createChannel(String address) throws InterruptedException {
        NettyChannel channel = channelTables.get(address);
        if (channel != null && channel.isOK()) {
            return channel;
        }

        if (channelLock.tryLock(nettyClientConfig.getConnectTimeoutMillis(), TimeUnit.MILLISECONDS)) {
            try {
                channel = channelTables.get(address);
                if (channel != null) {
                    if (channel.isOK()) {
                        return channel;
                    } else {
                        channelTables.remove(address);
                        channel.close();
                    }
                }

                ChannelFuture channelFuture = this.bootstrap.connect(NettyUtils.string2SocketAddress(address));
                channel = new NettyChannel(channelFuture);
                if (!channelFuture.awaitUninterruptibly(nettyClientConfig.getConnectTimeoutMillis())) {
                    throw new RemotingException(RemotingException.Type.Connect, "Cannot connect to the server: " + address);
                }
                if (!channel.isOK()) {
                    throw new RemotingException(RemotingException.Type.Connect, "Cannot connect to the server: " + address);
                }
                channelTables.put(address, channel);

                ClientInfoHeader serverHeaderResponse = reportClient(channel);
                log.info("server response info unique:{}, module:{}, attributes:{}",
                        serverHeaderResponse.getUnique(), serverHeaderResponse.getModule(), serverHeaderResponse.getAttributes());
            } catch (RemotingException e) {
                channelTables.remove(address);
                throw e;
            } finally {
                channelLock.unlock();
            }
        }
        return channel;
    }

    @Override
    public void registerAuth(final RemotingAuth auth, final String username, final String password) {
        this.auth = auth;
        if (this.auth != null) {
            this.setAttribute(RemotingAuth.AUTH_USERNAME, username);
            this.setAttribute(RemotingAuth.AUTH_PASSWORD, password);
        }
    }

    public ClientInfoHeader reportClient(NettyChannel channel) {
        //report client
        ClientInfoHeader requestHeader = new ClientInfoHeader();
        requestHeader.setUnique(getUnique());
        requestHeader.setModule(getModule());
        Map<String, String> attr = this.getAttributes();
        if (attr != null) {
            requestHeader.setAttributes(new HashMap<>(attr));
        }

        appendReportHeaderAttributes(channel, requestHeader);

        log.info("report client info to server.");
        RemotingCommand request = RemotingCommand.request(0).setCustomHeaders(requestHeader);
        try {
            RemotingCommand response = this.invokeSyncHandler(channel, request, nettyClientConfig.getConnectTimeoutMillis());
            if (response.isSuccess()) {
                ClientInfoHeader responseHeader = response.getCustomHeaders(ClientInfoHeader.class);
                channel.fromHeader(responseHeader);
                return responseHeader;
            } else {
                String error = response.getError();
                throw new RemotingException(RemotingException.Type.Auth, error);
            }
        } catch (RemotingException e) {
            throw e;
        } catch (Exception e) {
            throw new RemotingException(RemotingException.Type.Connect, e);
        }
    }

    protected void appendReportHeaderAttributes(NettyChannel channel, ClientInfoHeader clientInfoHeader) {
        final RemotingAuth auth = getAuth();
        if (auth != null) {
            final Map<String, String> attrs = clientInfoHeader.getAttributes();
            String authUsername = Optional.ofNullable(attrs).map(s -> s.get(RemotingAuth.AUTH_USERNAME)).orElse("");
            String authPassword = Optional.ofNullable(attrs).map(s -> s.get(RemotingAuth.AUTH_PASSWORD)).orElse("");
            log.info("enable auth: {} {}", channel.address(), authUsername);
            String authSignature = auth.signature(authUsername, authPassword);
            attrs.remove(RemotingAuth.AUTH_PASSWORD);
            attrs.put(RemotingAuth.AUTH_SIGNATURE, authSignature);
            clientInfoHeader.setAttributes(attrs);
        }
    }

    @Override
    protected void shutdownSocket(boolean interrupted) {
        try {
            this.eventLoopGroupWorker.shutdownGracefully();

            if (this.callbackExecutor != null) {
                this.callbackExecutor.shutdown();
            }

            super.shutdownSocket(interrupted);
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
            NettyChannel channel = channelTables.get(address);
            processMessageReceived(channel, msg);
        }
    }

    class NettyConnectManageHandler extends ChannelDuplexHandler {
        @Override
        public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
            final String local = localAddress == null ? "UNKNOWN" : NettyUtils.parseSocketAddressAddr(localAddress);
            final String remote = remoteAddress == null ? "UNKNOWN" : NettyUtils.parseSocketAddressAddr(remoteAddress);
            log.info("NETTY CLIENT PIPELINE: CONNECT  {} => {}", local, remote);
            super.connect(ctx, remoteAddress, localAddress, promise);
            NettyChannel channel = channelTables.get(remote);
            channelConnectHandler(ctx);
            putNettyEvent(new ChannelEvent<>(ChannelEvent.Type.CONNECT, channel));
        }

        @Override
        public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            final String remoteAddress = NettyUtils.parseChannelRemoteAddr(ctx.channel());
            log.info("NETTY CLIENT PIPELINE: DISCONNECT {}", remoteAddress);
            NettyChannel channel = channelTables.get(remoteAddress);
            putNettyEvent(new ChannelEvent<>(ChannelEvent.Type.CLOSE, channel));
            channelDisconnectHandler(ctx);
            closeChannel(channel);
            super.disconnect(ctx, promise);
        }

        @Override
        public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            final String remoteAddress = NettyUtils.parseChannelRemoteAddr(ctx.channel());
            log.info("NETTY CLIENT PIPELINE: CLOSE {}", remoteAddress);
            NettyChannel channel = new NettyChannel(ctx);
            putNettyEvent(new ChannelEvent<>(ChannelEvent.Type.CLOSE, channel));
            channelCloseHandler(ctx);
            closeChannel(channel);
            super.close(ctx, promise);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                if (event.state().equals(IdleState.ALL_IDLE)) {
                    final String remoteAddress = NettyUtils.parseChannelRemoteAddr(ctx.channel());
                    log.warn("NETTY CLIENT PIPELINE: IDLE exception [{}]", remoteAddress);
                    NettyChannel channel = channelTables.get(remoteAddress);
                    putNettyEvent(new ChannelEvent<>(ChannelEvent.Type.IDLE, channel));
                    closeChannel(new NettyChannel(ctx));
                }
            }
            ctx.fireUserEventTriggered(evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            final String remoteAddress = NettyUtils.parseChannelRemoteAddr(ctx.channel());
            log.warn("NETTY CLIENT PIPELINE: exceptionCaught exception {}.", remoteAddress, cause);
            closeChannel(channelTables.get(remoteAddress));
            putNettyEvent(new ChannelEvent<Channel>(ChannelEvent.Type.EXCEPTION, channelTables.get(remoteAddress)).setData(cause));
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
