package la.renzhen.remoting;

import la.renzhen.remoting.netty.NettyRemoting;
import la.renzhen.remoting.netty.NettyRemotingServer;
import la.renzhen.remoting.netty.security.jks.JKSConfig;
import la.renzhen.remoting.netty.security.jks.JKSKeyStoresSecurityProvider;
import la.renzhen.remoting.protocol.RemotingCommand;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-28 16:07
 */
public class SecurityTestTest extends RemotingNettyTest {

    @Override
    public void registerTestProcessor(NettyRemoting remoting) {
        super.registerTestProcessor(remoting);

        boolean server = remoting instanceof NettyRemotingServer;
        remoting.setModule(server ? "Server" : "Client");
        //remoting.setSecurityProvider(new InternalSecurityProvider(server));
        //remoting.setSecurityProvider(new JKSTestTLSSecurityProvider(server));

        String ROOT = "/Users/haiker/Documents/project/myself/JavaWork/remoting/netty/src/main/resources";

        JKSConfig config;
        String password = "nettyDemo";

        //oneway
        if (server) {
            String path = ROOT + "/certs/oneway/serverStore.jks";
            config = JKSConfig.onewayAuthServer(path, password);
        } else {
            String path = ROOT + "/certs/oneway/clientStore.jks";
            config = JKSConfig.onewayAuthClient(path, password);
        }

        //twoway
        /*if (server) {
            String path = ROOT + "/certs/test/serverStore.jks";
            config = JKSConfig.twowayAuthServer(path, path, password);
        } else {
            String path = ROOT + "/certs/test/clientStore.jks";
            config = JKSConfig.twowayAuthClient(path, path, password);
        }*/

        //config.fromResource(true);
        remoting.setSecurityProvider(new JKSKeyStoresSecurityProvider(config));
    }

    @Test
    public void testSecurity() throws Exception {
        RemotingCommand request = RemotingCommand.request(0).setStringHeaders("security");
        RemotingCommand response = client.invokeSync(request, TimeUnit.SECONDS.toMillis(3));
        assert "receiver security".equals(response.getStringHeaders());
    }
}
