/*
 * Copyright (c) 2017 m2049r et al.
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

package com.m2049r.xmrwallet.xmrto.api;

import android.support.annotation.NonNull;

public interface XmrToApi {

    /**
     * Queries the order parameter.
     *
     * @param callback the callback with the OrderParameter object
     */
    void queryOrderParameters(@NonNull final XmrToCallback<QueryOrderParameters> callback);

    /**
     * Creates an order
     *
     * @param amount  the desired amount
     * @param address the target bitcoin address
     */
    void createOrder(final double amount, @NonNull final String address, @NonNull final XmrToCallback<CreateOrder> callback);

    /**
     * Creates an order through BIP70 payment protocol like bitpay
     *
     * @param url the BIP70 URL
     */
    void createOrder(@NonNull final String url, @NonNull final XmrToCallback<CreateOrder> callback);

    /**
     * Queries the order status for given current order
     *
     * @param uuid     the order uuid
     * @param callback the callback with the OrderStatus object
     */
    void queryOrderStatus(@NonNull final String uuid, @NonNull final XmrToCallback<QueryOrderStatus> callback);

}
