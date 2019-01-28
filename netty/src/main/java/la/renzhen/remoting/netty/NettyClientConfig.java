package la.renzhen.remoting.netty;

import la.renzhen.remoting.RemotingConfig;
import lombok.Data;

@Data
public class NettyClientConfig extends RemotingConfig {

    private int workerThreads = 4;

    private int callbackThreadSize = 0;

    private int connectTimeoutMillis = 3000;
    private long channelNotActiveInterval = 1000 * 60;

    /**
     * IdleStateEvent will be triggered when neither read nor write was performed for
     * the specified period of this time. Specify {@code 0} to disable
     */
    private int channelMaxIdleTimeSeconds = 120;

    private boolean closeSocketIfTimeout = false;
}