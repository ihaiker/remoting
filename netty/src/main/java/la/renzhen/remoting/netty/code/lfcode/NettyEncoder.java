package la.renzhen.remoting.netty.code.lfcode;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import la.renzhen.remoting.netty.utils.NettyUtils;
import la.renzhen.remoting.protocol.RemotingCommand;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyEncoder extends MessageToByteEncoder<RemotingCommand> {
    public static final int BYTES =
            +Integer.BYTES /* id */
                    + Short.BYTES   /* code */
                    + Integer.BYTES /* headers.length + version + flag */;

    @Override
    public void encode(ChannelHandlerContext ctx, RemotingCommand remotingCommand, ByteBuf out) {
        try {
            encode(remotingCommand, out);
        } catch (Exception e) {
            log.error("encode exception, " + NettyUtils.parseChannelRemoteAddr(ctx.channel()), e);
            log.error(remotingCommand.toString());
            NettyUtils.closeChannel(ctx.channel());
        }
    }

    public void encode(RemotingCommand cmd, ByteBuf out) {
        int headerLength = 0;
        if (cmd.getHeaders() != null) {
            headerLength = cmd.getHeaders().length;
        }
        int bodyLength = 0;
        if (cmd.getBody() != null) {
            bodyLength = cmd.getBody().length;
        }
        int length = BYTES + headerLength + bodyLength;
        out.writeInt(length);

        out.writeInt(cmd.getId());
        out.writeShort((short) cmd.getCode());
        int three = (headerLength << (8 + 2)) | (cmd.getVersion() << 2) | cmd.getFlag();
        out.writeInt(three);

        if (headerLength > 0) {
            out.writeBytes(cmd.getHeaders());
        }
        if (bodyLength > 0) {
            out.writeBytes(cmd.getBody());
        }
    }
}
