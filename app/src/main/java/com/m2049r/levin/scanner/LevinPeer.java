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

package com.m2049r.levin.scanner;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class LevinPeer {
    final public InetSocketAddress socketAddress;
    final public int version;
    final public long height;
    final public String top;


    public InetSocketAddress getSocketAddress() {
        return socketAddress;
    }

    LevinPeer(InetAddress address, int port, int version, long height, String top) {
        this.socketAddress = new InetSocketAddress(address, port);
        this.version = version;
        this.height = height;
        this.top = top;
    }
}
