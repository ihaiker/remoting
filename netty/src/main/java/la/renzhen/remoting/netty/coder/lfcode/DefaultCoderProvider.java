package la.renzhen.remoting.netty.coder.lfcode;

import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandler;
import la.renzhen.remoting.RemotingConfig;
import la.renzhen.remoting.netty.coder.CoderProvider;
import lombok.Data;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-29 13:58
 */
@Data
public class DefaultCoderProvider implements CoderProvider {

    private RemotingConfig remotingConfig;

    public DefaultCoderProvider(RemotingConfig remotingConfig) {
        this.remotingConfig = remotingConfig;
    }

    @Override
    public ChannelInboundHandler decode() {
        return new NettyDecoder(remotingConfig.getMaxFrameLength());
    }

    @Override
    public ChannelOutboundHandler encode() {
        return new NettyEncoder();
    }
}
