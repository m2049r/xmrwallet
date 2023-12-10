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
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStateManagerControl;

import com.m2049r.xmrwallet.model.NetworkType;
import com.m2049r.xmrwallet.util.LocaleHelper;
import com.m2049r.xmrwallet.util.NetCipherHelper;
import com.m2049r.xmrwallet.util.NightmodeHelper;
import com.m2049r.xmrwallet.util.ServiceHelper;

import java.util.Arrays;

import timber.log.Timber;

public class XmrWalletApplication extends Application {

    @Override
    @OptIn(markerClass = FragmentStateManagerControl.class)
    public void onCreate() {
        super.onCreate();
        FragmentManager.enableNewStateManager(false);
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        NightmodeHelper.setPreferredNightmode(this);

        NetCipherHelper.createInstance(this);
    }

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(LocaleHelper.setPreferredLocale(context));
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleHelper.updateSystemDefaultLocale(configuration.getLocales().get(0));
        } else {
            LocaleHelper.updateSystemDefaultLocale(configuration.locale);
        }
        LocaleHelper.setPreferredLocale(this);
    }

    static public NetworkType getNetworkType() {
        switch (BuildConfig.FLAVOR_net) {
            case "mainnet":
                return NetworkType.NetworkType_Mainnet;
            case "stagenet":
                return NetworkType.NetworkType_Stagenet;
            case "devnet": // flavors cannot start with "test"
                return NetworkType.NetworkType_Testnet;
            default:
                throw new IllegalStateException("unknown net flavor " + BuildConfig.FLAVOR_net);
        }
    }
}
