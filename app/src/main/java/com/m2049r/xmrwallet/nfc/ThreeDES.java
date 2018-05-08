package com.m2049r.xmrwallet.nfc;


import java.security.Key;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;

/**
 * 3DES
 */
public class ThreeDES {


    public static byte[] secretKey = {0x4A, 0x35, (byte) 0xB5, (byte) 0xD5, 0x45, 0x41, 0x51, 0x52
            , 0x21, 0x40, 0x51, 0x53, 0x55, 0x40, 0x58, 0x47
            , 0x4A, 0x35, (byte)0xB5, (byte)0xD5, 0x45, 0x41, 0x51, 0x52};    //default key


    private final static byte[] iv = {0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
    private final static String encoding = "utf-8";

    static{
        Security.addProvider(new com.sun.crypto.provider.SunJCE());
    }
    /**
     * 3DES ENCODE
     *
     * @param plainText
     * @param ivs
     * @return
     * @throws Exception
     */
    public static byte[] encode(byte[] plainText,byte[] ivs,byte[] secretKeys) throws Exception {
        Key deskey = null;
        DESedeKeySpec spec = new DESedeKeySpec(secretKeys);
        SecretKeyFactory keyfactory = SecretKeyFactory.getInstance("desede");
        deskey = keyfactory.generateSecret(spec);

        Cipher cipher = Cipher.getInstance("desede/CBC/NoPadding");
        IvParameterSpec ips = new IvParameterSpec(ivs);
        cipher.init(Cipher.ENCRYPT_MODE, deskey, ips);
        byte[] encryptData = cipher.doFinal(plainText);

        return encryptData;
    }

    /**
     * 3DES DECODE
     *
     * @param encryptText
     * @param ivs
     * @return
     * @throws Exception
     */
    public static byte[] decode(byte[] encryptText,byte[] ivs,byte[] secretKeys) throws Exception {
        Key deskey = null;
        DESedeKeySpec spec = new DESedeKeySpec(secretKeys);
        SecretKeyFactory keyfactory = SecretKeyFactory.getInstance("desede");
        deskey = keyfactory.generateSecret(spec);
        Cipher cipher = Cipher.getInstance("desede/CBC/NoPadding");
        IvParameterSpec ips = new IvParameterSpec(ivs);
        cipher.init(Cipher.DECRYPT_MODE, deskey, ips);
        byte[] decryptData = cipher.doFinal(encryptText);

        return decryptData;
    }
}