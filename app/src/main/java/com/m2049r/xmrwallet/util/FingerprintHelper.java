package com.m2049r.xmrwallet.util;

import android.app.KeyguardManager;
import android.content.Context;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;

import timber.log.Timber;

public class FingerprintHelper {

    public static boolean isDeviceSupported(Context context) {
        FingerprintManagerCompat fingerprintManager = FingerprintManagerCompat.from(context);
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);

        return keyguardManager != null &&
                keyguardManager.isKeyguardSecure() &&
                fingerprintManager.isHardwareDetected() &&
                fingerprintManager.hasEnrolledFingerprints();
    }

    public static boolean isFingerPassValid(Context context, String wallet) {
        try {
            KeyStoreHelper.loadWalletUserPass(context, wallet);
            return true;
        } catch (KeyStoreHelper.BrokenPasswordStoreException ex) {
            Timber.w(ex);
            return false;
        }
    }

    public static void authenticate(Context context, CancellationSignal cancelSignal,
                                    FingerprintManagerCompat.AuthenticationCallback callback) {
        FingerprintManagerCompat manager = FingerprintManagerCompat.from(context);
        manager.authenticate(null, 0, cancelSignal, callback, null);
    }
}
