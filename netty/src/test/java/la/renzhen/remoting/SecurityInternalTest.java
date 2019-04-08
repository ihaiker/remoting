package la.renzhen.remoting;

import la.renzhen.remoting.netty.NettyRemoting;
import la.renzhen.remoting.netty.NettyRemotingServer;
import la.renzhen.remoting.netty.security.jks.InternalSecurityProvider;
import la.renzhen.remoting.protocol.RemotingCommand;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-28 16:07
 */
public class SecurityInternalTest extends RemotingNettyAbstract {
    @Override
    public void registerTestProcessor(NettyRemoting remoting) {
        super.registerTestProcessor(remoting);
        boolean server = remoting instanceof NettyRemotingServer;
        remoting.setSecurityProvider(InternalSecurityProvider.create(server, false));
    }

    @Test
    public void testSecurity() throws Exception {
        RemotingCommand request = RemotingCommand.request(0).setStringHeaders("security");
        RemotingCommand response = client.invokeSync(request, TimeUnit.SECONDS.toMillis(3));
        assert "receiver security".equals(response.getStringHeaders());
    }
}
