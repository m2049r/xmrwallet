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

package com.m2049r.levin.util;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class HexHelper {

    static public String bytesToHex(byte[] data) {
        if ((data != null) && (data.length > 0))
            return String.format("%0" + (data.length * 2) + "X",
                    new BigInteger(1, data));
        else
            return "";
    }

    static public InetAddress toInetAddress(int ip) {
        try {
            String ipAddress = String.format("%d.%d.%d.%d", (ip & 0xff),
                    (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
            return InetAddress.getByName(ipAddress);
        } catch (UnknownHostException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
}
