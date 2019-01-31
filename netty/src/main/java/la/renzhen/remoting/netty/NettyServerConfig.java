package la.renzhen.remoting.netty;

import la.renzhen.remoting.RemotingConfig;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class NettyServerConfig extends RemotingConfig implements Cloneable {

    private int callbackExecutorThreads = 0;

    private int bossThreads = 1;
    private int workerThreads = 8;
    private int selectorThreads = 3;//TOME why ??? is 3 ?


    private boolean pooledByteBufAllocatorEnable = false;

    private boolean useEPollNativeSelector = true;

    @Override
    public Object clone() throws CloneNotSupportedException {
        return (NettyServerConfig) super.clone();
    }
}