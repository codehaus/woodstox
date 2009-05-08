package com.ctc.wstx.io;

import java.io.*;

/**
 * Escaping writer that will properly escape characters of the attribute
 * values that need to be escaped, when outputting using a Writer that
 * produces a subset of Unicode values.
 * When underlying Writer only allows for direct outputting of a subset of
 * Unicode values, it is generally done so that only lowest
 * Unicode characters (7-bit ones for Ascii, 8-bit ones for ISO-Latin,
 * something similar for other ISO-8859-1 encodings) can be output
 * as is, and the rest need to be output as character entities.
 */
public class SingleByteAttrValueWriter
    extends WriterBase
{
    /**
     * First Unicode character (one with lowest value) after (and including)
     * which character entities have to be used.
     */
    private final char mHighChar;

    /**
     * Character that is considered to be the enclosing quote character;
     * for XML either single or double quote.
     */
    final char mQuoteChar;

    /**
     * Entity String to use for escaping the quote character.
     */
    final String mQuoteEntity;

    public SingleByteAttrValueWriter(Writer out, String enc, char qchar,
                                     int charsetSize)
    {
        super(out);
        mQuoteChar = qchar;
        mQuoteEntity = getQuoteEntity(qchar);
        mHighChar = (char) charsetSize;
    }

    public void write(int c) throws IOException
    {
        if (c >= mHighChar) { // out of range, need to quote:
            writeAsEntity(c);
        } else if (c <= HIGHEST_ENCODABLE_ATTR_CHAR) { // special char?
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
                if (c >= mHighChar) { // out of range, have to escape
                    break;
                }
                if (c <= HIGHEST_ENCODABLE_ATTR_CHAR) { // special char?
                    if (c == qchar) {
                        ent = mQuoteEntity;
                        break;
                    } else if (c == '<') {
                        ent = "&lt;";
                        break;
                    } else if (c == '&') {
                        ent = "&amp;";
                        break;
                    } else if (c < CHAR_SPACE) { // tab, cr/lf need encoding too
                        if (c == CHAR_NULL) {
                            throwNullChar();
                        }
                        break; // need quoting ok
                    }
                }
                // otherwise ok
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
                if (c >= mHighChar) { // out of range, have to escape
                    break;
                }
                if (c <= HIGHEST_ENCODABLE_ATTR_CHAR) { // special char?
                    if (c == qchar) {
                        ent = mQuoteEntity;
                        break;
                    } else if (c == '<') {
                        ent = "&lt;";
                        break;
                    } else if (c == '&') {
                        ent = "&amp;";
                        break;
                    } else if (c < CHAR_SPACE) { // tab, cr/lf need encoding too
                        if (c == CHAR_NULL) {
                            throwNullChar();
                        }
                        break; // need quoting ok
                    }
                }
                // otherwise ok
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
