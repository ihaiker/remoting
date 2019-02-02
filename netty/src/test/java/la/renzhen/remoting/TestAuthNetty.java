package la.renzhen.remoting;

import io.netty.channel.Channel;
import la.renzhen.remoting.core.auth.MD5Auth;
import la.renzhen.remoting.core.auth.MD5AuthDefender;
import org.junit.Test;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-02-01 21:51
 */
public class TestAuthNetty extends RemotingNettyTest {

    RemotingAuth auth = new MD5Auth();
    RemotingDefender<Channel> defender = new MD5AuthDefender<>(auth);

    String username = "haiker";
    String password = "84AB47DF3A99DF702C608C78E693092D";

    @Override
    public RemotingClient createRemotingClient() {
        RemotingClient client = super.createRemotingClient();
        client.registerAuth(auth, username, password);
        return client;
    }

    @Override
    public RemotingServer createRemotingServer() throws InterruptedException {
        RemotingServer remotingServer = super.createRemotingServer();
        remotingServer.registerDefender(new MD5AuthDefender(auth));
        return remotingServer;
    }

    @Test
    public void testAuth(){

    }
}
