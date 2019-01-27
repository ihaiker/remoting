package la.renzhen.remoting.code;

import la.renzhen.remoting.ChannelEvent;
import la.renzhen.remoting.ChannelEventListener;
import la.renzhen.remoting.commons.ServiceThread;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ChannelEventExecutor<Channel> extends ServiceThread {

    private final LinkedBlockingQueue<ChannelEvent<Channel>> eventQueue = new LinkedBlockingQueue<>();
    private final int maxSize;

    @Setter @Getter private ChannelEventListener<Channel> channelEventListener;

    public ChannelEventExecutor(final int eventQueueMaxSize) {
        this.maxSize = eventQueueMaxSize;
    }

    public void putChannelEvent(final ChannelEvent<Channel> event) {
        if (this.eventQueue.size() <= maxSize) {
            this.eventQueue.add(event);
        } else {
            log.warn("event queue size[{}] enough, so drop this event {}", this.eventQueue.size(), event.toString());
        }
    }

    @Override
    public void run() {
        log.info(this.getServiceName() + " service started");
        final ChannelEventListener<Channel> listener = channelEventListener;
        while (!this.isStopped()) {
            try {
                ChannelEvent<Channel> event = this.eventQueue.poll(3000, TimeUnit.MILLISECONDS);
                if (event != null && listener != null) {
                    log.debug("{} received an event {} {}", this.getServiceName(), event.getChannel().address(), event.getType());
                    switch (event.getType()) {
                        case IDLE:
                            listener.onChannelIdle(event.getChannel());
                            break;
                        case CLOSE:
                            listener.onChannelClose(event.getChannel());
                            break;
                        case CONNECT:
                            listener.onChannelConnect(event.getChannel());
                            break;
                        case EXCEPTION:
                            listener.onChannelException(event.getChannel());
                            break;
                        default:
                            break;

                    }
                }
            } catch (Exception e) {
                log.warn(this.getServiceName() + " service has exception. ", e);
            }
        }
        log.info(this.getServiceName() + " service end");
    }

    @Override
    public String getServiceName() {
        return this.getClass().getSimpleName();
    }
}