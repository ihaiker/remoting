package la.renzhen.remoting.netty.security.jks;

import la.renzhen.remoting.netty.security.StormFrom;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-29 17:46
 */
public class InternalSecurityProvider extends JKSKeyStoresSecurityProvider {
    private static final String KEY_PASSWORD = "remoting";

    private static final String SERVER_KEYSTORE = "/certs/jks/server.jks";
    private static final String SERVER_TRUSTSTORE = "/certs/jks/serverTrust.jks";
    private static final String CLIENT_KEYSTORE = "/certs/jks/client.jks";
    private static final String CLIENT_TRUSTSTORE = "/certs/jks/clientTrust.jks";

    private InternalSecurityProvider(JKSConfig jksConfig) {
        super(jksConfig);
    }


    public static InternalSecurityProvider create(boolean server, boolean twowayAuth) {
        JKSConfig config = new JKSConfig();
        config.server(server);
        config.twoway(twowayAuth);
        if (server) {
            config.keystore(SERVER_KEYSTORE);
            if (twowayAuth) {
                config.truststore(SERVER_TRUSTSTORE);
            }
        } else {
            config.truststore(CLIENT_TRUSTSTORE);
            if (twowayAuth) {
                config.keystore(CLIENT_KEYSTORE);
            }
        }
        config.password(KEY_PASSWORD);
        config.stormFrom(StormFrom.RESOURCE);
        return new InternalSecurityProvider(config);
    }
}
