package la.renzhen.remoting;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-31 16:37
 */
public interface Protector<Channel> {

    boolean checked(RemotingChannel<Channel> channel);

}
