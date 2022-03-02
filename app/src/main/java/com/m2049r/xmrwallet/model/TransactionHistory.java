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
import java.util.Iterator;
import java.util.List;

import timber.log.Timber;

public class TransactionHistory {
    static {
        System.loadLibrary("monerujo");
    }

    private long handle;

    int accountIndex;

    public void setAccountFor(Wallet wallet) {
        if (accountIndex != wallet.getAccountIndex()) {
            this.accountIndex = wallet.getAccountIndex();
            refreshWithNotes(wallet);
        }
    }

    public TransactionHistory(long handle, int accountIndex) {
        this.handle = handle;
        this.accountIndex = accountIndex;
    }

    private void loadNotes(Wallet wallet) {
        for (TransactionInfo info : transactions) {
            info.notes = wallet.getUserNote(info.hash);
        }
    }

    public native int getCount(); // over all accounts/subaddresses

    //private native long getTransactionByIndexJ(int i);

    //private native long getTransactionByIdJ(String id);

    public List<TransactionInfo> getAll() {
        return transactions;
    }

    private List<TransactionInfo> transactions = new ArrayList<>();

    void refreshWithNotes(Wallet wallet) {
        refresh();
        loadNotes(wallet);
    }

    private void refresh() {
        List<TransactionInfo> transactionInfos = refreshJ();
        Timber.d("refresh size=%d", transactionInfos.size());
        for (Iterator<TransactionInfo> iterator = transactionInfos.iterator(); iterator.hasNext(); ) {
            TransactionInfo info = iterator.next();
            if (info.accountIndex != accountIndex) {
                iterator.remove();
            }
        }
        transactions = transactionInfos;
    }

    private native List<TransactionInfo> refreshJ();
}
