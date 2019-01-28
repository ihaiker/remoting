package la.renzhen.remoting;

import la.renzhen.remoting.protocol.RemotingCoder;
import la.renzhen.remoting.protocol.RemotingCommand;
import org.junit.Test;

import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-28 16:50
 */
public class TestSerializable implements Serializable {

    @Test
    public void testSerializable() {
        RemotingCommand request = RemotingCommand.request(1024);
        request.setVersion(12);
        request.setStringHeaders("headers1");
        request.setBody("--body--".getBytes());

        byte[] encode = RemotingCoder.encode(request).array();

        assert encode.length == RemotingCoder.BYTES + 16;
        assert encode.length == 14 + 16;

        RemotingCommand de = RemotingCoder.decode(ByteBuffer.wrap(encode));

        assert de.getId() == request.getId();
        assert de.getCode() == request.getCode();
        assert de.getVersion() == de.getVersion();
        assert de.getFlag() == de.getFlag();
        assert de.getStringHeaders().equals(request.getStringHeaders());
        assert de.getStringHeaders().equals("headers1");
    }
}