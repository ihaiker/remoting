package la.renzhen.remoting.netty;

import la.renzhen.remoting.RemotingConfig;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter
public class NettyClientConfig extends RemotingConfig implements Cloneable {

    private int workerThreads = 4;

    private int callbackThreadSize = 0;

    private int connectTimeoutMillis = 3000;
    private long channelNotActiveInterval = 1000 * 60;

    private boolean closeSocketIfTimeout = false;

    private String authUsername;
    private String authPassword;

    @Override
    public Object clone() throws CloneNotSupportedException {
        return (NettyClientConfig) super.clone();
    }
}