package la.renzhen.remoting;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-31 19:54
 */
public interface RemotingAuth<Channel> {

    String encode(RemotingChannel<Channel> channel, String authUsername, String authPassword);

}
