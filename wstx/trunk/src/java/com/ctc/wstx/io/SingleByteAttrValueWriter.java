package com.ctc.wstx.io;

import java.io.*;

/**
 * Escaping writer that will properly escape characters from the attribute
 * values that need to be escaped, when outputting using "native" Unicode
 * aware Writer.
 */
public class SingleByteAttrValueWriter
    extends WriterBase
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

    public SingleByteAttrValueWriter(Writer out, String enc, char qchar)
    {
        super(out);
        mQuoteChar = qchar;
        mQuoteEntity = getQuoteEntity(qchar);
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
            char c = CHAR_NULL;
	    String ent = null;

            for (; off < len; ++off) {
                c = cbuf[off]; 
		if (c > HIGHEST_ENCODABLE_ATTR_CHAR) {
		    continue;
		}
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
		// Do we want to encode "restricted" spaces as char entities?
		//if (c < CHAR_SPACE) { }
		if (c == CHAR_NULL) {
		    throwNullChar();
		}
            }
            int outLen = off - start;
            if (outLen > 0) {
                out.write(cbuf, start, outLen);
            } 
	    if (ent != null) {
		out.write(ent);
		ent = null;
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
	    String ent = null;

            for (; off < len; ++off) {
                c = str.charAt(off);
		if (c > HIGHEST_ENCODABLE_ATTR_CHAR) {
		    continue;
		}
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
		// Do we want to encode "restricted" spaces as char entities?
		//if (c < CHAR_SPACE) { }
		if (c == CHAR_NULL) {
		    throwNullChar();
		}
            }
            int outLen = off - start;
            if (outLen > 0) {
                out.write(str, start, outLen);
            }
	    if (ent != null) {
		out.write(ent);
		ent = null;
	    }
        } while (++off < len);
    }
}

