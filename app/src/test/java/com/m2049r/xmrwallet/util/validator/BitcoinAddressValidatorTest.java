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

package com.m2049r.xmrwallet.util.validator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;


public class BitcoinAddressValidatorTest {

    @Test
    public void validateBTC_shouldValidate() {
        assertTrue(BitcoinAddressValidator.validateBTC("2NBMEXXS4v8ubajzfQUjYvh2ptLkxzH8uTC", true));
        assertTrue(BitcoinAddressValidator.validateBTC("2N9fzq66uZYQXp7uqrPBH6jKBhjrgTzpGCy", true));
        assertTrue(BitcoinAddressValidator.validateBTC("1AGNa15ZQXAZUgFiqJ2i7Z2DPU2J6hW62i", false));
        assertTrue(BitcoinAddressValidator.validateBTC("1Q1pE5vPGEEMqRcVRMbtBK842Y6Pzo6nK9", false));
        assertTrue(BitcoinAddressValidator.validateBTC("3R2MPpTNQLCNs13qnHz89Rm82jQ27bAwft", false));
        assertTrue(BitcoinAddressValidator.validateBTC("34QjytsE8GVRbUBvYNheftqJ5CHfDHvQRD", false));
        assertTrue(BitcoinAddressValidator.validateBTC("3GsAUrD4dnCqtaTTUzzsQWoymHNkEFrgGF", false));
        assertTrue(BitcoinAddressValidator.validateBTC("3NagLCvw8fLwtoUrK7s2mJPy9k6hoyWvTU", false));
        assertTrue(BitcoinAddressValidator.validateBTC("mjn127C5wRQCULksMYMFHLp9UTdQuCfbZ9", true));
    }

    @Test
    public void validateBTC_shouldNotValidate() {
        assertTrue(BitcoinAddressValidator.validateBTC("3NagLCvw8fLwtoUrK7s2mJPy9k6hoyWvTU", false));
        assertTrue(BitcoinAddressValidator.validateBTC("mjn127C5wRQCULksMYMFHLp9UTdQuCfbZ9", true));
        assertTrue(!BitcoinAddressValidator.validateBTC("1AGNa15ZQXAZUgFiqJ2i7Z2DPU2J6hW62j", true));
        assertTrue(!BitcoinAddressValidator.validateBTC("1AGNa15ZQXAZUgFiqJ2i7Z2DPU2J6hW62X", true));
        assertTrue(!BitcoinAddressValidator.validateBTC("1ANNa15ZQXAZUgFiqJ2i7Z2DPU2J6hW62i", true));
        assertTrue(!BitcoinAddressValidator.validateBTC("1A Na15ZQXAZUgFiqJ2i7Z2DPU2J6hW62i", true));
        assertTrue(!BitcoinAddressValidator.validateBTC("BZbvjr", true));
        assertTrue(!BitcoinAddressValidator.validateBTC("i55j", true));
        assertTrue(!BitcoinAddressValidator.validateBTC("1AGNa15ZQXAZUgFiqJ2i7Z2DPU2J6hW62!", true));
        assertTrue(!BitcoinAddressValidator.validateBTC("1AGNa15ZQXAZUgFiqJ2i7Z2DPU2J6hW62iz", false));
        assertTrue(!BitcoinAddressValidator.validateBTC("1AGNa15ZQXAZUgFiqJ2i7Z2DPU2J6hW62izz", false));
        assertTrue(!BitcoinAddressValidator.validateBTC("1Q1pE5vPGEEMqRcVRMbtBK842Y6Pzo6nJ9", false));
        assertTrue(!BitcoinAddressValidator.validateBTC("1AGNa15ZQXAZUgFiqJ2i7Z2DPU2J6hW62I", false));
        assertTrue(!BitcoinAddressValidator.validateBTC("3NagLCvw8fLwtoUrK7s2mJPy9k6hoyWvTU ", false));
        assertTrue(!BitcoinAddressValidator.validateBTC(" 3NagLCvw8fLwtoUrK7s2mJPy9k6hoyWvTU ", false));
    }

    @Test
    public void validSegwit() {
        // see https://github.com/bitcoin/bips/blob/master/bip-0173.mediawiki
        assertTrue(BitcoinAddressValidator.validateBTC("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4", false));
        assertTrue(BitcoinAddressValidator.validateBTC("tb1qw508d6qejxtdg4y5r3zarvary0c5xw7kxpjzsx", true));
        assertTrue(BitcoinAddressValidator.validateBTC("bc1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3qccfmv3", false));
        assertTrue(BitcoinAddressValidator.validateBTC("tb1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3q0sl5k7", true));

        assertTrue(BitcoinAddressValidator.validateBTC("BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KV8F3T4", false));
        assertTrue(BitcoinAddressValidator.validateBTC("tb1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3q0sl5k7", true));
        assertTrue(BitcoinAddressValidator.validateBTC("bc1pw508d6qejxtdg4y5r3zarvary0c5xw7kw508d6qejxtdg4y5r3zarvary0c5xw7k7grplx", false));
        assertTrue(BitcoinAddressValidator.validateBTC("BC1SW50QA3JX3S", false));
        assertTrue(BitcoinAddressValidator.validateBTC("bc1zw508d6qejxtdg4y5r3zarvaryvg6kdaj", false));
        assertTrue(BitcoinAddressValidator.validateBTC("tb1qqqqqp399et2xygdj5xreqhjjvcmzhxw4aywxecjdzew6hylgvsesrxh6hy", true));

        assertTrue(BitcoinAddressValidator.validateBTC("bc1q76awjp3nmklgnf0yyu0qncsekktf4e3qj248t4", false)); // electrum blog

    }

    @Test
    public void invalidSegwit() {
        // see https://github.com/bitcoin/bips/blob/master/bip-0173.mediawiki
        assertFalse(BitcoinAddressValidator.validateBTC("tc1qw508d6qejxtdg4y5r3zarvary0c5xw7kg3g4ty", true)); // Invalid human-readable part
        assertFalse(BitcoinAddressValidator.validateBTC("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t5", true)); // Invalid checksum
        assertFalse(BitcoinAddressValidator.validateBTC("BC13W508D6QEJXTDG4Y5R3ZARVARY0C5XW7KN40WF2", true)); // Invalid witness version
        assertFalse(BitcoinAddressValidator.validateBTC("bc1rw5uspcuh", true)); // Invalid program length
        assertFalse(BitcoinAddressValidator.validateBTC("bc10w508d6qejxtdg4y5r3zarvary0c5xw7kw508d6qejxtdg4y5r3zarvary0c5xw7kw5rljs90", true)); // Invalid program length
        assertFalse(BitcoinAddressValidator.validateBTC("BC1QR508D6QEJXTDG4Y5R3ZARVARYV98GJ9P", true)); // Invalid program length for witness version 0 (per BIP141)
        assertFalse(BitcoinAddressValidator.validateBTC("tb1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3q0sL5k7", true)); // Mixed case
        assertFalse(BitcoinAddressValidator.validateBTC("bc1zw508d6qejxtdg4y5r3zarvaryvqyzf3du", true)); // zero padding of more than 4 bits
        assertFalse(BitcoinAddressValidator.validateBTC("tb1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3pjxtptv", true)); // Non-zero padding in 8-to-5 conversion
        assertFalse(BitcoinAddressValidator.validateBTC("bc1gmk9yu", true)); // Empty data section
    }

    @Test
    public void validateLTC_shouldValidate() {
        assertTrue(BitcoinAddressValidator.validate("ltc1qmgne2vzyk9c9zk7mak6u5gy02h242f5498pnsd", BitcoinAddressType.LTC, false));
        assertTrue(BitcoinAddressValidator.validate("M9pTFxP6MPTZT61EeNJcmenqR5G8gD3m9a", BitcoinAddressType.LTC, false));
        assertTrue(BitcoinAddressValidator.validate("LfiJ12PCWSFrRxoPiemiSXLUkT74oXWMv6", BitcoinAddressType.LTC, false));

        assertTrue(BitcoinAddressValidator.validate("ltc1qwg8d8240h8y8mcrn7j56mz9896y24rzdevf978", BitcoinAddressType.LTC, false));
        assertTrue(BitcoinAddressValidator.validate("ltc1qmqhg2ynwzdxphrw786xwkv5sl62xkwljpmas0y", BitcoinAddressType.LTC, false));
        assertTrue(BitcoinAddressValidator.validate("ltc1qz67ddnsl92r6skfrxyk2u4dv7qdy9zxpepn0vm", BitcoinAddressType.LTC, false));
        assertTrue(BitcoinAddressValidator.validate("ltc1qffjjuwfgylx685s00yfjk9l0mx4jsp7xvwwvqr", BitcoinAddressType.LTC, false));

        assertTrue(BitcoinAddressValidator.validate("LZNhSpMWsczM4VWm7poPVCLHf1iwmXynAn", BitcoinAddressType.LTC, false));
        assertTrue(BitcoinAddressValidator.validate("MFAECqt8RD3t4CeGLhteCCHAaK9KzNqvec", BitcoinAddressType.LTC, false));
    }

    @Test
    public void validateLTC_shouldNotValidate() {
        assertFalse(BitcoinAddressValidator.validate("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4", BitcoinAddressType.LTC, false));
        assertFalse(BitcoinAddressValidator.validate("2N9fzq66uZYQXp7uqrPBH6jKBhjrgTzpGCy", BitcoinAddressType.LTC, false));
    }

    @Test
    public void validateDASH_shouldValidate() {
        assertTrue(BitcoinAddressValidator.validate("XoSxpd5VYNQvKbXbEaDKt6P1aZANzAkXrJ", BitcoinAddressType.DASH, false));
        assertTrue(BitcoinAddressValidator.validate("Xgxd38qtwqEJDLJK1gSyzLWVtgTV26foGK", BitcoinAddressType.DASH, false));
        assertTrue(BitcoinAddressValidator.validate("XkfCmxuMwU8DSeUpEMqfmjz4QWyhebwPCD", BitcoinAddressType.DASH, false));
    }

    @Test
    public void validateDASH_shouldNotValidate() {
        assertFalse(BitcoinAddressValidator.validate("XkfCmxuMwU8DSeUpEMqfmjz4QWyhebwPCd", BitcoinAddressType.DASH, false));
        assertFalse(BitcoinAddressValidator.validate("2N9fzq66uZYQXp7uqrPBH6jKBhjrgTzpGCy", BitcoinAddressType.DASH, false));
    }

    @Test
    public void validateDOGE_shouldValidate() {
        assertTrue(BitcoinAddressValidator.validate("DPpJVPpvPNP6i6tMj4rTycAGh8wReTqaSU", BitcoinAddressType.DOGE, false));
        assertTrue(BitcoinAddressValidator.validate("DPS6iZj7roHquvwRYXNBua9QtKPzigUUhM", BitcoinAddressType.DOGE, false));
        assertTrue(BitcoinAddressValidator.validate("DFs6qrdCp4K4evv6jU5R3y2WjaWQbXzGsX", BitcoinAddressType.DOGE, false));
    }

    @Test
    public void validateDOGE_shouldNotValidate() {
        assertFalse(BitcoinAddressValidator.validate("Xgxd38qtwqEJDLJK1gSyzLWVtgTV26foGK", BitcoinAddressType.DOGE, false));
        assertFalse(BitcoinAddressValidator.validate("M9pTFxP6MPTZT61EeNJcmenqR5G8gD3m9a", BitcoinAddressType.DOGE, false));
    }
}
