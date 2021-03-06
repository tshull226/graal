/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.oracle.truffle.regex.tregex.buffer;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.regex.charset.RangesBuffer;

/**
 * Extension of {@link CharArrayBuffer} that adds convenience functions for arrays of character
 * ranges in the form:
 *
 * <pre>
 * [
 *     inclusive lower bound of range 1, inclusive upper bound of range 1,
 *     inclusive lower bound of range 2, inclusive upper bound of range 2,
 *     inclusive lower bound of range 3, inclusive upper bound of range 3,
 *     ...
 * ]
 * </pre>
 */
public class CharRangesBuffer extends CharArrayBuffer implements RangesBuffer {

    public CharRangesBuffer() {
        this(16);
    }

    public CharRangesBuffer(int initialSize) {
        super(initialSize);
    }

    @Override
    public int getMinValue() {
        return Character.MIN_VALUE;
    }

    @Override
    public int getMaxValue() {
        return Character.MAX_VALUE;
    }

    @Override
    public int getLo(int i) {
        return buf[i * 2];
    }

    @Override
    public int getHi(int i) {
        return buf[i * 2 + 1];
    }

    @Override
    public int size() {
        return length() / 2;
    }

    @Override
    public void appendRange(int lo, int hi) {
        assert isEmpty() || leftOf(size() - 1, lo, hi) && !adjacent(size() - 1, lo, hi);
        add((char) lo);
        add((char) hi);
    }

    @Override
    public void insertRange(int index, int lo, int hi) {
        assert index >= 0 && index < size();
        assert index == 0 || leftOf(index - 1, lo, hi) && !adjacent(index - 1, lo, hi);
        assert rightOf(index, lo, hi) && !adjacent(index, lo, hi);
        ensureCapacity(length + 2);
        int i = index * 2;
        System.arraycopy(buf, i, buf, i + 2, length - i);
        buf[i] = (char) lo;
        buf[i + 1] = (char) hi;
        length += 2;
    }

    @Override
    public void replaceRanges(int fromIndex, int toIndex, int lo, int hi) {
        assert fromIndex >= 0 && fromIndex < toIndex && toIndex >= 0 && toIndex <= size();
        assert fromIndex == 0 || leftOf(fromIndex - 1, lo, hi) && !adjacent(fromIndex - 1, lo, hi);
        assert toIndex == size() || rightOf(toIndex, lo, hi) && !adjacent(toIndex, lo, hi);
        buf[fromIndex * 2] = (char) lo;
        buf[fromIndex * 2 + 1] = (char) hi;
        if (toIndex < size()) {
            System.arraycopy(buf, toIndex * 2, buf, fromIndex * 2 + 2, length - (toIndex * 2));
        }
        length -= (toIndex - (fromIndex + 1)) * 2;
    }

    @Override
    public void appendRangesTo(RangesBuffer buffer, int startIndex, int endIndex) {
        assert buffer instanceof CharRangesBuffer;
        int bulkLength = (endIndex - startIndex) * 2;
        if (bulkLength == 0) {
            return;
        }
        CharRangesBuffer o = (CharRangesBuffer) buffer;
        int newSize = o.length() + bulkLength;
        o.ensureCapacity(newSize);
        assert o.isEmpty() || rightOf(startIndex, o, o.size() - 1);
        System.arraycopy(buf, startIndex * 2, o.getBuffer(), o.length(), bulkLength);
        o.setLength(newSize);
    }

    @SuppressWarnings("unchecked")
    @Override
    public CharRangesBuffer create() {
        return new CharRangesBuffer(buf.length);
    }

    @TruffleBoundary
    @Override
    public String toString() {
        return defaultToString();
    }
}
