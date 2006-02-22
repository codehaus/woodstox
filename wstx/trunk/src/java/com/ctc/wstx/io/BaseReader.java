package com.ctc.wstx.io;

import java.io.*;

/**
 * Simple basic class for optimized Readers Woodstox has; implements
 * "cookie-cutter" methods that are used by all actual implementations.
 */
abstract class BaseReader
    extends Reader
{
    protected final static char NULL_CHAR = (char) 0;
    protected final static char NULL_BYTE = (byte) 0;

    /**
     * In xml 1.1, NEL (0x85) behaves much the way \n does (can
     * be follow \r as part of the linefeed
     */
    protected final static char CONVERT_NEL_TO = '\n';

    /**
     * In xml 1.1, LSEP bit like \n, or \r. Need to choose one as the
     * result. Let's use \n, for simplicity
     */
    protected final static char CONVERT_LSEP_TO = '\n';

    /**
     * DEL character is both the last ascii char, and illegal in xml 1.1.
     */
    final static char CHAR_DEL = (char) 127;

    protected InputStream mIn;

    protected byte[] mBuffer;

    protected int mPtr;
    protected int mLength;

    /*
    ////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////
    */

    protected BaseReader(InputStream in, byte[] buf, int ptr, int len)
    {
        mIn = in;
        mBuffer = buf;
        mPtr = ptr;
        mLength = len;
    }

    /*
    ////////////////////////////////////////
    // Configuration
    ////////////////////////////////////////
    */

    /**
     * Method that can be called to indicate the xml conformance used
     * when reading content using this reader. Some of the character
     * validity checks need to be done at reader level, and sometimes
     * they depend on xml level (for example, xml 1.1 has new linefeeds
     * and both more and less restricted characters).
     */
    public abstract void setXmlCompliancy(int xmlVersion);

    /*
    ////////////////////////////////////////
    // Reader API
    ////////////////////////////////////////
    */

    public void close()
        throws IOException
    {
        InputStream in = mIn;

        if (in != null) {
            mIn = null;
            mBuffer = null;
            in.close();
        }
    }

    char[] mTmpBuf = null;

    /**
     * Although this method is implemented by the base class, AND it should
     * never be called by Woodstox code, let's still implement it bit more
     * efficiently just in case
     */
    public int read()
        throws IOException
    {
        if (mTmpBuf == null) {
            mTmpBuf = new char[1];
        }
        if (read(mTmpBuf, 0, 1) < 1) {
            return -1;
        }
        return mTmpBuf[0];
    }

    /*
    ////////////////////////////////////////
    // Internal/package methods:
    ////////////////////////////////////////
    */

    protected void reportBounds(char[] cbuf, int start, int len)
        throws IOException
    {
        throw new ArrayIndexOutOfBoundsException("read(buf,"+start+","+len+"), cbuf["+cbuf.length+"]");
    }

    protected void reportStrangeStream()
        throws IOException
    {
        throw new IOException("Strange I/O stream, returned 0 bytes on read");
    }

    protected void reportInvalidXml11(int value, int bytePos, int charPos)
        throws IOException
    {
        throw new CharConversionException("Invalid character 0x"
                                          +Integer.toHexString(value)
                                          +", can only be included in xml 1.1 using character entities (at char #"+charPos+", byte #"+bytePos+")");
    }
}

