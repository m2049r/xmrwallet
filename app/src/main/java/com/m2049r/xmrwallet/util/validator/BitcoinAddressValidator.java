/*
 * Copyright (c) 2017 m2049r er al.
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

package com.m2049r.xmrwallet.util.validator;

// mostly based on https://rosettacode.org/wiki/Bitcoin/address_validation#Java

import com.m2049r.xmrwallet.data.Crypto;
import com.m2049r.xmrwallet.model.NetworkType;
import com.m2049r.xmrwallet.model.WalletManager;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class BitcoinAddressValidator {
    private static final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

    public static Crypto validate(String address) {
        for (BitcoinAddressType type : BitcoinAddressType.values()) {
            if (validate(address, type))
                return Crypto.valueOf(type.name());
        }
        return null;
    }

    // just for tests
    public static boolean validateBTC(String addrress, boolean testnet) {
        return validate(addrress, BitcoinAddressType.BTC, testnet);
    }

    public static boolean validate(String addrress, BitcoinAddressType type, boolean testnet) {
        if (validate(addrress, testnet ? type.getTestnet() : type.getProduction()))
            return true;
        if (type.hasBech32())
            return validateBech32Segwit(addrress, type, testnet);
        else
            return false;
    }

    public static boolean validate(String addrress, BitcoinAddressType type) {
        final boolean testnet = WalletManager.getInstance().getNetworkType() != NetworkType.NetworkType_Mainnet;
        return validate(addrress, type, testnet);
    }

    public static boolean validate(String addrress, byte[] addressTypes) {
        if (addrress.length() < 26 || addrress.length() > 35)
            return false;
        byte[] decoded = decodeBase58To25Bytes(addrress);
        if (decoded == null)
            return false;
        int v = decoded[0] & 0xFF;
        boolean nok = true;
        for (byte b : addressTypes) {
            nok = nok && (v != (b & 0xFF));
        }
        if (nok) return false;

        byte[] hash1 = sha256(Arrays.copyOfRange(decoded, 0, 21));
        byte[] hash2 = sha256(hash1);

        return Arrays.equals(Arrays.copyOfRange(hash2, 0, 4), Arrays.copyOfRange(decoded, 21, 25));
    }

    private static byte[] decodeBase58To25Bytes(String input) {
        BigInteger num = BigInteger.ZERO;
        for (char t : input.toCharArray()) {
            int p = ALPHABET.indexOf(t);
            if (p == -1)
                return null;
            num = num.multiply(BigInteger.valueOf(58)).add(BigInteger.valueOf(p));
        }

        byte[] result = new byte[25];
        byte[] numBytes = num.toByteArray();
        if (num.bitLength() > 200) return null;

        if (num.bitLength() == 200) {
            System.arraycopy(numBytes, 1, result, 0, 25);
        } else {
            System.arraycopy(numBytes, 0, result, result.length - numBytes.length, numBytes.length);
        }
        return result;
    }

    private static byte[] sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(data);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    //
    // validate Bech32 segwit
    // see https://github.com/bitcoin/bips/blob/master/bip-0173.mediawiki for spec
    //

    private static final String DATA_CHARS = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";

    public static boolean validateBech32Segwit(String bech32, BitcoinAddressType type, boolean testnet) {
        if (!bech32.equals(bech32.toLowerCase()) && !bech32.equals(bech32.toUpperCase())) {
            return false; // mixing upper and lower case not allowed
        }
        bech32 = bech32.toLowerCase();

        if (!bech32.startsWith(type.getBech32Prefix(testnet))) return false;

        final int hrpLength = type.getBech32Prefix(testnet).length();

        if ((bech32.length() < (12 + hrpLength)) || (bech32.length() > (72 + hrpLength)))
            return false;
        int mod = (bech32.length() - hrpLength) % 8;
        if ((mod == 6) || (mod == 1) || (mod == 3)) return false;

        int sep = -1;
        final byte[] bytes = bech32.getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i < bytes.length; i++) {
            if ((bytes[i] < 33) || (bytes[i] > 126)) {
                return false;
            }
            if (bytes[i] == 49) sep = i; // 49 := '1' in ASCII
        }

        if (sep != hrpLength) return false;
        if (sep > bytes.length - 7) {
            return false; // min 6 bytes data
        }
        if (bytes.length < 8) { // hrp{min}=1 + sep=1 + data{min}=6 := 8
            return false; // too short
        }
        if (bytes.length > 90) {
            return false; // too long
        }

        final byte[] hrp = Arrays.copyOfRange(bytes, 0, sep);

        final byte[] data = Arrays.copyOfRange(bytes, sep + 1, bytes.length);
        for (int i = 0; i < data.length; i++) {
            int b = DATA_CHARS.indexOf(data[i]);
            if (b < 0) return false; // invalid character
            data[i] = (byte) b;
        }

        if (!validateBech32Data(data)) return false;

        return verifyChecksum(hrp, data);
    }

    private static int polymod(byte[] values) {
        final int[] GEN = {0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3};
        int chk = 1;
        for (byte v : values) {
            byte b = (byte) (chk >> 25);
            chk = ((chk & 0x1ffffff) << 5) ^ v;
            for (int i = 0; i < 5; i++) {
                chk ^= ((b >> i) & 1) == 1 ? GEN[i] : 0;
            }
        }
        return chk;
    }

    private static byte[] hrpExpand(byte[] hrp) {
        final byte[] expanded = new byte[(2 * hrp.length) + 1];
        int i = 0;
        for (byte b : hrp) {
            expanded[i++] = (byte) (b >> 5);
        }
        expanded[i++] = 0;
        for (byte b : hrp) {
            expanded[i++] = (byte) (b & 0x1f);
        }
        return expanded;
    }

    private static boolean verifyChecksum(byte[] hrp, byte[] data) {
        final byte[] hrpExpanded = hrpExpand(hrp);
        final byte[] values = new byte[hrpExpanded.length + data.length];
        System.arraycopy(hrpExpanded, 0, values, 0, hrpExpanded.length);
        System.arraycopy(data, 0, values, hrpExpanded.length, data.length);
        return (polymod(values) == 1);
    }

    private static boolean validateBech32Data(final byte[] data) {
        if ((data[0] < 0) || (data[0] > 16)) return false; // witness version
        final int programLength = data.length - 1 - 6; // 1-byte version at beginning & 6-byte checksum at end

        // since we are coming from our own decoder, we don't need to verify data is 5-bit bytes

        final int convertedSize = programLength * 5 / 8;
        final int remainderSize = programLength * 5 % 8;

        if ((convertedSize < 2) || (convertedSize > 40)) return false;

        if ((data[0] == 0) && (convertedSize != 20) && (convertedSize != 32)) return false;

        if (remainderSize >= 5) return false;
        // ignore checksum at end and get last byte of program
        if ((data[data.length - 1 - 6] & ((1 << remainderSize) - 1)) != 0) return false;

        return true;
    }
}
