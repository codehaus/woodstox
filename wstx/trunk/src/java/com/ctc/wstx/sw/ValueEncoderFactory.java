/* Woodstox XML processor
 *
 * Copyright (c) 2008- Tatu Saloranta, tatu.saloranta@iki.fi
 *
 * Licensed under the License specified in file LICENSE, included with
 * the source code.
 * You may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ctc.wstx.sw;

import com.ctc.wstx.util.NumberUtil;

/**
 * Factory class used to construct and recycle all
 * {@link AsciiValueEncoder} instances needed by a single
 * stream writer instance.
 *<p>
 * Since encoders are recycled, instances are not thread-safe.
 *
 * @since 4.0
 */
public final class ValueEncoderFactory
{
    final static byte BYTE_SPACE = (byte) ' ';

    // // // Lazily-constructed, recycled encoder instances

    protected TextualScalarEncoder mTextualEncoder = null;
    protected IntEncoder mIntEncoder = null;
    protected LongEncoder mLongEncoder = null;
    protected FloatEncoder mFloatEncoder = null;
    protected DoubleEncoder mDoubleEncoder = null;

    protected IntArrayEncoder mIntArrayEncoder = null;
    protected LongArrayEncoder mLongArrayEncoder = null;
    protected FloatArrayEncoder mFloatArrayEncoder = null;
    protected DoubleArrayEncoder mDoubleArrayEncoder = null;

    public ValueEncoderFactory() { }

    // // // Scalar encoder access

    public TextualScalarEncoder getScalarEncoder(String textual)
    {
        if (mTextualEncoder == null) {
            mTextualEncoder = new TextualScalarEncoder();
        }
        mTextualEncoder.reset(textual);
        return mTextualEncoder;
    }

    public IntEncoder getEncoder(int value)
    {
        if (mIntEncoder == null) {
            mIntEncoder = new IntEncoder();
        }
        mIntEncoder.reset(value);
        return mIntEncoder;
    }

    public LongEncoder getEncoder(long value)
    {
        if (mLongEncoder == null) {
            mLongEncoder = new LongEncoder();
        }
        mLongEncoder.reset(value);
        return mLongEncoder;
    }

    public FloatEncoder getEncoder(float value)
    {
        if (mFloatEncoder == null) {
            mFloatEncoder = new FloatEncoder();
        }
        mFloatEncoder.reset(value);
        return mFloatEncoder;
    }

    public DoubleEncoder getEncoder(double value)
    {
        if (mDoubleEncoder == null) {
            mDoubleEncoder = new DoubleEncoder();
        }
        mDoubleEncoder.reset(value);
        return mDoubleEncoder;
    }

    // // // Array encoder access

    public IntArrayEncoder getEncoder(int[] values, int from, int length)
    {
        if (mIntArrayEncoder == null) {
            mIntArrayEncoder = new IntArrayEncoder();
        }
        mIntArrayEncoder.reset(values, from, length);
        return mIntArrayEncoder;
    }

    public LongArrayEncoder getEncoder(long[] values, int from, int length)
    {
        if (mLongArrayEncoder == null) {
            mLongArrayEncoder = new LongArrayEncoder();
        }
        mLongArrayEncoder.reset(values, from, length);
        return mLongArrayEncoder;
    }

    public FloatArrayEncoder getEncoder(float[] values, int from, int length)
    {
        if (mFloatArrayEncoder == null) {
            mFloatArrayEncoder = new FloatArrayEncoder();
        }
        mFloatArrayEncoder.reset(values, from, length);
        return mFloatArrayEncoder;
    }

    public DoubleArrayEncoder getEncoder(double[] values, int from, int length)
    {
        if (mDoubleArrayEncoder == null) {
            mDoubleArrayEncoder = new DoubleArrayEncoder();
        }
        mDoubleArrayEncoder.reset(values, from, length);
        return mDoubleArrayEncoder;
    }

    /*
    ////////////////////////////////////////////////////////////////
    // Implementation classes; first, scalar (single-value) encoders
    ////////////////////////////////////////////////////////////////
     */

    /**
     * Intermediate base class for encoders that deal with single
     * primitive values.
     *<p>
     * No default implementations, because textual and typed
     * (non-textual) sub-classes differ significantly.
     * In a way, this is just a tag class
     */
    abstract static class ScalarEncoder
        extends AsciiValueEncoder
    {
        protected ScalarEncoder() { }
    }

    /**
     * Implementation of generic scalar value for which textual
     * representation is efficient (enough) representation.
     */
    final static class TextualScalarEncoder
        extends ScalarEncoder
    {
        String mValue;

        protected TextualScalarEncoder() { super(); }

        protected void reset(String value) {
            mValue = value;
        }

        protected int maxElementLength() { return mValue.length(); }

        public boolean bufferNeedsFlush(int freeChars)
        {
            return (mValue != null) && (freeChars < mValue.length());
        }

        public boolean isCompleted() { return (mValue == null); }

        public int encodeMore(char[] buffer, int ptr, int end)
        {
            if (mValue != null) {
                String str = mValue;
                mValue = null;
                int len = str.length();
                str.getChars(0, len, buffer, ptr);
                ptr += len;
            }
            return ptr;
        }

        public int encodeMore(byte[] buffer, int ptr, int end)
        {
            if (mValue != null) {
                String str = mValue;
                mValue = null;
                int len = str.length();
                for (int i = 0; i < len; ++i) {
                    buffer[ptr++] = (byte) str.charAt(i);
                }
            }
            return ptr;
        }
    }

    /**
     * Intermediate base class for typed (non-textual) scalar values
     */
    abstract static class TypedScalarEncoder
        extends ScalarEncoder
    {
        protected boolean mWritten;

        protected TypedScalarEncoder() { }

        protected void reset() {
            mWritten = false;
        }

        public final boolean bufferNeedsFlush(int freeChars)
        {
            return !mWritten && (freeChars < maxElementLength());
        }

        public boolean isCompleted() { return mWritten; }

        /**
         * @return Maximum length of an individual element <b>plus one</b>
         *   (for space separating elements)
         */
        protected abstract int maxElementLength();
    }

    final static class IntEncoder
        extends TypedScalarEncoder
    {
        int mValue;

        protected IntEncoder() { super(); }

        protected void reset(int value) {
            super.reset();
            mValue = value;
        }

        protected int maxElementLength() { return NumberUtil.MAX_INT_CLEN; }

        public int encodeMore(char[] buffer, int ptr, int end)
        {
            mWritten = true;
            return NumberUtil.writeInt(mValue, buffer, ptr);
        }

        public int encodeMore(byte[] buffer, int ptr, int end)
        {
            mWritten = true;
            return NumberUtil.writeInt(mValue, buffer, ptr);
        }
    }

    final static class LongEncoder
        extends TypedScalarEncoder
    {
        long mValue;

        protected LongEncoder() { super(); }

        protected void reset(long value) {
            super.reset();
            mValue = value;
        }

        protected int maxElementLength() { return NumberUtil.MAX_LONG_CLEN; }

        public int encodeMore(char[] buffer, int ptr, int end)
        {
            mWritten = true;
            return NumberUtil.writeLong(mValue, buffer, ptr);
        }

        public int encodeMore(byte[] buffer, int ptr, int end)
        {
            mWritten = true;
            return NumberUtil.writeLong(mValue, buffer, ptr);
        }
    }

    final static class FloatEncoder
        extends TypedScalarEncoder
    {
        float mValue;

        protected FloatEncoder() { super(); }

        protected void reset(float value) {
            super.reset();
            mValue = value;
        }

        protected int maxElementLength() { return NumberUtil.MAX_FLOAT_CLEN; }

        public int encodeMore(char[] buffer, int ptr, int end)
        {
            mWritten = true;
            return NumberUtil.writeFloat(mValue, buffer, ptr);
        }

        public int encodeMore(byte[] buffer, int ptr, int end)
        {
            mWritten = true;
            return NumberUtil.writeFloat(mValue, buffer, ptr);
        }
    }

    final static class DoubleEncoder
        extends TypedScalarEncoder
    {
        double mValue;

        protected DoubleEncoder() { super(); }

        protected void reset(double value) {
            super.reset();
            mValue = value;
        }

        protected int maxElementLength() { return NumberUtil.MAX_DOUBLE_CLEN; }

        public int encodeMore(char[] buffer, int ptr, int end)
        {
            mWritten = true;
            return NumberUtil.writeDouble(mValue, buffer, ptr);
        }

        public int encodeMore(byte[] buffer, int ptr, int end)
        {
            mWritten = true;
            return NumberUtil.writeDouble(mValue, buffer, ptr);
        }
    }

    /*
    ////////////////////////////////////////////////////////////////
    // Implementation classes; array encoders
    ////////////////////////////////////////////////////////////////
     */

    /**
     * Intermediate base class for encoders that deal with arrays
     * of values.
     */
    abstract static class ArrayEncoder
        extends AsciiValueEncoder
    {
        int mPtr;
        int mEnd;

        protected ArrayEncoder() { }

        protected void reset(int from, int length)
        {
            mPtr = from;
            mEnd = from+length;
        }

        public final boolean bufferNeedsFlush(int freeChars)
        {
            // subtract one for trailing space
            return (mPtr < mEnd) && (freeChars < (maxElementLength()+1));
        }

        public final boolean isCompleted() { return (mPtr >= mEnd); }

        public abstract int encodeMore(char[] buffer, int ptr, int end);

        /**
         * @return Maximum length of an individual element <b>plus one</b>
         *   (for space separating elements)
         */
        protected abstract int maxElementLength();
    }

    /**
     * Concrete implementation used for encoding int[] content.
     */
    final static class IntArrayEncoder
        extends ArrayEncoder
    {
        int[] mValues;

        protected IntArrayEncoder() { super(); }

        protected void reset(int[] values, int from, int length) {
            super.reset(from, length);
            mValues = values;
        }

        protected int maxElementLength() { return 1+NumberUtil.MAX_INT_CLEN; }

        public int encodeMore(char[] buffer, int ptr, int end)
        {
            int lastOk = end - maxElementLength();
            while (ptr <= lastOk && mPtr < mEnd) {
                buffer[ptr++] = ' ';
                ptr = NumberUtil.writeInt(mValues[mPtr++], buffer, ptr);
            }
            return ptr;
        }

        public int encodeMore(byte[] buffer, int ptr, int end)
        {
            int lastOk = end - maxElementLength();
            while (ptr <= lastOk && mPtr < mEnd) {
                buffer[ptr++] = BYTE_SPACE;
                ptr = NumberUtil.writeInt(mValues[mPtr++], buffer, ptr);
            }
            return ptr;
        }
    }

    final static class LongArrayEncoder
        extends ArrayEncoder
    {
        long[] mValues;

        protected LongArrayEncoder() { super(); }

        protected void reset(long[] values, int from, int length) {
            super.reset(from, length);
            mValues = values;
        }

        protected int maxElementLength() { return NumberUtil.MAX_LONG_CLEN+1; }

        public int encodeMore(char[] buffer, int ptr, int end)
        {
            int lastOk = end - maxElementLength();
            while (ptr <= lastOk && mPtr < mEnd) {
                buffer[ptr++] = ' ';
                ptr = NumberUtil.writeLong(mValues[mPtr++], buffer, ptr);
            }
            return ptr;
        }

        public int encodeMore(byte[] buffer, int ptr, int end)
        {
            int lastOk = end - maxElementLength();
            while (ptr <= lastOk && mPtr < mEnd) {
                buffer[ptr++] = BYTE_SPACE;
                ptr = NumberUtil.writeLong(mValues[mPtr++], buffer, ptr);
            }
            return ptr;
        }
    }

    final static class FloatArrayEncoder
        extends ArrayEncoder
    {
        float[] mValues;

        protected FloatArrayEncoder() { super(); }

        protected void reset(float[] values, int from, int length) {
            super.reset(from, length);
            mValues = values;
        }

        protected int maxElementLength() { return NumberUtil.MAX_FLOAT_CLEN+1; }

        public int encodeMore(char[] buffer, int ptr, int end)
        {
            int lastOk = end - maxElementLength();
            while (ptr <= lastOk && mPtr < mEnd) {
                buffer[ptr++] = ' ';
                ptr = NumberUtil.writeFloat(mValues[mPtr++], buffer, ptr);
            }
            return ptr;
        }

        public int encodeMore(byte[] buffer, int ptr, int end)
        {
            int lastOk = end - maxElementLength();
            while (ptr <= lastOk && mPtr < mEnd) {
                buffer[ptr++] = BYTE_SPACE;
                ptr = NumberUtil.writeFloat(mValues[mPtr++], buffer, ptr);
            }
            return ptr;
        }
    }

    final static class DoubleArrayEncoder
        extends ArrayEncoder
    {
        double[] mValues;

        protected DoubleArrayEncoder() { super(); }

        protected void reset(double[] values, int from, int length) {
            super.reset(from, length);
            mValues = values;
        }

        protected int maxElementLength() { return NumberUtil.MAX_DOUBLE_CLEN+1; }

        public int encodeMore(char[] buffer, int ptr, int end)
        {
            int lastOk = end - maxElementLength();
            while (ptr <= lastOk && mPtr < mEnd) {
                buffer[ptr++] = ' ';
                ptr = NumberUtil.writeDouble(mValues[mPtr++], buffer, ptr);
            }
            return ptr;
        }

        public int encodeMore(byte[] buffer, int ptr, int end)
        {
            int lastOk = end - maxElementLength();
            while (ptr <= lastOk && mPtr < mEnd) {
                buffer[ptr++] = BYTE_SPACE;
                ptr = NumberUtil.writeDouble(mValues[mPtr++], buffer, ptr);
            }
            return ptr;
        }
    }
}
