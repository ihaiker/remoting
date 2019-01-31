package la.renzhen.remoting.netty.security;

import la.renzhen.remoting.netty.security.jks.JKSConfig;
import la.renzhen.remoting.netty.security.jks.JKSKeyStoresSecurityProvider;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-29 17:46
 */
public class InternalSecurityProvider extends JKSKeyStoresSecurityProvider {
    private static final String KEY_PASSWORD = "remoting";

    private static final String SERVER_KEYSTORE = "/certs/jks/serverStore.jks";
    private static final String SERVER_TRUSTSTORE = "/certs/jks/serverStore.jks";
    private static final String CLIENT_KEYSTORE = "/certs/jks/clientStore.jks";
    private static final String CLIENT_TRUSTSTORE = "/certs/jks/clientStore.jks";

    private InternalSecurityProvider(JKSConfig jksConfig) {
        super(jksConfig);
    }

}
