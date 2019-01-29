package la.renzhen.remoting;

import la.renzhen.remoting.protocol.CommandCustomHeader;
import la.renzhen.remoting.protocol.RemotingCommand;
import lombok.Data;
import lombok.SneakyThrows;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-28 16:07
 */
public class NettyTest extends RemotingNettyTest {


    @Test
    public void testSyncHeader() throws Exception {
        RemotingCommand request = RemotingCommand.request(0).setStringHeaders("header - test");
        RemotingCommand response = client.invokeSync(request, TimeUnit.SECONDS.toMillis(3));
        assert "receiver header - test".equals(response.getStringHeaders());

        request = RemotingCommand.request(0).setStringHeaders("test1");
        response = client.invokeSync(request, TimeUnit.SECONDS.toMillis(3));
        String header = response.getStringHeaders();
        assert "receiver test1".equals(header);
    }

    @Test
    public void testSyncBody() throws Exception {
        RemotingCommand request = RemotingCommand.request(0).setBody("body - test".getBytes());
        RemotingCommand response = client.invokeSync(request, TimeUnit.SECONDS.toMillis(3));
        assert "receiver body body - test".equals(response.getStringHeaders());
    }

    @Data
    public static class TestDemoHeader implements CommandCustomHeader {
        String string1;
        int int1;
        List<String> list;
    }

    @Test
    public void testSyncCustomerHeader() throws Exception {
        TestDemoHeader testDemoHeader = new TestDemoHeader();
        testDemoHeader.setString1("test");

        RemotingCommand request = RemotingCommand.request(10)
                .setCustomHeaders(testDemoHeader);
        RemotingCommand response = client.invokeSync(request, TimeUnit.SECONDS.toMillis(3));

        assert response.getCustomHeaders(TestDemoHeader.class).getString1().equals(testDemoHeader.getString1());
    }

    @Test
    public void testSecurity() throws Exception {
        
        RemotingCommand request = RemotingCommand.request(0).setStringHeaders("header - test");
        RemotingCommand response = client.invokeSync(request, TimeUnit.SECONDS.toMillis(3));
        assert "receiver header - test".equals(response.getStringHeaders());
    }
}
