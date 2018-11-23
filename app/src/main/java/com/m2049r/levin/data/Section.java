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

package com.m2049r.levin.data;

import com.m2049r.levin.util.HexHelper;
import com.m2049r.levin.util.LevinReader;
import com.m2049r.levin.util.LevinWriter;
import com.m2049r.levin.util.LittleEndianDataOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Section {

    // constants copied from monero p2p & epee

    static final public int PORTABLE_STORAGE_SIGNATUREA = 0x01011101;
    static final public int PORTABLE_STORAGE_SIGNATUREB = 0x01020101;

    static final public byte PORTABLE_STORAGE_FORMAT_VER = 1;

    static final public byte PORTABLE_RAW_SIZE_MARK_MASK = 0x03;
    static final public byte PORTABLE_RAW_SIZE_MARK_BYTE = 0;
    static final public byte PORTABLE_RAW_SIZE_MARK_WORD = 1;
    static final public byte PORTABLE_RAW_SIZE_MARK_DWORD = 2;
    static final public byte PORTABLE_RAW_SIZE_MARK_INT64 = 3;

    static final long MAX_STRING_LEN_POSSIBLE = 2000000000; // do not let string be so big

    // data types
    static final public byte SERIALIZE_TYPE_INT64 = 1;
    static final public byte SERIALIZE_TYPE_INT32 = 2;
    static final public byte SERIALIZE_TYPE_INT16 = 3;
    static final public byte SERIALIZE_TYPE_INT8 = 4;
    static final public byte SERIALIZE_TYPE_UINT64 = 5;
    static final public byte SERIALIZE_TYPE_UINT32 = 6;
    static final public byte SERIALIZE_TYPE_UINT16 = 7;
    static final public byte SERIALIZE_TYPE_UINT8 = 8;
    static final public byte SERIALIZE_TYPE_DUOBLE = 9;
    static final public byte SERIALIZE_TYPE_STRING = 10;
    static final public byte SERIALIZE_TYPE_BOOL = 11;
    static final public byte SERIALIZE_TYPE_OBJECT = 12;
    static final public byte SERIALIZE_TYPE_ARRAY = 13;

    static final public byte SERIALIZE_FLAG_ARRAY = (byte) 0x80;

    private final Map<String, Object> entries = new HashMap<String, Object>();

    public void add(String key, Object entry) {
        entries.put(key, entry);
    }

    public int size() {
        return entries.size();
    }

    public Set<Map.Entry<String, Object>> entrySet() {
        return entries.entrySet();
    }

    public Object get(String key) {
        return entries.get(key);
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("\n");
        for (Map.Entry<String, Object> entry : entries.entrySet()) {
            sb.append(entry.getKey()).append("=");
            final Object value = entry.getValue();
            if (value instanceof List) {
                @SuppressWarnings("unchecked") final List<Object> list = (List<Object>) value;
                for (Object listEntry : list) {
                    sb.append(listEntry.toString()).append("\n");
                }
            } else if (value instanceof String) {
                sb.append("(").append(value).append(")\n");
            } else if (value instanceof byte[]) {
                sb.append(HexHelper.bytesToHex((byte[]) value)).append("\n");
            } else {
                sb.append(value.toString()).append("\n");
            }
        }
        return sb.toString();
    }

    static public Section fromByteArray(byte[] buffer) {
        try {
            return LevinReader.readPayload(buffer);
        } catch (IOException ex) {
            throw new IllegalStateException();
        }
    }

    public byte[] asByteArray() {
        try {
            ByteArrayOutputStream bas = new ByteArrayOutputStream();
            DataOutput out = new LittleEndianDataOutputStream(bas);
            LevinWriter writer = new LevinWriter(out);
            writer.writePayload(this);
            return bas.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException();
        }
    }
}
