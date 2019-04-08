package la.renzhen.remoting.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import la.renzhen.remoting.RemotingChannel;
import la.renzhen.remoting.netty.utils.NettyUtils;
import la.renzhen.remoting.protocol.ClientInfoHeader;
import la.renzhen.remoting.protocol.RemotingCommand;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-27 17:47
 */
public class NettyChannel implements RemotingChannel<Channel> {

    //@formatter:off
    private Channel channel;
    private String address;
    @Setter @Getter String unique;
    @Setter @Getter Map<String,String> attrs;
    @Setter @Getter String module;
    //@formatter:on

    public NettyChannel(ChannelHandlerContext ctx) {
        this.channel = ctx.channel();
        this.address = NettyUtils.parseChannelRemoteAddr(ctx.channel());
    }

    public NettyChannel(ChannelFuture future) {
        this.channel = future.channel();
        this.address = NettyUtils.parseChannelRemoteAddr(future.channel());
    }

    @Override
    public Channel getChannel() {
        return this.channel;
    }

    @Override
    public String address() {
        return address;
    }

    @Override
    public boolean isOK() {
        return channel != null && channel.isOpen() && channel.isActive();
    }

    @Override
    public boolean isWritable() {
        return getChannel().isWritable();
    }

    @Override
    public void close() {
        if(channel != null){
            NettyUtils.closeChannel(channel);
        }
    }

    public void fromHeader(ClientInfoHeader header){
        setUnique(header.getUnique());
        setModule(header.getModule());
        setAttrs(header.getAttributes());
    }

    @Override
    public void writeAndFlush(RemotingCommand command, ChannelWriterListener<Channel> writerListener) {
        ChannelFuture future = channel.writeAndFlush(command);
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
