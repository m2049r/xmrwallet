/*
 * Copyright (c) 2018 m2049r
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.m2049r.xmrwallet.util;

import com.m2049r.xmrwallet.data.NodeInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class NodePinger {
    static final public int NUM_THREADS = 10;
    static final public long MAX_TIME = 5L; // seconds

    public interface Listener {
        void publish(NodeInfo node);
    }

    static public void execute(Collection<NodeInfo> nodes, final Listener listener) {
        final ExecutorService exeService = Executors.newFixedThreadPool(NUM_THREADS);
        List<Callable<Boolean>> taskList = new ArrayList<>();
        for (NodeInfo node : nodes) {
            taskList.add(() -> node.testRpcService(listener));
        }

        try {
            exeService.invokeAll(taskList, MAX_TIME, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Timber.w(ex);
        }
        exeService.shutdownNow();
    }
}
