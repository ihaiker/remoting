package la.renzhen.remoting;

public class RemotingException extends RuntimeException {
    private static final long serialVersionUID = -5690687334570505110L;

    public enum RemotingExceptionType {
        /**
         * Command missing required fields
         */
        Command,
        /**
         * Link remote service exception
         */
        Connect,
        SendRequest,
        /**
         * Send command timed out
         */
        Timeout,
        TooMuchRequest
    }

    private RemotingExceptionType remotingExceptionType;

    public RemotingException(RemotingExceptionType exceptionType, String message) {
        super(message);
        this.remotingExceptionType = exceptionType;
    }

    public RemotingException(RemotingExceptionType exceptionType, String message, Throwable cause) {
        super(message, cause);
        this.remotingExceptionType = exceptionType;
    }

    public RemotingException(RemotingExceptionType exceptionType, Throwable cause) {
        super(cause);
        this.remotingExceptionType = exceptionType;
    }

    @Override
    public String toString() {
        return "[" + remotingExceptionType.name() + "]" + super.toString();
    }
}
