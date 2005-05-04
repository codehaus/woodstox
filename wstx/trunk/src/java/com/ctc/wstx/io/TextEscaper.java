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
	char c = (enc.length() > 0) ? enc.charAt(0) : ' ';

	/* Let's just check first if it's a Unicode encoding... they
	 * start with "UTF" and "UCS" (UTF-8, UCS-xxx). Otherwise,
	 * let's just play safe and assume it's a single-byte encoding
	 */
	if (c == 'u' || c == 'U') {
	    if (StringUtil.encodingStartsWith(enc, "UTF")
		|| StringUtil.encodingStartsWith(enc, "UCS")) {
		return new UTFAttrValueWriter(w, enc, qchar);
	    }
	} else if (c == 'i' || c == 'I') {
	    if (StringUtil.encodingStartsWith(enc, "ISO-10646")) {
		return new UTFAttrValueWriter(w, enc, qchar);
	    }
	}
	// Most likely "ISO-8859-x" or "US-ASCII"...
	return new SingleByteAttrValueWriter(w, enc, qchar);
    }

    public static Writer constructTextWriter(Writer w, String enc)
	throws UnsupportedEncodingException
    {
	char c = (enc.length() > 0) ? enc.charAt(0) : ' ';

	/* Let's just check first if it's a Unicode encoding... they
	 * start with "UTF" and "UCS" (UTF-8, UCS-xxx). Otherwise,
	 * let's just play safe and assume it's a single-byte encoding
	 */
	if (c == 'u' || c == 'U') {
	    if (StringUtil.encodingStartsWith(enc, "UTF")
		|| StringUtil.encodingStartsWith(enc, "UCS")) {
		return new UTFTextWriter(w, enc);
	    }
	} else if (c == 'i' || c == 'I') {
	    if (StringUtil.encodingStartsWith(enc, "ISO-10646")) {
		return new UTFTextWriter(w, enc);
	    }
	}
	// Most likely "ISO-8859-x" or "US-ASCII"...
	return new SingleByteTextWriter(w, enc);
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
}

