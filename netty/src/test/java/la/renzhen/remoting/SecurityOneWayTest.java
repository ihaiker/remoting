package la.renzhen.remoting;

import la.renzhen.remoting.netty.NettyRemoting;
import la.renzhen.remoting.netty.NettyRemotingServer;
import la.renzhen.remoting.netty.security.StormFrom;
import la.renzhen.remoting.netty.security.jks.JKSConfig;
import la.renzhen.remoting.netty.security.jks.JKSKeyStoresSecurityProvider;
import la.renzhen.remoting.protocol.RemotingCommand;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-28 16:07
 */
public class SecurityOneWayTest extends RemotingNettyTest {
    String password = "remoting";
    StormFrom stormFrom = StormFrom.RESOURCE;

    @Override
    public void registerTestProcessor(NettyRemoting remoting) {
        super.registerTestProcessor(remoting);
        boolean server = remoting instanceof NettyRemotingServer;
        remoting.setModule(server ? "Server" : "Client");

        String ROOT = "./src/main/resources";
        if (stormFrom == StormFrom.RESOURCE) {
            ROOT = "";
        }

        JKSConfig config;
        if (server) {
            String path = ROOT + "/certs/jks/server.jks";
            config = JKSConfig.onewayAuthServer(path, password);
        } else {
            String path = ROOT + "/certs/jks/clientTrust.jks";
            config = JKSConfig.onewayAuthClient(path, password);
        }
        config.stormFrom(stormFrom);
        remoting.setSecurityProvider(new JKSKeyStoresSecurityProvider(config));
    }

    @Test
    public void testSecurity() throws Exception {
        RemotingCommand request = RemotingCommand.request(0).setStringHeaders("security");
        RemotingCommand response = client.invokeSync(request, TimeUnit.SECONDS.toMillis(3));
        assert "receiver security".equals(response.getStringHeaders());
    }
}
