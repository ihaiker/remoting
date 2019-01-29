package la.renzhen.remoting.netty.security;

import io.netty.channel.ChannelHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import la.renzhen.remoting.RemotingException;

import javax.net.ssl.SSLEngine;
import java.io.File;
import java.io.InputStream;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-29 16:40
 */
public abstract class AbstractSecurityProvider implements SecurityProvider {

    public ChannelHandler initChannel(SocketChannel ch) {
        SSLEngine sslEngine = null;
        if (isServer()) {
            sslEngine = serverSslContext().newEngine(ch.alloc());
            sslEngine.setUseClientMode(false);//服务器模式
            sslEngine.setUseClientMode(authRemoting());//需要客户端验证
        } else {
            sslEngine = clientSslContext().newEngine(ch.alloc());
            sslEngine.setUseClientMode(true);//客户方模式
        }
        return new SslHandler(sslEngine, true);
    }

    protected abstract InputStream getKeyCert();

    protected abstract String getKeyPassword();

    protected abstract InputStream getKey();

    protected abstract InputStream getTrustCert();

    /**
     * @return whether you need to verify the remote endpoint's certificate. default is true
     *
     * @see SslContextBuilder#trustManager(File)
     */
    public boolean authRemoting() {
        return true;
    }

    protected abstract boolean isServer();

    public void preCheck() throws Exception {
        try (InputStream ignored = getKeyCert()) {
        }
        try (InputStream ignored = getKey()) {
        }
        if (authRemoting()) {
            try (InputStream ignored = getTrustCert()) {
            }
        }
    }

    protected SslContext clientSslContext() {
        //TODO 静态化初始化一次
        SslContextBuilder sslContextBuilder = SslContextBuilder.forClient().sslProvider(SslProvider.JDK);

        InputStream keyCertChain = getKeyCert();
        InputStream key = getKey();
        String keyPassword = getKeyPassword();

        InputStream trustCert = getTrustCert();
        if (trustCert == null) {
            sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);
        } else {
            sslContextBuilder.trustManager(trustCert);
        }

        try {
            return sslContextBuilder.keyManager(keyCertChain, key, keyPassword).build();
        } catch (Exception e) {
            throw new RemotingException(RemotingException.Type.Connect, e);
        }
    }

    protected SslContext serverSslContext() {
        //TODO 静态化初始一次
        InputStream keyCertChain = getKeyCert();
        InputStream key = getKey();
        String keyPassword = getKeyPassword();

        SslContextBuilder sslContextBuilder = SslContextBuilder.forServer(keyCertChain, key, keyPassword).sslProvider(sslProvider());

        InputStream trustCert = getTrustCert();
        if (trustCert == null) {
            sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);
        } else {
            sslContextBuilder.trustManager(trustCert);
        }
        sslContextBuilder.clientAuth(clientAuthMode());
        try {
            return sslContextBuilder.build();
        } catch (Exception e) {
            throw new RemotingException(RemotingException.Type.Connect, e);
        }
    }

    protected SslProvider sslProvider() {
        return OpenSsl.isAvailable() ? SslProvider.OPENSSL : SslProvider.JDK;
    }

    protected ClientAuth clientAuthMode() {
        return ClientAuth.REQUIRE;
    }
}