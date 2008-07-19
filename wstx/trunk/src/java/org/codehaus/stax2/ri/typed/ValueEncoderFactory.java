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

package org.codehaus.stax2.ri.typed;


import com.ctc.wstx.util.NumberUtil;

/**
 * Factory class used to construct all
 * {@link AsciiValueEncoder} instances needed by a single
 * stream writer instance. Simple encoders are also recycled
 * (for the lifetime of an encoder, which is same as its owners,
 * i.e. stream reader or writer's) to minimize overhead.
 * More complex ones (array based, long string) are not recycled.
 *<p>
 * Since encoders are recycled, instances are not thread-safe.
 *
 * @since 4.0
 */
public final class ValueEncoderFactory
{
    final static byte BYTE_SPACE = (byte) ' ';

    // // // Lazily-constructed, recycled encoder instances

    protected TokenEncoder mTokenEncoder = null;
    protected IntEncoder mIntEncoder = null;
    protected LongEncoder mLongEncoder = null;
    protected FloatEncoder mFloatEncoder = null;
    protected DoubleEncoder mDoubleEncoder = null;

    public ValueEncoderFactory() { }

    // // // Scalar encoder access

    public ScalarEncoder getScalarEncoder(String value)
    {
        // Short or long?
        if (value.length() > AsciiValueEncoder.MIN_CHARS_WITHOUT_FLUSH) { // short
            if (mTokenEncoder == null) {
                mTokenEncoder = new TokenEncoder();
            }
            mTokenEncoder.reset(value);
            return mTokenEncoder;
        }
        // Nope, long: need segmented
        return new StringEncoder(value);
    }

    public ScalarEncoder getEncoder(boolean value)
    {
        // !!! TBI: optimize
        return getScalarEncoder(value ? "true" : "false");
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
        return new IntArrayEncoder(values, from, from+length);
    }

    public LongArrayEncoder getEncoder(long[] values, int from, int length)
    {
        return new LongArrayEncoder(values, from, from+length);
    }

    public FloatArrayEncoder getEncoder(float[] values, int from, int length)
    {
        return new FloatArrayEncoder(values, from, from+length);
    }

    public DoubleArrayEncoder getEncoder(double[] values, int from, int length)
    {
        return new DoubleArrayEncoder(values, from, from+length);
    }

    // // // And special one for Base64

    public Base64Encoder getEncoder(byte[] data, int from, int length)
    {
        return new Base64Encoder(data, from, from+length);
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
     * Implementation of textual encoder that operates on short
     * textual values ("tokens"). As such, it can count on being able
     * to output the whole output in one pass, without tracking
     * location
     */
    final static class TokenEncoder
        extends ScalarEncoder
    {
        String mValue;

        protected TokenEncoder() { super(); }

        protected void reset(String value) {
            mValue = value;
        }

        public boolean isCompleted() { return (mValue == null); }

        public int encodeMore(char[] buffer, int ptr, int end)
        {
            String str = mValue;
            mValue = null;
            int len = str.length();
            str.getChars(0, len, buffer, ptr);
            ptr += len;
            return ptr;
        }

        public int encodeMore(byte[] buffer, int ptr, int end)
        {
            String str = mValue;
            mValue = null;
            int len = str.length();
            for (int i = 0; i < len; ++i) {
                buffer[ptr++] = (byte) str.charAt(i);
            }
            return ptr;
        }
    }

    /**
     * Implementation of textual encoder that operates on longer
     * textual values. Because of length, it is possible that output
     * has to be done in multiple pieces. As a result, there is need
     * to track current position withing text.
     *<p>
     * In addition, instances of this class are not recycled, as
     * it seems less beneficial (less likely to need to be reused,
     * or offer performance improvements if they would be)
     */
    final static class StringEncoder
        extends ScalarEncoder
    {
        String mValue;

        int mOffset;

        protected StringEncoder(String value) {
            super();
            mValue = value;
        }

        public boolean isCompleted() { return (mValue == null); }

        public int encodeMore(char[] buffer, int ptr, int end)
        {
            int left = mValue.length() - mOffset;
            int free = end-ptr;
            if (free >= left) { // completed, simple
                mValue.getChars(mOffset, left, buffer, ptr);
                mValue = null;
                return (ptr+left);
            }
            mValue.getChars(mOffset, free, buffer, ptr);
            mOffset += free;
            return end;
        }

        public int encodeMore(byte[] buffer, int ptr, int end)
        {
            int left = mValue.length() - mOffset;
            int free = end-ptr;
            if (free >= left) { // completed, simple
                String str = mValue;
                mValue = null;
                for (int last = str.length(), offset = mOffset; offset < last; ++offset) {
                    buffer[ptr++] = (byte) mValue.charAt(offset);
                }
                return ptr;
            }
            for (; ptr < end; ++ptr) {
                buffer[ptr] = (byte) mValue.charAt(mOffset++);
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
        protected TypedScalarEncoder() { }

        /**
         * Since scalar typed values are guaranteed to always be
         * written in one go, they will always be completed by
         * time method is called./
         */
        public final boolean isCompleted() { return true; }
    }

    final static class IntEncoder
        extends TypedScalarEncoder
    {
        int mValue;

        protected IntEncoder() { super(); }

        protected void reset(int value) {
            mValue = value;
        }

        public int encodeMore(char[] buffer, int ptr, int end)
        {
            return NumberUtil.writeInt(mValue, buffer, ptr);
        }

        public int encodeMore(byte[] buffer, int ptr, int end)
        {
            return NumberUtil.writeInt(mValue, buffer, ptr);
        }
    }

    final static class LongEncoder
        extends TypedScalarEncoder
    {
        long mValue;

        protected LongEncoder() { super(); }

        protected void reset(long value) {
            mValue = value;
        }

        public int encodeMore(char[] buffer, int ptr, int end)
        {
            return NumberUtil.writeLong(mValue, buffer, ptr);
        }

        public int encodeMore(byte[] buffer, int ptr, int end)
        {
            return NumberUtil.writeLong(mValue, buffer, ptr);
        }
    }

    final static class FloatEncoder
        extends TypedScalarEncoder
    {
        float mValue;

        protected FloatEncoder() { super(); }

        protected void reset(float value) {
            mValue = value;
        }

        public int encodeMore(char[] buffer, int ptr, int end)
        {
            return NumberUtil.writeFloat(mValue, buffer, ptr);
        }

        public int encodeMore(byte[] buffer, int ptr, int end)
        {
            return NumberUtil.writeFloat(mValue, buffer, ptr);
        }
    }

    final static class DoubleEncoder
        extends TypedScalarEncoder
    {
        double mValue;

        protected DoubleEncoder() { super(); }

        protected void reset(double value) {
            mValue = value;
        }

        public int encodeMore(char[] buffer, int ptr, int end)
        {
            return NumberUtil.writeDouble(mValue, buffer, ptr);
        }

        public int encodeMore(byte[] buffer, int ptr, int end)
        {
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
        final int mEnd;

        protected ArrayEncoder(int ptr, int end)
        {
            mPtr = ptr;
            mEnd = end;
        }

        public final boolean isCompleted() { return (mPtr >= mEnd); }

        public abstract int encodeMore(char[] buffer, int ptr, int end);
    }

    /**
     * Concrete implementation used for encoding int[] content.
     */
    final static class IntArrayEncoder
        extends ArrayEncoder
    {
        final int[] mValues;

        protected IntArrayEncoder(int[] values, int from, int length)
        {
            super(from, length);
            mValues = values;
        }

        public int encodeMore(char[] buffer, int ptr, int end)
        {
            int lastOk = end - (1+NumberUtil.MAX_INT_CLEN);
            while (ptr <= lastOk && mPtr < mEnd) {
                buffer[ptr++] = ' ';
                ptr = NumberUtil.writeInt(mValues[mPtr++], buffer, ptr);
            }
            return ptr;
        }

        public int encodeMore(byte[] buffer, int ptr, int end)
        {
            int lastOk = end - (1+NumberUtil.MAX_INT_CLEN);
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
        final long[] mValues;

        protected LongArrayEncoder(long[] values, int from, int length)
        {
            super(from, length);
            mValues = values;
        }

        public int encodeMore(char[] buffer, int ptr, int end)
        {
            int lastOk = end - (1+NumberUtil.MAX_LONG_CLEN);
            while (ptr <= lastOk && mPtr < mEnd) {
                buffer[ptr++] = ' ';
                ptr = NumberUtil.writeLong(mValues[mPtr++], buffer, ptr);
            }
            return ptr;
        }

        public int encodeMore(byte[] buffer, int ptr, int end)
        {
            int lastOk = end - (1+NumberUtil.MAX_LONG_CLEN);
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
        final float[] mValues;

        protected FloatArrayEncoder(float[] values, int from, int length)
        {
            super(from, length);
            mValues = values;
        }

        public int encodeMore(char[] buffer, int ptr, int end)
        {
            int lastOk = end - (1+NumberUtil.MAX_FLOAT_CLEN);
            while (ptr <= lastOk && mPtr < mEnd) {
                buffer[ptr++] = ' ';
                ptr = NumberUtil.writeFloat(mValues[mPtr++], buffer, ptr);
            }
            return ptr;
        }

        public int encodeMore(byte[] buffer, int ptr, int end)
        {
            int lastOk = end - (1+NumberUtil.MAX_FLOAT_CLEN);
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
        final double[] mValues;

        protected DoubleArrayEncoder(double[] values, int from, int length)
        {
            super(from, length);
            mValues = values;
        }

        public int encodeMore(char[] buffer, int ptr, int end)
        {
            int lastOk = end - (1+NumberUtil.MAX_DOUBLE_CLEN);
            while (ptr <= lastOk && mPtr < mEnd) {
                buffer[ptr++] = ' ';
                ptr = NumberUtil.writeDouble(mValues[mPtr++], buffer, ptr);
            }
            return ptr;
        }

        public int encodeMore(byte[] buffer, int ptr, int end)
        {
            int lastOk = end - (1+NumberUtil.MAX_DOUBLE_CLEN);
            while (ptr <= lastOk && mPtr < mEnd) {
                buffer[ptr++] = BYTE_SPACE;
                ptr = NumberUtil.writeDouble(mValues[mPtr++], buffer, ptr);
            }
            return ptr;
        }
    }

    /*
    ////////////////////////////////////////////////////////////////
    // Implementation classes: binary (base64) encoder
    ////////////////////////////////////////////////////////////////
     */

    final static class Base64Encoder
        extends AsciiValueEncoder
    {
        /**
         * As per Base64 specs, need to insert linefeeds after
         * 76 characters.
         */
        final static int CHUNKS_BEFORE_LF = (76 >> 2);

        final static char PAD_CHAR = '=';
        final static byte PAD_BYTE = (byte) PAD_CHAR;

        /* Hmmh. Base64 specs suggest \r\n... but for xml, \n is the
         * canonical one. Let's take xml's choice here, more compact too.
         */
        final static byte LF_CHAR = '\n';
        final static byte LF_BYTE = (byte) LF_CHAR;

        final static char[] s64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

        final static byte[] s64Bytes = new byte[64];
        static {
            for (int i = 0, len = s64Chars.length; i < len; ++i) {
                s64Bytes[i] = (byte) s64Chars[i];
            }
        }

        final byte[] mData;

        int mPtr;

        final int mEnd;

        /**
         * We need a counter to know when to add mandatory
         * linefeed.
         */
        int mChunksBeforeLf = CHUNKS_BEFORE_LF;

        protected Base64Encoder(byte[] values, int from, int end)
        {
            mData = values;
            mPtr = from;
            mEnd = end;
        }

        public boolean isCompleted() { return (mPtr >= mEnd); }

        public int encodeMore(char[] buffer, int outPtr, int outEnd)
        {
            // Encoding is by chunks of 3 input, 4 output chars, so:
            int inEnd = mEnd-3;
            // But let's also reserve room for lf char
            outEnd -= 5;

            while (mPtr <= inEnd) {
                if (outPtr > outEnd) { // no more room: need to return for flush
                    return outPtr;
                }
                // First, mash 3 bytes into lsb of 32-bit int
                int b24 = ((int) mData[mPtr++]) << 8;
                b24 |= ((int) mData[mPtr++]) & 0xFF;
                b24 = (b24 << 8) | (((int) mData[mPtr++]) & 0xFF);
                // And then split resulting 4 6-bit segments
                buffer[outPtr++] = s64Chars[(b24 >> 18) & 0x3F];
                buffer[outPtr++] = s64Chars[(b24 >> 12) & 0x3F];
                buffer[outPtr++] = s64Chars[(b24 >> 6) & 0x3F];
                buffer[outPtr++] = s64Chars[b24 & 0x3F];

                if (--mChunksBeforeLf <= 0) {
                    buffer[outPtr++] = LF_BYTE;
                    mChunksBeforeLf = CHUNKS_BEFORE_LF;
                }
            }
            // main stuff done, any leftovers?
            int left = (mEnd-mPtr);
            if (left > 0) { // yes, but do we have room for output?
                if (outPtr <= outEnd) { // yup
                    int b24 = ((int) mData[mPtr++]) << 16;
                    if (left == 2) {
                        b24 |= (((int) mData[mPtr++]) & 0xFF) << 8;
                    }
                    buffer[outPtr++] = s64Chars[(b24 >> 18) & 0x3F];
                    buffer[outPtr++] = s64Chars[(b24 >> 12) & 0x3F];
                    buffer[outPtr++] = (left == 1) ?
                        PAD_CHAR : s64Chars[(b24 >> 6) & 0x3F];
                    buffer[outPtr++] = PAD_CHAR;
                }
            }
            return outPtr;
        }

        public int encodeMore(byte[] buffer, int outPtr, int outEnd)
        {
            int inEnd = mEnd-3;
            outEnd -= 5;

            while (mPtr <= inEnd) {
                if (outPtr > outEnd) { // no more room: need to return for flush
                    return outPtr;
                }
                // First, mash 3 bytes into lsb of 32-bit int
                int b24 = ((int) mData[mPtr++]) << 8;
                b24 |= ((int) mData[mPtr++]) & 0xFF;
                b24 = (b24 << 8) | (((int) mData[mPtr++]) & 0xFF);
                // And then split resulting 4 6-bit segments
                buffer[outPtr++] = s64Bytes[(b24 >> 18) & 0x3F];
                buffer[outPtr++] = s64Bytes[(b24 >> 12) & 0x3F];
                buffer[outPtr++] = s64Bytes[(b24 >> 6) & 0x3F];
                buffer[outPtr++] = s64Bytes[b24 & 0x3F];

                if (--mChunksBeforeLf <= 0) {
                    buffer[outPtr++] = LF_BYTE;
                    mChunksBeforeLf = CHUNKS_BEFORE_LF;
                }
            }
            // main stuff done, any leftovers?
            int left = (mEnd-mPtr);
            if (left > 0) { // yes, but do we have room for output?
                if (outPtr <= outEnd) { // yup
                    int b24 = ((int) mData[mPtr++]) << 16;
                    if (left == 2) {
                        b24 |= (((int) mData[mPtr++]) & 0xFF) << 8;
                    }
                    buffer[outPtr++] = s64Bytes[(b24 >> 18) & 0x3F];
                    buffer[outPtr++] = s64Bytes[(b24 >> 12) & 0x3F];
                    buffer[outPtr++] = (left == 1) ?
                        PAD_BYTE : s64Bytes[(b24 >> 6) & 0x3F];
                    buffer[outPtr++] = PAD_BYTE;
                }
            }
            return outPtr;
        }
    }
}
