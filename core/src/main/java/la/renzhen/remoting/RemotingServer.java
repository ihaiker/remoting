package la.renzhen.remoting;

import la.renzhen.remoting.commons.Pair;
import la.renzhen.remoting.protocol.RemotingCommand;

import java.util.concurrent.ExecutorService;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-25 14:43
 */
public interface RemotingServer<Channel> extends Remoting<Channel>, RemotingService {

    RemotingChannel<Channel> getChannel(String address);

    RemotingCommand invokeSync(final String address, final RemotingCommand request, final long timeoutMillis)
            throws InterruptedException, RemotingException;

    void invokeAsync(final String address, final RemotingCommand request, final long timeoutMillis, final InvokeCallback invokeCallback)
            throws InterruptedException, RemotingException;

    void invokeOneway(final String address, final RemotingCommand request, final long timeoutMillis)
            throws InterruptedException, RemotingException;


    Pair<RequestProcessor<Channel>, ExecutorService> getProcessor(final int requestCode);

    void setDefender(RemotingDefender<Channel> protector);
}
