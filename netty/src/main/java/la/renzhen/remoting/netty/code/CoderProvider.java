package la.renzhen.remoting.netty.code;

import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandler;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-29 13:56
 */
public interface CoderProvider {

    ChannelInboundHandler decode();

    ChannelOutboundHandler encode();
}
