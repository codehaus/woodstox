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
        return new UTFAttrValueWriter(w, enc, qchar);
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
        return new UTFTextWriter(w, enc);
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
        char c = enc.charAt(0);
        
        /* Hmmh. Now this is bit tricky... whether to do "anything other
         * than Ascii is 8-bit clean" or "anything other than ISO-Latin
         * _may_ only support 7-bit ones" (obviously there are even more
         * strict cases but these are main alternatives).
         * It's also possible that some schemes (Shift-JIS?) would support
         * much bigger subset of Unicode... but let's cross that bridge
         * if and when we get there.
         */
        /* For now, let's just do former, so that in general first 256
         * chars get output as is, while others get escaped.
         */
        
        /* Let's check first if it's a Unicode encoding... they
         * start with "UTF" and "UCS" (UTF-8, UCS-xxx). Otherwise,
         * let's just play safe and assume it's a single-byte encoding
         */
        if (c == 'u' || c == 'U') {
            if (StringUtil.encodingStartsWith(enc, "UTF")
                || StringUtil.encodingStartsWith(enc, "UCS")) {
                return 16;
            }
            if (StringUtil.equalEncodings(enc, "US-ASCII")) {
                return 7;
            }
            if (StringUtil.encodingStartsWith(enc, "UNICODE")) {
                // Not too standard... but is listed in IANA charset list
                return 16;
            }
        } else if (c == 'i' || c == 'I') {
            if (StringUtil.encodingStartsWith(enc, "ISO-10646")) {
                /* Hmmh. There are boatloads of alternatives here, it
                 * seems (see http://www.iana.org/assignments/character-sets
                 * for details)
                 */
                int ix = enc.indexOf("10646");
                String suffix = enc.substring(ix+5);
                
                if (StringUtil.equalEncodings(suffix, "UCS-Basic")) { // ascii
                    return 7;
                }
                if (StringUtil.equalEncodings(suffix, "Unicode-Latin1")) { // ISO-Latin
                    return 8;
                }
                if (StringUtil.equalEncodings(suffix, "UCS-2")) { // ISO-10646-UCS-2 == 2-byte unicode
                    return 16;
                }
                if (StringUtil.equalEncodings(suffix, "UCS-4")) {
                    return 16; // while it is 32-bit wide, we only have 16-bit chars
                }
                if (StringUtil.equalEncodings(suffix, "UTF-1")) {
                    // Universal Transfer Format (1), this is the multibyte encoding, that subsets ASCII-7.
                    return 7;
                }
                if (StringUtil.equalEncodings(suffix, "J-1")) {
                    // Name: ISO-10646-J-1, Source: ISO 10646 Japanese, see RFC 1815.
                    // ... so what does that really mean? let's limit to ascii
                    return 7;
                }
                if (StringUtil.equalEncodings(suffix, "US-ASCII")) {
                    return 7;
                }
                // and with nothing else, it's ISO-Latin1...
                if (StringUtil.equalEncodings(enc, "ISO-10646")) {
                    return 8;
                }
            } else if (StringUtil.encodingStartsWith(enc, "ISO-646")) {
                return 7; // another name for Ascii...
            } else if (StringUtil.encodingStartsWith(enc, "ISO-Latin")) {
                return 8;
            }
        } else if (c == 'a' || c == 'A') {
            if (StringUtil.equalEncodings(enc, "ASCII")) {
                return 7;
            }
        }
        
        // Ok, let's just assume it's 8-bit clean, but no more...
        return 8;
    }
}

