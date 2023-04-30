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


public class EthAddressValidatorTest {

    @Test
    public void validateETH_shouldValidate() {
        assertTrue(EthAddressValidator.validate("0x52d1d0de32322ec51c923b3d4d6c5ffcfcfa01a4"));
        assertTrue(EthAddressValidator.validate("0x74c287ad5328daca276c6a1c1f149415b12c148d"));
        assertTrue(EthAddressValidator.validate("0x12ae66cdc592e10b60f9097a7b0d3c59fce29876"));
        assertTrue(EthAddressValidator.validate("0x12AE66CDc592e10B60f9097a7b0D3C59fce29876"));
        assertTrue(EthAddressValidator.validate("0x541071beFDD2e68deaFb4889c8fdf005Bfdf2fb7"));
    }

    @Test
    public void validateETH_shouldNotValidate() {
        assertFalse(EthAddressValidator.validate("0x12AE66CDc592e10B60f9097a7b0d3C59fce29876"));
        assertFalse(EthAddressValidator.validate("052d1d0de32322ec51c923b3d4d6c5ffcfcfa01a4"));
        assertFalse(EthAddressValidator.validate("x74c287ad5328daca276c6a1c1f149415b12c148d"));
        assertFalse(EthAddressValidator.validate("12ae66cdc592e10b60f9097a7b0d3c59fce29876"));
        assertFalse(EthAddressValidator.validate("0x12ae66cdc592e10b60f9097a7b0d3c59fce2987"));
        assertFalse(EthAddressValidator.validate("0x12ae66cdc592e10b60f9097a7b0d3c59fce2987 "));
        assertFalse(EthAddressValidator.validate(" x12ae66cdc592e10b60f9097a7b0d3c59fce2987 "));
    }
}
