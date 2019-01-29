package la.renzhen.remoting;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import la.renzhen.remoting.netty.coder.lfcode.NettyDecoder;
import la.renzhen.remoting.netty.coder.lfcode.NettyEncoder;
import la.renzhen.remoting.protocol.RemotingCommand;
import org.junit.Test;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-29 11:00
 */
public class CoderTest {

    private NettyDecoder decoder = new NettyDecoder(Integer.MAX_VALUE);
    private NettyEncoder encoder = new NettyEncoder();

    @Test
    public void testTest() {
        ByteBuf out = Unpooled.buffer();

        RemotingCommand encode = RemotingCommand.request(1024);
        encode.setVersion(12);
        encode.setStringHeaders("headers1");
        encode.setBody("--body--".getBytes());

        encoder.encode(null, encode, out);

        out = out.resetReaderIndex();

        RemotingCommand decode = (RemotingCommand) decoder.decode(null, out);
        assert null != decode;
        assert decode.getVersion() == encode.getVersion();
        assert decode.getStringHeaders().equals(encode.getStringHeaders());
        assert decode.getCode() == encode.getCode();
    }
}
