package la.renzhen.remoting.netty.security;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-29 17:46
 */
public class InternalSecurityProvider   {
    private static final String SERVER_KEY_PASSWORD = "nettyDemo";
    private static final String CLIENT_KEY_PASSWORD = "nettyDemo";

    private static final String SERVER_KEYSTORE = "/certs/jks/serverStore.jks";
    private static final String SERVER_TRUSTSTORE = "/certs/jks/serverStore.jks";
    private static final String CLIENT_KEYSTORE = "/certs/jks/clientStore.jks";
    private static final String CLIENT_TRUSTSTORE = "/certs/jks/clientStore.jks";
}
