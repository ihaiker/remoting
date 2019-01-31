package la.renzhen.remoting.netty.security;

import io.netty.channel.ChannelHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslHandler;
import lombok.Getter;

import javax.net.ssl.SSLEngine;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-29 16:40
 */
public abstract class AbstractSecurityProvider implements SecurityProvider {

    //@formatter:off
    @Getter private final boolean server;
    @Getter private final boolean auth;
    @Getter private final boolean startTls;
    //@formatter:on

    public AbstractSecurityProvider(final boolean server, final boolean auth, final boolean startTls) {
        this.server = server;
        this.auth = auth;
        this.startTls = startTls;
    }

    public ChannelHandler initChannel(SocketChannel ch) {
        SSLEngine sslEngine = newEngine(ch);
        if (isServer()) {
            sslEngine.setUseClientMode(false);//服务器模式
            if (auth) {
                sslEngine.setNeedClientAuth(auth);//需要验证客户端
            }
        } else {
            sslEngine.setUseClientMode(true);//客户方模式
        }
        return new SslHandler(sslEngine, startTls);
    }

    protected abstract SSLEngine newEngine(SocketChannel ch);

    protected InputStream loadStream(String store /*keystore,truststore*/, String path) throws IOException {
        return new FileInputStream(path);
    }
}