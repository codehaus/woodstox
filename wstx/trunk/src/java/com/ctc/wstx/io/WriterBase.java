package com.ctc.wstx.io;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Common base class for escaping Writer implementations; contains
 * commonly used constants, as well as some convenience utility
 * methods.
 *<p>
 * Note: the class is only public for testing purposes.
 */
public class WriterBase
    extends FilterWriter
{
    // // // Constants:

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

    // // // Working space:

    /**
     * Temporary char buffer used to assemble character entities. Big
     * enough to contain full hex-encoded 16-bit char entities (like
     * '&amp;#x1234;')
     */
    protected char[] mEntityBuffer = null;

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

    protected final void writeAsEntity(int c)
        throws IOException
    {
        char[] cbuf = mEntityBuffer;
        if (cbuf == null) {
            cbuf = new char[8];
            mEntityBuffer = cbuf;
            cbuf[0] = '&';
            cbuf[1] = '#';
            cbuf[2] = 'x';
            cbuf[7] = ';';
        }
        for (int ix = 6; ix > 2; --ix) {
            int digit = (c & 0xF);
            c >>= 4;
            cbuf[ix] = (char) ((digit < 10) ?
                               ('0' + digit) :
                               (('a' - 10) + digit));
        }
        out.write(cbuf, 0, 8);
    }
}
