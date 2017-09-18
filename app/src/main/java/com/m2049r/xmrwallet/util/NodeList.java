/*
 * Copyright (c) 2017 m2049r
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

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NodeList {
    static private final String TAG = "NodeList";
    static public final int MAX_SIZE = 5;

    List<String> nodes = new ArrayList<>();

    public List<String> getNodes() {
        return nodes;
    }

    public void setRecent(String aNode) {
        if (aNode.trim().isEmpty()) return;
        boolean found = false;
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).equals(aNode)) { // node is already in the list => move it to top
                nodes.remove(i);
                found = true;
                break;
            }
        }
        if (!found) {
            if (nodes.size() > MAX_SIZE) {
                nodes.remove(nodes.size() - 1); // drop last one
            }
        }
        nodes.add(0, aNode);
    }

    public NodeList(String aString) {
        String[] newNodes = aString.split(";");
        nodes.addAll(Arrays.asList(newNodes));
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (String node : this.nodes) {
            sb.append(node).append(";");
        }
        return sb.toString();
    }
}
