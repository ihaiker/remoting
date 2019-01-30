package la.renzhen.remoting.netty.security;


import javax.validation.constraints.NotNull;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-29 17:51
 */
public abstract class FileSecurityProvider extends AbstractSecurityProvider {

    private String keyCert;
    private String key;
    private String trustCert;
    private String keyPassword;

    public FileSecurityProvider(boolean isServer, @NotNull String keyCert, @NotNull String key, String trustCert, String keyPassword) {
        super(isServer,true);
        this.keyCert = keyCert;
        this.key = key;
        this.trustCert = trustCert;
        this.keyPassword = keyPassword;
    }

}
