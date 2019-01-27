package la.renzhen.remoting.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import la.renzhen.remoting.RemotingChannel;
import la.renzhen.remoting.netty.utils.NettyUtils;
import la.renzhen.remoting.protocol.RemotingCommand;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-27 17:47
 */
public class NettyChannel implements RemotingChannel<Channel> {

    private ChannelHandlerContext channelHandlerContext;
    private String address;

    public NettyChannel(ChannelHandlerContext context) {
        this.channelHandlerContext = context;
        this.address = NettyUtils.parseChannelRemoteAddr(context.channel());
    }

    @Override
    public Channel getChannel() {
        return this.channelHandlerContext.channel();
    }

    @Override
    public String address() {
        return address;
    }

    @Override
    public boolean isOK() {
        Channel ch = this.getChannel();
        return ch.isOpen() && ch.isActive();
    }

    @Override
    public boolean isWritable() {
        return getChannel().isWritable();
    }

    @Override
    public void writeAndFlush(RemotingCommand command, ChannelWriterListener<Channel> writerListener) {
        ChannelFuture future = channelHandlerContext.writeAndFlush(command);
        if (writerListener != null) {
            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    writerListener.operationComplete(future.isSuccess(), future.cause());
                }
            });
        }
    }
}
