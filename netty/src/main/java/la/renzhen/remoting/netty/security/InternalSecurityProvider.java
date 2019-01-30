package la.renzhen.remoting.netty.security;

import java.io.InputStream;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-29 17:46
 */
public class InternalSecurityProvider extends AbstractSecurityProvider {
    private boolean isServer;

    public InternalSecurityProvider(boolean isServer) {
        this.isServer = isServer;
    }

    @Override
    protected InputStream getKeyCert() {
        if (isServer()) {
            //return fromResource("/remoting/security/server.key");
            return fromResource("/remoting/security/server.crt");
        } else {
            return fromResource("/remoting/security/client.crt");
        }
    }

    @Override
    protected String getKeyPassword() {
        return "remoting";
    }

    @Override
    protected InputStream getKey() {
        if (isServer()) {
            return fromResource("/remoting/security/pkcs8_server.key");
        } else {
            return fromResource("/remoting/security/pkcs8_client.key");
        }
    }

    @Override
    protected InputStream getTrustCert() {
        if (isServer()) {
            return fromResource("/remoting/security/ca.crt");
        } else {
            return fromResource("/remoting/security/ca.crt");
        }
    }

    @Override
    protected boolean isServer() {
        return isServer;
    }

    public static InputStream fromResource(String path) {
        return InternalSecurityProvider.class.getResourceAsStream(path);
    }
}
