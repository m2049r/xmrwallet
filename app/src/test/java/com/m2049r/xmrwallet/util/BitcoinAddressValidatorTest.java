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

import static org.junit.Assert.assertTrue;


public class BitcoinAddressValidatorTest {

    @Test
    public void validateBTC_shouldValidate() {
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
}
