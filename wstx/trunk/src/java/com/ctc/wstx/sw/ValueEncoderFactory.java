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
    // // // Lazily-constructed, recycled encoder instances

    protected IntArrayEncoder mIntArrayEncoder = null;

    public ValueEncoderFactory() { }

    public IntArrayEncoder getIntArrayEncoder(int[] values, int from, int length)
    {
        if (mIntArrayEncoder == null) {
            mIntArrayEncoder = new IntArrayEncoder();
        }
        mIntArrayEncoder.reset(values, from, length);
        return mIntArrayEncoder;
    }

    /*
    //////////////////////////////////////////////////////////
    // Implementation classes
    //////////////////////////////////////////////////////////
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
            return (mPtr < mEnd) && (freeChars < maxElementLength());
        }

        public abstract int encodeMore(char[] buffer, int ptr, int end);

        /**
         * @return Maximum length (in characters) of an individual 
         *   array element
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

        protected int maxElementLength() { return NumberUtil.MAX_LONG_CLEN; }

        public int encodeMore(char[] buffer, int ptr, int end)
        {
            int lastOk = end - maxElementLength();
            while (mPtr <= lastOk) {
                ptr = NumberUtil.writeInt(mValues[mPtr++], buffer, ptr);
            }
            return ptr;
        }
    }
}
