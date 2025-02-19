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

package com.baidu.hugegraph.computer.algorithm.path.subgraph;

import java.util.ArrayList;
import java.util.List;

import com.baidu.hugegraph.computer.core.graph.value.IdListList;
import com.baidu.hugegraph.computer.core.output.hg.HugeOutput;
import com.baidu.hugegraph.structure.constant.WriteType;
import com.baidu.hugegraph.structure.graph.Vertex;

public class SubGraphMatchHugeOutput extends HugeOutput {

    @Override
    public void prepareSchema() {
        this.client().schema().propertyKey(this.name())
            .asText()
            .writeType(WriteType.OLAP_COMMON)
            .valueList()
            .ifNotExist()
            .create();
    }

    @Override
    public Vertex constructHugeVertex(
           com.baidu.hugegraph.computer.core.graph.vertex.Vertex vertex) {
        com.baidu.hugegraph.structure.graph.Vertex hugeVertex =
                new com.baidu.hugegraph.structure.graph.Vertex(null);
        hugeVertex.id(vertex.id().asObject());

        SubGraphMatchValue value = vertex.value();
        IdListList res = value.res();

        List<String> propValue = new ArrayList<>();
        for (int i = 0; i < res.size(); i++) {
            propValue.add(res.values().get(i).toString());
        }

        hugeVertex.property(this.name(), propValue);
        return hugeVertex;
    }
}
