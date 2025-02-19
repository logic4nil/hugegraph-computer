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

package com.baidu.hugegraph.computer.algorithm.path.links;

import com.baidu.hugegraph.computer.core.common.Constants;
import com.baidu.hugegraph.computer.core.graph.value.IdList;
import com.baidu.hugegraph.computer.core.graph.value.Value;
import com.baidu.hugegraph.computer.core.graph.value.ValueType;
import com.baidu.hugegraph.computer.core.io.RandomAccessInput;
import com.baidu.hugegraph.computer.core.io.RandomAccessOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.NotSupportedException;
import org.apache.commons.collections.CollectionUtils;




public class LinksValue implements Value<LinksValue> {

    private final List<LinksValueItem> values;
    int shift = 0;

    public LinksValue() {
        this.values = new ArrayList<>();
    }

    @Override
    public ValueType valueType() {
        return ValueType.UNKNOWN;
    }

    @Override
    public void assign(Value<LinksValue> value) {
        throw new NotSupportedException();
    }

    @Override
    public Value<LinksValue> copy() {
        throw new NotSupportedException();
    }

    @Override
    public Object value() {
        throw new NotSupportedException();
    }

    @Override 
    public void parse(byte[] buffer, int offset) {
        this.shift = 0;
        int position = offset;

        this.values.clear();
        int count = buffer[position];
        position++;
        this.shift++;
        
        LinksValueItem value;
        for (int i = 0; i < count; i++) {
            value = new LinksValueItem();
            value.parse(buffer, position);
            int shifti = value.getShift();
            this.shift += shifti;
            position += shifti;
            
            this.values.add(value);
        }
    }

    @Override
    public int getShift() {
        return this.shift;
    }
    
    @Override
    public void read(RandomAccessInput in) throws IOException {
        this.values.clear();
        int count = in.readInt();
        LinksValueItem value;
        for (int i = 0; i < count; i++) {
            value = new LinksValueItem();
            value.read(in);
            this.values.add(value);
        }
    }

    @Override
    public void write(RandomAccessOutput out) throws IOException {
        out.writeInt(this.values.size());
        for (LinksValueItem value : this.values) {
            value.write(out);
        }
    }

    @Override
    public int compareTo(LinksValue o) {
        throw new NotSupportedException();
    }

    public void addValue(IdList vertexes, IdList edges) {
        this.values.add(new LinksValue.LinksValueItem(vertexes, edges));
    }

    @Override
    public String toString() {
        return this.values.toString();
    }

    @Override
    public String string() {
        return CollectionUtils.isNotEmpty(this.values) ?
               this.values.toString() :
               Constants.EMPTY_STR;
    }

    public List<LinksValueItem> values() {
        return Collections.unmodifiableList(this.values);
    }

    public int size() {
        return this.values.size();
    }

    public static class LinksValueItem implements Value<LinksValueItem> {

        private final IdList vertexes;
        private final IdList edges;
        private int shift;

        public LinksValueItem() {
            this.vertexes = new IdList();
            this.edges = new IdList();
        }

        public LinksValueItem(IdList vertexes, IdList edges) {
            this.vertexes = vertexes;
            this.edges = edges;
        }

        @Override
        public ValueType valueType() {
            return ValueType.UNKNOWN;
        }

        @Override
        public void assign(Value<LinksValueItem> value) {
            throw new NotSupportedException();
        }

        @Override
        public Value<LinksValueItem> copy() {
            throw new NotSupportedException();
        }

        @Override
        public Object value() {
            throw new NotSupportedException();
        }

        @Override 
        public void parse(byte[] buffer, int offset) {
            int position = offset;
            this.shift = 0;
            this.vertexes.parse(buffer, position);
            int shifti = this.vertexes.getShift();
            this.shift += shifti;
            position += shifti;
            this.edges.parse(buffer, position);
            shifti = this.edges.getShift();
            this.shift += shifti;
        }
    
        @Override
        public int getShift() {
            return this.shift;
        }
        
        @Override
        public void read(RandomAccessInput in) throws IOException {
            this.vertexes.read(in);
            this.edges.read(in);
        }

        @Override
        public void write(RandomAccessOutput out) throws IOException {
            this.vertexes.write(out);
            this.edges.write(out);
        }

        @Override
        public int compareTo(LinksValueItem other) {
            throw new NotSupportedException();
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("{");
            for (int i = 0; i < this.vertexes.size(); i++) {
                builder.append(this.vertexes.get(i).toString());
                if (i < this.vertexes.size() - 1) {
                    builder.append(", ");
                    builder.append(this.edges.get(i).toString());
                    builder.append(", ");
                }
            }
            builder.append("}");
            return builder.toString();
        }
    }
}
