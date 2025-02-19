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

package com.baidu.hugegraph.computer.core.compute.input;

import java.io.File;
import java.io.IOException;

import com.baidu.hugegraph.computer.core.common.ComputerContext;
import com.baidu.hugegraph.computer.core.common.exception.ComputerException;
import com.baidu.hugegraph.computer.core.graph.properties.Properties;
import com.baidu.hugegraph.computer.core.graph.vertex.Vertex;
import com.baidu.hugegraph.computer.core.io.BufferedFileInput;
import com.baidu.hugegraph.computer.core.io.RandomAccessInput;
import com.baidu.hugegraph.computer.core.io.StreamGraphInput;
import java.nio.ByteBuffer;

public class VertexInput {

    private final long vertexCount;
    private long readCount;
    private RandomAccessInput input;
    private final Vertex vertex;
    private ReusablePointer idPointer;
    private final ReusablePointer valuePointer;
    private final Properties properties;
    private final File vertexFile; 
    private boolean useFixLength;
    private final ComputerContext context;
    private int idBytes;

    public VertexInput(ComputerContext context,
                       File vertexFile,
                       long vertexCount) {
        this.vertexFile = vertexFile;
        this.vertexCount = vertexCount;
        this.readCount = 0L;
        this.vertex = context.graphFactory().createVertex();
        this.idPointer = new ReusablePointer();
        this.valuePointer = new ReusablePointer();
        this.properties = context.graphFactory().createProperties();
        this.readCount = 0;
        this.useFixLength = false;
        this.context = context;
        this.idBytes = 8;
    }

    public void init() throws IOException {
        this.input = new BufferedFileInput(this.vertexFile);
    }

    public void close() throws IOException {
        this.input.close();
    }

    public boolean hasNext() {
        return this.readCount < this.vertexCount;
    }

    public void switchToFixLength() {
       this.useFixLength = true;      
    }

    public void readIdBytes() {
        try {
            this.idBytes = this.input.readFixedInt();
        }  catch (IOException e) {
            throw new ComputerException("Can't read vertex from input '%s'",
                                        e, this.vertexFile.getAbsolutePath());
        }
    }

    public Vertex next() {
        this.readCount++;
        try {
            if (!this.useFixLength) {
                this.idPointer.read(this.input);
                this.valuePointer.read(this.input); 

                this.vertex.id(StreamGraphInput.readId(
                                this.idPointer.input()));   

                RandomAccessInput valueInput = this.valuePointer.input();
                this.vertex.label(StreamGraphInput.readLabel(valueInput));
                this.properties.read(valueInput);
                this.vertex.properties(this.properties);
            }
            else {
                byte[] bId = this.input.readBytes(this.idBytes);
                byte[] blId = new byte[8];
                for (int j = 0; j < this.idBytes; j++) {
                    int j_ = j + Long.BYTES - this.idBytes;
                    blId[j_] = bId[j];
                }
                ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES); 
                buffer.put(blId, 0, Long.BYTES);
                buffer.flip();
                Long lId = buffer.getLong();
                this.vertex.id(this.context.graphFactory().
                                            createId(lId)); 
                this.idPointer = new ReusablePointer(blId, Long.BYTES);   

                this.valuePointer.read(this.input);
                RandomAccessInput valueInput = this.valuePointer.input();
                this.vertex.label(StreamGraphInput.readLabel(valueInput));
                this.properties.read(valueInput);
                this.vertex.properties(this.properties);
            }
        } catch (IOException e) {
            throw new ComputerException("Can't read vertex from input '%s'",
                                        e, this.vertexFile.getAbsolutePath());
        }
        return this.vertex;
    }

    public ReusablePointer idPointer() {
        return this.idPointer;
    }

    public ReusablePointer valuePointer() {
        return this.valuePointer;
    }
}
