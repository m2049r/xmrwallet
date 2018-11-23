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
import com.m2049r.xmrwallet.model.WalletManager;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;

import timber.log.Timber;

public class Node {
    static public final String MAINNET = "mainnet";
    static public final String STAGENET = "stagenet";
    static public final String TESTNET = "testnet";

    private String name = null;
    final private NetworkType networkType;
    InetAddress hostAddress;
    private String host;
    int rpcPort = 0;
    private int levinPort = 0;
    private String username = "";
    private String password = "";
    private boolean favourite = false;

    @Override
    public int hashCode() {
        return hostAddress.hashCode();
    }

    // Nodes are equal if they are the same host address & are on the same network
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Node)) return false;
        final Node anotherNode = (Node) other;
        return (hostAddress.equals(anotherNode.hostAddress) && (networkType == anotherNode.networkType));
    }

    static public Node fromString(String nodeString) {
        try {
            return new Node(nodeString);
        } catch (IllegalArgumentException ex) {
            Timber.w(ex);
            return null;
        }
    }

    Node(String nodeString) {
        if ((nodeString == null) || nodeString.isEmpty())
            throw new IllegalArgumentException("daemon is empty");
        String daemonAddress;
        String a[] = nodeString.split("@");
        if (a.length == 1) { // no credentials
            daemonAddress = a[0];
            username = "";
            password = "";
        } else if (a.length == 2) { // credentials
            String userPassword[] = a[0].split(":");
            if (userPassword.length != 2)
                throw new IllegalArgumentException("User:Password invalid");
            username = userPassword[0];
            if (!username.isEmpty()) {
                password = userPassword[1];
            } else {
                password = "";
            }
            daemonAddress = a[1];
        } else {
            throw new IllegalArgumentException("Too many @");
        }

        String daParts[] = daemonAddress.split("/");
        if ((daParts.length > 3) || (daParts.length < 1))
            throw new IllegalArgumentException("Too many '/' or too few");

        daemonAddress = daParts[0];
        String da[] = daemonAddress.split(":");
        if ((da.length > 2) || (da.length < 1))
            throw new IllegalArgumentException("Too many ':' or too few");
        String host = da[0];

        if (daParts.length == 1) {
            networkType = NetworkType.NetworkType_Mainnet;
        } else {
            switch (daParts[1]) {
                case MAINNET:
                    networkType = NetworkType.NetworkType_Mainnet;
                    break;
                case STAGENET:
                    networkType = NetworkType.NetworkType_Stagenet;
                    break;
                case TESTNET:
                    networkType = NetworkType.NetworkType_Testnet;
                    break;
                default:
                    throw new IllegalArgumentException("invalid net: " + daParts[1]);
            }
        }
        if (networkType != WalletManager.getInstance().getNetworkType())
            throw new IllegalArgumentException("wrong net: " + networkType);

        String name = host;
        if (daParts.length == 3) {
            try {
                name = URLDecoder.decode(daParts[2], "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                Timber.w(ex); // if we can't encode it, we don't use it
            }
        }
        this.name = name;

        int port;
        if (da.length == 2) {
            try {
                port = Integer.parseInt(da[1]);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Port not numeric");
            }
        } else {
            port = getDefaultRpcPort();
        }
        try {
            setHost(host);
        } catch (UnknownHostException ex) {
            throw new IllegalArgumentException("cannot resolve host " + host);
        }
        this.rpcPort = port;
        this.levinPort = getDefaultLevinPort();
    }

    public String toNodeString() {
        return toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!username.isEmpty() && !password.isEmpty()) {
            sb.append(username).append(":").append(password).append("@");
        }
        sb.append(host).append(":").append(rpcPort);
        sb.append("/");
        switch (networkType) {
            case NetworkType_Mainnet:
                sb.append(MAINNET);
                break;
            case NetworkType_Stagenet:
                sb.append(STAGENET);
                break;
            case NetworkType_Testnet:
                sb.append(TESTNET);
                break;
        }
        if (name != null)
            try {
                sb.append("/").append(URLEncoder.encode(name, "UTF-8"));
            } catch (UnsupportedEncodingException ex) {
                Timber.w(ex); // if we can't encode it, we don't store it
            }
        return sb.toString();
    }

    public Node() {
        this.networkType = WalletManager.getInstance().getNetworkType();
    }

    // constructor used for created nodes from retrieved peer lists
    public Node(InetSocketAddress socketAddress) {
        this();
        this.hostAddress = socketAddress.getAddress();
        this.host = socketAddress.getHostString();
        this.rpcPort = 0; // unknown
        this.levinPort = socketAddress.getPort();
        this.username = "";
        this.password = "";
        //this.name = socketAddress.getHostName(); // triggers DNS so we don't do it by default
    }

    public String getAddress() {
        return getHostAddress() + ":" + rpcPort;
    }

    public String getHostAddress() {
        return hostAddress.getHostAddress();
    }

    public String getHost() {
        return host;
    }

    public int getRpcPort() {
        return rpcPort;
    }

    public void setHost(String host) throws UnknownHostException {
        if ((host == null) || (host.isEmpty()))
            throw new UnknownHostException("loopback not supported (yet?)");
        this.host = host;
        this.hostAddress = InetAddress.getByName(host);
    }

    public void setUsername(String user) {
        username = user;
    }

    public void setPassword(String pass) {
        password = pass;
    }

    public void setRpcPort(int port) {
        this.rpcPort = port;
    }

    public void setName() {
        if (name == null)
            this.name = hostAddress.getHostName();
    }

    public void setName(String name) {
        if ((name == null) || (name.isEmpty()))
            this.name = hostAddress.getHostName();
        else
            this.name = name;
    }

    public String getName() {
        return name;
    }

    public NetworkType getNetworkType() {
        return networkType;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isFavourite() {
        return favourite;
    }

    public void setFavourite(boolean favourite) {
        this.favourite = favourite;
    }

    public void toggleFavourite() {
        favourite = !favourite;
    }

    public Node(Node anotherNode) {
        networkType = anotherNode.networkType;
        overwriteWith(anotherNode);
    }

    public void overwriteWith(Node anotherNode) {
        if (networkType != anotherNode.networkType)
            throw new IllegalStateException("network types do not match");
        name = anotherNode.name;
        hostAddress = anotherNode.hostAddress;
        host = anotherNode.host;
        rpcPort = anotherNode.rpcPort;
        levinPort = anotherNode.levinPort;
        username = anotherNode.username;
        password = anotherNode.password;
        favourite = anotherNode.favourite;
    }

    static private int DEFAULT_LEVIN_PORT = 0;

    // every node knows its network, but they are all the same
    static public int getDefaultLevinPort() {
        if (DEFAULT_LEVIN_PORT > 0) return DEFAULT_LEVIN_PORT;
        switch (WalletManager.getInstance().getNetworkType()) {
            case NetworkType_Mainnet:
                DEFAULT_LEVIN_PORT = 18080;
                break;
            case NetworkType_Testnet:
                DEFAULT_LEVIN_PORT = 28080;
                break;
            case NetworkType_Stagenet:
                DEFAULT_LEVIN_PORT = 38080;
                break;
            default:
                throw new IllegalStateException("unsupported net " + WalletManager.getInstance().getNetworkType());
        }
        return DEFAULT_LEVIN_PORT;
    }

    static private int DEFAULT_RPC_PORT = 0;

    // every node knows its network, but they are all the same
    static public int getDefaultRpcPort() {
        if (DEFAULT_RPC_PORT > 0) return DEFAULT_RPC_PORT;
        switch (WalletManager.getInstance().getNetworkType()) {
            case NetworkType_Mainnet:
                DEFAULT_RPC_PORT = 18081;
                break;
            case NetworkType_Testnet:
                DEFAULT_RPC_PORT = 28081;
                break;
            case NetworkType_Stagenet:
                DEFAULT_RPC_PORT = 38081;
                break;
            default:
                throw new IllegalStateException("unsupported net " + WalletManager.getInstance().getNetworkType());
        }
        return DEFAULT_RPC_PORT;
    }
}
