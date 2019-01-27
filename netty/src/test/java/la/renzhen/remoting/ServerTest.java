package la.renzhen.remoting;

import io.netty.channel.Channel;
import la.renzhen.remoting.netty.NettyRemotingServer;
import la.renzhen.remoting.netty.NettyServerConfig;
import org.junit.Test;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-27 19:16
 */
public class ServerTest {

    @Test
    public void testService(){
        NettyServerConfig config = new NettyServerConfig();
        RemotingServer<Channel> server = new NettyRemotingServer("test",config);
        server.startup();

        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                server.shutdown();
            }
        });

    }
}
