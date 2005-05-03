package com.ctc.wstx.io;

import java.io.*;

/**
 *
 */
public class AttrValueEscapingWriter
    extends FilterWriter
{
    /**
     * Character that is considered to be the enclosing quote character;
     * for XML either single or double quote.
     */
    final char mQuoteChar;

    /**
     * Entity String to use for escaping the quote character.
     */
    final String mQuoteEntity;

    public AttrValueEscapingWriter(Writer out, String enc,
				   char qchar, String qent)
    {
        super(out);
        mQuoteChar = qchar;
        mQuoteEntity = qent;
    }

    public void write(int c) throws IOException
    {
        if (c == mQuoteChar) {
            out.write(mQuoteEntity);
        } else if (c == '<') {
            out.write("&lt;");
        } else if (c == '&') {
            out.write("&amp;");
        } else {
            out.write(c);
        }
    }

    public void write(char cbuf[], int off, int len) throws IOException
    {
        len += off;
        final char qchar = mQuoteChar;
        do {
            int start = off;
            char c = '\u0000';

            for (; off < len; ++off) {
                c = cbuf[off];
                if (c == '<' || c == '&' || c == qchar) {
                    break;
                }
            }
            int outLen = off - start;
            if (outLen > 0) {
                out.write(cbuf, start, outLen);
            }
            if (off < len) {
                if (c == '<') {
                    out.write("&lt;");
                } else if (c == '&') {
                    out.write("&amp;");
                } else if (c == qchar) {
                    out.write(mQuoteEntity);
                }
            }
        } while (++off < len);
    }

    public void write(String str, int off, int len) throws IOException
    {
        len += off;
        final char qchar = mQuoteChar;
        do {
            int start = off;
            char c = '\u0000';

            for (; off < len; ++off) {
                c = str.charAt(off);
                if (c == '<' || c == '&' || c == qchar) {
                    break;
                }
            }
            int outLen = off - start;
            if (outLen > 0) {
                out.write(str, start, outLen);
            }
            if (off < len) {
                if (c == '<') {
                    out.write("&lt;");
                } else if (c == '&') {
                    out.write("&amp;");
                } else if (c == qchar) {
                    out.write(mQuoteEntity);
                }
            }
        } while (++off < len);
    }

    /*
    /////////////////////////////////////////////////////////////
    // Static utility methods, for non-state-aware escaping
    /////////////////////////////////////////////////////////////
     */

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
}

