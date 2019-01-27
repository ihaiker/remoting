package la.renzhen.remoting;

public class ChannelEvent<Channel> {
    private final ChannelEventType type;
    private final RemotingChannel<Channel> channel;

    public ChannelEvent(ChannelEventType type, RemotingChannel<Channel> channel) {
        this.type = type;
        this.channel = channel;
    }

    public ChannelEventType getType() {
        return type;
    }

    public RemotingChannel<Channel> getChannel() {
        return channel;
    }

    @Override
    public String toString() {
        return "ChannelEvent [type=" + type + ", remoteAddr=" + ", channel=" + channel + "]";
    }
}
