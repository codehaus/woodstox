package com.ctc.wstx.stax.io;

import java.io.*;

/**
 * Simple {@link Reader} implementation that is used to "unwind" some
 * data previously read from a Reader; so that as long as some of
 * that data remains, it's returned; but as long as it's read, we'll
 * just use data from the underlying original Reader.
 * This is similar to {@link java.io.PushbackReader}, but with this class
 * there's only one implicit pushback, when instance is constructed; not
 * general pushback buffer and methods to use it.
 */
public final class MergedReader
    extends Reader
{
    final Reader mIn;

    char[] mData;

    int mPtr;

    final int mEnd;

    public MergedReader(Reader in, char[] buf, int start, int end)
    {
        mIn = in;
        mData = buf;
        mPtr = start;
        mEnd = end;
    }

    public void close()
        throws IOException
    {
        mData = null;
        mIn.close();
    }

    public void mark(int readlimit)
        throws IOException
    {
        if (mData == null) {
            mIn.mark(readlimit);
        }
    }
    
    public boolean markSupported()
    {
        /* Only supports marks past the initial rewindable section...
         */
        return (mData == null) && mIn.markSupported();
    }
    
    public int read()
        throws IOException
    {
        if (mData != null) {
            int c = mData[mPtr++] & 0xFF;
            if (mPtr >= mEnd) {
                mData = null;
            }
            return c;
        }
        return mIn.read();
    }
    
    public int read(char[] cbuf)
        throws IOException
    {
        return read(cbuf, 0, cbuf.length);
    }

    public int 	read(char[] cbuf, int off, int len)
        throws IOException
    {
        if (mData != null) {
            int avail = mEnd - mPtr;
            if (len > avail) {
                len = avail;
            }
            System.arraycopy(mData, mPtr, cbuf, off, len);
            mPtr += len;
            if (mPtr >= mEnd) {
                mData = null;
            }
            return len;
        }

        return mIn.read(cbuf, off, len);
    }

    public boolean ready()
        throws IOException
    {
        return (mData != null) || mIn.ready();
    }

    public void reset()
        throws IOException
    {
        if (mData == null) {
            mIn.reset();
        }
    }

    public long skip(long n)
        throws IOException
    {
        long count = 0L;

        if (mData != null) {
            int amount = mEnd - mPtr;

            if (amount > n) { // all in pushed back segment?
                mPtr += (int) n;
                return amount;
            }
            mData = null;
            count += amount;
            n -= amount;
        }

        if (n > 0) {
            count += mIn.skip(n);
        }
        return count;
    }

}
