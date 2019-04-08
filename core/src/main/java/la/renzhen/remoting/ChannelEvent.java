package la.renzhen.remoting;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain = true)
public class ChannelEvent<Channel> {

    public enum Type {
        CONNECT,
        CLOSE,
        IDLE,
        EXCEPTION
    }

    //@formatter:off
    @Getter private final Type type;
    @Getter private final RemotingChannel<Channel> channel;
    @Setter @Getter private Object data;
    //@formatter:on

    public ChannelEvent(Type type, RemotingChannel<Channel> channel) {
        this.type = type;
        this.channel = channel;
    }

    @Override
    public String toString() {
        return "ChannelEvent [type=" + type + ", channel=" + channel + "]";
    }
}
