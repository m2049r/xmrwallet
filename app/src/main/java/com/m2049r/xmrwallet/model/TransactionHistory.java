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

import java.util.ArrayList;
import java.util.List;

public class TransactionHistory {
    static {
        System.loadLibrary("monerujo");
    }

    private long handle;

    public TransactionHistory(long handle) {
        this.handle = handle;
    }

    public native int getCount();

    //private native long getTransactionByIndexJ(int i);

    //private native long getTransactionByIdJ(String id);

    public List<TransactionInfo> getAll() {
        return transactions;
    }

    private List<TransactionInfo> transactions = new ArrayList<>();

    public void refresh() {
        transactions = refreshJ();
    }

    private native List<TransactionInfo> refreshJ();

}
