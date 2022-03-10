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
import com.m2049r.xmrwallet.service.shift.sideshift.api.CreateOrder;
import com.m2049r.xmrwallet.service.shift.sideshift.api.SideShiftApi;
import com.m2049r.xmrwallet.service.shift.sideshift.network.SideShiftApiImpl;
import com.m2049r.xmrwallet.util.NetCipherHelper;
import com.m2049r.xmrwallet.util.ServiceHelper;

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

public class SideShiftApiCreateOrderTest {

    private MockWebServer mockWebServer;

    private SideShiftApi xmrToApi;

    private Waiter waiter;

    @Mock
    ShiftCallback<CreateOrder> mockOrderXmrToCallback;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        waiter = new Waiter();

        MockitoAnnotations.initMocks(this);
        NetCipherHelper.Request.mockClient = new OkHttpClient();
        xmrToApi = new SideShiftApiImpl(mockWebServer.url("/"));
        ServiceHelper.ASSET = "btc"; // all tests run with BTC
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
        ServiceHelper.ASSET = null;
    }

    @Test
    public void createOrder_shouldBePostMethod()
            throws InterruptedException {

        xmrToApi.createOrder("01234567-89ab-cdef-0123-456789abcdef", "19y91nJyzXsLEuR7Nj9pc3o5SeHNc8A9RW", mockOrderXmrToCallback);

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("POST", request.getMethod());
    }

    @Test
    public void createOrder_shouldBeContentTypeJson()
            throws InterruptedException {

        xmrToApi.createOrder("01234567-89ab-cdef-0123-456789abcdef", "19y91nJyzXsLEuR7Nj9pc3o5SeHNc8A9RW", mockOrderXmrToCallback);

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("application/json; charset=utf-8", request.getHeader("Content-Type"));
    }

    @Test
    public void createOrder_shouldContainValidBody()
            throws InterruptedException {

        final String validBody = "{\"settleAddress\":\"19y91nJyzXsLEuR7Nj9pc3o5SeHNc8A9RW\",\"type\":\"fixed\",\"quoteId\":\"01234567-89ab-cdef-0123-456789abcdef\"}";

        xmrToApi.createOrder("01234567-89ab-cdef-0123-456789abcdef", "19y91nJyzXsLEuR7Nj9pc3o5SeHNc8A9RW", mockOrderXmrToCallback);

        RecordedRequest request = mockWebServer.takeRequest();
        String body = request.getBody().readUtf8();
        assertEquals(validBody, body);
    }

    @Test
    public void createOrder_wasSuccessfulShouldRespondWithOrder()
            throws TimeoutException, InterruptedException {
        final double btcAmount = 1.23456789;
        final String btcAddress = "19y91nJyzXsLEuR7Nj9pc3o5SeHNc8A9RW";
        final double xmrAmount = 0.6;
        final String quoteId = "01234567-89ab-cdef-0123-456789abcdef";
        final String orderId = "09090909090909090911";
        MockResponse jsonMockResponse = new MockResponse().setBody(
                createMockCreateOrderResponse(btcAmount, btcAddress, xmrAmount, quoteId, orderId));
        mockWebServer.enqueue(jsonMockResponse);
        xmrToApi.createOrder(quoteId, btcAddress, new ShiftCallback<CreateOrder>() {
            @Override
            public void onSuccess(final CreateOrder order) {
                waiter.assertEquals(order.getBtcAmount(), btcAmount);
                waiter.assertEquals(order.getBtcAddress(), btcAddress);
                waiter.assertEquals(order.getQuoteId(), quoteId);
                waiter.assertEquals(order.getOrderId(), orderId);
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
    public void createOrder_wasNotSuccessfulShouldCallOnError()
            throws TimeoutException, InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        xmrToApi.createOrder("01234567-89ab-cdef-0123-456789abcdef", "19y91nJyzXsLEuR7Nj9pc3o5SeHNc8A9RW", new ShiftCallback<CreateOrder>() {
            @Override
            public void onSuccess(final CreateOrder order) {
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
    public void createOrder_malformedAddressShouldCallOnError()
            throws TimeoutException, InterruptedException {
        mockWebServer.enqueue(new MockResponse().
                setResponseCode(500).
                setBody("{\"error\":{\"message\":\"Invalid settleDestination\"}}"));
        xmrToApi.createOrder("01234567-89ab-cdef-0123-456789abcdef", "xxx", new ShiftCallback<CreateOrder>() {
            @Override
            public void onSuccess(final CreateOrder order) {
                waiter.fail();
                waiter.resume();
            }

            @Override
            public void onError(final Exception e) {
                waiter.assertTrue(e instanceof ShiftException);
                ShiftException xmrEx = (ShiftException) e;
                waiter.assertTrue(xmrEx.getCode() == 500);
                waiter.assertNotNull(xmrEx.getError());
                waiter.assertEquals(xmrEx.getError().getErrorMsg(), "Invalid settleDestination");
                waiter.resume();
            }

        });
        waiter.await();
    }

    private String createMockCreateOrderResponse(final double btcAmount, final String btcAddress,
                                                 final double xmrAmount,
                                                 final String quoteId, final String orderId) {

        return "{\"createdAt\":\"1612705584613\"," +
                "\"createdAtISO\":\"2021-02-07T13:46:24.613Z\"," +
                "\"expiresAt\":\"1612706453080\"," +
                "\"expiresAtISO\":\"2021-02-07T14:00:53.080Z\"," +
                "\"depositAddress\":{" +
                "\"address\":\"4Bh68jCUZGHbVu45zCVvtcMYesHuduwgajoQcdYRjUQcY6MNa8qd67vTfSNWdtrc33dDECzbPCJeQ8HbiopdeM7Ej8MBVLCYz6cVqy6utz\"," +
                "\"paymentId\":\"dbe876f0374db1ff\"," +
                "\"integratedAddress\":\"4Bh68jCUZGHbVu45zCVvtcMYesHuduwgajoQcdYRjUQcY6MNa8qd67vTfSNWdtrc33dDECzbPCJeQ8HbiopdeM7Ej8MBVLCYz6cVqy6utz\"" +
                "}," +
                "\"depositMethodId\":\"xmr\"," +
                "\"id\":\"" + orderId + "\"," +
                "\"orderId\":\"" + orderId + "\"," +
                "\"settleAddress\":{" +
                "\"address\":\"" + btcAddress + "\"" +
                "}," +
                "\"settleMethodId\":\"btc\"," +
                "\"depositMax\":\"" + xmrAmount + "\"," +
                "\"depositMin\":\"" + xmrAmount + "\"," +
                "\"quoteId\":\"" + quoteId + "\"," +
                "\"settleAmount\":\"" + btcAmount + "\"," +
                "\"depositAmount\":\"" + xmrAmount + "\"," +
                "\"deposits\":[]}";
    }
}