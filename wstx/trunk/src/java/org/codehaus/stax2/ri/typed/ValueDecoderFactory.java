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

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

import org.codehaus.stax2.typed.TypedArrayDecoder;
import org.codehaus.stax2.typed.TypedValueDecoder;

/**
 * Factory class used to construct all
 * {@link AsciiValueDecoder} instances needed by a single
 * stream reader instance. Some decoders are also recycled
 * (for the lifetime of an encoder, which is same as its owners,
 * i.e. stream reader or writer's) to minimize overhead.
 *<p>
 * Since encoders may be recycled, instances are not thread-safe.
 *
 * @since 4.0
 */
public final class ValueDecoderFactory
{
    // // // Lazily-constructed, recycled decoder instances
    // // // (only for simple commonly needed types)

    protected BooleanDecoder mBooleanDecoder = null;
    protected IntDecoder mIntDecoder = null;
    protected LongDecoder mLongDecoder = null;
    protected FloatDecoder mFloatDecoder = null;
    protected DoubleDecoder mDoubleDecoder = null;

    public ValueDecoderFactory() { }

    /*
    /////////////////////////////////////////////////////
    // Factory methods, scalar decoders
    /////////////////////////////////////////////////////
    */

    public BooleanDecoder getBooleanDecoder()
    {
        if (mBooleanDecoder == null) {
            mBooleanDecoder = new BooleanDecoder();
        }
        return mBooleanDecoder;
    }

    public IntDecoder getIntDecoder()
    {
        if (mIntDecoder == null) {
            mIntDecoder = new IntDecoder();
        }
        return mIntDecoder;
    }

    public LongDecoder getLongDecoder()
    {
        if (mLongDecoder == null) {
            mLongDecoder = new LongDecoder();
        }
        return mLongDecoder;
    }

    public FloatDecoder getFloatDecoder()
    {
        if (mFloatDecoder == null) {
            mFloatDecoder = new FloatDecoder();
        }
        return mFloatDecoder;
    }

    public DoubleDecoder getDoubleDecoder()
    {
        if (mDoubleDecoder == null) {
            mDoubleDecoder = new DoubleDecoder();
        }
        return mDoubleDecoder;
    }

    // // // Other scalar decoder: not recycled

    public IntegerDecoder getIntegerDecoder() { return new IntegerDecoder(); }

    public DecimalDecoder getDecimalDecoder() { return new DecimalDecoder(); }

    public QNameDecoder getQNameDecoder(NamespaceContext nsc) {
        return new QNameDecoder(nsc);
    }

    /*
    /////////////////////////////////////////////////////
    // Factory methods, array decoders
    /////////////////////////////////////////////////////
    */

    public IntArrayDecoder getIntArrayDecoder(int[] result, int offset, int len)
    {
        return new IntArrayDecoder(result, offset, len, getIntDecoder());
    }

    public LongArrayDecoder getLongArrayDecoder(long[] result, int offset, int len)
    {
        return new LongArrayDecoder(result, offset, len, getLongDecoder());
    }

    public FloatArrayDecoder getFloatArrayDecoder(float[] result, int offset, int len)
    {
        return new FloatArrayDecoder(result, offset, len, getFloatDecoder());
    }


    public DoubleArrayDecoder getDoubleArrayDecoder(double[] result, int offset, int len)
    {
        return new DoubleArrayDecoder(result, offset, len, getDoubleDecoder());
    }

    /*
    /////////////////////////////////////////////////////
    // Shared decoder base class
    /////////////////////////////////////////////////////
    */

    /**
     * There are some things common to all textual decoders (like
     * white space trimming).
     */
    public abstract static class DecoderBase
        extends TypedValueDecoder
    {
        final static long L_BILLION = 1000000000;

        final static long L_MAX_INT = (long) Integer.MAX_VALUE;

        final static long L_MIN_INT = (long) Integer.MIN_VALUE;

        final static BigInteger BD_MIN_LONG = BigInteger.valueOf(Long.MIN_VALUE);
        final static BigInteger BD_MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);
        
        /**
         * Pointer to the next character to check, within lexical value
         */
        protected int mPtr;
        
        /**
         * Pointer to the pointer in lexical value <b>after</b> last
         * included character.
         */
        protected int mEnd;

        protected DecoderBase() { }

        public abstract String getType();

        /*
        //////////////////////////////////////////////////
        // Shared methods, trimming
        //////////////////////////////////////////////////
         */

        /**
         *<p>
         * Note that it is assumed that any "weird" white space
         * (xml 1.1 LSEP and NEL) have been replaced by canonical
         * alternatives (linefeed for element content, regular space
         * for attributes)
         */
        protected final boolean isSpace(char c)
        {
            return ((int) c) <= 0x0020;
        }

        /**
         * @param start First character of the lexical value to process
         * @param end Pointer character <b>after</b> last valid character
         *   of the lexical value
         *
         * @return First non-white space character from the String
         */
        protected char resetAndTrimLeading(String lexical, int start, int end)
        {
            mEnd = end;
            
            while (start < end) {
                char c = lexical.charAt(start++);
                if (!isSpace(c)) {
                    mPtr = start;
                    return c;
                }
            }
            throw constructMissingValue();
        }

        protected int trimLeading(String lexical, int start, int end)
        {
            while (start < end) {
                char c = lexical.charAt(start);
                if (!isSpace(c)) {
                    return start;
                }
                ++start;
            }
            throw constructMissingValue();
        }
        
        protected int trimTrailing(String lexical, int start, int end)
        {
            while (--end > start && isSpace(lexical.charAt(end))) { }
            return end+1;
        }
        
        protected char resetAndTrimLeading(char[] lexical, int start, int end)
        {
            mEnd = end;
            while (start < end) {
                char c = lexical[start++];
                if (!isSpace(c)) {
                    mPtr = start;
                    return c;
                }
            }
            throw constructMissingValue();
        }
        
        protected int trimLeading(char[] lexical, int start, int end)
        {
            while (start < end) {
                char c = lexical[start];
                if (!isSpace(c)) {
                    return start;
                }
                ++start;
            }
            throw constructMissingValue();
        }

        protected int trimTrailing(char[] lexical, int start, int end)
        {
            while (--end > start && isSpace(lexical[end])) { }
            return end+1;
        }

        /**
         * Method called to check that remaining String consists of zero or
         * more digits, followed by zero or more white space, and nothing else;
         * and to trim trailing white space, if any.
         *
         * @return Number of valid digits found; or -1 to indicate invalid
         *   input
         */
        protected int trimTrailingAndCheckDigits(String lexical)
        {
            // Let's start from beginning
            int ptr = mPtr;
            
            // Note: caller won't call this with empty String
            char ch = lexical.charAt(ptr);
            
            // First, skim through digits
            while (ch <= '9' && ch >= '0') {
                if (++ptr >= mEnd) { // no trailing white space, valid
                    return (ptr - mPtr);
                }
                ch = lexical.charAt(ptr);
            }
            
            // And then see what follows digits...
            int len = ptr - mPtr;
            while (true) {
                if (!isSpace(ch)) { // garbage following white space (or digits)
                    return -1;
                }
                if (++ptr >= mEnd) {
                    return len;
            }
                ch = lexical.charAt(ptr);
            }
        }
        
        protected int trimTrailingAndCheckDigits(char[] lexical)
        {
            // Let's start from beginning
            int ptr = mPtr;
            
            // Note: caller won't call this with empty String
            char ch = lexical[ptr];
            
            // First, skim through digits
            while (ch <= '9' && ch >= '0') {
                if (++ptr >= mEnd) { // no trailing white space, valid
                    return (ptr - mPtr);
                }
                ch = lexical[ptr];
            }
            
            // And then see what follows digits...
            int len = ptr - mPtr;
            while (true) {
                if (!isSpace(ch)) { // garbage following white space (or digits)
                    return -1;
                }
                if (++ptr >= mEnd) {
                    return len;
                }
                ch = lexical[ptr];
            }
        }

        /**
         * @return Numeric value of the first non-zero character (or, in
         *   case of a zero value, zero)
         */
        protected int skipSignAndZeroes(String lexical, char ch, boolean hasSign)
        {
            int ptr = mPtr;
            
            // Then optional sign
            if (hasSign) {
                if (ptr >= mEnd) {
                    throw constructInvalidValue(String.valueOf(ch));
                }
                ch = lexical.charAt(ptr++);
            }
            
            // Has to start with a digit
            int value = ch - '0';
            if (value < 0 || value > 9) {
                throw constructInvalidValue(lexical, mPtr-1);
            }
            
            // Then, leading zero(es) to skip? (or just value zero itself)
            while (value == 0 && ptr < mEnd) {
                int v2 = lexical.charAt(ptr) - '0';
                if (v2 < 0 || v2 > 9) {
                    break;
                }
                ++ptr;
                value = v2;
            }
            mPtr = ptr;
            return value;
        }
        
        protected int skipSignAndZeroes(char[] lexical, char ch, boolean hasSign)
        {
            int ptr = mPtr;
            
            // Then optional sign
            if (hasSign) {
                if (ptr >= mEnd) {
                    throw constructInvalidValue(String.valueOf(ch));
                }
                ch = lexical[ptr++];
            }
            
            // Has to start with a digit
            int value = ch - '0';
            if (value < 0 || value > 9) {
                throw constructInvalidValue(lexical, mPtr-1);
            }
            
            // Then leading zero(es) to skip? (or just value zero itself)
            while (value == 0 && ptr < mEnd) {
                int v2 = lexical[ptr] - '0';
                if (v2 < 0 || v2 > 9) {
                    break;
                }
                ++ptr;
                value = v2;
            }
            mPtr = ptr;
            return value;
        }
        
        protected final boolean allWhitespace(String lexical, int start, int end)
        {
            while (start < end) {
                if (!isSpace(lexical.charAt(start++))) {
                    return false;
                }
            }
            return true;
        }
        
        protected final boolean allWhitespace(char[] lexical, int start, int end)
        {
            while (start < end) {
                if (!isSpace(lexical[start++])) {
                    return false;
                }
            }
            return true;
        }
        
        /*
        ///////////////////////////////////////////////
        // Shared methods, int conversions
        ///////////////////////////////////////////////
        */

        /**
         * Fast method for parsing integers that are known to fit into
         * regular 32-bit signed int type. This means that length is
         * between 1 and 9 digits (inclusive)
         *
         * @return Parsed integer value
         */
        protected final static int parseInt(char[] digitChars, int start, int end)
        {
            /* This looks ugly, but appears to be the fastest way
             * (based on perf testing, profiling)
             */
            int num = digitChars[start] - '0';
            if (++start < end) {
                num = (num * 10) + (digitChars[start] - '0');
                if (++start < end) {
                    num = (num * 10) + (digitChars[start] - '0');
                    if (++start < end) {
                        num = (num * 10) + (digitChars[start] - '0');
                        if (++start < end) {
                            num = (num * 10) + (digitChars[start] - '0');
                            if (++start < end) {
                                num = (num * 10) + (digitChars[start] - '0');
                                if (++start < end) {
                                    num = (num * 10) + (digitChars[start] - '0');
                                    if (++start < end) {
                                        num = (num * 10) + (digitChars[start] - '0');
                                        if (++start < end) {
                                            num = (num * 10) + (digitChars[start] - '0');
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return num;
        }

        protected final static int parseInt(int num, char[] digitChars, int start, int end)
        {
            num = (num * 10) + (digitChars[start] - '0');
            if (++start < end) {
                num = (num * 10) + (digitChars[start] - '0');
                if (++start < end) {
                    num = (num * 10) + (digitChars[start] - '0');
                    if (++start < end) {
                        num = (num * 10) + (digitChars[start] - '0');
                        if (++start < end) {
                            num = (num * 10) + (digitChars[start] - '0');
                            if (++start < end) {
                                num = (num * 10) + (digitChars[start] - '0');
                                if (++start < end) {
                                    num = (num * 10) + (digitChars[start] - '0');
                                    if (++start < end) {
                                        num = (num * 10) + (digitChars[start] - '0');
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return num;
        }
        
        protected final static int parseInt(String digitChars, int start, int end)
        {
            int num = digitChars.charAt(start) - '0';
            if (++start < end) {
                num = (num * 10) + (digitChars.charAt(start) - '0');
                if (++start < end) {
                    num = (num * 10) + (digitChars.charAt(start) - '0');
                    if (++start < end) {
                        num = (num * 10) + (digitChars.charAt(start) - '0');
                        if (++start < end) {
                            num = (num * 10) + (digitChars.charAt(start) - '0');
                            if (++start < end) {
                                num = (num * 10) + (digitChars.charAt(start) - '0');
                                if (++start < end) {
                                    num = (num * 10) + (digitChars.charAt(start) - '0');
                                    if (++start < end) {
                                        num = (num * 10) + (digitChars.charAt(start) - '0');
                                        if (++start < end) {
                                            num = (num * 10) + (digitChars.charAt(start) - '0');
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return num;
        }
        
        protected final static int parseInt(int num, String digitChars, int start, int end)
        {
            num = (num * 10) + (digitChars.charAt(start) - '0');
            if (++start < end) {
                num = (num * 10) + (digitChars.charAt(start) - '0');
                if (++start < end) {
                    num = (num * 10) + (digitChars.charAt(start) - '0');
                    if (++start < end) {
                        num = (num * 10) + (digitChars.charAt(start) - '0');
                        if (++start < end) {
                            num = (num * 10) + (digitChars.charAt(start) - '0');
                            if (++start < end) {
                                num = (num * 10) + (digitChars.charAt(start) - '0');
                                if (++start < end) {
                                    num = (num * 10) + (digitChars.charAt(start) - '0');
                                    if (++start < end) {
                                        num = (num * 10) + (digitChars.charAt(start) - '0');
                                    }
                                }
                        }
                        }
                    }
                }
            }
            return num;
        }
        
        protected final static long parseLong(char[] digitChars, int start, int end)
        {
            // Note: caller must ensure length is [10, 18]
            int start2 = end-9;
            long val = parseInt(digitChars, start, start2) * L_BILLION;
            return val + (long) parseInt(digitChars, start2, end);
        }
        
        protected final static long parseLong(String digitChars, int start, int end)
        {
            // Note: caller must ensure length is [10, 18]
            int start2 = end-9;
            long val = parseInt(digitChars, start, start2) * L_BILLION;
            return val + (long) parseInt(digitChars, start2, end);
        }

        /*
        ///////////////////////////////////////////////
        // Shared methods, error reporting
        ///////////////////////////////////////////////
        */

        protected IllegalArgumentException constructMissingValue()
        {
            throw new IllegalArgumentException("Empty value (all white space) not a valid lexical representation of "+getType());
        }
        
        protected IllegalArgumentException constructInvalidValue(String lexical)
        {
            // !!! Should we escape ctrl+chars etc?
            return new IllegalArgumentException("Value \""+lexical+"\" not a valid lexical representation of "+getType());
        }
        
        protected IllegalArgumentException constructInvalidValue(String lexical, int startOffset)
        {
            return new IllegalArgumentException("Value \""+lexicalDesc(lexical, startOffset)+"\" not a valid lexical representation of "+getType());
        }
        
        protected IllegalArgumentException constructInvalidValue(char[] lexical, int startOffset)
        {
            return new IllegalArgumentException("Value \""+lexicalDesc(lexical, startOffset)+"\" not a valid lexical representation of "+getType());
        }
        
        protected String lexicalDesc(char[] lexical, int startOffset)
        {
            int len = mEnd-startOffset;
            String str = new String(lexical, startOffset, len);
            // !!! Should we escape ctrl+chars etc?
            return str.trim();
        }
        
        protected String lexicalDesc(String lexical, int startOffset)
        {
            String str = lexical.substring(startOffset);
            // !!! Should we escape ctrl+chars etc?
            return str.trim();
        }
        
        protected String lexicalDesc(String lexical)
        {
            // !!! Should we escape ctrl+chars etc?
            return lexical.trim();
        }
    }

    /*
    /////////////////////////////////////////////////////
    // Decoders, scalar primitives
    /////////////////////////////////////////////////////
    */

    public final static class BooleanDecoder
        extends DecoderBase
    {
        protected boolean mValue;

        public BooleanDecoder() { }

        public String getType() { return "boolean"; }

        public boolean getValue() { return mValue; }

        public void decode(String lexical)
            throws IllegalArgumentException
        {
            // First, skip leading ws if any
            char c = resetAndTrimLeading(lexical, 0, lexical.length());
            int ptr = mPtr;
            int len = mEnd-ptr;
            if (c == 't') {
                if (len >= 3
                    && lexical.charAt(ptr) == 'r'
                    && lexical.charAt(++ptr) == 'u'
                    && lexical.charAt(++ptr) == 'e') {
                    if (++ptr >= mEnd || allWhitespace(lexical, ptr, mEnd)) {
                        mValue = true;
                        return;
                    }
                }
            } else if (c == 'f') {
                if (len >= 4
                    && lexical.charAt(ptr) == 'a'
                    && lexical.charAt(++ptr) == 'l'
                    && lexical.charAt(++ptr) == 's'
                    && lexical.charAt(++ptr) == 'e') {
                    if (++ptr >= mEnd || allWhitespace(lexical, ptr, mEnd)) {
                        mValue = false;
                        return;
                    }
                }
            } else if (c == '0') {
                if (ptr >= mEnd || allWhitespace(lexical, ptr, mEnd)) {
                    mValue = false;
                    return;
                }
            } else if (c == '1') {
                if (ptr >= mEnd || allWhitespace(lexical, ptr, mEnd)) {
                    mValue = true;
                    return;
                }
            }
            throw constructInvalidValue(lexical);
        }
        
        public void decode(char[] lexical, int start, int end)
            throws IllegalArgumentException
        {
            // First, skip leading ws if any
            char c = resetAndTrimLeading(lexical, start, end);
            int ptr = mPtr;
            int len = mEnd-ptr;
            if (c == 't') {
                if (len >= 3
                    && lexical[ptr] == 'r'
                    && lexical[++ptr] == 'u'
                    && lexical[++ptr] == 'e') {
                    if (++ptr >= mEnd || allWhitespace(lexical, ptr, mEnd)) {
                        mValue = true;
                        return;
                    }
                }
            } else if (c == 'f') {
                if (len >= 4
                    && lexical[ptr] == 'a'
                    && lexical[++ptr] == 'l'
                    && lexical[++ptr] == 's'
                    && lexical[++ptr] == 'e') {
                    if (++ptr >= mEnd || allWhitespace(lexical, ptr, mEnd)) {
                        mValue = false;
                        return;
                    }
                }
            } else if (c == '0') {
                if (ptr >= mEnd || allWhitespace(lexical, ptr, mEnd)) {
                    mValue = false;
                    return;
                }
            } else if (c == '1') {
                if (ptr >= mEnd || allWhitespace(lexical, ptr, mEnd)) {
                    mValue = true;
                    return;
                }
            }
            throw constructInvalidValue(lexical, start);
        }
    }

    public final static class IntDecoder
        extends DecoderBase
    {
        protected int mValue;

        public IntDecoder() { }

        public String getType() { return "int"; }

        public int getValue() { return mValue; }

        public void decode(String lexical)
            throws IllegalArgumentException
        {
            char ch = resetAndTrimLeading(lexical, 0, lexical.length());
            boolean neg = (ch == '-');
            int nr = skipSignAndZeroes(lexical, ch, neg || (ch == '+'));

            // Quick check for short (single-digit) values:
            if (mPtr >= mEnd) {
                mValue = neg ? -nr : nr;
                return;
            }

            // Otherwise, need to verify that is [digit*][ws*]
            int len = trimTrailingAndCheckDigits(lexical);
            if (len < 1) {
                if (len == 0) {
                    mValue = neg ? -nr : nr;
                    return;
                }
                throw constructInvalidValue(lexical, 0);
            }
            // Note: len is one less than total length (skipped first digit)
            if (len <= 8) { // no overflow
                int i = parseInt(nr, lexical, mPtr, mPtr+len);
                mValue = neg ? -i : i;
                return;
            }
            // Otherwise, may have overflow
            // Max 10 digits for a legal int
            if (len == 9 && nr < 3) { // min/max is ~2 billion (+/-)
                long base = L_BILLION;
                if (nr == 2) {
                    base += L_BILLION;
                }
                int i = parseInt(lexical, mPtr, mPtr+len);
                long l = base + (long) i;
                if (neg) {
                    l = -l;
                    if (l >= L_MIN_INT) {
                        mValue = (int) l;
                        return;
                    }
                } else {
                    if (l <= L_MAX_INT) {
                        mValue = (int) l;
                        return;
                    }
                }
            }
            throw new IllegalArgumentException("value \""+lexicalDesc(lexical, 0)+"\" not a valid 32-bit integer: overflow.");

        }
        
        public void decode(char[] lexical, int start, int end)
            throws IllegalArgumentException
        {
            char ch = resetAndTrimLeading(lexical, start, end);
            boolean neg = (ch == '-');
            int nr = skipSignAndZeroes(lexical, ch, neg || (ch == '+'));

            // Quick check for short (single-digit) values:
            if (mPtr >= mEnd) {
                mValue = neg ? -nr : nr;
                return;
            }

            // Otherwise, need to verify that is [digit*][ws*]
            int len = trimTrailingAndCheckDigits(lexical);
            if (len < 1) {
                if (len == 0) {
                    mValue = neg ? -nr : nr;
                    return;
                }
                throw constructInvalidValue(lexical, start);
            }
            // Note: len is one less than total length (skipped first digit)
            if (len <= 8) { // no overflow
                int i = parseInt(nr, lexical, mPtr, mPtr+len);
                mValue = neg ? -i : i;
                return;
            }
            // Otherwise, may have overflow
            // Max 10 digits for a legal int
            if (len == 9 && nr < 3) { // min/max is ~2 billion (+/-)
                long base = L_BILLION;
                if (nr == 2) {
                    base += L_BILLION;
                }
                int i = parseInt(lexical, mPtr, mPtr+len);
                long l = base + (long) i;
                if (neg) {
                    l = -l;
                    if (l >= L_MIN_INT) {
                        mValue = (int) l;
                        return;
                    }
                } else {
                    if (l <= L_MAX_INT) {
                        mValue = (int) l;
                        return;
                    }
                }
            }
            throw new IllegalArgumentException("value \""+lexicalDesc(lexical, start)+"\" not a valid 32-bit integer: overflow.");
        }
    }

    public final static class LongDecoder
        extends DecoderBase
    {
        protected long mValue;

        public LongDecoder() { }

        public String getType() { return "long"; }

        public long getValue() { return mValue; }

        public void decode(String lexical)
            throws IllegalArgumentException
        {
            char ch = resetAndTrimLeading(lexical, 0, lexical.length());
            boolean neg = (ch == '-');

            int nr = skipSignAndZeroes(lexical, ch, neg || (ch == '+'));
            
            // Quick check for short (single-digit) values:
            if (mPtr >= mEnd) {
                mValue = (long) (neg ? -nr : nr);
                return;
            }

            // Otherwise, need to verify that is [digit*][ws*]
            int len = trimTrailingAndCheckDigits(lexical);
            if (len < 1) {
                if (len == 0) {
                    mValue = (long) (neg ? -nr : nr);
                    return;
                }
                throw constructInvalidValue(lexical, 0);
            }
            // Note: len is one less than total length (skipped first digit)
            // Can parse more cheaply, if it's really just an int...
            if (len <= 8) { // no overflow
                int i = parseInt(nr, lexical, mPtr, mPtr+len);
                mValue = (long) (neg ? -i : i);
                return;
            }
            // At this point, let's just push back the first digit... simpler
            --mPtr;
            ++len;
            
            // Still simple long? 
            if (len <= 18) {
                long l = parseLong(lexical, mPtr, mPtr+len);
                mValue = neg ? -l : l;
                return;
            }
            /* Otherwise, let's just fallback to an expensive option,
             * BigInteger. While relatively inefficient, it's simple
             * to use, reliable etc.
             */
            mValue = parseUsingBD(lexical.substring(mPtr, mPtr+len), neg);
        }

        public void decode(char[] lexical, int start, int end)
            throws IllegalArgumentException
        {
            char ch = resetAndTrimLeading(lexical, start, end);
            boolean neg = (ch == '-');
            
            int nr = skipSignAndZeroes(lexical, ch, neg || (ch == '+'));
            
            // Quick check for short (single-digit) values:
            if (mPtr >= mEnd) {
                mValue = (long) (neg ? -nr : nr);
                return;
            }
            
            // Otherwise, need to verify that is [digit*][ws*]
            int len = trimTrailingAndCheckDigits(lexical);
            if (len < 1) {
                if (len == 0) {
                    mValue = (long) (neg ? -nr : nr);
                    return;
                }
                throw constructInvalidValue(lexical, start);
            }
            // Note: len is one less than total length (skipped first digit)
            // Can parse more cheaply, if it's really just an int...
            if (len <= 8) { // no overflow
                int i = parseInt(nr, lexical, mPtr, mPtr+len);
                mValue = neg ? -i : i;
                return;
            }
            // At this point, let's just push back the first digit... simpler
            --mPtr;
            ++len;
            
            // Still simple long? 
            if (len <= 18) {
                long l = parseLong(lexical, mPtr, mPtr+len);
                mValue = neg ? -l : l;
                return;
            }

            // Otherwise, let's just fallback to an expensive option
            mValue = parseUsingBD(new String(lexical, mPtr, len), neg);
        }

        private long parseUsingBD(String lexical, boolean neg)
        {
            BigInteger bi = new BigInteger(lexical);
            
            // But we may over/underflow, let's check:
            if (neg) {
                bi = bi.negate();
                if (bi.compareTo(BD_MIN_LONG) >= 0) {
                    return bi.longValue();
                }
            } else {
                if (bi.compareTo(BD_MAX_LONG) <= 0) {
                    return bi.longValue();
                }
            }

            throw new IllegalArgumentException("value \""+lexicalDesc(lexical, 0)+"\" not a valid long: overflow.");
        }
    }

    public final static class FloatDecoder
        extends DecoderBase
    {
        protected float mValue;

        public FloatDecoder() { }

        public String getType() { return "float"; }

        public float getValue() { return mValue; }

        public void decode(String lexical)
            throws IllegalArgumentException
        {
            int origEnd = lexical.length();
            int start = trimLeading(lexical, 0, origEnd);
            int end = trimTrailing(lexical, start, origEnd);

            if (start > 0 || end < origEnd) {
                lexical = lexical.substring(start, end);
            }

            /* Then, leading digit; or one of 3 well-known constants
             * (INF, -INF, NaN)
             */
            int len = lexical.length();
            if (len == 3) {
                char c = lexical.charAt(0);
                if (c == 'I') {
                    if (lexical.charAt(1) == 'N' && lexical.charAt(2) == 'F') {
                        mValue = Float.POSITIVE_INFINITY;
                        return;
                    }
                } else if (c == 'N') {
                    if (lexical.charAt(1) == 'a' && lexical.charAt(2) == 'N') {
                        mValue = Float.NaN;
                        return;
                    }
                }
            } else if (len == 4) {
                char c = lexical.charAt(0);
                if (c == '-') {
                    if (lexical.charAt(1) == 'I'
                        && lexical.charAt(2) == 'N'
                        && lexical.charAt(3) == 'F') {
                        mValue = Float.NEGATIVE_INFINITY;
                        return;
                    }
                }
            }
            
            try {
                mValue = Float.parseFloat(lexical);
            } catch (NumberFormatException nex) {
                throw constructInvalidValue(lexical);
            }
        }

        public void decode(char[] lexical, int start, int end)
            throws IllegalArgumentException
        {
            start = trimLeading(lexical, start, end);
            end = trimTrailing(lexical, start, end);
            int len = end-start;
            
            if (len == 3) {
                char c = lexical[start];
                if (c == 'I') {
                    if (lexical[start+1] == 'N' && lexical[start+2] == 'F') {
                        mValue = Float.POSITIVE_INFINITY;
                        return;
                    }
                } else if (c == 'N') {
                    if (lexical[start+1] == 'a' && lexical[start+2] == 'N') {
                        mValue = Float.NaN;
                        return;
                    }
                }
            } else if (len == 4) {
                char c = lexical[start];
                if (c == '-') {
                    if (lexical[start+1] == 'I'
                        && lexical[start+2] == 'N'
                        && lexical[start+3] == 'F') {
                        mValue = Float.NEGATIVE_INFINITY;
                        return;
                    }
                }
            }
            
            String lexicalStr = new String(lexical, start, len);
            try {
                mValue = Float.parseFloat(lexicalStr);
            } catch (NumberFormatException nex) {
                throw constructInvalidValue(lexicalStr);
            }
        }
    }

    public final static class DoubleDecoder
        extends DecoderBase
    {
        protected double mValue;

        public DoubleDecoder() { }

        public String getType() { return "double"; }

        public double getValue() { return mValue; }

        public void decode(String lexical)
            throws IllegalArgumentException
        {
            int origEnd = lexical.length();
            int start = trimLeading(lexical, 0, origEnd);
            int end = trimTrailing(lexical, start, origEnd);
            
            if (start > 0 || end < origEnd) {
                lexical = lexical.substring(start, end);
            }
            
            /* Then, leading digit; or one of 3 well-known constants
             * (INF, -INF, NaN)
             */
            int len = lexical.length();
            if (len == 3) {
                char c = lexical.charAt(0);
                if (c == 'I') {
                    if (lexical.charAt(1) == 'N' && lexical.charAt(2) == 'F') {
                        mValue = Double.POSITIVE_INFINITY;
                        return;
                    }
                } else if (c == 'N') {
                    if (lexical.charAt(1) == 'a' && lexical.charAt(2) == 'N') {
                        mValue = Double.NaN;
                        return;
                    }
                }
            } else if (len == 4) {
                char c = lexical.charAt(0);
                if (c == '-') {
                    if (lexical.charAt(1) == 'I'
                        && lexical.charAt(2) == 'N'
                        && lexical.charAt(3) == 'F') {
                        mValue = Double.NEGATIVE_INFINITY;
                        return;
                    }
                }
            }
            
            try {
                mValue = Double.parseDouble(lexical);
            } catch (NumberFormatException nex) {
                throw constructInvalidValue(lexical);
            }
        }

        public void decode(char[] lexical, int start, int end)
            throws IllegalArgumentException
        {
            start = trimLeading(lexical, start, end);
            end = trimTrailing(lexical, start, end);
            int len = end-start;
            
            if (len == 3) {
                char c = lexical[start];
                if (c == 'I') {
                    if (lexical[start+1] == 'N' && lexical[start+2] == 'F') {
                        mValue = Double.POSITIVE_INFINITY;
                        return;
                    }
                } else if (c == 'N') {
                    if (lexical[start+1] == 'a' && lexical[start+2] == 'N') {
                        mValue = Double.NaN;
                        return;
                    }
                }
            } else if (len == 4) {
                char c = lexical[start];
                if (c == '-') {
                    if (lexical[start+1] == 'I'
                        && lexical[start+2] == 'N'
                        && lexical[start+3] == 'F') {
                        mValue = Double.NEGATIVE_INFINITY;
                        return;
                    }
                }
            }
            
            String lexicalStr = new String(lexical, start, len);
            try {
                mValue = Double.parseDouble(lexicalStr);
            } catch (NumberFormatException nex) {
                throw constructInvalidValue(lexicalStr);
            }
        }
    }

    /*
    /////////////////////////////////////////////////////
    // Decoders, other scalars
    /////////////////////////////////////////////////////
    */

    public final static class IntegerDecoder
        extends DecoderBase
    {
        protected BigInteger mValue;

        public IntegerDecoder() { }

        public String getType() { return "integer"; }

        public BigInteger getValue() { return mValue; }

        public void decode(String lexical)
            throws IllegalArgumentException
        {
            int end = lexical.length();
            int start = trimLeading(lexical, 0, end);
            end = trimTrailing(lexical, start, end);
            if (start > 0 || end < lexical.length()) {
                lexical = lexical.substring(start, end);
            }
            try {
                mValue = new BigInteger(lexical);
            } catch (NumberFormatException nex) {
                throw constructInvalidValue(lexical);
            }
        }

        public void decode(char[] lexical, int start, int end)
            throws IllegalArgumentException
        {
            start = trimLeading(lexical, start, end);
            end = trimTrailing(lexical, start, end);
            String lexicalStr = new String(lexical, start, (end-start));
            try {
                mValue = new BigInteger(lexicalStr);
            } catch (NumberFormatException nex) {
                throw constructInvalidValue(lexicalStr);
            }
        }
    }

    public final static class DecimalDecoder
        extends DecoderBase
    {
        protected BigDecimal mValue;

        public DecimalDecoder() { }

        public String getType() { return "decimal"; }

        public BigDecimal getValue() { return mValue; }

        public void decode(String lexical)
            throws IllegalArgumentException
        {
            int end = lexical.length();
            int start = trimLeading(lexical, 0, end);
            end = trimTrailing(lexical, start, end);
            if (start > 0 || end < lexical.length()) {
                lexical = lexical.substring(start, end);
            }
            try {
                mValue = new BigDecimal(lexical);
            } catch (NumberFormatException nex) {
                throw constructInvalidValue(lexical);
            }
        }

        public void decode(char[] lexical, int start, int end)
            throws IllegalArgumentException
        {
            start = trimLeading(lexical, start, end);
            end = trimTrailing(lexical, start, end);
            int len = end-start;
            try {
                mValue = new BigDecimal(lexical, start, len);
            } catch (NumberFormatException nex) {
                throw constructInvalidValue(new String(lexical, start, len));
            }
        }
    }

    public final static class QNameDecoder
        extends DecoderBase
    {
        final NamespaceContext mNsCtxt;

        protected QName mValue;

        public QNameDecoder(NamespaceContext nsc) {
            mNsCtxt = nsc;
        }

        public String getType() { return "QName"; }

        public QName getValue() { return mValue; }

        public void decode(String lexical)
            throws IllegalArgumentException
        {
            lexical = lexical.trim();
            if (lexical.length() == 0) {
                throw constructMissingValue();
            }
            int ix = lexical.indexOf(':');
            if (ix >= 0) { // qualified name
               mValue = resolveQName(lexical.substring(0, ix),
                                     lexical.substring(ix+1));
               return;
            }
            mValue = resolveQName(lexical);
        }

        public void decode(char[] lexical, int start, int end)
            throws IllegalArgumentException
        {
            start = trimLeading(lexical, start, end);
            end = trimTrailing(lexical, start, end);
            
            int i = start;
            for (; i < end; ++i) {
                if (lexical[i] == ':') {
                    mValue = resolveQName(new String(lexical, start, i-start),
                                          new String(lexical, i+1, end-i-1));
                    return;
                }
            }
            mValue = resolveQName(new String(lexical, start, end-start));
        }

        protected QName resolveQName(String localName)
            throws IllegalArgumentException
        {
            // No prefix -> default namespace ("element rules")
            String uri = mNsCtxt.getNamespaceURI("");
            if (uri == null) { // some impls may return null
                uri = "";
            }
            return new QName(uri, localName, "");
        }

        protected QName resolveQName(String prefix, String localName)
            throws IllegalArgumentException
        {
            if (prefix.length() == 0 || localName.length() == 0) {
                // either prefix or local name is empty String, illegal
                throw constructInvalidValue(prefix+":"+localName);
            }
            /* Explicit prefix, must map to a bound namespace; and that
             * namespace can not be empty (only "no prefix", i.e. 'default
             * namespace' has empty URI)
             */
            String uri = mNsCtxt.getNamespaceURI(prefix);
            if (uri == null || uri.length() == 0) {
                throw new IllegalArgumentException("Value \""+lexicalDesc(prefix+":"+localName)+"\" not a valid QName: prefix not bound to a namespace");
            }
            return new QName(uri, localName, prefix);
        }
    }

    /*
    /////////////////////////////////////////////////////
    // Decoders, array
    /////////////////////////////////////////////////////
    */

    abstract static class BaseArrayDecoder
        extends TypedArrayDecoder
    {
        protected final int mStart;

        protected final int mMaxCount;

        protected int mCount = 0;

        protected BaseArrayDecoder(int start, int maxCount)
        {
            mStart = start;
            mMaxCount = maxCount;
        }

        public final int getCount() { return mCount; }
        public final boolean hasRoom() { return mCount < mMaxCount; }
    }

    public final static class IntArrayDecoder
        extends BaseArrayDecoder
    {
        final int[] mResult;

        final IntDecoder mDecoder;

        public IntArrayDecoder(int[] result, int start, int maxCount,
                               IntDecoder intDecoder)
        {
            super(start, maxCount);
            mResult = result;
            mDecoder = intDecoder;
        }

        public boolean decodeValue(String input) throws IllegalArgumentException
        {
            mDecoder.decode(input);
            mResult[mCount++] = mDecoder.getValue();
            return (mCount >= mMaxCount);
        }

        public boolean decodeValue(char[] buffer, int start, int end) throws IllegalArgumentException
        {
            mDecoder.decode(buffer, start, end);
            mResult[mCount++] = mDecoder.getValue();
            return (mCount >= mMaxCount);
        }
    }

    public final static class LongArrayDecoder
        extends BaseArrayDecoder
    {
        final long[] mResult;

        final LongDecoder mDecoder;

        public LongArrayDecoder(long[] result, int start, int maxCount,
                               LongDecoder longDecoder)
        {
            super(start, maxCount);
            mResult = result;
            mDecoder = longDecoder;
        }

        public boolean decodeValue(String input) throws IllegalArgumentException
        {
            mDecoder.decode(input);
            mResult[mCount++] = mDecoder.getValue();
            return (mCount >= mMaxCount);
        }

        public boolean decodeValue(char[] buffer, int start, int end) throws IllegalArgumentException
        {
            mDecoder.decode(buffer, start, end);
            mResult[mCount++] = mDecoder.getValue();
            return (mCount >= mMaxCount);
        }
    }

    public final static class FloatArrayDecoder
        extends BaseArrayDecoder
    {
        final float[] mResult;

        final FloatDecoder mDecoder;

        public FloatArrayDecoder(float[] result, int start, int maxCount,
                               FloatDecoder floatDecoder)
        {
            super(start, maxCount);
            mResult = result;
            mDecoder = floatDecoder;
        }

        public boolean decodeValue(String input) throws IllegalArgumentException
        {
            mDecoder.decode(input);
            mResult[mCount++] = mDecoder.getValue();
            return (mCount >= mMaxCount);
        }

        public boolean decodeValue(char[] buffer, int start, int end) throws IllegalArgumentException
        {
            mDecoder.decode(buffer, start, end);
            mResult[mCount++] = mDecoder.getValue();
            return (mCount >= mMaxCount);
        }
    }

    public final static class DoubleArrayDecoder
        extends BaseArrayDecoder
    {
        final double[] mResult;

        final DoubleDecoder mDecoder;

        public DoubleArrayDecoder(double[] result, int start, int maxCount,
                               DoubleDecoder doubleDecoder)
        {
            super(start, maxCount);
            mResult = result;
            mDecoder = doubleDecoder;
        }

        public boolean decodeValue(String input) throws IllegalArgumentException
        {
            mDecoder.decode(input);
            mResult[mCount++] = mDecoder.getValue();
            return (mCount >= mMaxCount);
        }

        public boolean decodeValue(char[] buffer, int start, int end) throws IllegalArgumentException
        {
            mDecoder.decode(buffer, start, end);
            mResult[mCount++] = mDecoder.getValue();
            return (mCount >= mMaxCount);
        }
    }

    /*
    /////////////////////////////////////////////////////
    // Decoder(s), binary (base64)
    /////////////////////////////////////////////////////
    */
}
