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
