package la.renzhen.remoting.netty.security;

import io.netty.channel.ChannelHandler;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-29 14:41
 */
public interface SecurityProvider {

    ChannelHandler clientHandler();

    ChannelHandler serverHandler();

}