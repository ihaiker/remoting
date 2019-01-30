package la.renzhen.remoting.netty.security;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-29 17:46
 */
public class InternalSecurityProvider extends JKSKeyStoresSecurityProvider {
    private static final String SERVER_KEY_PASSWORD = "nettyDemo";
    private static final String CLIENT_KEY_PASSWORD = "nettyDemo";

    private static final String SERVER_KEYSTORE = "/certs/jks/serverStore.jks";
    private static final String SERVER_TRUSTSTORE = "/certs/jks/serverStore.jks";
    private static final String CLIENT_KEYSTORE = "/certs/jks/clientStore.jks";
    private static final String CLIENT_TRUSTSTORE = "/certs/jks/clientStore.jks";

    public InternalSecurityProvider(boolean server) {
        super(server, server ? SERVER_KEYSTORE : CLIENT_KEYSTORE,
                server ? SERVER_TRUSTSTORE : CLIENT_TRUSTSTORE, server ? SERVER_KEY_PASSWORD : CLIENT_KEY_PASSWORD);
    }

    @Override
    protected InputStream loadStream(String path) throws IOException {
        return InternalSecurityProvider.class.getClassLoader().getResourceAsStream(path);
    }
}
