package la.renzhen.remoting;


import la.renzhen.remoting.protocol.RemotingCommand;

public interface RequestProcessor<Channel> {

    RemotingCommand processRequest(RemotingChannel<Channel> channel, RemotingCommand request) throws Exception;

    default boolean rejectRequest(RemotingChannel<Channel> channel) {
        return false;
    }
}
