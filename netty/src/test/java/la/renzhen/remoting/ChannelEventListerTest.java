package la.renzhen.remoting;

import io.netty.channel.Channel;
import la.renzhen.remoting.netty.NettyRemoting;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-02-20 21:04
 */
@Slf4j
public class ChannelEventListerTest extends RemotingNettyAbstract {

    @Override
    public void registerTestProcessor(NettyRemoting remoting) {
        super.registerTestProcessor(remoting);

        remoting.registerChannelEventListener(new ChannelEventListener<Channel>() {
            @Override
            public void onChannelConnect(RemotingChannel<Channel> channel) {
                log.info("通道连接 {} : {}", remoting.getClass().getSimpleName(), channel);
            }

            @Override
            public void onChannelClose(RemotingChannel<Channel> channel) {
                log.info("通道关闭 {} : {}", remoting.getClass().getSimpleName(), channel);
            }

            @Override
            public void onChannelException(RemotingChannel<Channel> channel) {
                log.info("通道异常 {} : {}", remoting.getClass().getSimpleName(), channel);
            }

            @Override
            public void onChannelIdle(RemotingChannel<Channel> channel) {
                log.info("心跳 {} : {}", remoting.getClass().getSimpleName(), channel);
            }
        });
    }

    @Test
    public void testChannelEventListener() {

    }
}
