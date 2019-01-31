package la.renzhen.remoting;

import la.renzhen.remoting.protocol.RemotingCommand;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-31 16:30
 */
public interface RemotingMultipleClient<Channel> extends RemotingClient<Channel> {

    RemotingChannel<Channel> selectChannel(final String address);

    RemotingCommand invokeSync(final String address, final RemotingCommand request, final long timeoutMillis)
            throws InterruptedException, RemotingException;

    void invokeAsync(final String address, final RemotingCommand request, final long timeoutMillis, final InvokeCallback invokeCallback)
            throws InterruptedException, RemotingException;

    void invokeOneway(final String address, final RemotingCommand request, final long timeoutMillis)
            throws InterruptedException, RemotingException;
}
