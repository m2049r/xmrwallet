/*
 * Copyright (c) 2018 m2049r
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

import java.math.BigInteger;

public class CrazyPassEncoder {
    static final String BASE = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    static final int PW_CHARS = 52;

    // this takes a 32 byte buffer and converts it to 52 alphnumeric characters
    // separated by blanks every 4 characters = 13 groups of 4
    // always (padding by Xs if need be
    static public String encode(byte[] data) {
        if (data.length != 32) throw new IllegalArgumentException("data[] is not 32 bytes long");
        BigInteger rest = new BigInteger(1, data);
        BigInteger remainder;
        final StringBuilder result = new StringBuilder();
        final BigInteger base = BigInteger.valueOf(BASE.length());
        int i = 0;
        do {
            if ((i > 0) && (i % 4 == 0)) result.append(' ');
            i++;
            remainder = rest.remainder(base);
            rest = rest.divide(base);
            result.append(BASE.charAt(remainder.intValue()));
        } while (!BigInteger.ZERO.equals(rest));
        // pad it
        while (i < PW_CHARS) {
            if ((i > 0) && (i % 4 == 0)) result.append(' ');
            result.append('2');
            i++;
        }
        return result.toString();
    }

    static public String reformat(String password) {
        // maybe this is a CrAzYpass without blanks? or lowercase letters
        String noBlanks = password.toUpperCase().replaceAll(" ", "");
        if (noBlanks.length() == PW_CHARS) { // looks like a CrAzYpass
            // insert blanks every 4 characters
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < PW_CHARS; i++) {
                if ((i > 0) && (i % 4 == 0)) sb.append(' ');
                char c = noBlanks.charAt(i);
                if (BASE.indexOf(c) < 0) return null; // invalid character found
                sb.append(c);
            }
            return sb.toString();
        } else {
            return null; // not a CrAzYpass
        }
    }
}
