package la.renzhen.remoting;

import la.renzhen.remoting.commons.Pair;
import la.renzhen.remoting.protocol.RemotingCommand;

import java.util.concurrent.ExecutorService;

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

    RemotingChannel<Channel> getChannel();
}