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

import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class OkHttpHelper {
    static private OkHttpClient Singleton;

    static public OkHttpClient getOkHttpClient() {
        if (Singleton == null) {
            synchronized (OkHttpHelper.class) {
                if (Singleton == null) {
                    Singleton = new OkHttpClient();
                }
            }
        }
        return Singleton;
    }

    public static final int HTTP_TIMEOUT = 1000; //ms

    static private OkHttpClient EagerSingleton;

    static public OkHttpClient getEagerClient() {
        if (EagerSingleton == null) {
            synchronized (OkHttpHelper.class) {
                if (EagerSingleton == null) {
                    EagerSingleton = new OkHttpClient.Builder()
                            .connectTimeout(HTTP_TIMEOUT, TimeUnit.MILLISECONDS)
                            .writeTimeout(HTTP_TIMEOUT, TimeUnit.MILLISECONDS)
                            .readTimeout(HTTP_TIMEOUT, TimeUnit.MILLISECONDS)
                            .build();
                }
            }
        }
        return EagerSingleton;
    }

    static final public String USER_AGENT = "Monerujo/1.0";

    static public Request getPostRequest(HttpUrl url, RequestBody requestBody) {
        return new Request.Builder().url(url).post(requestBody)
                .header("User-Agent", USER_AGENT)
                .build();
    }

    static public Request getGetRequest(HttpUrl url) {
        return new Request.Builder().url(url).get()
                .header("User-Agent", USER_AGENT)
                .build();
    }
}
