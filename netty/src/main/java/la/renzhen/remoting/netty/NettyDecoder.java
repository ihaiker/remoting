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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import la.renzhen.remoting.netty.utils.NettyUtils;
import la.renzhen.remoting.protocol.RemotingCoder;
import la.renzhen.remoting.protocol.RemotingCommand;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;

@Slf4j
public class NettyDecoder extends LengthFieldBasedFrameDecoder {

    public NettyDecoder(int frameMaxLength) {
        super(frameMaxLength, 0, Integer.BYTES, 0, 0);
    }

    @Override
    public Object decode(ChannelHandlerContext ctx, ByteBuf in) {
        ByteBuf frame = null;
        try {
            frame = (ByteBuf) super.decode(ctx, in);
            if (null == frame) {
                return null;
            }

            ByteBuffer byteBuffer = frame.nioBuffer();
            RemotingCommand command = RemotingCoder.decode(byteBuffer);
            return command;
        } catch (Exception e) {
            log.error("decode exception, " + NettyUtils.parseChannelRemoteAddr(ctx.channel()), e);
            NettyUtils.closeChannel(ctx.channel());
        } finally {
            if (null != frame) {
                frame.release();
            }
        }
        return null;
    }
}
