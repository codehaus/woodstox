package com.ctc.wstx.io;

import java.io.*;

/**
 * Basic escaping writer used when outputting normal textual content.
 * Only needs to escape '&lt;' and '&amp;' characters, plus '&gt' when
 * following "]]" string. Note that to detect the last case, the logic
 * is bit simplified, so that any '&gt;' that immediately follows a ']'
 * gets escaped. Further, since the Writer does not know of text segment
 * boundaries, it is possible that there is no immediate sequence in the
 * output. This is not a correctness problem,
 * however (since such escaping is perfectly legal, although not
 * strictly necessary), just a slightly "unoptimal" behaviour.
 *<p>
 */
public class TextEscapingWriter
    extends FilterWriter
{
    private boolean mJustWroteBracket = false;

    public TextEscapingWriter(Writer out) {
        super(out);
    }

    public void write(int c) throws IOException
    {
        if (c == '<') {
            out.write("&lt;");
        } else if (c == '&') {
            out.write("&amp;");
        } else if (c == '>') {
            if (mJustWroteBracket) {
                out.write("&gt;");
            } else {
                out.write(c);
            }
        } else if (c == ']') {
            out.write(c);
            mJustWroteBracket = true;
            return;
        } else {
            out.write(c);
        }
        mJustWroteBracket = false;
    }

    public void write(char cbuf[], int offset, int len) throws IOException
    {
        // Let's simplify code a bit and offload the trivial case...
        if (len < 2) {
            if (len == 1) {
                write(cbuf[offset]);
            }
            return;
        }

        len += offset; // to get the index past last char to output
        // Ok, leftover ']' to cause quoting of '>'?
        if (mJustWroteBracket) {
            char c = cbuf[offset];
            if (c == '>') {
                out.write("&gt;");
                ++offset;
            }
        }

        char c = '\u0000';
        do {
            int start = offset;

            for (; offset < len; ++offset) {
                c = cbuf[offset];
                if (c == '<' || c == '&') {
                    break;
                }
                if (c == '>' && cbuf[offset-1] == ']') {
                    break;
                }
            }
            int outLen = offset - start;
            if (outLen > 0) {
                out.write(cbuf, start, outLen);
            }
            if (offset < len) { // didn't just run out of data
                if (c == '<') {
                    out.write("&lt;");
                } else if (c == '&') {
                    out.write("&amp;");
                } else if (c == '>') {
                    out.write("&gt;");
                }
            }
        } while (++offset < len);

        // Ok, did we end up with a bracket?
        mJustWroteBracket = (c == ']');
    }

    public void write(String str, int offset, int len) throws IOException
    {
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
                write(str.charAt(offset));
            }
            return;
        }

        len += offset; // to get the index past last char to output
        // Ok, leftover ']' to cause quoting of '>'?
        if (mJustWroteBracket) {
            char c = str.charAt(offset);
            if (c == '>') {
                out.write("&gt;");
                ++offset;
            }
        }

        char c = '\u0000';
        do {
            int start = offset;

            for (; offset < len; ++offset) {
                c = str.charAt(offset);
                if (c == '<' || c == '&') {
                    break;
                }
                if (c == '>' && str.charAt(offset-1) == ']') {
                    break;
                }
            }
            int outLen = offset - start;
            if (outLen > 0) {
                out.write(str, start, outLen);
            } 
            if (offset < len) { // didn't just run out of data
                if (c == '<') {
                    out.write("&lt;");
                } else if (c == '&') {
                    out.write("&amp;");
                } else if (c == '>') {
                    out.write("&gt;");
                }
            }
        } while (++offset < len);

        // Ok, did we end up with a bracket?
        mJustWroteBracket = (c == ']');
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

    /*
    public static void writeEscapedXMLText(Writer w, char[] ch, int offset, int len)
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
    */
}
