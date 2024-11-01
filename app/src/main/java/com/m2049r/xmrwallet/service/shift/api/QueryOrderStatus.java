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

package com.m2049r.xmrwallet.service.shift.api;

import androidx.annotation.Nullable;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QueryOrderStatus {
    final String orderId;
    final Status status;
    final String btcCurrency;
    final double btcAmount;
    final String btcAddress;
    final double xmrAmount;
    final String xmrAddress;
    @Nullable
    final Date createdAt;
    @Nullable
    final Date expiresAt;

    public double getPrice() {
        return btcAmount / xmrAmount;
    }

    public boolean isError() {
        return status.isError();
    }

    public boolean isTerminal() {
        return status.isTerminal();
    }

    public boolean isWaiting() {
        return status.isWaiting();
    }

    public boolean isPending() {
        return status.isPending();
    }

    public boolean isSent() {
        return status.isSent();
    }

    public boolean isPaid() {
        return status.isPaid();
    }

    public enum Status {
        WAITING,    // Waiting for mempool
        PENDING,    // Detected (waiting for confirmations)
        SETTLING,   // Settlement in progress
        SETTLED,    // Settlement completed
        // no refunding in monerujo so these are ignored:
//        REFUND,     // Queued for refund
//        REFUNDING,  // Refund in progress
//        REFUNDED    // Refund completed
        UNDEFINED,
        EXPIRED,
        ERROR; // Something went wrong and the user needs to interact with the provider

        public boolean isError() {
            return this == Status.UNDEFINED || this == Status.ERROR || this == Status.EXPIRED;
        }

        public boolean isTerminal() {
            return (this == Status.SETTLED) || isError();
        }

        public boolean isWaiting() {
            return this == Status.WAITING;
        }

        public boolean isPending() {
            return this == Status.PENDING;
        }

        public boolean isSent() {
            return this == Status.SETTLING;
        }

        public boolean isPaid() {
            return this == Status.SETTLED;
        }
    }
}
