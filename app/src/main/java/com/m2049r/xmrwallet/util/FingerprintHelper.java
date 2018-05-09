package com.m2049r.xmrwallet.util;

import android.app.KeyguardManager;
import android.content.Context;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;

import java.security.KeyStore;
import java.security.KeyStoreException;

public class FingerprintHelper {

    public static boolean isDeviceSupported(Context context) {
        FingerprintManagerCompat fingerprintManager = FingerprintManagerCompat.from(context);
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);

        return keyguardManager != null &&
                keyguardManager.isKeyguardSecure() &&
                fingerprintManager.isHardwareDetected() &&
                fingerprintManager.hasEnrolledFingerprints();
    }

    public static boolean isFingerprintAuthAllowed(String wallet) throws KeyStoreException {
        KeyStore keyStore = KeyStore.getInstance(KeyStoreHelper.SecurityConstants.KEYSTORE_PROVIDER_ANDROID_KEYSTORE);
        try {
            keyStore.load(null);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not load KeyStore", ex);
        }

        return keyStore.containsAlias(KeyStoreHelper.SecurityConstants.WALLET_PASS_KEY_PREFIX + wallet);
    }

    public static void authenticate(Context context, CancellationSignal cancelSignal,
                                    FingerprintManagerCompat.AuthenticationCallback callback) {
        FingerprintManagerCompat manager = FingerprintManagerCompat.from(context);
        manager.authenticate(null, 0, cancelSignal, callback, null);
    }

}
