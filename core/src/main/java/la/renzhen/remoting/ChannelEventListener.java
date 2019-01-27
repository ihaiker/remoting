package la.renzhen.remoting;


public interface ChannelEventListener<Channel> {

    void onChannelConnect(final RemotingChannel<Channel> channel);

    void onChannelClose(final RemotingChannel<Channel> channel);

    void onChannelException(final RemotingChannel<Channel> channel);

    void onChannelIdle(final RemotingChannel<Channel> channel);

}
