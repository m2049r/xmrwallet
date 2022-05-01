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

package com.m2049r.xmrwallet.service.shift.sideshift;

import static org.junit.Assert.assertEquals;

import com.m2049r.xmrwallet.service.shift.ShiftCallback;
import com.m2049r.xmrwallet.service.shift.ShiftException;
import com.m2049r.xmrwallet.service.shift.sideshift.api.QueryOrderStatus;
import com.m2049r.xmrwallet.service.shift.sideshift.api.SideShiftApi;
import com.m2049r.xmrwallet.service.shift.sideshift.network.SideShiftApiImpl;
import com.m2049r.xmrwallet.util.NetCipherHelper;

import net.jodah.concurrentunit.Waiter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeoutException;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class SideShiftApiQueryOrderStatusTest {
    private MockWebServer mockWebServer;

    private SideShiftApi xmrToApi;

    private Waiter waiter;

    @Mock
    ShiftCallback<QueryOrderStatus> mockQueryXmrToCallback;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        waiter = new Waiter();

        MockitoAnnotations.initMocks(this);
        NetCipherHelper.Request.mockClient = new OkHttpClient();
        xmrToApi = new SideShiftApiImpl(mockWebServer.url("/"));
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    public void orderStatus_shouldBePostMethod()
            throws InterruptedException {

        xmrToApi.queryOrderStatus("09090909090909090911", mockQueryXmrToCallback);

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("GET", request.getMethod());
    }

    @Test
    public void orderStatus_wasSuccessfulShouldRespondWithOrder()
            throws TimeoutException, InterruptedException {

        final String state = "settled";
        final String orderId = "09090909090909090911";

        MockResponse jsonMockResponse = new MockResponse().setBody(
                createMockQueryOrderResponse(
                        state,
                        orderId));
        mockWebServer.enqueue(jsonMockResponse);

        xmrToApi.queryOrderStatus(orderId, new ShiftCallback<QueryOrderStatus>() {
            @Override
            public void onSuccess(final QueryOrderStatus orderStatus) {
                waiter.assertEquals(orderStatus.getOrderId(), orderId);
                waiter.assertEquals(orderStatus.getState(), QueryOrderStatus.State.SETTLED);
                waiter.resume();
            }

            @Override
            public void onError(final Exception e) {
                waiter.fail(e);
                waiter.resume();
            }
        });
        waiter.await();
    }

    @Test
    public void orderStatus_wasNotSuccessfulShouldCallOnError()
            throws TimeoutException, InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        xmrToApi.queryOrderStatus("09090909090909090911", new ShiftCallback<QueryOrderStatus>() {
            @Override
            public void onSuccess(final QueryOrderStatus orderStatus) {
                waiter.fail();
                waiter.resume();
            }

            @Override
            public void onError(final Exception e) {
                waiter.assertTrue(e instanceof ShiftException);
                waiter.assertTrue(((ShiftException) e).getCode() == 500);
                waiter.resume();
            }

        });
        waiter.await();
    }

    @Test
    public void orderStatus_orderNotFoundShouldCallOnError()
            throws TimeoutException, InterruptedException {
        mockWebServer.enqueue(new MockResponse().
                setResponseCode(500).
                setBody("{\"error\":{\"message\":\"Order not found\"}}"));
        xmrToApi.queryOrderStatus("09090909090909090911", new ShiftCallback<QueryOrderStatus>() {
            @Override
            public void onSuccess(final QueryOrderStatus orderStatus) {
                waiter.fail();
                waiter.resume();
            }

            @Override
            public void onError(final Exception e) {
                waiter.assertTrue(e instanceof ShiftException);
                ShiftException xmrEx = (ShiftException) e;
                waiter.assertTrue(xmrEx.getCode() == 500);
                waiter.assertNotNull(xmrEx.getError());
                waiter.assertEquals(xmrEx.getError().getErrorMsg(), "Order not found");
                waiter.resume();
            }
        });
        waiter.await();
    }

    private String createMockQueryOrderResponse(
            final String state,
            final String orderId) {
        return "{\"createdAt\":\"1612700947550\"," +
                "\"createdAtISO\":\"2021-02-07T12:29:07.550Z\"," +
                "\"expiresAt\":\"1612701817682\"," +
                "\"expiresAtISO\":\"2021-02-07T12:43:37.682Z\"," +
                "\"depositAddress\":" +
                "{\"address\":\"4Bh68jCUZGHbVu45zCVvtcMYesHuduwgajoQcdYRjUQcY6MNa8qd67vTfSNWdtrc33dDECzbPCJeQ8HbiopdeM7Ej2DVsSohug9QxMJnN2\",\"paymentId\":\"3a151908242c6ed4\",\"integratedAddress\":\"4Bh68jCUZGHbVu45zCVvtcMYesHuduwgajoQcdYRjUQcY6MNa8qd67vTfSNWdtrc33dDECzbPCJeQ8HbiopdeM7Ej2DVsSohug9QxMJnN2\"}," +
                "\"depositMethodId\":\"xmr\"," +
                "\"id\":\"" + orderId + "\"," +
                "\"orderId\":\"" + orderId + "\"," +
                "\"settleAddress\":{\"address\":\"19y91nJyzXsLEuR7Nj9pc3o5SeHNc8A9RW\"}," +
                "\"settleMethodId\":\"btc\"," +
                "\"depositMax\":\"0.01\",\"depositMin\":\"0.01\"," +
                "\"quoteId\":\"01234567-89ab-cdef-0123-456789abcdef\"," +
                "\"settleAmount\":\"0.008108\"," +
                "\"depositAmount\":\"0.01\"," +
                "\"deposits\":[" +
                "{\"createdAt\":\"1612701112634\",\"createdAtISO\":\"2021-02-07T12:31:52.634Z\"," +
                "\"depositAmount\":\"0.01\"," +
                "\"depositTx\":{\"type\":\"monero\",\"txHash\":\"a0b674f6033f5f5398dacea9dddedf8d12e35f46c29dfeaf5fac724d7c678fb7\",\"transferIndex\":0}," +
                "\"depositId\":\"3e700e108fb31a4b1f7f\"," +
                "\"id\":\"3e700e108fb31a4b1f7f\"," +
                "\"status\":\"" + state + "\"," +
                "\"refundAddress\":null,\"refundTx\":null," +
                "\"settleAmount\":\"0.008108\"," +
                "\"settleRate\":\"0.810756\"," +
                "\"settleTx\":{\"type\":\"bitcoin\",\"txHash\":\"7bd5d0c3daac6a087ddf81411c8135fae55078334780debe47df775d596d4561\"}," +
                "\"orderId\":\"" + orderId + "\"" +
                "}" + // deposits[0]
                "]" + // deposits
                "}";
    }
}