package la.renzhen.remoting.core;

import la.renzhen.remoting.RemotingChannel;
import la.renzhen.remoting.RemotingDefender;

import java.util.List;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-31 21:45
 */
public class MultipleRemotingDefender<Channel> implements RemotingDefender<Channel> {

    private final List<RemotingDefender<Channel>> defenders;

    public MultipleRemotingDefender(List<RemotingDefender<Channel>> defenders) {
        this.defenders = defenders;
    }

    @Override
    public boolean checked(RemotingChannel<Channel> channel) {
        for (RemotingDefender<Channel> defender : defenders) {
            if (!defender.checked(channel)) {
                return false;
            }
        }
        return true;
    }

}
