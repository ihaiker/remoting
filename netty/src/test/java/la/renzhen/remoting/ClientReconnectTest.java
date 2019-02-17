package la.renzhen.remoting;

import la.renzhen.remoting.protocol.RemotingCommand;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-02-17 15:31
 */
public class ClientReconnectTest extends RemotingNettyTest {

    @Test
    public void testReconnect() throws Exception {
        server.shutdown();

        RemotingCommand request = RemotingCommand.request(1).setBody("reconnect".getBytes());

        try {
            RemotingCommand response = client.invokeSync(request, TimeUnit.SECONDS.toMillis(3));
            assert !response.isSuccess();
        } catch (RemotingException e) {
            e.printStackTrace();
        }

        Thread.sleep(5000);

        server = createRemotingServer();
        server.startup();

        assert client.reconnect();

        RemotingCommand response = client.invokeSync(request, TimeUnit.SECONDS.toMillis(3));
        assert response.isSuccess();
        String body = new String(response.getBody());
        assert "receiver body reconnect".equals(body);
    }
}