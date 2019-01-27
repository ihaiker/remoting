package la.renzhen.remoting;

import java.util.concurrent.ExecutorService;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-25 19:31
 */
public interface Remoting<Channel> {

    void registerProcessor(final int requestCode, final RequestProcessor<Channel> processor, final ExecutorService executor);

    void registerDefaultProcessor(final RequestProcessor<Channel> processor, final ExecutorService executor);

    void registerChannelEventListener(ChannelEventListener<Channel> channelEventListener);

}