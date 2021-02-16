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

package com.m2049r.xmrwallet.data;

import com.m2049r.xmrwallet.service.shift.sideshift.api.CreateOrder;
import com.m2049r.xmrwallet.util.Helper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserNotes {
    public String txNotes = "";
    public String note = "";
    public String xmrtoTag = null;
    public String xmrtoKey = null;
    public String xmrtoAmount = null; // could be a double - but we are not doing any calculations
    public String xmrtoCurrency = null;
    public String xmrtoDestination = null;

    public UserNotes(final String txNotes) {
        if (txNotes == null) {
            return;
        }
        this.txNotes = txNotes;
        Pattern p = Pattern.compile("^\\{([a-z]+)-(\\w{6,}),([0-9.]*)([A-Z]+),(\\w*)\\} ?(.*)");
        Matcher m = p.matcher(txNotes);
        if (m.find()) {
            xmrtoTag = m.group(1);
            xmrtoKey = m.group(2);
            xmrtoAmount = m.group(3);
            xmrtoCurrency = m.group(4);
            xmrtoDestination = m.group(5);
            note = m.group(6);
        } else {
            note = txNotes;
        }
    }

    public void setNote(String newNote) {
        if (newNote != null) {
            note = newNote;
        } else {
            note = "";
        }
        txNotes = buildTxNote();
    }

    public void setXmrtoOrder(CreateOrder order) {
        if (order != null) {
            xmrtoTag = order.TAG;
            xmrtoKey = order.getOrderId();
            xmrtoAmount = Helper.getDisplayAmount(order.getBtcAmount());
            xmrtoCurrency = order.getBtcCurrency();
            xmrtoDestination = order.getBtcAddress();
        } else {
            xmrtoTag = null;
            xmrtoKey = null;
            xmrtoAmount = null;
            xmrtoDestination = null;
        }
        txNotes = buildTxNote();
    }

    private String buildTxNote() {
        StringBuilder sb = new StringBuilder();
        if (xmrtoKey != null) {
            if ((xmrtoAmount == null) || (xmrtoDestination == null))
                throw new IllegalArgumentException("Broken notes");
            sb.append("{");
            sb.append(xmrtoTag);
            sb.append("-");
            sb.append(xmrtoKey);
            sb.append(",");
            sb.append(xmrtoAmount);
            sb.append(xmrtoCurrency);
            sb.append(",");
            sb.append(xmrtoDestination);
            sb.append("}");
            if ((note != null) && (!note.isEmpty()))
                sb.append(" ");
        }
        sb.append(note);
        return sb.toString();
    }
}