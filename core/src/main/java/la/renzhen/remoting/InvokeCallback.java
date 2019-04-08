package la.renzhen.remoting;

import la.renzhen.remoting.protocol.RemotingCommand;

public interface InvokeCallback<Channel> {

    void operationComplete(final RemotingChannel<Channel> channel, final RemotingCommand request, final RemotingCommand response);

}
