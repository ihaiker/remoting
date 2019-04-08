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
public class SecurityTwoWayTest extends RemotingNettyAbstract {


    String password = "remoting";
    StormFrom stormFrom = StormFrom.RESOURCE;

    @Override
    public void registerTestProcessor(NettyRemoting remoting) {
        super.registerTestProcessor(remoting);
        boolean server = remoting instanceof NettyRemotingServer;

        String ROOT = "./src/main/resources";
        if (stormFrom == StormFrom.RESOURCE) {
            ROOT = "";
        }

        JKSConfig config;
        if (server) {
            String keystore = ROOT + "/certs/jks/server.jks";
            String truststore = ROOT + "/certs/jks/serverTrust.jks";
            config = JKSConfig.twowayAuthServer(keystore, truststore, password);
        } else {
            String keystore = ROOT + "/certs/jks/client.jks";
            String truststore = ROOT + "/certs/jks/clientTrust.jks";
            config = JKSConfig.twowayAuthClient(keystore, truststore, password);
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
