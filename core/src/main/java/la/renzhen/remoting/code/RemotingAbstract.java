package la.renzhen.remoting.code;

import la.renzhen.remoting.*;
import la.renzhen.remoting.commons.Pair;
import la.renzhen.remoting.commons.RemotingHelper;
import la.renzhen.remoting.protocol.RemotingCommand;
import la.renzhen.remoting.protocol.RemotingSysResponseCode;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-27 11:39
 */
@Slf4j
public abstract class RemotingAbstract<Channel> implements Remoting<Channel> {
    /**
     * Semaphore to limit maximum number of on-going asynchronous requests, which protects system memory footprint.
     */
    protected final Semaphore semaphoreAsync;

    /**
     * Semaphore to limit maximum number of on-going asynchronous requests, which protects system memory footprint.
     */
    protected final Semaphore semaphoreOneway;

    /**
     * This map caches all on-going requests.
     */
    protected final ConcurrentMap<Integer /* requestId */, ResponseFuture<Channel>> responseTable = new ConcurrentHashMap<>(256);

    /**
     * This container holds all processors per request code, aka, for each incoming request, we may look up the
     * responding processor in this map to handle the request.
     */
    protected final ConcurrentMap<Integer/* request code */, Pair<RequestProcessor<Channel>, ExecutorService>> processorTable = new ConcurrentHashMap<>(64);

    /**
     * Executor to feed channel events.
     */
    protected final ChannelEventExecutor eventExecutor;

    /**
     * The default request processor to use in case there is no exact match in {@link #processorTable} per request code.
     */
    protected Pair<RequestProcessor<Channel>, ExecutorService> defaultRequestProcessor;

    /**
     * custom rpc hooks
     */
    protected List<CommandHook<Channel>> commandHooks = new ArrayList<>();

    public RemotingAbstract(final int permitsOneway, final int permitsAsync, final int eventMaxSize) {
        this.semaphoreOneway = new Semaphore(permitsOneway, true);
        this.semaphoreAsync = new Semaphore(permitsAsync, true);
        this.eventExecutor = new ChannelEventExecutor(eventMaxSize);
    }

    @Override
    public void registerChannelEventListener(ChannelEventListener<Channel> channelEventListener) {
        this.eventExecutor.setChannelEventListener(channelEventListener);
    }


    @Override
    public void registerDefaultProcessor(RequestProcessor<Channel> processor, ExecutorService executor) {
        defaultRequestProcessor = new Pair<>(processor, executor);
    }

    @Override
    public void registerProcessor(int requestCode, RequestProcessor<Channel> processor, ExecutorService executor) {
        processorTable.put(requestCode, new Pair<>(processor, executor));
    }

    /**
     * Put a netty event to the executor.
     *
     * @param event Netty event instance.
     */
    public void putNettyEvent(final ChannelEvent<Channel> event) {
        eventExecutor.putChannelEvent(event);
    }


    protected void doBeforeRpcHooks(RemotingChannel<Channel> channelWrapper, RemotingCommand request) {
        if (commandHooks.size() > 0) {
            for (CommandHook<Channel> commandHook : commandHooks) {
                commandHook.doBeforeRequest(channelWrapper, request);
            }
        }
    }

    protected void doAfterRpcHooks(RemotingChannel<Channel> channelWrapper, RemotingCommand request, RemotingCommand response) {
        if (commandHooks.size() > 0) {
            for (CommandHook<Channel> commandHook : commandHooks) {
                commandHook.doAfterResponse(channelWrapper, request, response);
            }
        }
    }


    public void processMessageReceived(RemotingChannel<Channel> ctx, RemotingCommand msg) throws Exception {
        final RemotingCommand cmd = msg;
        if (cmd != null) {
            if (cmd.isResponse()) {
                processRequestCommand(ctx, cmd);
            } else {
                processResponseCommand(ctx, cmd);
            }
        }
    }

    /**
     * Process incoming request command issued by remote peer.
     *
     * @param ctx channel handler context.
     * @param cmd request command.
     */
    public void processRequestCommand(final RemotingChannel<Channel> ctx, final RemotingCommand cmd) {
        final Pair<RequestProcessor<Channel>, ExecutorService> matched = this.processorTable.get(cmd.getCode());
        final Pair<RequestProcessor<Channel>, ExecutorService> pair = null == matched ? this.defaultRequestProcessor : matched;
        final int requestId = cmd.getId();

        if (pair != null) {
            Runnable run = new Runnable() {
                @Override
                public void run() {
                    try {
                        doBeforeRpcHooks(ctx, cmd);
                        final RemotingCommand response = pair.getFirst().processRequest(ctx, cmd);
                        doAfterRpcHooks(ctx, cmd, response);

                        if (!cmd.isOneway()) {
                            if (response != null) {
                                response.setId(requestId);
                                response.makeResponse();
                                try {
                                    ctx.writeAndFlush(response);
                                } catch (Throwable e) {
                                    log.error("process request over, but response failed", e);
                                    log.error(cmd.toString());
                                    log.error(response.toString());
                                }

                            }
                        }
                    } catch (Throwable e) {
                        log.error("process request exception", e);
                        log.error(cmd.toString());

                        if (!cmd.isOneway()) {
                            final RemotingCommand response = RemotingCommand.error(cmd, RemotingSysResponseCode.EXCEPTION, RemotingHelper.exceptionSimpleDesc(e));
                            response.setId(requestId).makeResponse();
                            ctx.writeAndFlush(response);
                        }
                    }
                }
            };

            if (pair.getFirst().rejectRequest(ctx)) {
                final RemotingCommand response = RemotingCommand.error(cmd, RemotingSysResponseCode.BUSY, "[REJECTREQUEST]system busy, start flow control for a while");
                response.setId(requestId).makeResponse();
                ctx.writeAndFlush(response);
                return;
            }

            try {
                final RequestTask requestTask = new RequestTask(run, ctx, cmd);
                pair.getSecond().submit(requestTask);
            } catch (RejectedExecutionException e) {
                if ((System.currentTimeMillis() % 10000) == 0) {
                    log.warn(ctx.address()
                            + ", too many requests and system thread pool busy, RejectedExecutionException "
                            + pair.getSecond().toString()
                            + " request code: " + cmd.getCode());
                }

                if (!cmd.isOneway()) {
                    final RemotingCommand response = RemotingCommand.error(RemotingSysResponseCode.BUSY, "[OVERLOAD]system busy, start flow control for a while");
                    response.setId(requestId);
                    ctx.writeAndFlush(response);
                }
            }
        } else {
            String error = " request type " + cmd.getCode() + " not supported";
            final RemotingCommand response = RemotingCommand.error(RemotingSysResponseCode.NOT_SUPPORTED, error);
            response.setId(requestId).makeResponse();
            ctx.writeAndFlush(response);
            log.error(" {} ", ctx.address(), error);
        }
    }

    /**
     * Process response from remote peer to the previous issued requests.
     *
     * @param ctx      channel handler context.
     * @param response response command instance.
     */
    public void processResponseCommand(RemotingChannel<Channel> ctx, RemotingCommand response) {
        final int requestId = response.getId();
        final ResponseFuture responseFuture = responseTable.remove(requestId);
        if (responseFuture != null) {
            responseFuture.setResponseCommand(response);
            if (responseFuture.getInvokeCallback() != null) {
                executeInvokeCallback(responseFuture);
            } else {
                responseFuture.putResponse(response);
                responseFuture.release();
            }
        } else {
            log.warn("receive response, but not matched any request, " + ctx.address());
            log.warn(response.toString());
        }
    }

    /**
     * This method specifies thread pool to use while invoking callback methods.
     *
     * @return Dedicated thread pool instance if specified; or null if the callback is supposed to be executed in the
     * netty event-loop thread.
     */
    public abstract ExecutorService getCallbackExecutor();

    /**
     * Execute callback in callback executor. If callback executor is null, run directly in current thread
     */
    private void executeInvokeCallback(final ResponseFuture responseFuture) {
        boolean runInThisThread = false;
        ExecutorService executor = this.getCallbackExecutor();
        if (executor != null) {
            try {
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            responseFuture.executeInvokeCallback();
                        } catch (Throwable e) {
                            log.warn("execute callback in executor exception, and callback throw", e);
                        } finally {
                            responseFuture.release();
                        }
                    }
                });
            } catch (Exception e) {
                runInThisThread = true;
                log.warn("execute callback in executor exception, maybe executor busy", e);
            }
        } else {
            runInThisThread = true;
        }

        if (runInThisThread) {
            try {
                responseFuture.executeInvokeCallback();
            } catch (Throwable e) {
                log.warn("executeInvokeCallback Exception", e);
            } finally {
                responseFuture.release();
            }
        }
    }


    protected RemotingCommand invokeSyncHandler(final RemotingChannel<Channel> channel, final RemotingCommand request, final long timeoutMillis) throws InterruptedException, RemotingException {
        final int requestId = request.getId();
        try {
            final ResponseFuture responseFuture = new ResponseFuture(channel, request, timeoutMillis, null, null);

            this.responseTable.put(requestId, responseFuture);
            final String addr = channel.address();
            channel.writeAndFlush(request, (success, throwable) -> {
                if (success) {
                    responseFuture.setSendRequestOK(true);
                    return;
                } else {
                    responseFuture.setSendRequestOK(false);
                }
                responseTable.remove(requestId);
                responseFuture.setCause(throwable);
                responseFuture.putResponse(null);
                log.warn("send a request command to channel <" + addr + "> failed.");
            });

            RemotingCommand responseCommand = responseFuture.waitResponse();
            if (null == responseCommand) {
                if (responseFuture.isSendRequestOK()) {
                    throw new RemotingException(RemotingException.RemotingExceptionType.Timeout, addr, responseFuture.getCause());
                } else {
                    throw new RemotingException(RemotingException.RemotingExceptionType.SendRequest, addr, responseFuture.getCause());
                }
            }
            return responseCommand;
        } finally {
            this.responseTable.remove(requestId);
        }
    }

    public void invokeAsyncHandler(final RemotingChannel<Channel> channel, final RemotingCommand request, final long timeoutMillis, final InvokeCallback invokeCallback)
            throws InterruptedException, RemotingException {
        final long beginStartTime = System.currentTimeMillis();
        final int requestId = request.getId();
        boolean acquired = this.semaphoreAsync.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS);
        if (acquired) {
            long costTime = System.currentTimeMillis() - beginStartTime;
            if (timeoutMillis < costTime) {
                this.semaphoreAsync.release();
                throw new RemotingException(RemotingException.RemotingExceptionType.Timeout, "invokeAsyncImpl call timeout");
            }

            final ResponseFuture responseFuture = new ResponseFuture(channel, request, timeoutMillis - costTime, invokeCallback, semaphoreAsync);
            this.responseTable.put(requestId, responseFuture);
            try {
                channel.writeAndFlush(request, (success, throwable) -> {
                    if (success) {
                        responseFuture.setSendRequestOK(true);
                        return;
                    }
                    requestFail(requestId);
                    log.warn("send a request command to channel <{}> failed.", channel.address());
                });
            } catch (Exception e) {
                responseFuture.release();
                log.warn("send a request command to channel <" + channel.address() + "> Exception", e);
                throw new RemotingException(RemotingException.RemotingExceptionType.SendRequest, e);
            }
        } else {
            if (timeoutMillis <= 0) {
                throw new RemotingException(RemotingException.RemotingExceptionType.TooMuchRequest, "invokeAsyncImpl invoke too fast");
            } else {
                String info = String.format("invokeAsyncImpl tryAcquire semaphore timeout, %dms, waiting thread nums: %d semaphoreAsyncValue: %d",
                        timeoutMillis, this.semaphoreAsync.getQueueLength(), this.semaphoreAsync.availablePermits()
                );
                log.warn(info);
                throw new RemotingException(RemotingException.RemotingExceptionType.Timeout, info);
            }
        }
    }

    public void invokeOnewayHandler(final RemotingChannel<Channel> channel, final RemotingCommand request, final long timeoutMillis)
            throws InterruptedException, RemotingException {
        request.makeOneway();
        boolean acquired = this.semaphoreOneway.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS);
        if (acquired) {
            final SemaphoreReleaseOnlyOnce once = new SemaphoreReleaseOnlyOnce(this.semaphoreOneway);
            try {
                channel.writeAndFlush(request, (success, throwable) -> {
                    once.release();
                    if (!success) {
                        log.warn("send a request command to channel <" + channel.address() + "> failed.");
                    }
                });
            } catch (Exception e) {
                once.release();
                log.warn("write send a request command to channel <" + channel.address() + "> failed.");
                throw new RemotingException(RemotingException.RemotingExceptionType.SendRequest, e);
            }
        } else {
            if (timeoutMillis <= 0) {
                throw new RemotingException(RemotingException.RemotingExceptionType.TooMuchRequest, "invokeOnewayImpl invoke too fast");
            } else {
                String info = String.format(
                        "invokeOnewayImpl tryAcquire semaphore timeout, %dms, waiting thread nums: %d semaphoreAsyncValue: %d",
                        timeoutMillis,
                        this.semaphoreOneway.getQueueLength(),
                        this.semaphoreOneway.availablePermits()
                );
                log.warn(info);
                throw new RemotingException(RemotingException.RemotingExceptionType.Timeout, info);
            }
        }
    }

    /**
     * <p>
     * This method is periodically invoked to scan and expire deprecated request.
     * </p>
     */
    public void scanResponseTable() { //TODO 使用HealWheellTimer
        final List<ResponseFuture> rfList = new LinkedList<ResponseFuture>();
        Iterator<Map.Entry<Integer, ResponseFuture<Channel>>> it = this.responseTable.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, ResponseFuture<Channel>> next = it.next();
            ResponseFuture rep = next.getValue();
            if ((rep.getBeginTimestamp() + rep.getTimeoutMillis() + 1000) <= System.currentTimeMillis()) {
                rep.release();
                it.remove();
                rfList.add(rep);
                log.warn("remove timeout request, " + rep);
            }
        }

        for (ResponseFuture rf : rfList) {
            try {
                executeInvokeCallback(rf);
            } catch (Throwable e) {
                log.warn("scanResponseTable, operationComplete Exception", e);
            }
        }
    }


    private void requestFail(final int requestId) {
        ResponseFuture responseFuture = responseTable.remove(requestId);
        if (responseFuture != null) {
            responseFuture.setSendRequestOK(false);
            responseFuture.putResponse(null);
            try {
                executeInvokeCallback(responseFuture);
            } catch (Throwable e) {
                log.warn("execute callback in requestFail, and callback throw", e);
            } finally {
                responseFuture.release();
            }
        }
    }

    /**
     * mark the request of the specified channel as fail and to invoke fail callback immediately
     *
     * @param channel the channel which is close already
     */
    protected void failFast(final RemotingChannel<Channel> channel) {
        Iterator<Map.Entry<Integer, ResponseFuture<Channel>>> it = responseTable.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, ResponseFuture<Channel>> entry = it.next();
            if (entry.getValue().getChannel() == channel) {
                Integer opaque = entry.getKey();
                if (opaque != null) {
                    requestFail(opaque);
                }
            }
        }
    }


}