package la.renzhen.remoting.netty.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import la.renzhen.remoting.protocol.RemotingCommand;

/**
 * 用于启动接收客户端的服务信息。
 *
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-31 19:47
 */
public class ClientInfoHandler extends SimpleChannelInboundHandler<RemotingCommand> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
        ctx.pipeline().remove(this);
    }
}
