package la.renzhen.remoting.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import la.renzhen.remoting.RemotingConfig;
import la.renzhen.remoting.RemotingException;
import la.renzhen.remoting.code.RemotingAbstract;
import la.renzhen.remoting.commons.NamedThreadFactory;
import la.renzhen.remoting.netty.coder.CoderProvider;
import la.renzhen.remoting.netty.coder.lfcode.DefaultCoderProvider;
import la.renzhen.remoting.netty.security.SecurityProvider;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-29 16:30
 */
public abstract class NettyRemoting extends RemotingAbstract<Channel> {

    //@formatter:off
    @Setter @Getter protected CoderProvider coderProvider;
    @Setter @Getter protected SecurityProvider securityProvider;
    @Getter protected EventExecutorGroup eventExecutorGroup;
    //@formatter:on

    public NettyRemoting(RemotingConfig remotingConfig) {
        super(remotingConfig);
    }

    public void setEventExecutorGroup(EventExecutorGroup eventExecutorGroup) {
        final EventExecutorGroup eventExecutors = getEventExecutorGroup();
        if (eventExecutors != null) {
            eventExecutors.shutdownGracefully();
        }
        this.eventExecutorGroup = eventExecutorGroup;
    }

    @Override
    protected void startupTCPListener() {
        if (getSecurityProvider() != null) {
            try {
                getSecurityProvider().preCheck();
            } catch (Exception e) {
                throw new RemotingException(RemotingException.Type.Connect, e);
            }
        }

        if (getCoderProvider() == null) {
            setCoderProvider(new DefaultCoderProvider(getRemotingConfig()));
        }

        if (getEventExecutorGroup() == null) {
            final int workThreads = 4;
            setEventExecutorGroup(new DefaultEventExecutorGroup(workThreads, new NamedThreadFactory("NettyCodecThread", workThreads)));
        }
    }

    @Override
    protected void shutdownTCPListener(boolean interrupted) {
        getEventExecutorGroup().shutdownGracefully();
    }

    protected void beforeInitChannel(SocketChannel ch) {
        if (getSecurityProvider() != null) {
            LOGGER.info("Enable client security mode: {}", securityProvider.getClass().getSimpleName());
            ChannelHandler securityHandler = securityProvider.initChannel(ch);
            if (securityHandler != null) {
                ch.pipeline().addLast(getEventExecutorGroup(), SecurityProvider.HANDLER_NAME, securityHandler);
            }
        }
    }

    protected void afterInitChannel(SocketChannel ch) {

    }
}
