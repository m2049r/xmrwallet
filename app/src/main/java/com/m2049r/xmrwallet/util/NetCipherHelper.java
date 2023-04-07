/*
 * Copyright (c) 2021 m2049r
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.burgstaller.okhttp.AuthenticationCacheInterceptor;
import com.burgstaller.okhttp.CachingAuthenticatorDecorator;
import com.burgstaller.okhttp.digest.CachingAuthenticator;
import com.burgstaller.okhttp.digest.Credentials;
import com.burgstaller.okhttp.digest.DigestAuthenticator;

import org.json.JSONObject;

import java.io.IOException;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import info.guardianproject.netcipher.client.StrongOkHttpClientBuilder;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import info.guardianproject.netcipher.proxy.SignatureUtils;
import info.guardianproject.netcipher.proxy.StatusCallback;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import timber.log.Timber;

@RequiredArgsConstructor
public class NetCipherHelper implements StatusCallback {
    public static final String USER_AGENT = "Monerujo/1.0";
    public static final int HTTP_TIMEOUT_CONNECT = 1000; //ms
    public static final int HTTP_TIMEOUT_READ = 2000; //ms
    public static final int HTTP_TIMEOUT_WRITE = 1000; //ms
    public static final int TOR_TIMEOUT_CONNECT = 5000; //ms
    public static final int TOR_TIMEOUT = 2000; //ms

    public interface OnStatusChangedListener {
        void connected();

        void disconnected();

        void notInstalled();

        void notEnabled();
    }

    final private Context context;
    final private OrbotHelper orbot;

    @SuppressLint("StaticFieldLeak")
    private static NetCipherHelper Instance;

    public static void createInstance(Context context) {
        if (Instance == null) {
            synchronized (NetCipherHelper.class) {
                if (Instance == null) {
                    final Context applicationContext = context.getApplicationContext();
                    Instance = new NetCipherHelper(applicationContext, OrbotHelper.get(context).statusTimeout(5000));
                }
            }
        }
    }

    public static NetCipherHelper getInstance() {
        if (Instance == null) throw new IllegalStateException("NetCipherHelper is null");
        return Instance;
    }

    private OkHttpClient client;

    private void createTorClient(Intent statusIntent) {
        String orbotStatus = statusIntent.getStringExtra(OrbotHelper.EXTRA_STATUS);
        if (orbotStatus == null) throw new IllegalStateException("status is null");
        if (!orbotStatus.equals(OrbotHelper.STATUS_ON))
            throw new IllegalStateException("Orbot is not ON");
        try {
            final OkHttpClient.Builder okBuilder = new OkHttpClient.Builder()
                    .connectTimeout(TOR_TIMEOUT_CONNECT, TimeUnit.MILLISECONDS)
                    .writeTimeout(TOR_TIMEOUT, TimeUnit.MILLISECONDS)
                    .readTimeout(TOR_TIMEOUT, TimeUnit.MILLISECONDS);
            client = new StrongOkHttpClientBuilder(context)
                    .withSocksProxy()
                    .applyTo(okBuilder, statusIntent)
                    .build();
            Helper.ALLOW_SHIFT = false; // no shifting with Tor
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void createClearnetClient() {
        try {
            client = new OkHttpClient.Builder()
                    .connectTimeout(HTTP_TIMEOUT_CONNECT, TimeUnit.MILLISECONDS)
                    .writeTimeout(HTTP_TIMEOUT_WRITE, TimeUnit.MILLISECONDS)
                    .readTimeout(HTTP_TIMEOUT_READ, TimeUnit.MILLISECONDS)
                    .build();
            Helper.ALLOW_SHIFT = true;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private OnStatusChangedListener onStatusChangedListener;

    public static void deregister() {
        getInstance().onStatusChangedListener = null;
    }

    public static void register(OnStatusChangedListener listener) {
        final NetCipherHelper me = getInstance();
        me.onStatusChangedListener = listener;

        // NOT_INSTALLED is dealt with through the callbacks
        me.orbot.removeStatusCallback(me) // make sure we are registered just once
                .addStatusCallback(me);

        // deal with  org.torproject.android.intent.action.STATUS = STARTS_DISABLED
        me.context.registerReceiver(orbotStatusReceiver, new IntentFilter(OrbotHelper.ACTION_STATUS));

        me.startTor();
    }

    // for StatusCallback
    public enum Status {
        STARTING,
        ENABLED,
        STOPPING,
        DISABLED,
        NOT_INSTALLED,
        NOT_ENABLED,
        UNKNOWN;
    }

    private Status status = Status.UNKNOWN;

    @Override
    public void onStarting() {
        Timber.d("onStarting");
        status = Status.STARTING;
    }

    @Override
    public void onEnabled(Intent statusIntent) {
        Timber.d("onEnabled");
        if (getTorPref() != Status.ENABLED) return; // do we want Tor?
        createTorClient(statusIntent);
        status = Status.ENABLED;
        if (onStatusChangedListener != null) {
            new Thread(() -> onStatusChangedListener.connected()).start();
        }
    }

    @Override
    public void onStopping() {
        Timber.d("onStopping");
        status = Status.STOPPING;
    }

    @Override
    public void onDisabled() {
        Timber.d("onDisabled");
        createClearnetClient();
        status = Status.DISABLED;
        if (onStatusChangedListener != null) {
            new Thread(() -> onStatusChangedListener.disconnected()).start();
        }
    }

    @Override
    public void onStatusTimeout() {
        Timber.d("onStatusTimeout");
        createClearnetClient();
        // (timeout does not not change the status)
        if (onStatusChangedListener != null) {
            new Thread(() -> onStatusChangedListener.disconnected()).start();
        }
        orbotInit = false; // do init() next time we try to open Tor
    }

    @Override
    public void onNotYetInstalled() {
        Timber.d("onNotYetInstalled");
        // never mind then
        orbot.removeStatusCallback(this);
        createClearnetClient();
        status = Status.NOT_INSTALLED;
        if (onStatusChangedListener != null) {
            new Thread(() -> onStatusChangedListener.notInstalled()).start();
        }
    }

    // user has not enabled background Orbot starts
    public void onNotEnabled() {
        Timber.d("onNotEnabled");
        // keep the callback in case they turn it on manually
        setTorPref(Status.DISABLED);
        createClearnetClient();
        status = Status.NOT_ENABLED;
        if (onStatusChangedListener != null) {
            new Thread(() -> onStatusChangedListener.notEnabled()).start();
        }
    }

    static public Status getStatus() {
        return getInstance().status;
    }

    public void toggle() {
        switch (getStatus()) {
            case ENABLED:
                onDisabled();
                setTorPref(Status.DISABLED);
                break;
            case DISABLED:
                setTorPref(Status.ENABLED);
                startTor();
                break;
        }
    }

    private boolean orbotInit = false;

    private void startTor() {
        if (!isOrbotInstalled()) {
            onNotYetInstalled();
        } else if (getTorPref() == Status.DISABLED) {
            onDisabled();
        } else if (!orbotInit) {
            orbotInit = orbot.init();
        } else {
            orbot.requestStart(context);
        }
    }

    // extracted from OrbotHelper
    private boolean isOrbotInstalled() {
        ArrayList<String> hashes = new ArrayList<>();
        // Tor Project signing key
        hashes.add("A4:54:B8:7A:18:47:A8:9E:D7:F5:E7:0F:BA:6B:BA:96:F3:EF:29:C2:6E:09:81:20:4F:E3:47:BF:23:1D:FD:5B");
        // f-droid.org signing key
        hashes.add("A7:02:07:92:4F:61:FF:09:37:1D:54:84:14:5C:4B:EE:77:2C:55:C1:9E:EE:23:2F:57:70:E1:82:71:F7:CB:AE");

        return null != SignatureUtils.validateBroadcastIntent(context,
                OrbotHelper.getOrbotStartIntent(context),
                hashes, false);
    }


    static public boolean hasClient() {
        return getInstance().client != null;
    }

    static public boolean isTor() {
        return getStatus() == Status.ENABLED;
    }

    static public String getProxy() {
        if (!isTor()) return "";
        final Proxy proxy = getInstance().client.proxy();
        if (proxy == null) return "";
        return proxy.address().toString().substring(1);
    }

    @ToString
    static public class Request {
        final HttpUrl url;
        final String json;
        final String username;
        final String password;

        public Request(final HttpUrl url, final String json, final String username, final String password) {
            this.url = url;
            this.json = json;
            this.username = username;
            this.password = password;
        }

        public Request(final HttpUrl url, final JSONObject json) {
            this(url, json == null ? null : json.toString(), null, null);
        }

        public Request(final HttpUrl url) {
            this(url, null, null, null);
        }

        public void enqueue(Callback callback) {
            newCall().enqueue(callback);
        }

        public Response execute() throws IOException {
            return newCall().execute();
        }

        private Call newCall() {
            return getClient().newCall(getRequest());
        }

        private OkHttpClient getClient() {
            if (mockClient != null) return mockClient; // Unit-test mode
            final OkHttpClient client = getInstance().client;
            if ((username != null) && (!username.isEmpty())) {
                final DigestAuthenticator authenticator = new DigestAuthenticator(new Credentials(username, password));
                final Map<String, CachingAuthenticator> authCache = new ConcurrentHashMap<>();
                return client.newBuilder()
                        .authenticator(new CachingAuthenticatorDecorator(authenticator, authCache))
                        .addInterceptor(new AuthenticationCacheInterceptor(authCache))
                        .build();
                // TODO: maybe cache & reuse the client for these credentials?
            } else {
                return client;
            }
        }

        private okhttp3.Request getRequest() {
            final okhttp3.Request.Builder builder =
                    new okhttp3.Request.Builder()
                            .url(url)
                            .header("User-Agent", USER_AGENT);
            if (json != null) {
                builder.post(RequestBody.create(json, MediaType.parse("application/json")));
            } else {
                builder.get();
            }
            return builder.build();
        }

        // for unit tests only
        static public OkHttpClient mockClient = null;
    }

    private static final String PREFS_NAME = "tor";
    private static final String PREFS_STATUS = "status";
    private Status currentPref = Status.UNKNOWN;

    private Status getTorPref() {
        if (currentPref != Status.UNKNOWN) return currentPref;
        currentPref = Status.valueOf(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(PREFS_STATUS, "DISABLED"));
        return currentPref;
    }

    private void setTorPref(Status status) {
        if (getTorPref() == status) return; // no change
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(PREFS_STATUS, status.name())
                .apply();
        currentPref = status;
    }

    private static final BroadcastReceiver orbotStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Timber.d("%s/%s", intent.getAction(), intent.getStringExtra(OrbotHelper.EXTRA_STATUS));
            if (OrbotHelper.ACTION_STATUS.equals(intent.getAction())) {
                if (OrbotHelper.STATUS_STARTS_DISABLED.equals(intent.getStringExtra(OrbotHelper.EXTRA_STATUS))) {
                    getInstance().onNotEnabled();
                }
            }
        }
    };

    public void installOrbot(Activity host) {
        host.startActivity(OrbotHelper.getOrbotInstallIntent(context));
    }
}
