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

package com.m2049r.xmrwallet.data;

import com.m2049r.xmrwallet.model.NetworkType;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class WalletNode {
    private final String name;
    private final String host;
    private final int port;
    private final String user;
    private final String password;
    private final NetworkType networkType;

    public WalletNode(String walletName, String daemon, NetworkType networkType) {
        if ((daemon == null) || daemon.isEmpty())
            throw new IllegalArgumentException("daemon is empty");
        this.name = walletName;
        String daemonAddress;
        String a[] = daemon.split("@");
        if (a.length == 1) { // no credentials
            daemonAddress = a[0];
            user = "";
            password = "";
        } else if (a.length == 2) { // credentials
            String userPassword[] = a[0].split(":");
            if (userPassword.length != 2)
                throw new IllegalArgumentException("User:Password invalid");
            user = userPassword[0];
            if (!user.isEmpty()) {
                password = userPassword[1];
            } else {
                password = "";
            }
            daemonAddress = a[1];
        } else {
            throw new IllegalArgumentException("Too many @");
        }

        String da[] = daemonAddress.split(":");
        if ((da.length > 2) || (da.length < 1))
            throw new IllegalArgumentException("Too many ':' or too few");
        host = da[0];
        if (da.length == 2) {
            try {
                port = Integer.parseInt(da[1]);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Port not numeric");
            }
        } else {
            switch (networkType) {
                case NetworkType_Mainnet:
                    port = 18081;
                    break;
                case NetworkType_Testnet:
                    port = 28081;
                    break;
                case NetworkType_Stagenet:
                    port = 38081;
                    break;
                default:
                    port = 0;
            }
        }
        this.networkType = networkType;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return host + ":" + port;
    }

    public String getUsername() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public SocketAddress getSocketAddress() {
        return new InetSocketAddress(host, port);
    }

    public boolean isValid() {
        return !host.isEmpty();
    }
}