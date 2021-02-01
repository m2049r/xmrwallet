/*
 * Copyright (c) 2018-2020 m2049r et al.
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

import android.app.KeyguardManager;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;

public class FingerprintHelper {

    public static boolean isDeviceSupported(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }

        FingerprintManager fingerprintManager = context.getSystemService(FingerprintManager.class);
        KeyguardManager keyguardManager = context.getSystemService(KeyguardManager.class);

        return (keyguardManager != null) && (fingerprintManager != null) &&
                keyguardManager.isKeyguardSecure() &&
                fingerprintManager.isHardwareDetected() &&
                fingerprintManager.hasEnrolledFingerprints();
    }

    public static boolean isFingerPassValid(Context context, String wallet) {
        try {
            KeyStoreHelper.loadWalletUserPass(context, wallet);
            return true;
        } catch (KeyStoreHelper.BrokenPasswordStoreException ex) {
            return false;
        }
    }

    public static void authenticate(Context context, CancellationSignal cancelSignal,
                                    FingerprintManager.AuthenticationCallback callback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        FingerprintManager manager = context.getSystemService(FingerprintManager.class);
        if (manager != null) {
            manager.authenticate(null, cancelSignal, 0, callback, null);
        }
    }
}
