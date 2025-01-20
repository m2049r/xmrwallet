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

package com.m2049r.xmrwallet.service.shift.provider.exolix;

import androidx.annotation.NonNull;

import com.m2049r.xmrwallet.data.Crypto;
import com.m2049r.xmrwallet.data.CryptoAmount;
import com.m2049r.xmrwallet.service.shift.NetworkCallback;
import com.m2049r.xmrwallet.service.shift.ShiftApiCall;
import com.m2049r.xmrwallet.service.shift.ShiftCallback;
import com.m2049r.xmrwallet.service.shift.ShiftService;
import com.m2049r.xmrwallet.service.shift.api.QueryOrderParameters;
import com.m2049r.xmrwallet.util.AmountHelper;
import com.m2049r.xmrwallet.util.Helper;

import org.json.JSONException;
import org.json.JSONObject;

import lombok.Getter;

@Getter
class QueryOrderParametersImpl implements QueryOrderParameters {

    private final double lowerLimit;
    private final double price;
    private final double upperLimit;

    QueryOrderParametersImpl(final JSONObject jsonObject) throws JSONException {
        price = jsonObject.getDouble("rate");
        lowerLimit = jsonObject.getDouble("minAmount"); // XMR
        upperLimit = jsonObject.getDouble("maxAmount"); // XMR
    }

    public static void call(@NonNull final ShiftApiCall api, CryptoAmount btcAmount,
                            @NonNull final ShiftCallback<QueryOrderParameters> callback) {
        if (btcAmount.getAmount() == 0) { // just checking rate without real amount
            btcAmount = new CryptoAmount(Crypto.withSymbol(Helper.BASE_CRYPTO), 1); // might as well check for 1 XMR
        }
        final CryptoAmount cryptoAmount = btcAmount;
        final StringBuilder params = new StringBuilder();
        if (btcAmount.getCrypto() == Crypto.XMR) { // we are sending XMR, so float
            params.append("rateType=float");
            params.append("&amount=").append(AmountHelper.format(cryptoAmount.getAmount()));
            params.append("&coinFrom=").append(Crypto.XMR.getSymbol());
            params.append("&networkFrom=").append(Crypto.XMR.getNetwork());
        } else { // we are receiving non-XMR, i.e. paying something, so fixed
            params.append("rateType=fixed");
            params.append("&withdrawalAmount=").append(AmountHelper.format(cryptoAmount.getAmount()));
            params.append("&coinFrom=").append(Crypto.XMR.getSymbol());
            params.append("&networkFrom=").append(Crypto.XMR.getNetwork());
        }
        params.append("&coinTo=").append(ShiftService.ASSET.getSymbol());
        params.append("&networkTo=").append(ShiftService.ASSET.getNetwork());

        api.get("rate", params.toString(), new NetworkCallback() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                try {
                    callback.onSuccess(new QueryOrderParametersImpl(jsonObject));
                } catch (JSONException ex) {
                    callback.onError(ex);
                }
            }

            @Override
            public void onError(Exception ex, JSONObject json) {
                if ((json != null) && json.has("minAmount")) {
                    try {
                        final double lowerLimit = json.getDouble((cryptoAmount.getCrypto() == Crypto.XMR) ? "minAmount" : "withdrawMin");
                        call(api, cryptoAmount.newWithAmount(1.5 * lowerLimit), callback); // try again with 150% of minimum
                    } catch (JSONException jex) {
                        callback.onError(jex);
                    }
                } else {
                    callback.onError(ex);
                }
            }
        });
    }
}
