package la.renzhen.remoting;

public interface RemotingService {

    void startup();

    default void shutdown() {
        this.shutdown(false);
    }

    void shutdown(boolean interrupted);

    boolean isRunning();

}