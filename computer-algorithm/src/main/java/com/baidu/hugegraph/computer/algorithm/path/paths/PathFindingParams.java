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

package com.baidu.hugegraph.computer.algorithm.path.paths;

import com.baidu.hugegraph.computer.algorithm.AlgorithmParams;
import com.baidu.hugegraph.computer.core.config.ComputerOptions;
import com.baidu.hugegraph.computer.core.graph.value.DoubleValue;
import com.baidu.hugegraph.computer.core.combiner.ValueMinCombiner;
import java.util.Map;

public class PathFindingParams implements AlgorithmParams {

    @Override
    public void setAlgorithmParameters(Map<String, String> params) {
        this.setIfAbsent(params, ComputerOptions.WORKER_COMPUTATION_CLASS,
                         PathFinding.class.getName());
        this.setIfAbsent(params, ComputerOptions.ALGORITHM_MESSAGE_CLASS,
                         DoubleValue.class.getName());
        this.setIfAbsent(params, ComputerOptions.ALGORITHM_RESULT_CLASS,
                         DoubleValue.class.getName());
        this.setIfAbsent(params, ComputerOptions.WORKER_COMBINER_CLASS,
                         ValueMinCombiner.class.getName());
        this.setIfAbsent(params, ComputerOptions.OUTPUT_CLASS,
                         PathFindingOutput.class.getName());
        this.setIfAbsent(params,
                         ComputerOptions.VERTEX_WITH_EDGES_BOTHDIRECTION.name(),
                         Boolean.TRUE.toString());
        this.setIfAbsent(params,
                         PathFinding.OPTION_PATHFINDING_TARGET,
                         "A");
    }
}