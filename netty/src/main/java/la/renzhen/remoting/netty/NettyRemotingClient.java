package la.renzhen.remoting.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import la.renzhen.remoting.InvokeCallback;
import la.renzhen.remoting.RemotingChannel;
import la.renzhen.remoting.RemotingClient;
import la.renzhen.remoting.RemotingException;
import la.renzhen.remoting.code.RemotingAbstract;
import la.renzhen.remoting.protocol.RemotingCommand;

import java.util.concurrent.ExecutorService;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-27 20:17
 */
public class NettyRemotingClient extends RemotingAbstract<Channel> implements RemotingClient<Channel> {

    private String clientName;
    private final Bootstrap bootstrap = new Bootstrap();

    /**
     * Invoke the callback methods in this executor when process response.
     */
    private ExecutorService callbackExecutor;
    private DefaultEventExecutorGroup defaultEventExecutorGroup;


    public NettyRemotingClient(final String clientName, final NettyClientConfig nettyClientConfig) {
        super(nettyClientConfig);

        this.clientName = clientName;
    }

    @Override
    public RemotingChannel<Channel> getChannel() {
        return null;
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
    protected void startupTCPListener() {

    }

    @Override
    protected void shutdownTCPListener(boolean interrupted) {

    }

    @Override
    public String getUnique() {
        return this.clientName;
    }

    @Override
    public String getStartBanner() {
        return super.getStartBanner() + " ... Netty Client ...";
    }
}
