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

import okhttp3.OkHttpClient;

public class OkHttpClientSingleton {
    static private OkHttpClient Singleton;

    static public final OkHttpClient getOkHttpClient() {
        if (Singleton == null) {
            synchronized (com.m2049r.xmrwallet.util.OkHttpClientSingleton.class) {
                if (Singleton == null) {
                    Singleton = new OkHttpClient();
                }
            }
        }
        return Singleton;
    }
}
