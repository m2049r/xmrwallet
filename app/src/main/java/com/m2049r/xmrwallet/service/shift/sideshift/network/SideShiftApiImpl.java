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

package com.m2049r.xmrwallet.service.shift.sideshift.network;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.m2049r.xmrwallet.service.shift.NetworkCallback;
import com.m2049r.xmrwallet.service.shift.ShiftApiCall;
import com.m2049r.xmrwallet.service.shift.ShiftCallback;
import com.m2049r.xmrwallet.service.shift.ShiftError;
import com.m2049r.xmrwallet.service.shift.ShiftException;
import com.m2049r.xmrwallet.service.shift.sideshift.api.CreateOrder;
import com.m2049r.xmrwallet.service.shift.sideshift.api.QueryOrderParameters;
import com.m2049r.xmrwallet.service.shift.sideshift.api.QueryOrderStatus;
import com.m2049r.xmrwallet.service.shift.sideshift.api.RequestQuote;
import com.m2049r.xmrwallet.service.shift.sideshift.api.SideShiftApi;
import com.m2049r.xmrwallet.util.OkHttpHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import timber.log.Timber;

public class SideShiftApiImpl implements SideShiftApi, ShiftApiCall {

    @NonNull
    private final OkHttpClient okHttpClient;

    private final HttpUrl baseUrl;

    public SideShiftApiImpl(@NonNull final OkHttpClient okHttpClient, final HttpUrl baseUrl) {
        this.okHttpClient = okHttpClient;
        this.baseUrl = baseUrl;
    }

    @Override
    public void queryOrderParameters(@NonNull final ShiftCallback<QueryOrderParameters> callback) {
        QueryOrderParametersImpl.call(this, callback);
    }

    @Override
    public void requestQuote(final double xmrAmount, @NonNull final ShiftCallback<RequestQuote> callback) {
        RequestQuoteImpl.call(this, xmrAmount, callback);
    }

    @Override
    public void createOrder(final String quoteId, @NonNull final String btcAddress,
                            @NonNull final ShiftCallback<CreateOrder> callback) {
        CreateOrderImpl.call(this, quoteId, btcAddress, callback);
    }

    @Override
    public void queryOrderStatus(@NonNull final String uuid,
                                 @NonNull final ShiftCallback<QueryOrderStatus> callback) {
        QueryOrderStatusImpl.call(this, uuid, callback);
    }

    @Override
    public Uri getQueryOrderUri(String orderId) {
        return Uri.parse("https://sideshift.ai/orders/" + orderId);
    }

    @Override
    public void call(@NonNull final String path, @NonNull final NetworkCallback callback) {
        call(path, null, callback);
    }

    @Override
    public void call(@NonNull final String path, final JSONObject request, @NonNull final NetworkCallback callback) {
        final HttpUrl url = baseUrl.newBuilder()
                .addPathSegments(path)
                .build();
        final Request httpRequest = createHttpRequest(request, url);

        okHttpClient.newCall(httpRequest).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(final Call call, final IOException ex) {
                callback.onError(ex);
            }

            @Override
            public void onResponse(@NonNull final Call call, @NonNull final Response response) throws IOException {
                Timber.d("onResponse code=%d", response.code());
                if (response.isSuccessful()) {
                    try {
                        final JSONObject json = new JSONObject(response.body().string());
                        callback.onSuccess(json);
                    } catch (JSONException ex) {
                        callback.onError(ex);
                    }
                } else {
                    try {
                        final JSONObject json = new JSONObject(response.body().string());
                        Timber.d(json.toString(2));
                        final ShiftError error = new ShiftError(json);
                        Timber.w("%s says %d/%s", CreateOrder.TAG, response.code(), error.toString());
                        callback.onError(new ShiftException(response.code(), error));
                    } catch (JSONException ex) {
                        callback.onError(new ShiftException(response.code()));
                    }
                }
            }
        });
    }

    private Request createHttpRequest(final JSONObject request, final HttpUrl url) {
        if (request != null) {
            final RequestBody body = RequestBody.create(request.toString(), MediaType.parse("application/json"));
            return OkHttpHelper.getPostRequest(url, body);
        } else {
            return OkHttpHelper.getGetRequest(url);
        }
    }
}
