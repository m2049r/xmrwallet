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

package com.m2049r.levin.util;

import com.m2049r.levin.data.Section;

import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

// a simplified Levin Writer WITHOUT support for arrays

public class LevinWriter {
    private DataOutput out;

    public LevinWriter(DataOutput out) {
        this.out = out;
    }

    public void writePayload(Section section) throws IOException {
        out.writeInt(Section.PORTABLE_STORAGE_SIGNATUREA);
        out.writeInt(Section.PORTABLE_STORAGE_SIGNATUREB);
        out.writeByte(Section.PORTABLE_STORAGE_FORMAT_VER);
        putSection(section);
    }

    private void writeSection(Section section) throws IOException {
        out.writeByte(Section.SERIALIZE_TYPE_OBJECT);
        putSection(section);
    }

    private void putSection(Section section) throws IOException {
        writeVarint(section.size());
        for (Map.Entry<String, Object> kv : section.entrySet()) {
            byte[] key = kv.getKey().getBytes(StandardCharsets.US_ASCII);
            out.writeByte(key.length);
            out.write(key);
            write(kv.getValue());
        }
    }

    private void writeVarint(long i) throws IOException {
        if (i <= 63) {
            out.writeByte(((int) i << 2) | Section.PORTABLE_RAW_SIZE_MARK_BYTE);
        } else if (i <= 16383) {
            out.writeShort(((int) i << 2) | Section.PORTABLE_RAW_SIZE_MARK_WORD);
        } else if (i <= 1073741823) {
            out.writeInt(((int) i << 2) | Section.PORTABLE_RAW_SIZE_MARK_DWORD);
        } else {
            if (i > 4611686018427387903L)
                throw new IllegalArgumentException();
            out.writeLong((i << 2) | Section.PORTABLE_RAW_SIZE_MARK_INT64);
        }
    }

    private void write(Object object) throws IOException {
        if (object instanceof byte[]) {
            byte[] value = (byte[]) object;
            out.writeByte(Section.SERIALIZE_TYPE_STRING);
            writeVarint(value.length);
            out.write(value);
        } else if (object instanceof String) {
            byte[] value = ((String) object)
                    .getBytes(StandardCharsets.US_ASCII);
            out.writeByte(Section.SERIALIZE_TYPE_STRING);
            writeVarint(value.length);
            out.write(value);
        } else if (object instanceof Integer) {
            out.writeByte(Section.SERIALIZE_TYPE_UINT32);
            out.writeInt((int) object);
        } else if (object instanceof Long) {
            out.writeByte(Section.SERIALIZE_TYPE_UINT64);
            out.writeLong((long) object);
        } else if (object instanceof Byte) {
            out.writeByte(Section.SERIALIZE_TYPE_UINT8);
            out.writeByte((byte) object);
        } else if (object instanceof Section) {
            writeSection((Section) object);
        } else {
            throw new IllegalArgumentException();
        }
    }
}
