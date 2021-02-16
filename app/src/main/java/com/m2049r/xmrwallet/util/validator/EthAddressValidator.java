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

// mostly based on https://github.com/ognus/wallet-address-validator/blob/master/src/ethereum_validator.js

import com.theromus.sha.Keccak;
import com.theromus.sha.Parameters;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class EthAddressValidator {
    static private final Pattern ETH_ADDRESS = Pattern.compile("^0x[0-9a-fA-F]{40}$");
    static private final Pattern ETH_ALLLOWER = Pattern.compile("^0x[0-9a-f]{40}$");
    static private final Pattern ETH_ALLUPPER = Pattern.compile("^0x[0-9A-F]{40}$");

    public static boolean validate(String address) {
        // Check if it has the basic requirements of an address
        if (!ETH_ADDRESS.matcher(address).matches())
            return false;

        // If it's all small caps or all all caps, return true
        if (ETH_ALLLOWER.matcher(address).matches() || ETH_ALLUPPER.matcher(address).matches()) {
            return true;
        }

        // Otherwise check each case
        return validateChecksum(address);
    }

    private static boolean validateChecksum(String address) {
        // Check each case
        address = address.substring(2); // strip 0x

        Keccak keccak = new Keccak();
        final byte[] addressHash = keccak.getHash(
                address.toLowerCase().getBytes(StandardCharsets.US_ASCII),
                Parameters.KECCAK_256);
        for (int i = 0; i < 40; i++) {
            boolean upper = (addressHash[i / 2] & ((i % 2) == 0 ? 128 : 8)) != 0;
            char c = address.charAt(i);
            if (Character.isAlphabetic(c)) {
                if (Character.isUpperCase(c) && !upper) return false;
                if (Character.isLowerCase(c) && upper) return false;
            }
        }
        return true;
    }
}