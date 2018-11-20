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

import java.io.DataInput;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UTFDataFormatException;

/**
 * A little endian java.io.DataInputStream (without readLine())
 */

public class LittleEndianDataInputStream extends FilterInputStream implements
        DataInput {

    /**
     * Creates a DataInputStream that uses the specified underlying InputStream.
     *
     * @param in the specified input stream
     */
    public LittleEndianDataInputStream(InputStream in) {
        super(in);
    }

    @Deprecated
    public final String readLine() {
        throw new UnsupportedOperationException();
    }

    /**
     * Reads some number of bytes from the contained input stream and stores
     * them into the buffer array <code>b</code>. The number of bytes actually
     * read is returned as an integer. This method blocks until input data is
     * available, end of file is detected, or an exception is thrown.
     *
     * <p>
     * If <code>b</code> is null, a <code>NullPointerException</code> is thrown.
     * If the length of <code>b</code> is zero, then no bytes are read and
     * <code>0</code> is returned; otherwise, there is an attempt to read at
     * least one byte. If no byte is available because the stream is at end of
     * file, the value <code>-1</code> is returned; otherwise, at least one byte
     * is read and stored into <code>b</code>.
     *
     * <p>
     * The first byte read is stored into element <code>b[0]</code>, the next
     * one into <code>b[1]</code>, and so on. The number of bytes read is, at
     * most, equal to the length of <code>b</code>. Let <code>k</code> be the
     * number of bytes actually read; these bytes will be stored in elements
     * <code>b[0]</code> through <code>b[k-1]</code>, leaving elements
     * <code>b[k]</code> through <code>b[b.length-1]</code> unaffected.
     *
     * <p>
     * The <code>read(b)</code> method has the same effect as: <blockquote>
     *
     * <pre>
     * read(b, 0, b.length)
     * </pre>
     *
     * </blockquote>
     *
     * @param b the buffer into which the data is read.
     * @return the total number of bytes read into the buffer, or
     * <code>-1</code> if there is no more data because the end of the
     * stream has been reached.
     * @throws IOException if the first byte cannot be read for any reason other than
     *                     end of file, the stream has been closed and the underlying
     *                     input stream does not support reading after close, or
     *                     another I/O error occurs.
     * @see FilterInputStream#in
     * @see InputStream#read(byte[], int, int)
     */
    public final int read(byte b[]) throws IOException {
        return in.read(b, 0, b.length);
    }

    /**
     * Reads up to <code>len</code> bytes of data from the contained input
     * stream into an array of bytes. An attempt is made to read as many as
     * <code>len</code> bytes, but a smaller number may be read, possibly zero.
     * The number of bytes actually read is returned as an integer.
     *
     * <p>
     * This method blocks until input data is available, end of file is
     * detected, or an exception is thrown.
     *
     * <p>
     * If <code>len</code> is zero, then no bytes are read and <code>0</code> is
     * returned; otherwise, there is an attempt to read at least one byte. If no
     * byte is available because the stream is at end of file, the value
     * <code>-1</code> is returned; otherwise, at least one byte is read and
     * stored into <code>b</code>.
     *
     * <p>
     * The first byte read is stored into element <code>b[off]</code>, the next
     * one into <code>b[off+1]</code>, and so on. The number of bytes read is,
     * at most, equal to <code>len</code>. Let <i>k</i> be the number of bytes
     * actually read; these bytes will be stored in elements <code>b[off]</code>
     * through <code>b[off+</code><i>k</i><code>-1]</code>, leaving elements
     * <code>b[off+</code><i>k</i><code>]</code> through
     * <code>b[off+len-1]</code> unaffected.
     *
     * <p>
     * In every case, elements <code>b[0]</code> through <code>b[off]</code> and
     * elements <code>b[off+len]</code> through <code>b[b.length-1]</code> are
     * unaffected.
     *
     * @param b   the buffer into which the data is read.
     * @param off the start offset in the destination array <code>b</code>
     * @param len the maximum number of bytes read.
     * @return the total number of bytes read into the buffer, or
     * <code>-1</code> if there is no more data because the end of the
     * stream has been reached.
     * @throws NullPointerException      If <code>b</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException If <code>off</code> is negative, <code>len</code> is
     *                                   negative, or <code>len</code> is greater than
     *                                   <code>b.length - off</code>
     * @throws IOException               if the first byte cannot be read for any reason other than
     *                                   end of file, the stream has been closed and the underlying
     *                                   input stream does not support reading after close, or
     *                                   another I/O error occurs.
     * @see FilterInputStream#in
     * @see InputStream#read(byte[], int, int)
     */
    public final int read(byte b[], int off, int len) throws IOException {
        return in.read(b, off, len);
    }

    /**
     * See the general contract of the <code>readFully</code> method of
     * <code>DataInput</code>.
     * <p>
     * Bytes for this operation are read from the contained input stream.
     *
     * @param b the buffer into which the data is read.
     * @throws EOFException if this input stream reaches the end before reading all
     *                      the bytes.
     * @throws IOException  the stream has been closed and the contained input stream
     *                      does not support reading after close, or another I/O error
     *                      occurs.
     * @see FilterInputStream#in
     */
    public final void readFully(byte b[]) throws IOException {
        readFully(b, 0, b.length);
    }

    /**
     * See the general contract of the <code>readFully</code> method of
     * <code>DataInput</code>.
     * <p>
     * Bytes for this operation are read from the contained input stream.
     *
     * @param b   the buffer into which the data is read.
     * @param off the start offset of the data.
     * @param len the number of bytes to read.
     * @throws EOFException if this input stream reaches the end before reading all
     *                      the bytes.
     * @throws IOException  the stream has been closed and the contained input stream
     *                      does not support reading after close, or another I/O error
     *                      occurs.
     * @see FilterInputStream#in
     */
    public final void readFully(byte b[], int off, int len) throws IOException {
        if (len < 0)
            throw new IndexOutOfBoundsException();
        int n = 0;
        while (n < len) {
            int count = in.read(b, off + n, len - n);
            if (count < 0)
                throw new EOFException();
            n += count;
        }
    }

    /**
     * See the general contract of the <code>skipBytes</code> method of
     * <code>DataInput</code>.
     * <p>
     * Bytes for this operation are read from the contained input stream.
     *
     * @param n the number of bytes to be skipped.
     * @return the actual number of bytes skipped.
     * @throws IOException if the contained input stream does not support seek, or
     *                     the stream has been closed and the contained input stream
     *                     does not support reading after close, or another I/O error
     *                     occurs.
     */
    public final int skipBytes(int n) throws IOException {
        int total = 0;
        int cur = 0;

        while ((total < n) && ((cur = (int) in.skip(n - total)) > 0)) {
            total += cur;
        }

        return total;
    }

    /**
     * See the general contract of the <code>readBoolean</code> method of
     * <code>DataInput</code>.
     * <p>
     * Bytes for this operation are read from the contained input stream.
     *
     * @return the <code>boolean</code> value read.
     * @throws EOFException if this input stream has reached the end.
     * @throws IOException  the stream has been closed and the contained input stream
     *                      does not support reading after close, or another I/O error
     *                      occurs.
     * @see FilterInputStream#in
     */
    public final boolean readBoolean() throws IOException {
        int ch = in.read();
        if (ch < 0)
            throw new EOFException();
        return (ch != 0);
    }

    /**
     * See the general contract of the <code>readByte</code> method of
     * <code>DataInput</code>.
     * <p>
     * Bytes for this operation are read from the contained input stream.
     *
     * @return the next byte of this input stream as a signed 8-bit
     * <code>byte</code>.
     * @throws EOFException if this input stream has reached the end.
     * @throws IOException  the stream has been closed and the contained input stream
     *                      does not support reading after close, or another I/O error
     *                      occurs.
     * @see FilterInputStream#in
     */
    public final byte readByte() throws IOException {
        int ch = in.read();
        if (ch < 0)
            throw new EOFException();
        return (byte) (ch);
    }

    /**
     * See the general contract of the <code>readUnsignedByte</code> method of
     * <code>DataInput</code>.
     * <p>
     * Bytes for this operation are read from the contained input stream.
     *
     * @return the next byte of this input stream, interpreted as an unsigned
     * 8-bit number.
     * @throws EOFException if this input stream has reached the end.
     * @throws IOException  the stream has been closed and the contained input stream
     *                      does not support reading after close, or another I/O error
     *                      occurs.
     * @see FilterInputStream#in
     */
    public final int readUnsignedByte() throws IOException {
        int ch = in.read();
        if (ch < 0)
            throw new EOFException();
        return ch;
    }

    /**
     * See the general contract of the <code>readShort</code> method of
     * <code>DataInput</code>.
     * <p>
     * Bytes for this operation are read from the contained input stream.
     *
     * @return the next two bytes of this input stream, interpreted as a signed
     * 16-bit number.
     * @throws EOFException if this input stream reaches the end before reading two
     *                      bytes.
     * @throws IOException  the stream has been closed and the contained input stream
     *                      does not support reading after close, or another I/O error
     *                      occurs.
     * @see FilterInputStream#in
     */
    public final short readShort() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return (short) ((ch1 << 0) + (ch2 << 8));
    }

    /**
     * See the general contract of the <code>readUnsignedShort</code> method of
     * <code>DataInput</code>.
     * <p>
     * Bytes for this operation are read from the contained input stream.
     *
     * @return the next two bytes of this input stream, interpreted as an
     * unsigned 16-bit integer.
     * @throws EOFException if this input stream reaches the end before reading two
     *                      bytes.
     * @throws IOException  the stream has been closed and the contained input stream
     *                      does not support reading after close, or another I/O error
     *                      occurs.
     * @see FilterInputStream#in
     */
    public final int readUnsignedShort() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return (ch1 << 0) + (ch2 << 8);
    }

    /**
     * See the general contract of the <code>readChar</code> method of
     * <code>DataInput</code>.
     * <p>
     * Bytes for this operation are read from the contained input stream.
     *
     * @return the next two bytes of this input stream, interpreted as a
     * <code>char</code>.
     * @throws EOFException if this input stream reaches the end before reading two
     *                      bytes.
     * @throws IOException  the stream has been closed and the contained input stream
     *                      does not support reading after close, or another I/O error
     *                      occurs.
     * @see FilterInputStream#in
     */
    public final char readChar() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return (char) ((ch1 << 0) + (ch2 << 8));
    }

    /**
     * See the general contract of the <code>readInt</code> method of
     * <code>DataInput</code>.
     * <p>
     * Bytes for this operation are read from the contained input stream.
     *
     * @return the next four bytes of this input stream, interpreted as an
     * <code>int</code>.
     * @throws EOFException if this input stream reaches the end before reading four
     *                      bytes.
     * @throws IOException  the stream has been closed and the contained input stream
     *                      does not support reading after close, or another I/O error
     *                      occurs.
     * @see FilterInputStream#in
     */
    public final int readInt() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        return ((ch1 << 0) + (ch2 << 8) + (ch3 << 16) + (ch4 << 24));
    }

    private byte readBuffer[] = new byte[8];

    /**
     * See the general contract of the <code>readLong</code> method of
     * <code>DataInput</code>.
     * <p>
     * Bytes for this operation are read from the contained input stream.
     *
     * @return the next eight bytes of this input stream, interpreted as a
     * <code>long</code>.
     * @throws EOFException if this input stream reaches the end before reading eight
     *                      bytes.
     * @throws IOException  the stream has been closed and the contained input stream
     *                      does not support reading after close, or another I/O error
     *                      occurs.
     * @see FilterInputStream#in
     */
    public final long readLong() throws IOException {
        readFully(readBuffer, 0, 8);
        return (((long) readBuffer[7] << 56)
                + ((long) (readBuffer[6] & 255) << 48)
                + ((long) (readBuffer[5] & 255) << 40)
                + ((long) (readBuffer[4] & 255) << 32)
                + ((long) (readBuffer[3] & 255) << 24)
                + ((readBuffer[2] & 255) << 16) + ((readBuffer[1] & 255) << 8) + ((readBuffer[0] & 255) << 0));
    }

    /**
     * See the general contract of the <code>readFloat</code> method of
     * <code>DataInput</code>.
     * <p>
     * Bytes for this operation are read from the contained input stream.
     *
     * @return the next four bytes of this input stream, interpreted as a
     * <code>float</code>.
     * @throws EOFException if this input stream reaches the end before reading four
     *                      bytes.
     * @throws IOException  the stream has been closed and the contained input stream
     *                      does not support reading after close, or another I/O error
     *                      occurs.
     * @see java.io.DataInputStream#readInt()
     * @see Float#intBitsToFloat(int)
     */
    public final float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    /**
     * See the general contract of the <code>readDouble</code> method of
     * <code>DataInput</code>.
     * <p>
     * Bytes for this operation are read from the contained input stream.
     *
     * @return the next eight bytes of this input stream, interpreted as a
     * <code>double</code>.
     * @throws EOFException if this input stream reaches the end before reading eight
     *                      bytes.
     * @throws IOException  the stream has been closed and the contained input stream
     *                      does not support reading after close, or another I/O error
     *                      occurs.
     * @see java.io.DataInputStream#readLong()
     * @see Double#longBitsToDouble(long)
     */
    public final double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    /**
     * See the general contract of the <code>readUTF</code> method of
     * <code>DataInput</code>.
     * <p>
     * Bytes for this operation are read from the contained input stream.
     *
     * @return a Unicode string.
     * @throws EOFException           if this input stream reaches the end before reading all
     *                                the bytes.
     * @throws IOException            the stream has been closed and the contained input stream
     *                                does not support reading after close, or another I/O error
     *                                occurs.
     * @throws UTFDataFormatException if the bytes do not represent a valid modified UTF-8
     *                                encoding of a string.
     * @see java.io.DataInputStream#readUTF(DataInput)
     */
    public final String readUTF() throws IOException {
        return readUTF(this);
    }

    /**
     * working arrays initialized on demand by readUTF
     */
    private byte bytearr[] = new byte[80];
    private char chararr[] = new char[80];

    /**
     * Reads from the stream <code>in</code> a representation of a Unicode
     * character string encoded in <a
     * href="DataInput.html#modified-utf-8">modified UTF-8</a> format; this
     * string of characters is then returned as a <code>String</code>. The
     * details of the modified UTF-8 representation are exactly the same as for
     * the <code>readUTF</code> method of <code>DataInput</code>.
     *
     * @param in a data input stream.
     * @return a Unicode string.
     * @throws EOFException           if the input stream reaches the end before all the bytes.
     * @throws IOException            the stream has been closed and the contained input stream
     *                                does not support reading after close, or another I/O error
     *                                occurs.
     * @throws UTFDataFormatException if the bytes do not represent a valid modified UTF-8
     *                                encoding of a Unicode string.
     * @see java.io.DataInputStream#readUnsignedShort()
     */
    public final static String readUTF(DataInput in) throws IOException {
        int utflen = in.readUnsignedShort();
        byte[] bytearr = null;
        char[] chararr = null;
        if (in instanceof LittleEndianDataInputStream) {
            LittleEndianDataInputStream dis = (LittleEndianDataInputStream) in;
            if (dis.bytearr.length < utflen) {
                dis.bytearr = new byte[utflen * 2];
                dis.chararr = new char[utflen * 2];
            }
            chararr = dis.chararr;
            bytearr = dis.bytearr;
        } else {
            bytearr = new byte[utflen];
            chararr = new char[utflen];
        }

        int c, char2, char3;
        int count = 0;
        int chararr_count = 0;

        in.readFully(bytearr, 0, utflen);

        while (count < utflen) {
            c = (int) bytearr[count] & 0xff;
            if (c > 127)
                break;
            count++;
            chararr[chararr_count++] = (char) c;
        }

        while (count < utflen) {
            c = (int) bytearr[count] & 0xff;
            switch (c >> 4) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    /* 0xxxxxxx */
                    count++;
                    chararr[chararr_count++] = (char) c;
                    break;
                case 12:
                case 13:
                    /* 110x xxxx 10xx xxxx */
                    count += 2;
                    if (count > utflen)
                        throw new UTFDataFormatException(
                                "malformed input: partial character at end");
                    char2 = (int) bytearr[count - 1];
                    if ((char2 & 0xC0) != 0x80)
                        throw new UTFDataFormatException(
                                "malformed input around byte " + count);
                    chararr[chararr_count++] = (char) (((c & 0x1F) << 6) | (char2 & 0x3F));
                    break;
                case 14:
                    /* 1110 xxxx 10xx xxxx 10xx xxxx */
                    count += 3;
                    if (count > utflen)
                        throw new UTFDataFormatException(
                                "malformed input: partial character at end");
                    char2 = (int) bytearr[count - 2];
                    char3 = (int) bytearr[count - 1];
                    if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80))
                        throw new UTFDataFormatException(
                                "malformed input around byte " + (count - 1));
                    chararr[chararr_count++] = (char) (((c & 0x0F) << 12)
                            | ((char2 & 0x3F) << 6) | ((char3 & 0x3F) << 0));
                    break;
                default:
                    /* 10xx xxxx, 1111 xxxx */
                    throw new UTFDataFormatException("malformed input around byte "
                            + count);
            }
        }
        // The number of chars produced may be less than utflen
        return new String(chararr, 0, chararr_count);
    }
}
