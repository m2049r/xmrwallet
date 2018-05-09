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

package com.m2049r.xmrwallet.model;

public enum NetworkType {
    NetworkType_Mainnet(0),
    NetworkType_Testnet(1),
    NetworkType_Stagenet(2);

    public static NetworkType fromInteger(int n) {
        switch (n) {
            case 0:
                return NetworkType_Mainnet;
            case 1:
                return NetworkType_Testnet;
            case 2:
                return NetworkType_Stagenet;
        }
        return null;
    }

    public int getValue() {
        return value;
    }

    private int value;

    NetworkType(int value) {
        this.value = value;
    }
}
