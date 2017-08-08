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

public interface WalletListener {
    /**
     * moneySpent - called when money spent
     * @param txId   - transaction id
     * @param amount - tvAmount
     */
    void moneySpent(String txId, long amount);

    /**
     * moneyReceived - called when money received
     * @param txId   - transaction id
     * @param amount - tvAmount
     */
    void moneyReceived(String txId, long amount);

    /**
     * unconfirmedMoneyReceived - called when payment arrived in tx pool
     * @param txId   - transaction id
     * @param amount - tvAmount
     */
    void unconfirmedMoneyReceived(String txId, long amount);

    /**
     * newBlock      - called when new block received
     * @param height - block height
     */
    void newBlock(long height);

    /**
     * updated  - generic callback, called when any event (sent/received/block reveived/etc) happened with the wallet;
     */
    void updated();

    /**
     * refreshed - called when wallet refreshed by background thread or explicitly refreshed by calling "refresh" synchronously
     */
    void refreshed();

}
