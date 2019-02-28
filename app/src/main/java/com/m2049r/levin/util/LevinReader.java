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

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// Full Levin reader as seen on epee

public class LevinReader {
    private DataInput in;

    private LevinReader(byte[] buffer) {
        ByteArrayInputStream bis = new ByteArrayInputStream(buffer);
        in = new LittleEndianDataInputStream(bis);
    }

    static public Section readPayload(byte[] payload) throws IOException {
        LevinReader r = new LevinReader(payload);
        return r.readPayload();
    }

    private Section readPayload() throws IOException {
        if (in.readInt() != Section.PORTABLE_STORAGE_SIGNATUREA)
            throw new IllegalStateException();
        if (in.readInt() != Section.PORTABLE_STORAGE_SIGNATUREB)
            throw new IllegalStateException();
        if (in.readByte() != Section.PORTABLE_STORAGE_FORMAT_VER)
            throw new IllegalStateException();
        return readSection();
    }

    private Section readSection() throws IOException {
        Section section = new Section();
        long count = readVarint();
        while (count-- > 0) {
            // read section name string
            String sectionName = readSectionName();
            section.add(sectionName, loadStorageEntry());
        }
        return section;
    }

    private Object loadStorageArrayEntry(int type) throws IOException {
        type &= ~Section.SERIALIZE_FLAG_ARRAY;
        return readArrayEntry(type);
    }

    private List<Object> readArrayEntry(int type) throws IOException {
        List<Object> list = new ArrayList<Object>();
        long size = readVarint();
        while (size-- > 0)
            list.add(read(type));
        return list;
    }

    private Object read(int type) throws IOException {
        switch (type) {
            case Section.SERIALIZE_TYPE_UINT64:
            case Section.SERIALIZE_TYPE_INT64:
                return in.readLong();
            case Section.SERIALIZE_TYPE_UINT32:
            case Section.SERIALIZE_TYPE_INT32:
                return in.readInt();
            case Section.SERIALIZE_TYPE_UINT16:
                return in.readUnsignedShort();
            case Section.SERIALIZE_TYPE_INT16:
                return in.readShort();
            case Section.SERIALIZE_TYPE_UINT8:
                return in.readUnsignedByte();
            case Section.SERIALIZE_TYPE_INT8:
                return in.readByte();
            case Section.SERIALIZE_TYPE_OBJECT:
                return readSection();
            case Section.SERIALIZE_TYPE_STRING:
                return readByteArray();
            default:
                throw new IllegalArgumentException("type " + type
                        + " not supported");
        }
    }

    private Object loadStorageEntry() throws IOException {
        int type = in.readUnsignedByte();
        if ((type & Section.SERIALIZE_FLAG_ARRAY) != 0)
            return loadStorageArrayEntry(type);
        if (type == Section.SERIALIZE_TYPE_ARRAY)
            return readStorageEntryArrayEntry();
        else
            return readStorageEntry(type);
    }

    private Object readStorageEntry(int type) throws IOException {
        return read(type);
    }

    private Object readStorageEntryArrayEntry() throws IOException {
        int type = in.readUnsignedByte();
        if ((type & Section.SERIALIZE_FLAG_ARRAY) != 0)
            throw new IllegalStateException("wrong type sequences");
        return loadStorageArrayEntry(type);
    }

    private String readSectionName() throws IOException {
        int nameLen = in.readUnsignedByte();
        return readString(nameLen);
    }

    private byte[] read(long count) throws IOException {
        if (count > Integer.MAX_VALUE)
            throw new IllegalArgumentException();
        int len = (int) count;
        final byte buffer[] = new byte[len];
        in.readFully(buffer);
        return buffer;
    }

    private String readString(long count) throws IOException {
        return new String(read(count), StandardCharsets.US_ASCII);
    }

    private byte[] readByteArray(long count) throws IOException {
        return read(count);
    }

    private byte[] readByteArray() throws IOException {
        long len = readVarint();
        return readByteArray(len);
    }

    private long readVarint() throws IOException {
        long v = 0;
        int b = in.readUnsignedByte();
        int sizeMask = b & Section.PORTABLE_RAW_SIZE_MARK_MASK;
        switch (sizeMask) {
            case Section.PORTABLE_RAW_SIZE_MARK_BYTE:
                v = b >>> 2;
                break;
            case Section.PORTABLE_RAW_SIZE_MARK_WORD:
                v = readRest(b, 1) >>> 2;
                break;
            case Section.PORTABLE_RAW_SIZE_MARK_DWORD:
                v = readRest(b, 3) >>> 2;
                break;
            case Section.PORTABLE_RAW_SIZE_MARK_INT64:
                v = readRest(b, 7) >>> 2;
                break;
            default:
                throw new IllegalStateException();
        }
        return v;
    }

    // this should be in LittleEndianDataInputStream because it has little
    // endian logic
    private long readRest(final int firstByte, final int bytes) throws IOException {
        long result = firstByte;
        for (int i = 1; i < bytes + 1; i++) {
            result = result + (((long) in.readUnsignedByte()) << (8 * i));
        }
        return result;
    }

}
