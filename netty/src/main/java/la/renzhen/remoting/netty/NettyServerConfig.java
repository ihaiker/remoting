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
package la.renzhen.remoting.netty;

import la.renzhen.remoting.commons.Mixins;
import la.renzhen.remoting.netty.tls.TlsMode;
import lombok.Data;

@Data
public class NettyServerConfig implements Cloneable {

    private String module = Mixins.get("remoting.module", null);

    private int listenPort = Mixins.getInt(module, "remoting.listenPort", 8888);

    private int serverCallbackExecutorThreads = Mixins.getInt(module, "remoting.serverCallbackExecutorThreads", 4);

    private int serverWorkerThreads = Mixins.getInt(module, "remoting.serverWorkerThreads", 8);
    private int serverBossThreads = Mixins.getInt(module, "remoting.serverBossThreads", 1);
    private int serverSelectorThreads = Mixins.getInt(module, "remoting.serverSelectorThreads", 3);

    private int channelEventQueueMaxSize = Mixins.getInt(module, "remoting.channelEventQueueMaxSize", 10000);
    private int serverOnewaySemaphoreLimits = Mixins.getInt(module, "remoting.serverOnewaySemaphoreLimits", 256);
    private int serverAsyncSemaphoreLimits = Mixins.getInt(module, "remoting.serverAsyncSemaphoreLimits", 64);

    private int serverChannelMaxIdleTimeSeconds = Mixins.getInt(module, "remoting.serverChannelMaxIdleTimeSeconds", 120);


    private boolean serverPooledByteBufAllocatorEnable = Boolean.valueOf(Mixins.get(module, "remoting.serverPooledByteBufAllocatorEnable", "true"));

    private boolean useEPollNativeSelector = Boolean.valueOf(Mixins.get(module, "remoting.serverPooledByteBufAllocatorEnable", "true"));

    @Data
    public static class SocketConfig {
        private String module;

        private int serverSocketSndBufSize = Mixins.getInt(module, "remoting.sndBufSize", 65535);
        private int serverSocketRcvBufSize = Mixins.getInt(module, "remoting.rcvBufSize", 65535);
        private int frameMaxLength = Mixins.getInt(module, "remoting.frameMaxLength", 16777216);

        public SocketConfig() {
            this.module = null;
        }

        public SocketConfig(String module) {
            this.module = module;
        }
    }

    private SocketConfig socket = new SocketConfig(module);

    private TlsMode tlsMode = TlsMode.valueOf(Mixins.get(module, "remoting.tlsMode", "PERMISSIVE"));

    @Override
    public Object clone() throws CloneNotSupportedException {
        return (NettyServerConfig) super.clone();
    }
}
