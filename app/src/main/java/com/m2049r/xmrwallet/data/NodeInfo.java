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

import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.widget.TextView;

import com.m2049r.levin.scanner.LevinPeer;
import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.util.NetCipherHelper;
import com.m2049r.xmrwallet.util.NetCipherHelper.Request;
import com.m2049r.xmrwallet.util.NodePinger;
import com.m2049r.xmrwallet.util.ThemeHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Calendar;
import java.util.Comparator;

import lombok.Getter;
import lombok.Setter;
import okhttp3.HttpUrl;
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
            levinSocketAddress = new InetSocketAddress(hostAddress.getHostAddress(), getDefaultLevinPort());
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

    private static final int HTTP_TIMEOUT = 1000; //ms
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

    private Request rpcServiceRequest(int port) {
        final HttpUrl url = new HttpUrl.Builder()
                .scheme("http")
                .host(getHost())
                .port(port)
                .addPathSegment("json_rpc")
                .build();
        final String json = "{\"jsonrpc\":\"2.0\",\"id\":\"0\",\"method\":\"getlastblockheader\"}";
        return new Request(url, json, getUsername(), getPassword());
    }

    private boolean testRpcService(int port) {
        Timber.d("Testing %s", toNodeString());
        clear();
        if (hostAddress.isOnion() && !NetCipherHelper.isTor()) {
            tested = true; // sortof
            responseCode = 418; // I'm a teapot - or I need an Onion - who knows
            return false; // autofail
        }
        try {
            long ta = System.nanoTime();
            try (Response response = rpcServiceRequest(port).execute()) {
                Timber.d("%s: %s", response.code(), response.request().url());
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
            Timber.d("EX: %s", ex.getMessage()); //TODO: do something here (show error?)
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

    static public final int STALE_NODE_HOURS = 2;

    public void showInfo(TextView view, String info, boolean isError) {
        final Context ctx = view.getContext();
        final Spanned text = Html.fromHtml(ctx.getString(R.string.status,
                Integer.toHexString(ThemeHelper.getThemedColor(ctx, R.attr.positiveColor) & 0xFFFFFF),
                Integer.toHexString(ThemeHelper.getThemedColor(ctx, android.R.attr.colorBackground) & 0xFFFFFF),
                (hostAddress.isOnion() ? "&nbsp;.onion&nbsp;&nbsp;" : ""), " " + info));
        view.setText(text);
        if (isError)
            view.setTextColor(ThemeHelper.getThemedColor(ctx, androidx.appcompat.R.attr.colorError));
        else
            view.setTextColor(ThemeHelper.getThemedColor(ctx, android.R.attr.textColorSecondary));
    }

    public void showInfo(TextView view) {
        if (!isTested()) {
            showInfo(view, "", false);
            return;
        }
        final Context ctx = view.getContext();
        final long now = Calendar.getInstance().getTimeInMillis() / 1000;
        final long secs = (now - timestamp);
        final long mins = secs / 60;
        final long hours = mins / 60;
        final long days = hours / 24;
        String info;
        if (mins < 2) {
            info = ctx.getString(R.string.node_updated_now, secs);
        } else if (hours < 2) {
            info = ctx.getString(R.string.node_updated_mins, mins);
        } else if (days < 2) {
            info = ctx.getString(R.string.node_updated_hours, hours);
        } else {
            info = ctx.getString(R.string.node_updated_days, days);
        }
        showInfo(view, info, hours >= STALE_NODE_HOURS);
    }
}
