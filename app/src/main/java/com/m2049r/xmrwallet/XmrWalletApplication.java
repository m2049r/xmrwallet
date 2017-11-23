package com.m2049r.xmrwallet;


import android.app.Application;

import timber.log.Timber;

public class XmrWalletApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }
}
