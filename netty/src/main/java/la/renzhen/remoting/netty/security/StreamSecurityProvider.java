package la.renzhen.remoting.netty.security;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-30 14:08
 */
public abstract class StreamSecurityProvider extends AbstractSecurityProvider {

    public StreamSecurityProvider(boolean server, boolean authTwoway) {
        super(server, authTwoway);
    }
}
