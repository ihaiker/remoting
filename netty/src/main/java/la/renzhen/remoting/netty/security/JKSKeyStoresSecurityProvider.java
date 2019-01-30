package la.renzhen.remoting.netty.security;

import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.internal.StringUtil;
import la.renzhen.remoting.commons.Pair;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.validation.constraints.NotNull;
import java.security.KeyStore;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-30 14:15
 */
public class JKSKeyStoresSecurityProvider extends AbstractSecurityProvider {

    //@formatter:off
    private SslContext serverContext = null;
    private SslContext clientContext = null;
    private final String keystore;
    private final String truststore;
    private final String password;
    //@formatter:on

    private static final String PROTOCOL = "TLS";

    public JKSKeyStoresSecurityProvider(final boolean server, @NotNull final String keystore, final String truststore, final String keyStorePassword) {
        super(server, StringUtil.length(truststore) != 0);
        this.keystore = keystore;
        this.truststore = truststore;
        this.password = keyStorePassword;
    }

    @Override
    protected SslContext getServerSSlContext(SocketChannel ch) {
        return serverContext;
    }

    @Override
    protected SslContext getClientSSlContext(SocketChannel ch) {
        return clientContext;
    }

    @Override
    public void preCheck() throws Exception {
        serverContext = createServerSSlContext();
        clientContext = createClientSSlContext();
    }

    protected SslContext createServerSSlContext() throws Exception {
        Pair<KeyManagerFactory, TrustManagerFactory> pair = load(keystore, truststore, password);

        ClientAuth clientAuth = ClientAuth.NONE;
        SslContextBuilder builder = SslContextBuilder.forServer(pair.getFirst());
        if (isAuth()) {
            builder.trustManager(pair.getSecond());
            clientAuth = ClientAuth.REQUIRE;
        } else {
            builder.trustManager(InsecureTrustManagerFactory.INSTANCE);
        }

        return builder
                .sslProvider(SslProvider.JDK)
                .clientAuth(clientAuth)
                .protocols(PROTOCOL)
                .build();
    }

    protected SslContext createClientSSlContext() throws Exception {
        Pair<KeyManagerFactory, TrustManagerFactory> pair = load(keystore, truststore, password);
        SslContextBuilder builder = SslContextBuilder.forClient().keyManager(pair.getFirst());
        if (isAuth()) {
            builder.trustManager(pair.getSecond());
        } else {
            builder.trustManager(InsecureTrustManagerFactory.INSTANCE);
        }
        return builder.sslProvider(SslProvider.JDK)
                .protocols(PROTOCOL)
                .build();
    }

    private Pair<KeyManagerFactory, TrustManagerFactory> load(final String keystore, final String truststore, final String password) throws Exception {
        //keystore
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(loadStream(keystore), password.toCharArray());
        //Set up key manager factory to use our key store
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, password.toCharArray());

        TrustManagerFactory tmf = null;
        if (isAuth()) {
            // truststore
            KeyStore ts = KeyStore.getInstance("JKS");
            ts.load(loadStream(truststore), password.toCharArray());
            // set up trust manager factory to use our trust store
            tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ts);
        }
        return new Pair<>(kmf, tmf);
    }

}