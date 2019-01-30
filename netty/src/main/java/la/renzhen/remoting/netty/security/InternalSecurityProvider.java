package la.renzhen.remoting.netty.security;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-29 17:46
 */
public abstract class InternalSecurityProvider extends AbstractSecurityProvider {

    public InternalSecurityProvider(boolean isServer) {
        super(isServer, true);
    }
}
