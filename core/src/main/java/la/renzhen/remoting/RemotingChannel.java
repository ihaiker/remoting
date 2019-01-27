package la.renzhen.remoting;

import la.renzhen.remoting.protocol.RemotingCommand;

/**
 * wrapper of the connector in different implementations.
 *
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-26 14:56
 */
public interface RemotingChannel<Channel> {

    public interface ChannelWriterListener<Channel> {
        void operationComplete(boolean success, Throwable throwable);
    }

    Channel getChannel();

    String address();

    boolean isOK();

    boolean isWritable();

    default void writeAndFlush(RemotingCommand command) {
        this.writeAndFlush(command, null);
    }

    void writeAndFlush(RemotingCommand command, ChannelWriterListener<Channel> writerListener);
}