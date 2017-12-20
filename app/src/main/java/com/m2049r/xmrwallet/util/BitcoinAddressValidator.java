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

package com.m2049r.xmrwallet.util;

// based on https://rosettacode.org/wiki/Bitcoin/address_validation#Java

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class BitcoinAddressValidator {

    private static final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

    public static boolean validate(String addrress, boolean testnet) {
        if (addrress.length() < 26 || addrress.length() > 35)
            return false;
        byte[] decoded = decodeBase58To25Bytes(addrress);
        if (decoded == null)
            return false;

        if (!testnet) {
            if ((decoded[0] != 0x00) && (decoded[0] != 0x05)) return false;
        } else {
            if ((decoded[0] != 0x6f) && (decoded[0] != 0xc4)) return false;
        }

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
        System.arraycopy(numBytes, 0, result, result.length - numBytes.length, numBytes.length);
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
}