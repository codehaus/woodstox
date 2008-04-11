package org.codehaus.stax2.ri.typed;

import java.math.BigDecimal;

import org.codehaus.stax2.typed.ValueDecoder;

/**
 * Default implementation that strives for correctness, but is not
 * ridiculously heavily optimized.
 *
 * @author Tatu Saloranta
 */
public class DefaultValueDecoder
{
    private final static long L_BILLION = 1000000000;

    private final static long L_MAX_INT = (long) Integer.MAX_VALUE;

    private final static long L_MIN_INT = (long) Integer.MIN_VALUE;

    /*
    ///////////////////////////////////////////////
    // Public API implementation
    ///////////////////////////////////////////////
     */

    public int decodeInt(String lexical, int start, int end)
        throws IllegalArgumentException
    {
        /* Start need to trim leading and/or trailing white space.
         * We need not worry about other possible white space,
         * since it won't be legal, collapsed or not.
         */
        start = trimLeadingSpace(lexical, start, end);
        end = trimTrailingSpace(lexical, start, end);

        boolean neg = false;
        int offset = start;

        if (start < end) {
            char ch = lexical.charAt(offset);
            if (ch == '-') {
                neg = true;
                ++offset;
            } else if (ch == '+') {
                ++offset;
            }
        }

        // First need to ensure all chars are digits
        if (!allDigits(lexical, offset, end)) {
            throw new IllegalArgumentException("value \""+lexical.substring(start, end)+"\" not a valid lexical representation of an integer");
        }
        int len = end-offset;
        if (len < 10) { // no overflow
            int i = parseInt(lexical, offset, end);
            return neg ? -i : i;
        }
        // Otherwise, may have overflow
        // Max 10 digits for a legal int
        if (len == 10) { // may be ok but let's check for over/underflow
            long l = parseLong(lexical, start, end);
            if (neg) {
                l = -l;
                if (l >= L_MIN_INT) {
                    return (int) l;
                }
            } else {
                if (l <= L_MAX_INT) {
                    return (int) l;
                }
            }
        }
        throw new IllegalArgumentException("value \""+lexical.substring(start, end)+"\" not a valid 32-bit integer: overflow.");
    }

    public int decodeInt(char[] lexical, int start, int end)
        throws IllegalArgumentException
    {
        start = trimLeadingSpace(lexical, start, end);
        end = trimTrailingSpace(lexical, start, end);

        boolean neg = false;
        int offset = start;

        if (start < end) {
            char ch = lexical[offset];
            if (ch == '-') {
                neg = true;
                ++offset;
            } else if (ch == '+') {
                ++offset;
            }
        }

        // First need to ensure all chars are digits
        if (!allDigits(lexical, offset, end)) {
            throw new IllegalArgumentException("value \""+new String(lexical, start, end-start)+"\" not a valid lexical representation of an integer");
        }
        int len = end-offset;
        if (len < 10) { // no overflow
            int i = parseInt(lexical, offset, end);
            return neg ? -i : i;
        }
        // Otherwise, may have overflow
        // Max 10 digits for a legal int
        if (len == 10) { // may be ok but let's check for over/underflow
            long l = parseLong(lexical, start, end);
            if (neg) {
                l = -l;
                if (l >= L_MIN_INT) {
                    return (int) l;
                }
            } else {
                if (l <= L_MAX_INT) {
                    return (int) l;
                }
            }
        }
        throw new IllegalArgumentException("value \""+new String(lexical, start, end-start)+"\" not a valid 32-bit integer: overflow.");
    }

    public long decodeLong(String lexical, int start, int end)
        throws IllegalArgumentException
    {
        // !!! TBI
        return 0L;
    }

    public long decodeLong(char[] lexical, int start, int end)
        throws IllegalArgumentException
    {
        // !!! TBI
        return 0L;
    }

    // Fixed-length floating-point types

    public float decodeFloat(String lexical, int start, int end)
        throws IllegalArgumentException
    {
        // !!! TBI
        return 0.0f;
    }

    public float decodeFloat(char[] lexical, int start, int end)
        throws IllegalArgumentException
    {
        // !!! TBI
        return 0.0f;
    }

    public double decodeDouble(String lexical, int start, int end)
        throws IllegalArgumentException
    {
        // !!! TBI
        return 0.0;
    }

    public double decodeDouble(char[] lexical, int start, int end)
        throws IllegalArgumentException
    {
        // !!! TBI
        return 0.0f;
    }

    // Unlimited precision floating-point types

    public BigDecimal decodeDecimal(String lexical, int start, int end)
        throws IllegalArgumentException
    {
        // !!! TBI
        return null;
    }

    public BigDecimal decodeDecimal(char[] lexical, int start, int end)
        throws IllegalArgumentException
    {
        // !!! TBI
        return null;
    }

    // Enumerated types

    public boolean decodeBoolean(String lexical, int start, int end)
        throws IllegalArgumentException
    {
        /* Start need to trim leading and/or trailing white space.
         * We need not worry about other possible white space,
         * since it won't be legal, collapsed or not.
         */
        start = trimLeadingSpace(lexical, start, end);
        end = trimTrailingSpace(lexical, start, end);

        int len = end-start;

        if (len == 4) {
            if (lexical.charAt(start) == 't'
                && lexical.charAt(++start) == 'r'
                && lexical.charAt(++start) == 'u'
                && lexical.charAt(++start) == 'e') {
                return true;
            }
        } else if (len == 5) {
            if (lexical.charAt(start) == 'f'
                && lexical.charAt(++start) == 'a'
                && lexical.charAt(++start) == 'l'
                && lexical.charAt(++start) == 's'
                && lexical.charAt(++start) == 'e') {
                return false;
            }
        } else if (len == 1) {
            char c = lexical.charAt(start);
            if (c == '0') return false;
            if (c == '1') return true;
        }
        throw new IllegalArgumentException("value \""+lexical.substring(start, end)+"\" not a valid lexical representation of boolean");
    }

    public boolean decodeBoolean(char[] lexical, int start, int end)
        throws IllegalArgumentException
    {
        start = trimLeadingSpace(lexical, start, end);
        end = trimTrailingSpace(lexical, start, end);

        int len = end-start;

        if (len == 4) {
            if (lexical[start] == 't'
                && lexical[++start] == 'r'
                && lexical[++start] == 'u'
                && lexical[++start] == 'e') {
                return true;
            }
        } else if (len == 5) {
            if (lexical[start] == 'f'
                && lexical[++start] == 'a'
                && lexical[++start] == 'l'
                && lexical[++start] == 's'
                && lexical[++start] == 'e') {
                return false;
            }
        } else if (len == 1) {
            char c = lexical[start];
            if (c == '0') return false;
            if (c == '1') return true;
        }
        throw new IllegalArgumentException("value \""+new String(lexical, start, len)+"\" not a valid lexical representation of boolean");
    }

    /*
    ///////////////////////////////////////////////
    // Internal methods, int conversions
    ///////////////////////////////////////////////
     */

    /**
     * Fast method for parsing integers that are known to fit into
     * regular 32-bit signed int type. This means that length is
     * between 1 and 9 digits (inclusive)
     *<p>
     * Note: public to let unit tests call it
     */
    protected final static int parseInt(char[] digitChars, int start, int end)
    {
        int num = digitChars[start] - '0';
        // This looks ugly, but appears to be the fastest way:
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

    protected final static int parseInt(String digitChars, int start, int end)
    {
        int num = digitChars.charAt(start) - '0';
        // This looks ugly, but appears to be the fastest way:
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

    public final static long parseLong(char[] digitChars, int start, int end)
    {
        // Note: caller must ensure length is [10, 18]
        int start2 = end-9;
        long val = parseInt(digitChars, start, start2) * L_BILLION;
        return val + (long) parseInt(digitChars, start2, end);
    }

    public final static long parseLong(String digitChars, int start, int end)
    {
        // Note: caller must ensure length is [10, 18]
        int start2 = end-9;
        long val = parseInt(digitChars, start, start2) * L_BILLION;
        return val + (long) parseInt(digitChars, start2, end);
    }

    /*
    ///////////////////////////////////////////////
    // Internal methods, low-level string manipulation
    ///////////////////////////////////////////////
     */

    protected int trimLeadingSpace(String str, int start, int end)
    {
        while (start < end && isSpace(str.charAt(start))) {
            ++start;
        }
        return start;
    }

    protected int trimLeadingSpace(char[] c, int start, int end)
    {
        while (start < end && isSpace(c[start])) {
            ++start;
        }
        return start;
    }

    /**
     *<p>
     * Note: it is assumed that this method is always called after
     * calling <code>trimLeadingSpace</trim>, and that consequently
     * it is known that either character at <code>start</code> is
     * not white space, or that <code>end</code> is no larger
     * than <code>start</code>. That is just a long way of saying
     * that the character at <code>start</code> need not be
     * checked.
     */
    protected int trimTrailingSpace(String str, int start, int end)
    {
        while (end > start && isSpace(str.charAt(end))) {
            --end;
        }
        return end;
    }

    protected int trimTrailingSpace(char[] c, int start, int end)
    {
        while (end > start && isSpace(c[end])) {
            --end;
        }
        return end;
    }

    protected boolean allDigits(String str, int start, int end)
    {
        while (start < end) {
            char ch = str.charAt(start++);
            if (ch > '9' || ch < '0') {
                return false;
            }
        }
        return true;
    }

    protected boolean allDigits(char[] c, int start, int end)
    {
        while (start < end) {
            char ch = c[start++];
            if (ch > '9' || ch < '0') {
                return false;
            }
        }
        return true;
    }

    /**
     *<p>
     * Note: final for efficient inlining by JVM/HotSpot. Means
     * one can not redefine it; and instead need to override
     * callers (which just means methods above used for trimming
     * white space)
     */
    private final boolean isSpace(char c)
    {
        return ((int) c) < 0x0020;
    }
}
