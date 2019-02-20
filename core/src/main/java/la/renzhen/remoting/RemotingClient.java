package la.renzhen.remoting;

import la.renzhen.remoting.protocol.RemotingCommand;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-25 14:53
 */
public interface RemotingClient<Channel> extends Remoting<Channel>, RemotingService {

    RemotingCommand invokeSync(final RemotingCommand request, final long timeoutMillis)
            throws InterruptedException, RemotingException;

    void invokeAsync(final RemotingCommand request, final long timeoutMillis, final InvokeCallback invokeCallback)
            throws InterruptedException, RemotingException;

    void invokeOneway(final RemotingCommand request, final long timeoutMillis)
            throws InterruptedException, RemotingException;

    RemotingCommand invokeSync(final String address, final RemotingCommand request, final long timeoutMillis)
            throws InterruptedException, RemotingException;

    void invokeAsync(final String address, final RemotingCommand request, final long timeoutMillis, final InvokeCallback invokeCallback)
            throws InterruptedException, RemotingException;

    void invokeOneway(final String address, final RemotingCommand request, final long timeoutMillis)
            throws InterruptedException, RemotingException;


    RemotingChannel<Channel> selectOrCreateChannel(final String address) throws InterruptedException;

    RemotingChannel<Channel> getChannel() throws InterruptedException;

    void registerAuth(final RemotingAuth auth,final String username,final String password);

    boolean reconnect();
}