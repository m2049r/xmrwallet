/*
 * Copyright (c) 2019-2023 m2049r et al.
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

package com.m2049r.xmrwallet.service.exchange.krakenFiat;

import androidx.annotation.NonNull;

import com.m2049r.xmrwallet.service.exchange.api.ExchangeApi;
import com.m2049r.xmrwallet.service.exchange.api.ExchangeRate;

import lombok.Getter;

class ExchangeRateImpl implements ExchangeRate {
    @Getter
    private final String serviceName;
    @Getter
    private final String baseCurrency;
    @Getter
    private final String quoteCurrency;
    @Getter
    private final double rate;

    ExchangeRateImpl(@NonNull final String serviceName, @NonNull final String baseCurrency, @NonNull final String quoteCurrency, double rate) {
        super();
        this.serviceName = serviceName;
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;
        this.rate = rate;
    }
}
