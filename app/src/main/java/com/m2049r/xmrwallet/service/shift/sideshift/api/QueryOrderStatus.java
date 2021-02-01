/*
 * Copyright (c) 2017-2021 m2049r et al.
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

package com.m2049r.xmrwallet.service.shift.sideshift.api;

import java.util.Date;

public interface QueryOrderStatus {
    enum State {
        WAITING,    // Waiting for mempool
        PENDING,    // Detected (waiting for confirmations)
        SETTLING,   // Settlement in progress
        SETTLED,    // Settlement completed
        // no refunding in monerujo so theese are ignored:
//        REFUND,     // Queued for refund
//        REFUNDING,  // Refund in progress
//        REFUNDED    // Refund completed
        UNDEFINED
    }

    boolean isCreated();

    boolean isTerminal();

    boolean isWaiting();

    boolean isPending();

    boolean isSent();

    boolean isPaid();

    boolean isError();

    QueryOrderStatus.State getState();

    String getOrderId();

    Date getCreatedAt();

    Date getExpiresAt();

    double getBtcAmount();

    String getBtcAddress();

    double getXmrAmount();

    String getXmrAddress();

    double getPrice();
}