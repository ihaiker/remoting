package la.renzhen.remoting.netty.security;

import io.netty.channel.ChannelHandler;
import io.netty.channel.socket.SocketChannel;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-29 14:41
 */
public interface SecurityProvider {

    String HANDLER_NAME = "security";

    /**
     * init channel
     * @param ch
     * @return If it is not empty, the handler will be named {@link SecurityProvider#HANDLER_NAME} and added to the handler queue.
     */
    ChannelHandler initChannel(SocketChannel ch);

    default void preCheck() throws Exception { }
}