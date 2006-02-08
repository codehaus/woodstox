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
}


