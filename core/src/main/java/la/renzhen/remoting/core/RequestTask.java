/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package la.renzhen.remoting.core;


import la.renzhen.remoting.RemotingChannel;
import la.renzhen.remoting.protocol.RemotingCommand;

public class RequestTask<Channel> implements Runnable {
    private final Runnable runnable;
    private final long createTimestamp = System.currentTimeMillis();
    private final RemotingChannel<Channel> channel;
    private final RemotingCommand request;
    private boolean stopRun = false;

    public RequestTask(final Runnable runnable, final RemotingChannel<Channel> channel, final RemotingCommand request) {
        this.runnable = runnable;
        this.channel = channel;
        this.request = request;
    }

    @Override
    public int hashCode() {
        int result = runnable != null ? runnable.hashCode() : 0;
        result = 31 * result + (int) (getCreateTimestamp() ^ (getCreateTimestamp() >>> 32));
        result = 31 * result + (channel != null ? channel.hashCode() : 0);
        result = 31 * result + (request != null ? request.hashCode() : 0);
        result = 31 * result + (isStopRun() ? 1 : 0);
        return result;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RequestTask))
            return false;

        final RequestTask that = (RequestTask) o;

        if (getCreateTimestamp() != that.getCreateTimestamp())
            return false;
        if (isStopRun() != that.isStopRun())
            return false;
        if (channel != null ? !channel.equals(that.channel) : that.channel != null)
            return false;
        return request != null ? request.getId() == that.request.getId() : that.request == null;

    }

    public long getCreateTimestamp() {
        return createTimestamp;
    }

    public boolean isStopRun() {
        return stopRun;
    }

    public void setStopRun(final boolean stopRun) {
        this.stopRun = stopRun;
    }

    @Override
    public void run() {
        if (!this.stopRun)
            this.runnable.run();
    }

    public void returnResponse(int code, String remark) {
        final RemotingCommand response = RemotingCommand.error(code, remark);
        response.setId(request.getId());
        this.channel.writeAndFlush(response);
    }
}
