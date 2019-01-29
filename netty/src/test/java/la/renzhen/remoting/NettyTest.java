package la.renzhen.remoting;

import la.renzhen.remoting.protocol.RemotingCommand;
import lombok.SneakyThrows;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-28 16:07
 */
public class NettyTest extends RemotingNettyTest {

    @Test
    @SneakyThrows
    public void testServer() {
        //Thread.currentThread().join(TimeUnit.MINUTES.toMillis(1));
    }

    @Test
    public void testSync() throws Exception {
        RemotingCommand request = RemotingCommand.request(0).setStringHeaders("header - test");
        RemotingCommand response = client.invokeSync(request, TimeUnit.SECONDS.toMillis(3));
        assert "receiver header - test ".equals(response.getStringHeaders());

        request.setStringHeaders("test1");
        response = client.invokeSync(request, TimeUnit.SECONDS.toMillis(3));
        String header = response.getStringHeaders();
        assert "receiver test1".equals(header);
    }
}
