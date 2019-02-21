package la.renzhen.remoting;

import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-25 19:31
 */
public interface Remoting<Channel> {

    String getUnique();

    Map<String,String> getAttributes();

    String getModule();

    default void registerProcessor(final int requestCode, final RequestProcessor<Channel> processor){
        registerProcessor(requestCode,processor,null);
    }

    void registerProcessor(final int requestCode, final RequestProcessor<Channel> processor, final ExecutorService executor);

    default void registerDefaultProcessor(final RequestProcessor<Channel> processor){
        registerDefaultProcessor(processor,null);
    }

    void registerDefaultProcessor(final RequestProcessor<Channel> processor, final ExecutorService executor);

    void registerChannelEventListener(ChannelEventListener<Channel> channelEventListener);

}