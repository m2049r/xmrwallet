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

package com.m2049r.xmrwallet.ledger;

public enum Instruction {

    INS_NONE(0x00),
    INS_RESET(0x02),
    INS_GET_KEY(0x20),
    INS_DISPLAY_ADDRESS(0x21),
    INS_PUT_KEY(0x22),
    INS_GET_CHACHA8_PREKEY(0x24),
    INS_VERIFY_KEY(0x26),
    INS_MANAGE_SEEDWORDS(0x28),

    INS_SECRET_KEY_TO_PUBLIC_KEY(0x30),
    INS_GEN_KEY_DERIVATION(0x32),
    INS_DERIVATION_TO_SCALAR(0x34),
    INS_DERIVE_PUBLIC_KEY(0x36),
    INS_DERIVE_SECRET_KEY(0x38),
    INS_GEN_KEY_IMAGE(0x3A),

    INS_SECRET_KEY_ADD(0x3C),
    INS_SECRET_KEY_SUB(0x3E),
    INS_GENERATE_KEYPAIR(0x40),
    INS_SECRET_SCAL_MUL_KEY(0x42),
    INS_SECRET_SCAL_MUL_BASE(0x44),

    INS_DERIVE_SUBADDRESS_PUBLIC_KEY(0x46),
    INS_GET_SUBADDRESS(0x48),
    INS_GET_SUBADDRESS_SPEND_PUBLIC_KEY(0x4A),
    INS_GET_SUBADDRESS_SECRET_KEY(0x4C),

    INS_OPEN_TX(0x70),
    INS_SET_SIGNATURE_MODE(0x72),
    INS_GET_ADDITIONAL_KEY(0x74),
    INS_STEALTH(0x76),
    INS_GEN_COMMITMENT_MASK(0x77),
    INS_BLIND(0x78),
    INS_UNBLIND(0x7A),
    INS_GEN_TXOUT_KEYS(0x7B),
    INS_VALIDATE(0x7C),
    INS_PREFIX_HASH(0x7D),
    INS_MLSAG(0x7E),
    INS_CLOSE_TX(0x80),

    INS_GET_TX_PROOF(0xA0),

    INS_GET_RESPONSE(0xC0),

    INS_UNDEFINED(0xFF);

    public static Instruction fromByte(byte n) {
        switch (n & 0xFF) {
            case 0x00:
                return INS_NONE;
            case 0x02:
                return INS_RESET;

            case 0x20:
                return INS_GET_KEY;
            case 0x22:
                return INS_PUT_KEY;
            case 0x24:
                return INS_GET_CHACHA8_PREKEY;
            case 0x26:
                return INS_VERIFY_KEY;

            case 0x30:
                return INS_SECRET_KEY_TO_PUBLIC_KEY;
            case 0x32:
                return INS_GEN_KEY_DERIVATION;
            case 0x34:
                return INS_DERIVATION_TO_SCALAR;
            case 0x36:
                return INS_DERIVE_PUBLIC_KEY;
            case 0x38:
                return INS_DERIVE_SECRET_KEY;
            case 0x3A:
                return INS_GEN_KEY_IMAGE;
            case 0x3C:
                return INS_SECRET_KEY_ADD;
            case 0x3E:
                return INS_SECRET_KEY_SUB;
            case 0x40:
                return INS_GENERATE_KEYPAIR;
            case 0x42:
                return INS_SECRET_SCAL_MUL_KEY;
            case 0x44:
                return INS_SECRET_SCAL_MUL_BASE;

            case 0x46:
                return INS_DERIVE_SUBADDRESS_PUBLIC_KEY;
            case 0x48:
                return INS_GET_SUBADDRESS;
            case 0x4A:
                return INS_GET_SUBADDRESS_SPEND_PUBLIC_KEY;
            case 0x4C:
                return INS_GET_SUBADDRESS_SECRET_KEY;

            case 0x70:
                return INS_OPEN_TX;
            case 0x72:
                return INS_SET_SIGNATURE_MODE;
            case 0x74:
                return INS_GET_ADDITIONAL_KEY;
            case 0x76:
                return INS_STEALTH;
            case 0x78:
                return INS_BLIND;
            case 0x7A:
                return INS_UNBLIND;
            case 0x7C:
                return INS_VALIDATE;
            case 0x7E:
                return INS_MLSAG;
            case 0x80:
                return INS_CLOSE_TX;

            case 0xc0:
                return INS_GET_RESPONSE;

            default:
                return INS_UNDEFINED;
        }
    }

    public int getValue() {
        return value;
    }

    public byte getByteValue() {
        return (byte) (value & 0xFF);
    }

    private int value;

    Instruction(int value) {
        this.value = value;
    }
}