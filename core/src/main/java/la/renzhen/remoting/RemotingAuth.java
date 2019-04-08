package la.renzhen.remoting;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-31 19:54
 */
public interface RemotingAuth {
    /**
     * When using authentication, the authenticated username is stored in the key of the attribute.
     */
    String AUTH_USERNAME = "auth.username";

    /**
     * When using authentication, the authenticated password is stored in the key of the attribute.
     */
    String AUTH_PASSWORD = "auth.password";

    String AUTH_SIGNATURE = "auth.signature";

    /**
     * Generate an certification signature based on the authentication username and password.
     * @param authUsername the authentication username
     * @param authPassword the authentication password
     * @return certification signature
     */
    String signature(String authUsername, String authPassword);

}
