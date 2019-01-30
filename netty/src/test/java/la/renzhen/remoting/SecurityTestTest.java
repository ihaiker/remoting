package la.renzhen.remoting;

import la.renzhen.remoting.netty.NettyRemoting;
import la.renzhen.remoting.netty.NettyRemotingServer;
import la.renzhen.remoting.netty.security.test.TestTLSSecurityProvider;
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
        remoting.setSecurityProvider(new TestTLSSecurityProvider(server));
    }

    @Test
    public void testSecurity() throws Exception {
        RemotingCommand request = RemotingCommand.request(0).setStringHeaders("security");
        RemotingCommand response = client.invokeSync(request, TimeUnit.SECONDS.toMillis(3));
        assert "receiver security".equals(response.getStringHeaders());
    }
}
