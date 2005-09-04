package com.ctc.wstx.io;

import java.io.*;

/**
 * Escaping writer that will properly escape normal textual content
 * that need to be escaped, when outputting using a Writer that
 * produces a subset of Unicode values.
 * When underlying Writer only allows for direct outputting of a subset of
 * Unicode values, it is generally done so that only lowest
 * Unicode characters (7-bit ones for Ascii, 8-bit ones for ISO-Latin,
 * something similar for other ISO-8859-1 encodings) can be output
 * as is, and the rest need to be output as character entities.
 */
public class SingleByteTextWriter
    extends WriterBase
{
    /**
     * First Unicode character (one with lowest value) after (and including)
     * which character entities have to be used.
     */
    private final char mLowestEntity;
    
    private boolean mJustWroteBracket = false;

    /**
     * @param out Underlying Writer to use for actual writes
     * @param enc Encoding that the Writer is using
     * @param charsetSize Number of Unicode characters (starting
     *   with the null one) that need not be escaped (for example,
     *   128 for US-ASCII, 256 for ISO-Latin etc)
     */
    public SingleByteTextWriter(Writer out, String enc,
                                int charsetSize)
    {
        super(out);
        mLowestEntity = (char) charsetSize;
    }


    public void write(int c) throws IOException
    {
        if (c <= HIGHEST_ENCODABLE_TEXT_CHAR) {
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
            } else {
                out.write(c);
            } 
            mJustWroteBracket = false;
        } else if (c >= mLowestEntity) {
            writeAsEntity(c);
        } else {
            out.write(c);
            mJustWroteBracket = (c == ']');
        }
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
        
        char c = CHAR_NULL;
        len += offset; // to get the index past last char to output
        // Need special handing for leftover ']' to cause quoting of '>'
        if (mJustWroteBracket) {
            c = cbuf[offset];
            if (c == '>') {
                out.write("&gt;");
                ++offset;
            }
        }
        
        do {
            int start = offset;
            String ent = null;
            
            for (; offset < len; ++offset) {
                c = cbuf[offset]; 
                if (c > HIGHEST_ENCODABLE_TEXT_CHAR) {
                    continue;
                }
                if (c < mLowestEntity) {
                    if (c == '<') {
                        ent = "&lt;";
                    } else if (c == '&') {
                        ent = "&amp;";
                    } else if (c == '>' && (offset > start)
                               && cbuf[offset-1] == ']') {
                        ent = "&gt;";
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
            ++offset;
            if (ent != null) {
                out.write(ent);
            } else if (offset < len) {
                writeAsEntity(c);
            }
        } while (offset < len);

        // Any leftovers?
        
        // Ok, did we end up with a bracket?
        mJustWroteBracket = (c == ']');
    }

    public void write(String str, int offset, int len) throws IOException
    {
        if (len < 2) { // let's do a simple check here
            if (len == 1) {
                write(str.charAt(offset));
            }
            return;
        }

        char c = CHAR_NULL;
        len += offset; // to get the index past last char to output
        // Ok, leftover ']' to cause quoting of '>'?
        if (mJustWroteBracket) {
            c = str.charAt(offset);
            if (c == '>') {
                out.write("&gt;");
                ++offset;
            }
        }

        do {
            int start = offset;
            String ent = null;

            for (; offset < len; ++offset) {
                c = str.charAt(offset); 
                if (c > HIGHEST_ENCODABLE_TEXT_CHAR) {
                    continue;
                }
                if (c < mLowestEntity) {
                    if (c == '<') {
                        ent = "&lt;";
                    } else if (c == '&') {
                        ent = "&amp;";
                    } else if (c == '>' && (offset > start)
                               && str.charAt(offset-1) == ']') {
                        ent = "&gt;";
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
            ++offset;
            if (ent != null) {
                out.write(ent);
            } else if (offset < len) {
                writeAsEntity(c);
            }
        } while (offset < len);

        // Ok, did we end up with a bracket?
        mJustWroteBracket = (c == ']');
    }
}
