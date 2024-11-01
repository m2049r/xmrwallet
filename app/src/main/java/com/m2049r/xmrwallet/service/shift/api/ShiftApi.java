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

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.m2049r.xmrwallet.data.CryptoAmount;
import com.m2049r.xmrwallet.service.shift.ShiftCallback;

public interface ShiftApi {
    int QUERY_INTERVAL = 5000; // ms

    /**
     * Queries the order parameter.
     *
     * @param callback the callback with the OrderParameter object
     */
    void queryOrderParameters(@NonNull final CryptoAmount btcAmount, @NonNull final ShiftCallback<QueryOrderParameters> callback);

    /**
     * Creates an order
     *
     * @param btcAddress destination
     * @param btcAmount  the desired amount to send
     */
    void requestQuote(@Nullable final String btcAddress, @NonNull final CryptoAmount btcAmount, @NonNull final ShiftCallback<RequestQuote> callback);

    /**
     * Creates an order
     *
     * @param quote      the quote from {@link #requestQuote(String, CryptoAmount, ShiftCallback)}
     * @param btcAddress the target bitcoin address
     */
    void createOrder(final RequestQuote quote, @NonNull final String btcAddress, @NonNull final ShiftCallback<CreateOrder> callback);

    /**
     * Queries the order status for given current order
     *
     * @param orderId  the order ID
     * @param callback the callback with the OrderStatus object
     */
    void queryOrderStatus(@NonNull final String orderId, @NonNull final ShiftCallback<QueryOrderStatus> callback);

    /*
     * Returns the URL for manually querying the order status
     *
     * @param orderId the order ID
     */
    Uri getQueryOrderUri(String orderId);
}
