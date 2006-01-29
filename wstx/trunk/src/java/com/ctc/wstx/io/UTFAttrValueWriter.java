package com.ctc.wstx.io;

import java.io.*;

/**
 * Escaping writer that will properly escape characters from the attribute
 * values that need to be escaped, when outputting using "native" Unicode
 * aware Writer.
 */
public class UTFAttrValueWriter
    extends WriterBase
{
    private final boolean mEscapeCR;

    /**
     * Character that is considered to be the enclosing quote character;
     * for XML either single or double quote.
     */
    final char mQuoteChar;

    /**
     * Entity String to use for escaping the quote character.
     */
    final String mQuoteEntity;

    /**
     * @param enc Name of actual encoding in use; ignored for UTF
     *   writers
     * @param escapeCR If true, will encode \r character; if false, will
     *   output as is (former is needed for reliable round-tripping, but
     *   adds verbosity without necessary benefits)
     */
    public UTFAttrValueWriter(Writer out, String enc, char qchar,
                              boolean escapeCR)
    {
        super(out);
        mEscapeCR = escapeCR;
        mQuoteChar = qchar;
        mQuoteEntity = getQuoteEntity(qchar);
    }

    public void write(int c) throws IOException
    {
        // Nothing above the range, just need to check for low range:
        if (c <= HIGHEST_ENCODABLE_ATTR_CHAR) {
            if (c == mQuoteChar) {
                out.write(mQuoteEntity);
                return;
            }
            if (c == '<') {
                out.write("&lt;");
                return;
            }
            if (c == '&') {
                out.write("&amp;");
                return;
            }
            if (c == '\r' && mEscapeCR) {
                out.write(STR_ESCAPED_CR);
                return;
            }
            if (c < CHAR_SPACE) { // tab, cr/lf need encoding too
                if (c == CHAR_NULL) {
                    throwNullChar();
                } else {
                    writeAsEntity(c);
                    return;
                }
            }
        }
        // fine as is
        out.write(c);
    }

    public void write(char cbuf[], int offset, int len) throws IOException
    {
        len += offset;
        final char qchar = mQuoteChar;
        do {
            int start = offset;
            char c = CHAR_NULL;
            String ent = null;

            for (; offset < len; ++offset) {
                c = cbuf[offset]; 
                if (c <= HIGHEST_ENCODABLE_ATTR_CHAR) { // special char?
                    if (c == qchar) {
                        ent = mQuoteEntity;
                        break;
                    }
                    if (c == '<') {
                        ent = "&lt;";
                        break;
                    }
                    if (c == '&') {
                        ent = "&amp;";
                        break;
                    }
                    if (c == '\r' && mEscapeCR) {
                        ent = STR_ESCAPED_CR;
                        break;
                    }
                    if (c < CHAR_SPACE) { // tab, cr/lf need encoding too
                        if (c == CHAR_NULL) {
                            throwNullChar();
                        }
                        break;
                    }
                }
            }
            int outLen = offset - start;
            if (outLen > 0) {
                out.write(cbuf, start, outLen);
            } 
            if (ent != null) {
                out.write(ent);
                ent = null;
            } else if (offset < len) {
                writeAsEntity(c);
            }
        } while (++offset < len);
    }
    
    public void write(String str, int offset, int len) throws IOException
    {
        len += offset;
        final char qchar = mQuoteChar;
        do {
            int start = offset;
            char c = '\u0000';
            String ent = null;

            for (; offset < len; ++offset) {
                c = str.charAt(offset);
                if (c <= HIGHEST_ENCODABLE_ATTR_CHAR) { // special char?
                    if (c == qchar) {
                        ent = mQuoteEntity;
                        break;
                    }
                    if (c == '<') {
                        ent = "&lt;";
                        break;
                    }
                    if (c == '&') {
                        ent = "&amp;";
                        break;
                    }
                    if (c == '\r' && mEscapeCR) {
                        ent = STR_ESCAPED_CR;
                        break;
                    }
                    if (c < CHAR_SPACE) { // tab, cr/lf need encoding too
                        if (c == CHAR_NULL) {
                            throwNullChar();
                        }
                        break;
                    }
                }
            }
            int outLen = offset - start;
            if (outLen > 0) {
                out.write(str, start, outLen);
            }
            if (ent != null) {
                out.write(ent);
                ent = null;
            } else if (offset < len) {
                writeAsEntity(c);
            }
        } while (++offset < len);
    }
}
