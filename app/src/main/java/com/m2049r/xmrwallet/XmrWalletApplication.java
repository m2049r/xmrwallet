/*
 * Copyright (c) 2017 m2049r et al.
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

package com.m2049r.xmrwallet;


import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;

import com.m2049r.xmrwallet.model.NetworkType;
import com.m2049r.xmrwallet.util.LocaleHelper;

import timber.log.Timber;

public class XmrWalletApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(LocaleHelper.setLocale(context, LocaleHelper.getLocale(context)));
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        LocaleHelper.updateSystemDefaultLocale(configuration.locale);
        LocaleHelper.setLocale(XmrWalletApplication.this, LocaleHelper.getLocale(XmrWalletApplication.this));
    }

    static public NetworkType getNetworkType() {
        switch (BuildConfig.FLAVOR_net) {
            case "mainnet":
                return NetworkType.NetworkType_Mainnet;
            case "stagenet":
                return NetworkType.NetworkType_Stagenet;
            case "testnet":
                return NetworkType.NetworkType_Testnet;
            default:
                throw new IllegalStateException("unknown net flavor " + BuildConfig.FLAVOR_net);
        }
    }
}
