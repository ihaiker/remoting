package la.renzhen.remoting.netty;

import io.netty.channel.Channel;
import la.renzhen.remoting.RemotingConfig;
import la.renzhen.remoting.code.RemotingAbstract;
import la.renzhen.remoting.netty.coder.CoderProvider;
import la.renzhen.remoting.netty.security.SecurityProvider;
import lombok.Getter;
import lombok.Setter;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-29 16:30
 */
public abstract class NettyRemoting extends RemotingAbstract<Channel> {

    //@formatter:off
    @Setter @Getter protected CoderProvider coderProvider;
    @Setter @Getter protected SecurityProvider securityProvider;
    //@formatter:on

    public NettyRemoting(RemotingConfig remotingConfig) {
        super(remotingConfig);
    }
}
