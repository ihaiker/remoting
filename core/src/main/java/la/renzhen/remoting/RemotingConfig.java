package la.renzhen.remoting;

import lombok.Data;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-28 11:17
 */
@Data
public class RemotingConfig {

    protected int channelEventMaxSize = 10000;

    protected int onewayRequestLimits = 256;

    protected int asyncRequestLimits = 64;

    protected int publicExecutorThreadSize = 4;


    protected int socketSndBufSize = 65535;
    protected int socketRcvBufSize = 65535;
    protected int maxFrameLength = 16777216;
    protected int channelMaxIdleTimeSeconds = 120;

    protected int readerIdleTimeSeconds = 0;
    protected int writerIdleTimeSeconds = 0;
    protected int allIdleTimeSeconds = 120;

    /**
     * the period for search timeout request timeout.
     */
    protected int scanResponseTimerPeriod = 300;
}
