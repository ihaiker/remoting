package la.renzhen.remoting.netty.coder.lfcode;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import la.renzhen.remoting.netty.utils.NettyUtils;
import la.renzhen.remoting.protocol.RemotingCommand;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;

@Slf4j
public class NettyDecoder extends LengthFieldBasedFrameDecoder {
    public static final int BYTES =
            +Integer.BYTES /*id*/
                    + Short.BYTES/*code*/
                    + Integer.BYTES/*(headers.length+version+flag)*/;


    public NettyDecoder(int frameMaxLength) {
        super(frameMaxLength, 0, Integer.BYTES, 0, 4);
    }

    @Override
    public Object decode(ChannelHandlerContext ctx, ByteBuf in) {
        ByteBuf frame = null;
        try {
            frame = (ByteBuf) super.decode(ctx, in);
            if (null == frame) {
                return null;
            }
            return decode(frame.nioBuffer());
        } catch (Exception e) {
            log.error("decode exception, " + NettyUtils.parseChannelRemoteAddr(ctx.channel()), e);
            NettyUtils.closeChannel(ctx.channel());
        } finally {
            if (null != frame) {
                frame.release();
            }
        }
        return null;
    }

    protected RemotingCommand decode(ByteBuffer buf) {
        int length = buf.limit();
        int id = buf.getInt();
        int code = buf.getShort();

        int three = buf.getInt();
        int flag = three & 0b11;
        int version = (three >> 2) & 0b1111_1111;
        int headLength = three >> 10;
        int bodyLength = length - headLength - BYTES;
        byte[] header = null, body = null;
        if (headLength > 0) {
            header = new byte[headLength];
            buf.get(header);
        }
        if (bodyLength > 0) {
            body = new byte[bodyLength];
            buf.get(body);
        }
        return new RemotingCommand(code).setId(id).setVersion(version).setFlag(flag).setHeaders(header).setBody(body);
    }
}
