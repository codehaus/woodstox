package com.ctc.wstx.util;

import java.io.IOException;
import java.io.Writer;

/**
 * Utility class that has static methods for doing XML quoted text and
 * attribute output.
 */
public final class XMLQuoter
{
    public static void outputXMLText(Writer w, String text)
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
                switch (c) {
                case '<':
                    w.write("&lt;");
                    break;
                case '&':
                    w.write("&amp;");
                    break;
                default:
                    w.write(c);
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

    public static void outputXMLText(Writer w, char[] ch, int offset, int len)
        throws IOException
    {
        int i = offset;
        len += offset;
        do {
            int start = i;
            char c = '\u0000';

            for (; i < len; ++i) {
                c = ch[i];
                if (c == '<' || c == '&') {
                    break;
                }
                if (c == '>' && (i >= (offset+2))) {
                    if (ch[i-1] == ']' && ch[i-2] == ']') {
                        break;
                    }
                }
            }
            int outLen = i - start;
            if (outLen > 0) {
                w.write(ch, start, outLen);
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
                if (c == '"' || c == '&' || c == '%') {
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
                } else if (c == '>') {
                    // Need to quote, to prevent use as Param Entity marker
                    w.write("&#37;");
                } else if (c == '"') {
                    // Need to quote assuming it encloses entity value
                    w.write("&#34;");
                }
            }
        } while (++i < len);
    }

    public static void outputDoubleQuotedAttr(Writer w, String value)
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

    public static boolean isAllWhitespace(String str) {
        for (int i = 0, len = str.length(); i < len; ++i) {
            if (str.charAt(i) > 0x0020) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAllWhitespace(char[] ch, int start, int len) {
        len += start;
        for (; start < len; ++start) {
            if (ch[start] > 0x0020) {
                return false;
            }
        }
        return true;
    }
}
