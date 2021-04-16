/*
 * Copyright 2017 HugeGraph Authors
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.baidu.hugegraph.computer.core.network.netty;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Test;
import org.mockito.Mockito;

import com.baidu.hugegraph.computer.core.common.exception.TransportException;
import com.baidu.hugegraph.computer.core.network.MockUnDecodeMessage;
import com.baidu.hugegraph.computer.core.network.buffer.ManagedBuffer;
import com.baidu.hugegraph.computer.core.network.buffer.NettyManagedBuffer;
import com.baidu.hugegraph.computer.core.network.buffer.NioManagedBuffer;
import com.baidu.hugegraph.computer.core.network.message.AckMessage;
import com.baidu.hugegraph.computer.core.network.message.DataMessage;
import com.baidu.hugegraph.computer.core.network.message.FailMessage;
import com.baidu.hugegraph.computer.core.network.message.FinishMessage;
import com.baidu.hugegraph.computer.core.network.message.MessageType;
import com.baidu.hugegraph.computer.core.network.message.PingMessage;
import com.baidu.hugegraph.computer.core.network.message.PongMessage;
import com.baidu.hugegraph.computer.core.network.message.StartMessage;
import com.baidu.hugegraph.computer.core.network.netty.codec.FrameDecoder;
import com.baidu.hugegraph.computer.core.util.StringEncoding;
import com.baidu.hugegraph.testutil.Assert;
import com.baidu.hugegraph.testutil.Whitebox;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.embedded.EmbeddedChannel;

public class NettyEncodeDecodeHandlerTest extends AbstractNetworkTest {

    @Override
    protected void initOption() {
    }

    @Test
    public void testSendMsgWithMock() throws IOException {
        NettyTransportClient client = (NettyTransportClient) this.oneClient();
        int requestId = 99;
        int partition = 1;
        byte[] bytes = StringEncoding.encode("mock msg");
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        ManagedBuffer body = new NioManagedBuffer(buffer);
        DataMessage dataMessage = new DataMessage(MessageType.MSG, requestId,
                                                  partition, body);
        client.channel().writeAndFlush(dataMessage)
              .addListener(new ChannelFutureListenerOnWrite(clientHandler));

        Mockito.verify(clientHandler, Mockito.timeout(2000L).times(1))
               .channelActive(client.connectionId());

        Mockito.verify(serverHandler, Mockito.timeout(2000L).times(1))
               .channelActive(Mockito.any());
    }

    @Test
    public void testSendMsgWithEncoderExceptionMock() throws IOException {
        NettyTransportClient client = (NettyTransportClient) this.oneClient();
        int requestId = 99;
        int partition = 1;
        byte[] bytes = StringEncoding.encode("mock msg");
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        ManagedBuffer body = new NioManagedBuffer(buffer);
        DataMessage dataMessage = new DataMessage(null, requestId,
                                                  partition, body);
        ChannelFutureListenerOnWrite listener =
        new ChannelFutureListenerOnWrite(clientHandler);
        ChannelFutureListenerOnWrite spyListener = Mockito.spy(listener);
        client.channel().writeAndFlush(dataMessage)
              .addListener(spyListener);

        Mockito.verify(clientHandler, Mockito.timeout(3000L).times(1))
              .channelActive(Mockito.any());
        Mockito.verify(clientHandler, Mockito.timeout(3000L).times(1))
               .exceptionCaught(Mockito.any(), Mockito.any());
        Mockito.verify(spyListener, Mockito.timeout(3000L).times(1))
               .onFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testSendMsgWithDecodeException() throws IOException {
        NettyTransportClient client = (NettyTransportClient) this.oneClient();
        client.channel().writeAndFlush(new MockUnDecodeMessage());

        Mockito.verify(serverHandler, Mockito.timeout(2000L).times(1))
               .channelActive(Mockito.any());
        Mockito.verify(serverHandler, Mockito.timeout(2000L).times(1))
               .exceptionCaught(Mockito.any(), Mockito.any());
    }

    @Test
    public void testSendMsgWithFrameDecode() {
        FrameDecoder frameDecoder = new FrameDecoder();
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(frameDecoder);
        ManagedBuffer buffer = new NettyManagedBuffer(Unpooled.buffer());
        ByteBuf buf = buffer.nettyByteBuf();
        StartMessage.INSTANCE.encode(buf);
        boolean writeInbound = embeddedChannel.writeInbound(buf);
        Assert.assertTrue(writeInbound);
        Assert.assertTrue(embeddedChannel.finish());
        buffer.release();
    }

    @Test
    public void testSendMsgWithFrameDecodeMagicError() {
        FrameDecoder frameDecoder = new FrameDecoder();
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(frameDecoder);
        ManagedBuffer buffer = new NettyManagedBuffer(Unpooled.buffer());
        short magicError = 10;
        ByteBuf buf = buffer.nettyByteBuf();
        StartMessage.INSTANCE.encode(buf);
        buf.setShort(0, magicError);

        embeddedChannel.writeInbound(buf);
        Assert.assertFalse(embeddedChannel.finish());
        Assert.assertNull(embeddedChannel.readInbound());
    }

    @Test
    public void testSendMsgWithFrameDecodeVersionError() {
        FrameDecoder frameDecoder = new FrameDecoder();
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(frameDecoder);
        ManagedBuffer buffer = new NettyManagedBuffer(Unpooled.buffer());
        byte versionError = 10;
        ByteBuf buf = buffer.nettyByteBuf();
        StartMessage.INSTANCE.encode(buf);
        buf.setByte(2, versionError);

        embeddedChannel.writeInbound(buf);
        Assert.assertFalse(embeddedChannel.finish());
        Assert.assertNull(embeddedChannel.readInbound());
    }

    @Test
    public void testClientDecodeException() throws Exception {
        Mockito.doAnswer(invocationOnMock -> {
            invocationOnMock.callRealMethod();
            Channel channel = invocationOnMock.getArgument(0);
            channel.writeAndFlush(new MockUnDecodeMessage());
            return null;
        }).when(serverProtocol).initializeServerPipeline(Mockito.any(),
                                                         Mockito.any());

        NettyTransportClient client = (NettyTransportClient) this.oneClient();

        Mockito.verify(clientHandler, Mockito.timeout(5000L).times(1))
               .exceptionCaught(Mockito.any(), Mockito.any());
    }

    @Test
    public void testSendOtherMessageType() throws Exception {
        NettyTransportClient client = (NettyTransportClient) this.oneClient();

        Object listener = Whitebox.getInternalState(client, "listenerOnWrite");
        ChannelFutureListener spyListener = (ChannelFutureListenerOnWrite)
                                            Mockito.spy(listener);
        Whitebox.setInternalState(client, "listenerOnWrite", spyListener);
        int requestId = 99;
        int partition = 1;
        byte[] bytes = StringEncoding.encode("mock msg");
        ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length).put(bytes);
        // Flip to make it readable
        buffer.flip();
        DataMessage dataMessage = new DataMessage(MessageType.MSG,
                                                  requestId, partition,
                                                  new NioManagedBuffer(buffer));
        FailMessage failMsg = new FailMessage(requestId,
                                              TransportException.DEFAULT_CODE,
                                              "mock fail msg");

        Assert.assertEquals("DataMessage[messageType=MSG,sequenceNumber=99," +
                            "partition=1,hasBody=true,bodyLength=8]",
                            dataMessage.toString());

        final int times = 10;

        for (int i = 0; i < times; i++) {
            client.channel().writeAndFlush(dataMessage)
                  .addListener(spyListener);

            client.channel().writeAndFlush(new AckMessage(requestId))
                  .addListener(spyListener);

            client.channel().writeAndFlush(failMsg)
                  .addListener(spyListener);

            client.channel().writeAndFlush(PingMessage.INSTANCE)
                  .addListener(spyListener);

            client.channel().writeAndFlush(PongMessage.INSTANCE)
                  .addListener(spyListener);

            client.channel().writeAndFlush(StartMessage.INSTANCE)
                  .addListener(spyListener);

            client.channel().writeAndFlush(new FinishMessage(requestId))
                  .addListener(spyListener);
        }

        Mockito.verify(spyListener, Mockito.timeout(10000L).times(times * 7))
               .operationComplete(Mockito.any());
    }

    @Test
    public void testMessageRelease() {
        int requestId = 99;
        int partition = 1;
        byte[] bytes = StringEncoding.encode("mock msg");
        ByteBuf buf = Unpooled.directBuffer().writeBytes(bytes);
        NettyManagedBuffer managedBuffer = new NettyManagedBuffer(buf);
        DataMessage dataMessage = new DataMessage(MessageType.MSG,
                                                  requestId, partition,
                                                  managedBuffer);
        Assert.assertEquals(1, managedBuffer.referenceCount());
        dataMessage.release();
        Assert.assertEquals(0, managedBuffer.referenceCount());
    }
}