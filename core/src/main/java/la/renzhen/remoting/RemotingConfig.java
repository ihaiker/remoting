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
    private int channelMaxIdleTimeSeconds = 120;

    /**
     * Server listening port, client link port
     */
    protected int port = 8888;

    /**
     * Server binding host, client link host
     */
    protected String host = "127.0.0.1";
}
