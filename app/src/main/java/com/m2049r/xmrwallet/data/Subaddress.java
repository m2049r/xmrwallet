/*
 * Copyright (c) 2018 m2049r
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

import java.util.regex.Pattern;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@RequiredArgsConstructor
@ToString
@EqualsAndHashCode
public class Subaddress implements Comparable<Subaddress> {
    @Getter
    final private int accountIndex;
    @Getter
    final private int addressIndex;
    @Getter
    final private String address;
    @Getter
    private final String label;
    @Getter
    @Setter
    private long amount;

    @Override
    public int compareTo(Subaddress another) { // newer is <
        final int compareAccountIndex = another.accountIndex - accountIndex;
        if (compareAccountIndex == 0)
            return another.addressIndex - addressIndex;
        return compareAccountIndex;
    }

    public String getSquashedAddress() {
        return address.substring(0, 8) + "…" + address.substring(address.length() - 8);
    }

    public static final Pattern DEFAULT_LABEL_FORMATTER = Pattern.compile("^[0-9]{4}-[0-9]{2}-[0-9]{2}-[0-9]{2}:[0-9]{2}:[0-9]{2}$");

    public String getDisplayLabel() {
        if (label.isEmpty() || (DEFAULT_LABEL_FORMATTER.matcher(label).matches()))
            return ("#" + addressIndex);
        else
            return label;
    }
}
