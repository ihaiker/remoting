package la.renzhen.remoting;

public class RemotingException extends RuntimeException {
    private static final long serialVersionUID = -5690687334570505110L;

    public enum Type {
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

    private Type remotingExceptionType;

    public RemotingException(Type exceptionType, String message) {
        super(message);
        this.remotingExceptionType = exceptionType;
    }

    public RemotingException(Type exceptionType, String message, Throwable cause) {
        super(message, cause);
        this.remotingExceptionType = exceptionType;
    }

    public RemotingException(Type exceptionType, Throwable cause) {
        super(cause);
        this.remotingExceptionType = exceptionType;
    }

    @Override
    public String toString() {
        return "[" + remotingExceptionType.name() + "]" + super.toString();
    }
}
