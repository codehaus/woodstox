package com.ctc.wstx.io;

import java.io.*;

import org.codehaus.stax2.EscapingWriterFactory;

import com.ctc.wstx.util.StringUtil;

public final class TextEscaper
{
    private TextEscaper() { }

    /*
    /////////////////////////////////////////////////////////////
    // Factory methods
    /////////////////////////////////////////////////////////////
     */

    public static Writer constructAttrValueWriter(Writer w, String enc,
						  char qchar)
        throws UnsupportedEncodingException
    {
        int bitSize = guessEncodingBitSize(enc);

        /* Anything less than full 16-bits (Unicode) needs to use a
         * Writer that can do escaping... simplistic, but should cover
         * usual cases (7-bit [ascii], 8-bit [ISO-Latin]).
         */
        if (bitSize < 16) {
            return new SingleByteAttrValueWriter(w, enc, qchar, (1 << bitSize));
        }
        return new UTFAttrValueWriter(w, enc, qchar, true);
    }

    public static Writer constructTextWriter(Writer w, String enc)
	throws UnsupportedEncodingException
    {
        int bitSize = guessEncodingBitSize(enc);

        /* Anything less than full 16-bits (Unicode) needs to use a
         * Writer that can do escaping... simplistic, but should cover
         * usual cases (7-bit [ascii], 8-bit [ISO-Latin]).
         */
        if (bitSize < 16) {
            return new SingleByteTextWriter(w, enc, (1 << bitSize));
        }
        return new UTFTextWriter(w, enc, true);
    }

    /*
    /////////////////////////////////////////////////////////////
    // Static utility methods, for non-state-aware escaping
    /////////////////////////////////////////////////////////////
     */

    public static void writeEscapedXMLText(Writer w, String text)
        throws IOException
    {
        final int len = text.length();

        /* 01-Jul-2004, TSa: There were some inexplicable stack traces
         *   that seemed like JIT malfunction, that occured with JDK 1.4.2
         *   on passing single-char String (but fairly infrequently),
         *   causing ArrayIndexOutOfBounds error. Thus, there's special
         *   handling of that one char case now. :-/
         *  (the whole explanation sounds like it came from The Twilight Zone,
         *  but alas it did happen; both on Red Hat 9.0 Linux, and Windows 2K)
         */
        if (len < 2) {
            if (len == 1) {
                char c = text.charAt(0);
                if (c == '<') {
                    w.write("&lt;");
                } else if (c == '&') {
                    w.write("&amp;");
                } else {
                    w.write(text.charAt(0));
                }
            }
            return;
        }
        
        int i = 0;
        while (i < len) {
            int start = i;
            char c = '\u0000';

            for (; i < len; ) {
                c = text.charAt(i);
                if (c == '<' || c == '&') {
                    break;
                }
                if (c == '>' && i >= 2 && text.charAt(i-1) == ']'
                    && text.charAt(i-2) == ']') {
                    break;
                }
                ++i;
            }
            int outLen = i - start;
            if (outLen > 0) {
                w.write(text, start, outLen);
            } 
            if (i < len) {
                if (c == '<') {
                    w.write("&lt;");
                } else if (c == '&') {
                    w.write("&amp;");
                } else if (c == '>') {
                    w.write("&gt;");
                }
            }
            ++i;
        }
    }

    public static void writeEscapedAttrValue(Writer w, String value)
        throws IOException
    {
        int i = 0;
        int len = value.length();
        do {
            int start = i;
            char c = '\u0000';

            for (; i < len; ++i) {
                c = value.charAt(i);
                if (c == '<' || c == '&' || c == '"') {
                    break;
                }
            }
            int outLen = i - start;
            if (outLen > 0) {
                w.write(value, start, outLen);
            }
            if (i < len) {
                if (c == '<') {
                    w.write("&lt;");
                } else if (c == '&') {
                    w.write("&amp;");
                } else if (c == '"') {
                    w.write("&quot;");

                }
            }
        } while (++i < len);
    }

    /**
     * Quoting method used when outputting content that will be part of
     * DTD (internal/external subset). Additional quoting is needed for
     * percentage char, which signals parameter entities.
     */
    public static void outputDTDText(Writer w, char[] ch, int offset, int len)
        throws IOException
    {
        int i = offset;
        len += offset;
        do {
            int start = i;
            char c = '\u0000';

            for (; i < len; ++i) {
                c = ch[i];
                if (c == '&' || c == '%' || c == '"') {
                    break;
                }
            }
            int outLen = i - start;
            if (outLen > 0) {
                w.write(ch, start, outLen);
            }
            if (i < len) {
                if (c == '&') {
                    /* Only need to quote to prevent it from being accidentally
                     * taken as part of char entity...
                     */
                    w.write("&amp;");
                } else if (c == '%') {
                    // Need to quote, to prevent use as Param Entity marker
                    w.write("&#37;");
                } else if (c == '"') {
                    // Need to quote assuming it encloses entity value
                    w.write("&#34;");
                }
            }
        } while (++i < len);
    }

    /**
     * Method used to figure out which part of the Unicode char set the
     * encoding can natively support. Values returned are 7, 8 and 16,
     * to indicate (respectively) "ascii", "ISO-Latin" and "native Unicode".
     * These just best guesses, but should work ok for the most common
     * encodings.
     */
    public static int guessEncodingBitSize(String enc)
    {
        if (enc.length() < 1) { // let's assume default is UTF-8...
            return 16;
        }
        // Let's see if we can find a normalized name, first:
        enc = CharsetNames.normalize(enc);

        // Ok, first, do we have known ones; starting with most common:
        if (enc == CharsetNames.CS_UTF8) {
            return 16; // meaning up to 2^16 can be represented natively
        } else if (enc == CharsetNames.CS_ISO_LATIN1) {
            return 8;
        } else if (enc == CharsetNames.CS_US_ASCII) {
            return 7;
        } else if (enc == CharsetNames.CS_UTF16
                   || enc == CharsetNames.CS_UTF16BE
                   || enc == CharsetNames.CS_UTF16LE
                   || enc == CharsetNames.CS_UTF32BE
                   || enc == CharsetNames.CS_UTF32LE) {
            return 16;
        }

        /* Above and beyond well-recognized names, it might still be
         * good to have more heuristics for as-of-yet unhandled cases...
         * But, it's probably easier to only assume 8-bit clean (could
         * even make it just 7, let's see how this works out)
         */
        return 8;
    }
}

