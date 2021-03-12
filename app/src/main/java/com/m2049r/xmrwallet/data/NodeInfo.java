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

import com.burgstaller.okhttp.AuthenticationCacheInterceptor;
import com.burgstaller.okhttp.CachingAuthenticatorDecorator;
import com.burgstaller.okhttp.digest.CachingAuthenticator;
import com.burgstaller.okhttp.digest.Credentials;
import com.burgstaller.okhttp.digest.DigestAuthenticator;
import com.m2049r.levin.scanner.LevinPeer;
import com.m2049r.xmrwallet.util.NodePinger;
import com.m2049r.xmrwallet.util.OkHttpHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.Setter;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import timber.log.Timber;

public class NodeInfo extends Node {
    final static public int MIN_MAJOR_VERSION = 14;
    final static public String RPC_VERSION = "2.0";

    @Getter
    private long height = 0;
    @Getter
    private long timestamp = 0;
    @Getter
    private int majorVersion = 0;
    @Getter
    private double responseTime = Double.MAX_VALUE;
    @Getter
    private int responseCode = 0;
    @Getter
    private boolean tested = false;
    @Getter
    @Setter
    private boolean selecting = false;

    public void clear() {
        height = 0;
        majorVersion = 0;
        responseTime = Double.MAX_VALUE;
        responseCode = 0;
        timestamp = 0;
        tested = false;
    }

    static public NodeInfo fromString(String nodeString) {
        try {
            return new NodeInfo(nodeString);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public NodeInfo(NodeInfo anotherNode) {
        super(anotherNode);
        overwriteWith(anotherNode);
    }

    private SocketAddress levinSocketAddress = null;

    synchronized public SocketAddress getLevinSocketAddress() {
        if (levinSocketAddress == null) {
            // use default peer port if not set - very few peers use nonstandard port
            levinSocketAddress = new InetSocketAddress(hostAddress, getDefaultLevinPort());
        }
        return levinSocketAddress;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other);
    }

    public NodeInfo(String nodeString) {
        super(nodeString);
    }

    public NodeInfo(LevinPeer levinPeer) {
        super(levinPeer.getSocketAddress());
    }

    public NodeInfo(InetSocketAddress address) {
        super(address);
    }

    public NodeInfo() {
        super();
    }

    public boolean isSuccessful() {
        return (responseCode >= 200) && (responseCode < 300);
    }

    public boolean isUnauthorized() {
        return responseCode == HttpURLConnection.HTTP_UNAUTHORIZED;
    }

    public boolean isValid() {
        return isSuccessful() && (majorVersion >= MIN_MAJOR_VERSION) && (responseTime < Double.MAX_VALUE);
    }

    static public Comparator<NodeInfo> BestNodeComparator = (o1, o2) -> {
        if (o1.isValid()) {
            if (o2.isValid()) { // both are valid
                // higher node wins
                int heightDiff = (int) (o2.height - o1.height);
                if (heightDiff != 0)
                    return heightDiff;
                // if they are equal, faster node wins
                return (int) Math.signum(o1.responseTime - o2.responseTime);
            } else {
                return -1;
            }
        } else {
            return 1;
        }
    };

    public void overwriteWith(NodeInfo anotherNode) {
        super.overwriteWith(anotherNode);
        height = anotherNode.height;
        timestamp = anotherNode.timestamp;
        majorVersion = anotherNode.majorVersion;
        responseTime = anotherNode.responseTime;
        responseCode = anotherNode.responseCode;
    }

    public String toNodeString() {
        return super.toString();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append("?rc=").append(responseCode);
        sb.append("?v=").append(majorVersion);
        sb.append("&h=").append(height);
        sb.append("&ts=").append(timestamp);
        if (responseTime < Double.MAX_VALUE) {
            sb.append("&t=").append(responseTime).append("ms");
        }
        return sb.toString();
    }

    private static final int HTTP_TIMEOUT = OkHttpHelper.HTTP_TIMEOUT;
    public static final double PING_GOOD = HTTP_TIMEOUT / 3.0; //ms
    public static final double PING_MEDIUM = 2 * PING_GOOD; //ms
    public static final double PING_BAD = HTTP_TIMEOUT;

    public boolean testRpcService() {
        return testRpcService(rpcPort);
    }

    public boolean testRpcService(NodePinger.Listener listener) {
        boolean result = testRpcService(rpcPort);
        if (listener != null)
            listener.publish(this);
        return result;
    }

    private boolean testRpcService(int port) {
        Timber.d("Testing %s", toNodeString());
        clear();
        try {
            OkHttpClient client = OkHttpHelper.getEagerClient();
            if (!getUsername().isEmpty()) {
                final DigestAuthenticator authenticator =
                        new DigestAuthenticator(new Credentials(getUsername(), getPassword()));
                final Map<String, CachingAuthenticator> authCache = new ConcurrentHashMap<>();
                client = client.newBuilder()
                        .authenticator(new CachingAuthenticatorDecorator(authenticator, authCache))
                        .addInterceptor(new AuthenticationCacheInterceptor(authCache))
                        .build();
            }
            HttpUrl url = new HttpUrl.Builder()
                    .scheme("http")
                    .host(getHostAddress())
                    .port(port)
                    .addPathSegment("json_rpc")
                    .build();
            final RequestBody reqBody = RequestBody
                    .create(MediaType.parse("application/json"),
                            "{\"jsonrpc\":\"2.0\",\"id\":\"0\",\"method\":\"getlastblockheader\"}");
            Request request = OkHttpHelper.getPostRequest(url, reqBody);
            long ta = System.nanoTime();
            try (Response response = client.newCall(request).execute()) {
                responseTime = (System.nanoTime() - ta) / 1000000.0;
                responseCode = response.code();
                if (response.isSuccessful()) {
                    ResponseBody respBody = response.body(); // closed through Response object
                    if ((respBody != null) && (respBody.contentLength() < 2000)) { // sanity check
                        final JSONObject json = new JSONObject(respBody.string());
                        String rpcVersion = json.getString("jsonrpc");
                        if (!RPC_VERSION.equals(rpcVersion))
                            return false;
                        final JSONObject result = json.getJSONObject("result");
                        if (!result.has("credits")) // introduced in monero v0.15.0
                            return false;
                        final JSONObject header = result.getJSONObject("block_header");
                        height = header.getLong("height");
                        timestamp = header.getLong("timestamp");
                        majorVersion = header.getInt("major_version");
                        return true; // success
                    }
                }
            }
        } catch (IOException | JSONException ex) {
            Timber.d(ex);
        } finally {
            tested = true;
        }
        return false;
    }

    static final private int[] TEST_PORTS = {18089}; // check only opt-in port

    public boolean findRpcService() {
        // if already have an rpcPort, use that
        if (rpcPort > 0) return testRpcService(rpcPort);
        // otherwise try to find one
        for (int port : TEST_PORTS) {
            if (testRpcService(port)) { // found a service
                this.rpcPort = port;
                return true;
            }
        }
        return false;
    }
}
