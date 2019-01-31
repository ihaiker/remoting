package la.renzhen.remoting.netty.security.jks;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-31 11:02
 */
@Setter(AccessLevel.PACKAGE)
@Getter(AccessLevel.PACKAGE)
@Accessors(chain = true, fluent = true)
public class JKSConfig {

    public enum StormFrom {
        BASE64, FILE, RESOURCE
    }

    /**
     * Is it a server configuration?
     */
    private boolean server;

    /**
     * Is it a two-way authentication?
     */
    private boolean twoway;

    @Setter(AccessLevel.PUBLIC) @Getter(AccessLevel.PUBLIC)
    private boolean startTls = false;

    private String keystore;

    private String truststore;

    private String password;

    /**
     * Whether the file is placed in the resource file, if return true,
     * it placed in the resource file, unless placed in the hard disk directory, defalut is false.
     */
    @Setter(AccessLevel.PUBLIC) @Getter(AccessLevel.PUBLIC)
    private StormFrom stormFrom = StormFrom.FILE;


    private JKSConfig() {
    }

    /**
     * One-way authentication mode server configuration
     *
     * @param keystore keystore file path
     * @param password the keystore password
     *
     * @return Server configuration
     */
    public static JKSConfig onewayAuthServer(String keystore, String password) {
        return new JKSConfig().server(true).twoway(false).keystore(keystore).password(password);
    }

    /**
     * One-way authentication mode client configuration
     *
     * @param truststore truststore file path
     * @param password   the truststore file password
     *
     * @return client configuration
     */
    public static JKSConfig onewayAuthClient(String truststore, String password) {
        return new JKSConfig().server(false).twoway(false).truststore(truststore).password(password);
    }


    /**
     * Two-way authentication mode server configuration
     *
     * @param keystore Keystore file path
     * @param password the keystore password
     *
     * @return configuration
     */
    public static JKSConfig twowayAuthServer(String keystore, String truststore, String password) {
        return new JKSConfig().server(true).twoway(true).keystore(keystore).truststore(truststore).password(password);
    }

    /**
     * Two-way authentication mode client configuration
     *
     * @param keystore   the keystore file path
     * @param truststore the truststore file path
     * @param password   the truststore password
     *
     * @return configuration
     */
    public static JKSConfig twowayAuthClient(String keystore, String truststore, String password) {
        return new JKSConfig().server(false).twoway(true).keystore(keystore).truststore(truststore).password(password);
    }
}
