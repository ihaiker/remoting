package la.renzhen.remoting.netty.security.jks;

import io.netty.channel.socket.SocketChannel;
import la.renzhen.remoting.commons.Pair;
import la.renzhen.remoting.netty.security.AbstractSecurityProvider;
import la.renzhen.remoting.netty.security.StormFrom;
import lombok.Getter;
import lombok.Setter;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Base64;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-30 14:15
 */
public class JKSKeyStoresSecurityProvider extends AbstractSecurityProvider {

    private static final String PROTOCOL = "TLS";

    //@formatter:off
    @Getter @Setter private SSLContext sslContext = null;
    @Getter private final JKSConfig jksConfig;
    //@formatter:on

    public JKSKeyStoresSecurityProvider(final JKSConfig jksConfig) {
        super(jksConfig.server(), jksConfig.twoway(), jksConfig.startTls());
        this.jksConfig = jksConfig;
    }

    @Override
    public void preCheck() throws Exception {
        sslContext = createSSlContext();
    }

    protected SSLContext createSSlContext() throws Exception {
        SSLContext serverContext = SSLContext.getInstance(PROTOCOL);
        Pair<KeyManagerFactory, TrustManagerFactory> pair = load(jksConfig.keystore(), jksConfig.truststore(), jksConfig.password());
        KeyManagerFactory kmf = pair.getFirst();
        TrustManagerFactory tmf = pair.getSecond();
        serverContext.init(kmf == null ? null : kmf.getKeyManagers(),
                tmf == null ? null : tmf.getTrustManagers(), null);
        return serverContext;
    }

    private Pair<KeyManagerFactory, TrustManagerFactory> load(final String keystore, final String truststore, final String password) throws Exception {
        //keystore
        KeyManagerFactory kmf = null;
        if (keystore != null) {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(loadStream("keystore", keystore), password.toCharArray());
            //Set up key manager factory to use our key store
            kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, password.toCharArray());
        }

        // truststore
        TrustManagerFactory tmf = null;
        if (!isServer() || isAuth()) {
            KeyStore ts = KeyStore.getInstance("JKS");
            ts.load(loadStream("truststore", truststore), password.toCharArray());
            // set up trust manager factory to use our trust store
            tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ts);
        }
        return new Pair<>(kmf, tmf);
    }

    @Override
    protected SSLEngine newEngine(SocketChannel ch) {
        return sslContext.createSSLEngine();
    }

    @Override
    protected StormFrom getStormFrom() {
        return jksConfig.stormFrom();
    }
}