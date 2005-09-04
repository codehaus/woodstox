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
    private final char mLowestEntity;

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
        mLowestEntity = (char) charsetSize;
    }

    public void write(int c) throws IOException
    {
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
            // Do we want to encode "restricted" spaces as char entities?
            //if (c < CHAR_SPACE) { }
            if (c == 0) {
                throwNullChar();
            }
        } else if (c >= mLowestEntity) {
            writeAsEntity(c);
        } else {
            out.write(c);
        }
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
                if (c > HIGHEST_ENCODABLE_ATTR_CHAR) {
                    continue;
                }
                if (c < mLowestEntity) {
                    if (c == qchar) {
                        ent = mQuoteEntity;
                    } else if (c == '<') {
                        ent = "&lt;";
                    } else if (c == '&') {
                        ent = "&amp;";
                    } else if (c == CHAR_NULL) {
                        throwNullChar();
                    } else {
                        continue;
                    }
                } // else 'ent' remains null
                break;
            }
            int outLen = offset - start;
            if (outLen > 0) {
                out.write(cbuf, start, outLen);
            } 
            if (ent != null) {
                out.write(ent);
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
                if (c > HIGHEST_ENCODABLE_ATTR_CHAR) {
                    continue;
                }
                if (c < mLowestEntity) {
                    if (c == qchar) {
                        ent = mQuoteEntity;
                    } else if (c == '<') {
                        ent = "&lt;";
                    } else if (c == '&') {
                        ent = "&amp;";
                    } else if (c == CHAR_NULL) {
                        throwNullChar();
                    } else {
                        continue;
                    }
                } // else 'ent' remains null
                break;
            }
            int outLen = offset - start;
            if (outLen > 0) {
                out.write(str, start, outLen);
            }
            if (ent != null) {
                out.write(ent);
            } else if (offset < len) {
                writeAsEntity(c);
            }
        } while (++offset < len);
    }
}
