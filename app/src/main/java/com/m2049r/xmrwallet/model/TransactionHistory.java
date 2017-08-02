/**
 * Copyright (c) 2017 m2049r
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.m2049r.xmrwallet.model;

import java.util.List;

public class TransactionHistory {
    static {
        System.loadLibrary("monerujo");
    }

    private long handle;

    public TransactionHistory(long handle) {
        this.handle = handle;
    }

    public TransactionInfo getTransaction(int i) {
        long infoHandle = getTransactionByIndexJ(i);
        return new TransactionInfo(infoHandle);
    }

    public TransactionInfo getTransaction(String id) {
        long infoHandle = getTransactionByIdJ(id);
        return new TransactionInfo(infoHandle);
    }

    /*
        public List<TransactionInfo> getAll() {
            List<Long> handles = getAllJ();
            List<TransactionInfo> infoList = new ArrayList<TransactionInfo>();
            for (Long handle : handles) {
                infoList.add(new TransactionInfo(handle.longValue()));
            }
            return infoList;
        }
    */
    public native int getCount();

    private native long getTransactionByIndexJ(int i);

    private native long getTransactionByIdJ(String id);

    public native List<TransactionInfo> getAll();

    public native void refresh();

}
