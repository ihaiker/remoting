package la.renzhen.remoting.commons;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-27 15:51
 */
public class NamedThreadFactory implements ThreadFactory {

    final String name;
    final int threadTotal;
    final private AtomicInteger threadIndex = new AtomicInteger(0);

    public NamedThreadFactory(String name) {
        this(name, 0);
    }

    public NamedThreadFactory(String name, int threadTotal) {
        this.name = name;
        this.threadTotal = threadTotal;
    }

    @Override
    public Thread newThread(Runnable r) {
        if (threadTotal != 0) {
            return new Thread(r, String.format("%s_%d_%d", name, this.threadIndex.incrementAndGet(), threadTotal));
        } else {
            return new Thread(r, String.format("%s_%d", name, this.threadIndex.incrementAndGet()));
        }
    }

}
