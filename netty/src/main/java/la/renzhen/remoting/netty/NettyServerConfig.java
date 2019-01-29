package la.renzhen.remoting.netty;

import la.renzhen.remoting.RemotingConfig;
import la.renzhen.remoting.netty.tls.TlsMode;
import lombok.Data;

@Data
public class NettyServerConfig extends RemotingConfig implements Cloneable {

    private int callbackExecutorThreads = 0;

    private int bossThreads = 1;
    private int workerThreads = 8;
    private int selectorThreads = 3;//TODO why ??? is 3 ?


    private boolean pooledByteBufAllocatorEnable = false;

    private boolean useEPollNativeSelector = true;

    private TlsMode tlsMode = TlsMode.PERMISSIVE;

    @Override
    public Object clone() throws CloneNotSupportedException {
        return (NettyServerConfig) super.clone();
    }
}
