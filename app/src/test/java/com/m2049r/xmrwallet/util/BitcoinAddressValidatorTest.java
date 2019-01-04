/*
 * Copyright (c) 2017 m2049r
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

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class BitcoinAddressValidatorTest {

    @Test
    public void validateBTC_shouldValidate() {
        assertTrue(BitcoinAddressValidator.validate("2NBMEXXS4v8ubajzfQUjYvh2ptLkxzH8uTC", true));
        assertTrue(BitcoinAddressValidator.validate("2N9fzq66uZYQXp7uqrPBH6jKBhjrgTzpGCy", true));
        assertTrue(BitcoinAddressValidator.validate("1AGNa15ZQXAZUgFiqJ2i7Z2DPU2J6hW62i", false));
        assertTrue(BitcoinAddressValidator.validate("1Q1pE5vPGEEMqRcVRMbtBK842Y6Pzo6nK9", false));
        assertTrue(BitcoinAddressValidator.validate("3R2MPpTNQLCNs13qnHz89Rm82jQ27bAwft", false));
        assertTrue(BitcoinAddressValidator.validate("34QjytsE8GVRbUBvYNheftqJ5CHfDHvQRD", false));
        assertTrue(BitcoinAddressValidator.validate("3GsAUrD4dnCqtaTTUzzsQWoymHNkEFrgGF", false));
        assertTrue(BitcoinAddressValidator.validate("3NagLCvw8fLwtoUrK7s2mJPy9k6hoyWvTU", false));
        assertTrue(BitcoinAddressValidator.validate("mjn127C5wRQCULksMYMFHLp9UTdQuCfbZ9", true));
    }

    @Test
    public void validateBTC_shouldNotValidate() {
        assertTrue(BitcoinAddressValidator.validate("3NagLCvw8fLwtoUrK7s2mJPy9k6hoyWvTU", false));
        assertTrue(BitcoinAddressValidator.validate("mjn127C5wRQCULksMYMFHLp9UTdQuCfbZ9", true));
        assertTrue(!BitcoinAddressValidator.validate("1AGNa15ZQXAZUgFiqJ2i7Z2DPU2J6hW62j", true));
        assertTrue(!BitcoinAddressValidator.validate("1AGNa15ZQXAZUgFiqJ2i7Z2DPU2J6hW62X", true));
        assertTrue(!BitcoinAddressValidator.validate("1ANNa15ZQXAZUgFiqJ2i7Z2DPU2J6hW62i", true));
        assertTrue(!BitcoinAddressValidator.validate("1A Na15ZQXAZUgFiqJ2i7Z2DPU2J6hW62i", true));
        assertTrue(!BitcoinAddressValidator.validate("BZbvjr", true));
        assertTrue(!BitcoinAddressValidator.validate("i55j", true));
        assertTrue(!BitcoinAddressValidator.validate("1AGNa15ZQXAZUgFiqJ2i7Z2DPU2J6hW62!", true));
        assertTrue(!BitcoinAddressValidator.validate("1AGNa15ZQXAZUgFiqJ2i7Z2DPU2J6hW62iz", false));
        assertTrue(!BitcoinAddressValidator.validate("1AGNa15ZQXAZUgFiqJ2i7Z2DPU2J6hW62izz", false));
        assertTrue(!BitcoinAddressValidator.validate("1Q1pE5vPGEEMqRcVRMbtBK842Y6Pzo6nJ9", false));
        assertTrue(!BitcoinAddressValidator.validate("1AGNa15ZQXAZUgFiqJ2i7Z2DPU2J6hW62I", false));
        assertTrue(!BitcoinAddressValidator.validate("3NagLCvw8fLwtoUrK7s2mJPy9k6hoyWvTU ", false));
        assertTrue(!BitcoinAddressValidator.validate(" 3NagLCvw8fLwtoUrK7s2mJPy9k6hoyWvTU ", false));
    }

    @Test
    public void validSegwit() {
        // see https://github.com/bitcoin/bips/blob/master/bip-0173.mediawiki
        assertTrue(BitcoinAddressValidator.validateBech32Segwit("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4", false));
        assertTrue(BitcoinAddressValidator.validateBech32Segwit("tb1qw508d6qejxtdg4y5r3zarvary0c5xw7kxpjzsx", true));
        assertTrue(BitcoinAddressValidator.validateBech32Segwit("bc1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3qccfmv3", false));
        assertTrue(BitcoinAddressValidator.validateBech32Segwit("tb1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3q0sl5k7", true));

        assertTrue(BitcoinAddressValidator.validateBech32Segwit("BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4", false));
        assertTrue(BitcoinAddressValidator.validateBech32Segwit("tb1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3q0sl5k7", true));
        assertTrue(BitcoinAddressValidator.validateBech32Segwit("bc1pw508d6qejxtdg4y5r3zarvary0c5xw7kw508d6qejxtdg4y5r3zarvary0c5xw7k7grplx", false));
        assertTrue(BitcoinAddressValidator.validateBech32Segwit("BC1SW50QA3JX3S", false));
        assertTrue(BitcoinAddressValidator.validateBech32Segwit("bc1zw508d6qejxtdg4y5r3zarvaryvg6kdaj", false));
        assertTrue(BitcoinAddressValidator.validateBech32Segwit("tb1qqqqqp399et2xygdj5xreqhjjvcmzhxw4aywxecjdzew6hylgvsesrxh6hy", true));

        assertTrue(BitcoinAddressValidator.validateBech32Segwit("bc1q76awjp3nmklgnf0yyu0qncsekktf4e3qj248t4", false)); // electrum blog

    }

    @Test
    public void invalidSegwit() {
        // see https://github.com/bitcoin/bips/blob/master/bip-0173.mediawiki
        assertFalse(BitcoinAddressValidator.validateBech32Segwit("tc1qw508d6qejxtdg4y5r3zarvary0c5xw7kg3g4ty", true)); // Invalid human-readable part
        assertFalse(BitcoinAddressValidator.validateBech32Segwit("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t5", true)); // Invalid checksum
        assertFalse(BitcoinAddressValidator.validateBech32Segwit("BC13W508D6QEJXTDG4Y5R3ZARVARY0C5XW7KN40WF2", true)); // Invalid witness version
        assertFalse(BitcoinAddressValidator.validateBech32Segwit("bc1rw5uspcuh", true)); // Invalid program length
        assertFalse(BitcoinAddressValidator.validateBech32Segwit("bc10w508d6qejxtdg4y5r3zarvary0c5xw7kw508d6qejxtdg4y5r3zarvary0c5xw7kw5rljs90", true)); // Invalid program length
        assertFalse(BitcoinAddressValidator.validateBech32Segwit("BC1QR508D6QEJXTDG4Y5R3ZARVARYV98GJ9P", true)); // Invalid program length for witness version 0 (per BIP141)
        assertFalse(BitcoinAddressValidator.validateBech32Segwit("tb1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3q0sL5k7", true)); // Mixed case
        assertFalse(BitcoinAddressValidator.validateBech32Segwit("bc1zw508d6qejxtdg4y5r3zarvaryvqyzf3du", true)); // zero padding of more than 4 bits
        assertFalse(BitcoinAddressValidator.validateBech32Segwit("tb1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3pjxtptv", true)); // Non-zero padding in 8-to-5 conversion
        assertFalse(BitcoinAddressValidator.validateBech32Segwit("bc1gmk9yu", true)); // Empty data section
    }
}
