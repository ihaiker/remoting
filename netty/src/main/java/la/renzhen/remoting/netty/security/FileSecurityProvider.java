package la.renzhen.remoting.netty.security;

import javax.validation.constraints.NotNull;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-29 17:51
 */
public class FileSecurityProvider extends AbstractSecurityProvider {

    private boolean isServer;
    private String keyCert;
    private String key;
    private String trustCert;
    private String keyPassword;

    public FileSecurityProvider(boolean isServer, @NotNull String keyCert, @NotNull String key, String trustCert, String keyPassword) {
        this.isServer = isServer;
        this.keyCert = keyCert;
        this.key = key;
        this.trustCert = trustCert;
        this.keyPassword = keyPassword;
    }

    public void preCheck() throws Exception {
        super.preCheck();
        if (trustCert != null) {
            try (FileInputStream ignored = new FileInputStream(trustCert)) {
            }
        }
    }

    @Override
    protected InputStream getKeyCert() {
        return safeGetInputStream(keyCert);
    }

    @Override
    protected String getKeyPassword() {
        return keyPassword;
    }

    @Override
    protected InputStream getKey() {
        return safeGetInputStream(key);
    }

    @Override
    protected InputStream getTrustCert() {
        return safeGetInputStream(trustCert);
    }

    protected InputStream safeGetInputStream(String path) {
        if (path == null) {
            return null;
        } else {
            try {
                return new FileInputStream(path);
            } catch (Exception e) {
                //Impossible, already used Pre-check.
            }
            return null;
        }
    }

    @Override
    protected boolean isServer() {
        return isServer;
    }
}
