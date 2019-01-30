package la.renzhen.remoting.netty.security;

import io.netty.channel.ChannelHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
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

    @Getter
    private final boolean server;
    @Getter
    private final boolean auth;

    public AbstractSecurityProvider(boolean server, boolean auth) {
        this.server = server;
        this.auth = auth;
    }

    public ChannelHandler initChannel(SocketChannel ch) {
        SslContext sslContext = (isServer() ? getServerSSlContext(ch) : getClientSSlContext(ch));
        SSLEngine sslEngine = sslContext.newEngine(ch.alloc());
        if (isServer()) {
            sslEngine.setUseClientMode(false);//服务器模式
            if (auth) {
                sslEngine.setNeedClientAuth(true);//需要客户端验证
            }
        } else {
            sslEngine.setUseClientMode(true);//客户方模式
        }
        return new SslHandler(sslEngine, true);
    }

    protected abstract SslContext getServerSSlContext(SocketChannel ch);

    protected abstract SslContext getClientSSlContext(SocketChannel ch);

    protected InputStream loadStream(String path) throws IOException {
        return new FileInputStream(path);
    }
}