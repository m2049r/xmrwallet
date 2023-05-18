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

package com.m2049r.xmrwallet.model;

import lombok.Getter;
import lombok.Setter;

public class PendingTransaction {
    static {
        System.loadLibrary("monerujo");
    }

    public long handle;

    PendingTransaction(long handle) {
        this.handle = handle;
    }

    public enum Status {
        Status_Ok,
        Status_Error,
        Status_Critical
    }

    public enum Priority {
        Priority_Default(0),
        Priority_Low(1),
        Priority_Medium(2),
        Priority_High(3),
        Priority_Last(4);

        public static Priority fromInteger(int n) {
            switch (n) {
                case 0:
                    return Priority_Default;
                case 1:
                    return Priority_Low;
                case 2:
                    return Priority_Medium;
                case 3:
                    return Priority_High;
            }
            return null;
        }

        public int getValue() {
            return value;
        }

        private int value;

        Priority(int value) {
            this.value = value;
        }
    }

    public Status getStatus() {
        return Status.values()[getStatusJ()];
    }

    public native int getStatusJ();

    public native String getErrorString();

    // commit transaction or save to file if filename is provided.
    public native boolean commit(String filename, boolean overwrite);

    public native long getAmount();

    public native long getDust();

    public native long getFee();

    public String getFirstTxId() {
        String id = getFirstTxIdJ();
        if (id == null)
            throw new IndexOutOfBoundsException();
        return id;
    }

    public native String getFirstTxIdJ();

    public native long getTxCount();

    @Getter
    @Setter
    private long pocketChange;

    public long getNetAmount() {
        return getAmount() - pocketChange;
    }
}
