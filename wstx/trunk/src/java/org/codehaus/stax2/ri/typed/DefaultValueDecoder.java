package org.codehaus.stax2.typed;

import java.math.BigDecimal;

/**
 * Default implementation that strives for correctness, but is not
 * ridiculously heavily optimized.
 *
 * @author Tatu Saloranta
 */
public class DefaultValueDecoder
{
    /*
    ///////////////////////////////////////////////
    // Public API implementation
    ///////////////////////////////////////////////
     */

    public int decodeInt(String lexical, int first, int last)
        throws IllegalArgumentException
    {
        /* First need to trim leading and/or trailing white space.
         * We need not worry about other possible white space,
         * since it won't be legal, collapsed or not.
         */
        first = trimLeadingSpace(lexical, first, last);
        last = trimTrailingSpace(lexical, first, last);

        // !!! TBI
        return 0;
    }

    public int decodeInt(char[] lexical, int first, int last)
        throws IllegalArgumentException
    {
        // !!! TBI
        return 0;
    }

    public long decodeLong(String lexical, int first, int last)
        throws IllegalArgumentException
    {
        // !!! TBI
        return 0L;
    }

    public long decodeLong(char[] lexical, int first, int last)
        throws IllegalArgumentException
    {
        // !!! TBI
        return 0L;
    }

    // Fixed-length floating-point types

    public float decodeFloat(String lexical, int first, int last)
        throws IllegalArgumentException
    {
        // !!! TBI
        return 0.0f;
    }

    public float decodeFloat(char[] lexical, int first, int last)
        throws IllegalArgumentException
    {
        // !!! TBI
        return 0.0f;
    }

    public double decodeDouble(String lexical, int first, int last)
        throws IllegalArgumentException
    {
        // !!! TBI
        return 0.0;
    }

    public double decodeDouble(char[] lexical, int first, int last)
        throws IllegalArgumentException
    {
        // !!! TBI
        return 0.0f;
    }

    // Unlimited precision floating-point types

    public BigDecimal decodeDecimal(String lexical, int first, int last)
        throws IllegalArgumentException
    {
        // !!! TBI
        return null;
    }

    public BigDecimal decodeDecimal(char[] lexical, int first, int last)
        throws IllegalArgumentException
    {
        // !!! TBI
        return null;
    }

    // Enumerated types

    public boolean decodeBoolean(String lexical, int first, int last)
        throws IllegalArgumentException
    {
        /* First need to trim leading and/or trailing white space.
         * We need not worry about other possible white space,
         * since it won't be legal, collapsed or not.
         */

        first = trimLeadingSpace(lexical, first, last);
        last = trimTrailingSpace(lexical, first, last);

        int len = last-first;

        if (len == 4) {
            if (lexical.charAt(first) == 't'
                && lexical.charAt(++first) == 'r'
                && lexical.charAt(++first) == 'u'
                && lexical.charAt(++first) == 'e') {
                return true;
            }
        } else if (len == 5) {
            if (lexical.charAt(first) == 'f'
                && lexical.charAt(++first) == 'a'
                && lexical.charAt(++first) == 'l'
                && lexical.charAt(++first) == 's'
                && lexical.charAt(++first) == 'e') {
                return false;
            }
        } else if (len == 1) {
            char c = lexical.charAt(first);
            if (c == '0') return false;
            if (c == '1') return true;
        }
        throw new IllegalArgumentException("value \""+lexical.substring(first, last)+"\" not a valid lexical representation of boolean");
    }

    public boolean decodeBoolean(char[] lexical, int first, int last)
        throws IllegalArgumentException
    {
        /* First need to trim leading and/or trailing white space.
         * We need not worry about other possible white space,
         * since it won't be legal, collapsed or not.
         */

        first = trimLeadingSpace(lexical, first, last);
        last = trimTrailingSpace(lexical, first, last);

        int len = last-first;

        if (len == 4) {
            if (lexical[first] == 't'
                && lexical[++first] == 'r'
                && lexical[++first] == 'u'
                && lexical[++first] == 'e') {
                return true;
            }
        } else if (len == 5) {
            if (lexical[first] == 'f'
                && lexical[++first] == 'a'
                && lexical[++first] == 'l'
                && lexical[++first] == 's'
                && lexical[++first] == 'e') {
                return false;
            }
        } else if (len == 1) {
            char c = lexical[first];
            if (c == '0') return false;
            if (c == '1') return true;
        }
        throw new IllegalArgumentException("value \""+new String(lexical, first, len)+"\" not a valid lexical representation of boolean");
    }

    /*
    ///////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////
     */

    protected int trimLeadingSpace(String str, int first, int last)
    {
        while (first < last && isSpace(str.charAt(first))) {
            ++first;
        }
        return first;
    }

    protected int trimLeadingSpace(char[] c, int first, int last)
    {
        while (first < last && isSpace(c[first])) {
            ++first;
        }
        return first;
    }

    /**
     *<p>
     * Note: it is assumed that this method is always called after
     * calling <code>trimLeadingSpace</trim>, and that consequently
     * it is known that either character at <code>first</code> is
     * not white space, or that <code>last</code> is no larger
     * than <code>first</code>. That is just a long way of saying
     * that the character at <code>first</code> need not be
     * checked.
     */
    protected int trimTrailingSpace(String str, int first, int last)
    {
        while (last > first && isSpace(str.charAt(last))) {
            --last;
        }
        return last;
    }

    protected int trimTrailingSpace(char[] c, int first, int last)
    {
        while (last > first && isSpace(c[last])) {
            --last;
        }
        return last;
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
