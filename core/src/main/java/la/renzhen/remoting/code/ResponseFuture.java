package la.renzhen.remoting.code;

import la.renzhen.remoting.InvokeCallback;
import la.renzhen.remoting.RemotingChannel;
import la.renzhen.remoting.protocol.RemotingCommand;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ResponseFuture<Channel> {
    private final RemotingChannel<Channel> channel;
    private final long timeoutMillis;
    private final InvokeCallback<Channel> invokeCallback;

    private final long beginTimestamp = System.currentTimeMillis();
    private final CountDownLatch countDownLatch;

    private final AtomicBoolean executeCallbackOnlyOnce = new AtomicBoolean(false);
    private final SemaphoreReleaseOnlyOnce semaphore;


    private RemotingCommand requestCommand;

    private volatile RemotingCommand responseCommand;
    private volatile boolean sendRequestOK = true;
    private volatile Throwable cause;

    public ResponseFuture(RemotingChannel<Channel> channel, RemotingCommand requestCommand, long timeoutMillis, InvokeCallback invokeCallback, Semaphore semaphore) {
        this.channel = channel;
        this.timeoutMillis = timeoutMillis;
        this.invokeCallback = invokeCallback;
        this.semaphore = semaphore == null ? null : new SemaphoreReleaseOnlyOnce(semaphore);
        this.requestCommand = requestCommand;
        this.countDownLatch = invokeCallback == null /*sync*/ ? new CountDownLatch(1) : null;
    }

    public void executeInvokeCallback() {
        if (invokeCallback != null) {
            if (this.executeCallbackOnlyOnce.compareAndSet(false, true)) {
                invokeCallback.operationComplete(channel, requestCommand, responseCommand);
            }
        }
    }

    public void release() {
        if (this.semaphore != null) {
            this.semaphore.release();
        }
    }

    public boolean isTimeout() {
        long diff = System.currentTimeMillis() - this.beginTimestamp;
        return diff > this.timeoutMillis;
    }

    public RemotingCommand waitResponse() throws InterruptedException {
        this.countDownLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
        return this.responseCommand;
    }

    public void putResponse(final RemotingCommand responseCommand) {
        this.responseCommand = responseCommand;
        this.countDownLatch.countDown();
    }

    public long getBeginTimestamp() {
        return beginTimestamp;
    }

    public boolean isSendRequestOK() {
        return sendRequestOK;
    }

    public void setSendRequestOK(boolean sendRequestOK) {
        this.sendRequestOK = sendRequestOK;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public InvokeCallback getInvokeCallback() {
        return invokeCallback;
    }

    public Throwable getCause() {
        return cause;
    }

    public void setCause(Throwable cause) {
        this.cause = cause;
    }

    public RemotingCommand getResponseCommand() {
        return responseCommand;
    }

    public void setResponseCommand(RemotingCommand responseCommand) {
        this.responseCommand = responseCommand;
    }

    public RemotingCommand getRequestCommand() {
        return requestCommand;
    }

    public RemotingChannel<Channel> getChannel() {
        return channel;
    }

    @Override
    public String toString() {
        return "ResponseFuture [responseCommand=" + responseCommand
                + ", sendRequestOK=" + sendRequestOK
                + ", cause=" + cause
                + ", requestCommand=" + requestCommand
                + ", channel=" + channel
                + ", timeoutMillis=" + timeoutMillis
                + ", invokeCallback=" + invokeCallback
                + ", beginTimestamp=" + beginTimestamp
                + ", countDownLatch=" + countDownLatch + "]";
    }
}
