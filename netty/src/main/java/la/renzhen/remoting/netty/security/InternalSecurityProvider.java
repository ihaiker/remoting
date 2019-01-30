package la.renzhen.remoting.netty.security;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-29 17:46
 */
public class InternalSecurityProvider extends JKSKeyStoresSecurityProvider {
    private static final String KEY_PASSWORD = "remoting";

    private static final String SERVER_KEYSTORE = "/certs/jks/serverKeys.jks";
    private static final String SERVER_TRUSTSTORE = "/certs/jks/serverTrust.jks";

    private static final String CLIENT_KEYSTORE = "/certs/jks/clientKeys.jks";
    private static final String CLIENT_TRUSTSTORE = "/certs/jks/clientTrust.jks";

    public InternalSecurityProvider(boolean server) {
        super(server, server ? SERVER_KEYSTORE : CLIENT_KEYSTORE,
                server ? null : CLIENT_TRUSTSTORE, KEY_PASSWORD);
    }

    @Override
    protected InputStream loadStream(String path) throws IOException {
        return InternalSecurityProvider.class.getClassLoader().getResourceAsStream(path);
    }
}
