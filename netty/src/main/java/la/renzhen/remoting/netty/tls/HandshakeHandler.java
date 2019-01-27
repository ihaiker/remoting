package la.renzhen.remoting.netty.tls;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import la.renzhen.remoting.netty.utils.Constants;
import lombok.extern.slf4j.Slf4j;

import java.util.NoSuchElementException;

@Slf4j
public class HandshakeHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final TlsMode tlsMode;
    private static final byte HANDSHAKE_MAGIC_CODE = 0x16;
    private SslContext sslContext;
    private final DefaultEventExecutorGroup defaultEventExecutorGroup;

    public HandshakeHandler(DefaultEventExecutorGroup eventLoopGroup, SslContext sslContext, TlsMode tlsMode) {
        this.tlsMode = tlsMode;
        this.sslContext = sslContext;
        this.defaultEventExecutorGroup = eventLoopGroup;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        msg.markReaderIndex();
        byte b = msg.getByte(0);

        if (b == HANDSHAKE_MAGIC_CODE) {
            switch (tlsMode) {
                case DISABLED:
                    ctx.close();
                    log.warn("Clients intend to establish a SSL connection while this server is running in SSL disabled mode");
                    break;
                case PERMISSIVE:
                case ENFORCING:
                    if (null != sslContext) {
                        ctx.pipeline()
                                .addAfter(defaultEventExecutorGroup, Constants.HANDSHAKE_HANDLER_NAME, Constants.TLS_HANDLER_NAME, sslContext.newHandler(ctx.channel().alloc()))
                                .addAfter(defaultEventExecutorGroup, Constants.TLS_HANDLER_NAME, Constants.FILE_REGION_ENCODER_NAME, new FileRegionEncoder());
                        log.info("Handlers prepended to channel pipeline to establish SSL connection");
                    } else {
                        ctx.close();
                        log.error("Trying to establish a SSL connection but sslContext is null");
                    }
                    break;

                default:
                    log.warn("Unknown TLS mode");
                    break;
            }
        } else if (tlsMode == TlsMode.ENFORCING) {
            ctx.close();
            log.warn("Clients intend to establish an insecure connection while this server is running in SSL enforcing mode");
        }

        // reset the reader index so that handshake negotiation may proceed as normal.
        msg.resetReaderIndex();
        try {
            // Remove this handler
            ctx.pipeline().remove(this);
        } catch (NoSuchElementException e) {
            log.error("Error while removing HandshakeHandler", e);
        }

        // Hand over this message to the next .
        ctx.fireChannelRead(msg.retain());
    }
}