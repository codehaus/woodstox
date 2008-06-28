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

    protected IntDecoder mIntDecoder = null;
    protected LongDecoder mLongDecoder = null;

    public ValueDecoderFactory() { }

    /*
    /////////////////////////////////////////////////////
    // Factory methods
    /////////////////////////////////////////////////////
    */

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

    /*
    /////////////////////////////////////////////////////
    // Shared decoder base class
    /////////////////////////////////////////////////////
    */

    /**
     * There are some things common to all textual decoders (like
     * white space trimming).
     */
    abstract static class DecoderBase
        extends AsciiValueDecoder
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
    // Decoders, simple scalar
    /////////////////////////////////////////////////////
    */

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

    /*
    /////////////////////////////////////////////////////
    // Decoders, array
    /////////////////////////////////////////////////////
    */

    /*
    /////////////////////////////////////////////////////
    // Decoder(s), binary (base64)
    /////////////////////////////////////////////////////
    */
}
