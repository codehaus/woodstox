package com.ctc.wstx.io;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Common base class for escaping Writer implementations; contains
 * commonly used constants, as well as some convenience utility
 * methods
 */
class WriterBase
    extends FilterWriter
{
    /**
     * Highest valued character that may need to be encoded (minus charset
     * encoding requirements) when writing attribute values.
     */
    protected final static char HIGHEST_ENCODABLE_ATTR_CHAR = '<';

    /**
     * Highest valued character that may need to be encoded (minus charset
     * encoding requirements) when writing attribute values.
     */
    protected final static char HIGHEST_ENCODABLE_TEXT_CHAR = '>';

    protected final static char CHAR_NULL = '\u0000';

    protected final static char CHAR_SPACE = ' ';


    protected WriterBase(Writer out) {
        super(out);
    }

    /*
    /////////////////////////////////////////////////
    // Utility methods
    /////////////////////////////////////////////////
     */

    protected final static String getQuoteEntity(char qchar)
        throws IllegalArgumentException
    {
        if (qchar == '"') {
            return "&quot;";
        }
        if (qchar == '\'') {
            return "&apos;";
        }
        throw new IllegalArgumentException("Unrecognized quote char ('"+
                                           qchar+" ["+((int) qchar)
                                           +"]; expected a single or double quote char");
    }

    protected void throwNullChar()
	throws IOException
    {
	throw new IOException("Null character in text to write");
    }

    /**
     * @return True, if the space is a 'RestrictedChar' (as per XML 1.1)
     *   and should be quoted when output as text. Note that this does NOT
     *   apply to space in attribute values, mostly due to it usually getting
     *   normalized no matter what.
     */
    protected final static boolean spaceNeedsEscaping(char c)
        throws IOException
    {
        if (c != '\r' && c != '\n' && c != '\t') {
            ;
        }
        return true;
    }

    protected final static void writeSpace(Writer w, char c)
        throws IOException
    {
        if (c != '\r' && c != '\n' && c != '\t') {
            if (c == CHAR_NULL) {
            }
            w.write(c);
        }
    }
}
