package la.renzhen.remoting;


import la.renzhen.remoting.protocol.RemotingCommand;

/**
 * Command hooks can be processed before and after the command is executed.
 */
public interface CommandHook<Channel> {

    /**
     * @param channel the wrapper of remote channel
     * @param request the request command
     *
     * @return Return whether to continue to execute downward
     */
    boolean doBeforeRequest(final RemotingChannel<Channel> channel, final RemotingCommand request);

    void doAfterResponse(final RemotingChannel<Channel> channel, final RemotingCommand request, final RemotingCommand response);

}
