/*
 * Copyright 2018 m2049r
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.m2049r.aeonwallet.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.util.Base64;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.security.auth.x500.X500Principal;

import timber.log.Timber;

public class KeyStoreHelper {

    static {
        System.loadLibrary("monerujo");
    }

    public static native byte[] slowHash(byte[] data);

    static final private String RSA_ALIAS = "MonerujoRSA";

    private static String getCrazyPass(Context context, String password, int brokenVariant) {
        byte[] data = password.getBytes(StandardCharsets.UTF_8);
        byte[] sig = null;
        try {
            KeyStoreHelper.createKeys(context, RSA_ALIAS);
            sig = KeyStoreHelper.signData(RSA_ALIAS, data);
            byte[] hash = slowHash(sig);
            if (hash == null) {
                throw new IllegalStateException("Slow Hash is null!");
            }
            return CrazyPassEncoder.encode(hash);
        } catch (NoSuchProviderException | NoSuchAlgorithmException |
                InvalidAlgorithmParameterException | KeyStoreException |
                InvalidKeyException | SignatureException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static String getCrazyPass(Context context, String password) {
        return getCrazyPass(context, password, 0);
    }

    public static boolean saveWalletUserPass(@NonNull Context context, String wallet, String password) {
        String walletKeyAlias = SecurityConstants.WALLET_PASS_KEY_PREFIX + wallet;
        byte[] data = password.getBytes(StandardCharsets.UTF_8);
        try {
            KeyStoreHelper.createKeys(context, walletKeyAlias);
            byte[] encrypted = KeyStoreHelper.encrypt(walletKeyAlias, data);
            SharedPreferences.Editor e = context.getSharedPreferences(SecurityConstants.WALLET_PASS_PREFS_NAME, Context.MODE_PRIVATE).edit();
            if (encrypted == null) {
                e.remove(wallet).apply();
                return false;
            }
            e.putString(wallet, Base64.encodeToString(encrypted, Base64.DEFAULT)).apply();
            return true;
        } catch (NoSuchProviderException | NoSuchAlgorithmException |
                InvalidAlgorithmParameterException | KeyStoreException ex) {
            Timber.w(ex);
            return false;
        }
    }

    static public class BrokenPasswordStoreException extends Exception {
        BrokenPasswordStoreException() {
            super();
        }

        BrokenPasswordStoreException(Throwable cause) {
            super(cause);
        }
    }

    public static String loadWalletUserPass(@NonNull Context context, String wallet) throws BrokenPasswordStoreException {
        String walletKeyAlias = SecurityConstants.WALLET_PASS_KEY_PREFIX + wallet;
        String encoded = context.getSharedPreferences(SecurityConstants.WALLET_PASS_PREFS_NAME, Context.MODE_PRIVATE)
                .getString(wallet, "");
        if (encoded.isEmpty()) throw new BrokenPasswordStoreException();
        byte[] data = Base64.decode(encoded, Base64.DEFAULT);
        byte[] decrypted = KeyStoreHelper.decrypt(walletKeyAlias, data);
        if (decrypted == null) throw new BrokenPasswordStoreException();
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    public static void removeWalletUserPass(Context context, String wallet) {
        String walletKeyAlias = SecurityConstants.WALLET_PASS_KEY_PREFIX + wallet;
        try {
            KeyStoreHelper.deleteKeys(walletKeyAlias);
        } catch (KeyStoreException ex) {
            Timber.w(ex);
        }
        context.getSharedPreferences(SecurityConstants.WALLET_PASS_PREFS_NAME, Context.MODE_PRIVATE).edit()
                .remove(wallet).apply();
    }

    /**
     * Creates a public and private key and stores it using the Android Key
     * Store, so that only this application will be able to access the keys.
     */
    private static void createKeys(Context context, String alias) throws NoSuchProviderException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException, KeyStoreException {
        KeyStore keyStore = KeyStore.getInstance(SecurityConstants.KEYSTORE_PROVIDER_ANDROID_KEYSTORE);
        try {
            keyStore.load(null);
        } catch (IOException | CertificateException ex) {
            throw new IllegalStateException("Could not load KeySotre", ex);
        }
        if (!keyStore.containsAlias(alias)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                createKeysJBMR2(context, alias);
            } else {
                createKeysM(alias);
            }
        }
    }

    private static boolean deleteKeys(String alias) throws KeyStoreException {
        KeyStore keyStore = KeyStore.getInstance(SecurityConstants.KEYSTORE_PROVIDER_ANDROID_KEYSTORE);
        try {
            keyStore.load(null);
            keyStore.deleteEntry(alias);
            return true;
        } catch (IOException | NoSuchAlgorithmException | CertificateException ex) {
            Timber.w(ex);
            return false;
        }
    }

    public static boolean keyExists(String wallet) throws BrokenPasswordStoreException {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStoreHelper.SecurityConstants.KEYSTORE_PROVIDER_ANDROID_KEYSTORE);
            keyStore.load(null);
            return keyStore.containsAlias(KeyStoreHelper.SecurityConstants.WALLET_PASS_KEY_PREFIX + wallet);
        } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyStoreException ex) {
            throw new BrokenPasswordStoreException(ex);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private static void createKeysJBMR2(Context context, String alias) throws NoSuchProviderException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException {

        Calendar start = new GregorianCalendar();
        Calendar end = new GregorianCalendar();
        end.add(Calendar.YEAR, 300);

        KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(context)
                .setAlias(alias)
                .setSubject(new X500Principal("CN=" + alias))
                .setSerialNumber(BigInteger.valueOf(Math.abs(alias.hashCode())))
                .setStartDate(start.getTime()).setEndDate(end.getTime())
                .build();
        // defaults to 2048 bit modulus
        KeyPairGenerator kpGenerator = KeyPairGenerator.getInstance(
                SecurityConstants.TYPE_RSA,
                SecurityConstants.KEYSTORE_PROVIDER_ANDROID_KEYSTORE);
        kpGenerator.initialize(spec);
        KeyPair kp = kpGenerator.generateKeyPair();
        Timber.d("preM Keys created");
    }

    @TargetApi(Build.VERSION_CODES.M)
    private static void createKeysM(String alias) throws NoSuchProviderException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA, SecurityConstants.KEYSTORE_PROVIDER_ANDROID_KEYSTORE);
        keyPairGenerator.initialize(
                new KeyGenParameterSpec.Builder(
                        alias, KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setDigests(KeyProperties.DIGEST_SHA256)
                        .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                        .build());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        Timber.d("M Keys created");
    }

    private static KeyStore.PrivateKeyEntry getPrivateKeyEntry(String alias) {
        try {
            KeyStore ks = KeyStore
                    .getInstance(SecurityConstants.KEYSTORE_PROVIDER_ANDROID_KEYSTORE);
            ks.load(null);
            KeyStore.Entry entry = ks.getEntry(alias, null);

            if (entry == null) {
                Timber.w("No key found under alias: %s", alias);
                return null;
            }

            if (!(entry instanceof KeyStore.PrivateKeyEntry)) {
                Timber.w("Not an instance of a PrivateKeyEntry");
                return null;
            }
            return (KeyStore.PrivateKeyEntry) entry;
        } catch (IOException | NoSuchAlgorithmException | CertificateException
                | UnrecoverableEntryException | KeyStoreException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static byte[] encrypt(String alias, byte[] data) {
        try {
            PublicKey publicKey = getPrivateKeyEntry(alias).getCertificate().getPublicKey();
            Cipher cipher = Cipher.getInstance(SecurityConstants.CIPHER_RSA_ECB_PKCS1);

            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(data);
        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException
                | NoSuchAlgorithmException | NoSuchPaddingException ex) {
            Timber.e(ex);
            return null;
        }
    }

    private static byte[] decrypt(String alias, byte[] data) {
        try {
            KeyStore.PrivateKeyEntry pke = getPrivateKeyEntry(alias);
            if (pke == null) return null;
            PrivateKey privateKey = pke.getPrivateKey();
            Cipher cipher = Cipher.getInstance(SecurityConstants.CIPHER_RSA_ECB_PKCS1);

            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                IllegalBlockSizeException | BadPaddingException ex) {
            Timber.e(ex);
            return null;
        }
    }

    /**
     * Signs the data using the key pair stored in the Android Key Store. This
     * signature can be used with the data later to verify it was signed by this
     * application.
     *
     * @return The data signature generated
     */
    private static byte[] signData(String alias, byte[] data) throws NoSuchAlgorithmException,
            InvalidKeyException, SignatureException {
        KeyStore.PrivateKeyEntry pke = getPrivateKeyEntry(alias);
        if (pke == null) return null;
        PrivateKey privateKey = getPrivateKeyEntry(alias).getPrivateKey();
        Signature s = Signature.getInstance(SecurityConstants.SIGNATURE_SHA256withRSA);
        s.initSign(privateKey);
        s.update(data);
        return s.sign();
    }

    /**
     * Given some data and a signature, uses the key pair stored in the Android
     * Key Store to verify that the data was signed by this application, using
     * that key pair.
     *
     * @param data      The data to be verified.
     * @param signature The signature provided for the data.
     * @return A boolean value telling you whether the signature is valid or
     * not.
     */
    private static boolean verifyData(String alias, byte[] data, byte[] signature)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        // Make sure the signature string exists
        if (signature == null) {
            Timber.w("Invalid signature.");
            return false;
        }

        KeyStore.PrivateKeyEntry keyEntry = getPrivateKeyEntry(alias);
        Signature s = Signature.getInstance(SecurityConstants.SIGNATURE_SHA256withRSA);
        s.initVerify(keyEntry.getCertificate());
        s.update(data);
        return s.verify(signature);
    }

    public interface SecurityConstants {
        String KEYSTORE_PROVIDER_ANDROID_KEYSTORE = "AndroidKeyStore";
        String TYPE_RSA = "RSA";
        String SIGNATURE_SHA256withRSA = "SHA256withRSA";
        String CIPHER_RSA_ECB_PKCS1 = "RSA/ECB/PKCS1Padding";
        String WALLET_PASS_PREFS_NAME = "wallet";
        String WALLET_PASS_KEY_PREFIX = "walletKey-";
    }
}