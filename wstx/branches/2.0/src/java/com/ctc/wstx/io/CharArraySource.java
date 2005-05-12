package com.ctc.wstx.io;

import javax.xml.stream.Location;

import java.net.URL;

/**
 * Input source that reads input from a static char array, usually used
 * when expanding internal entities.
 */
public final class CharArraySource
    extends BaseInputSource
{
    /**
     * Default buffer size should never really be needed, when input
     * sources are properly chained, but let's have some sane default
     * for weird cases. 1000k chars means 2Kbyte buffer.
     */
    final static int DEFAULT_BUFFER_SIZE = 1000;

    int mOffset;

    // // // Context info we may have to save

    int mInputPtr;

    // // // Plus, location offset info:

    final Location mContentStart;
        
    CharArraySource(WstxInputSource parent, String fromEntity,
                    char[] chars, int offset, int len,
                    Location loc, URL src)
    {
        super(parent, fromEntity, loc.getPublicId(), loc.getSystemId(), src);
        mBuffer = chars;
        mOffset = offset;
        mInputLen = offset + len;
        mContentStart = loc;
    }

    public int getInputBufferLength() {
        return (mParent == null) ? DEFAULT_BUFFER_SIZE
            : mParent.getInputBufferLength();
    }

    /**
     * Unlike with reader source, we won't start from beginning of a file,
     * but usually from somewhere in the middle...
     */
    public void initInputLocation(WstxInputData reader)
    {
        reader.mCurrInputProcessed = mContentStart.getCharacterOffset();
        reader.mCurrInputRow = mContentStart.getLineNumber();
        /* 13-Apr-2005, TSa: Since column offsets reported by Location
         *   objects are 1-based, but internally we use 0-based counts,
         *   need to offset this start by 1 to begin with.
         */
        reader.mCurrInputRowStart = -mContentStart.getColumnNumber() + 1;
    }

    public int readInto(WstxInputData reader)
    {
        /* Shouldn't really try to read after closing, but it may be easier
         * for caller not to have to keep track of closure...
         */
        if (mBuffer == null) {
            return -1;
        }

        /* In general, there are only 2 states; either this has been
         * read or not. Offset is used as the marker; plus, in case
         * somehow we get a dummy char source (length of 0), it'll
         * also prevent any reading.
         */
        int len = mInputLen - mOffset;
        if (len < 1) {
            return -1;
        }
        reader.mInputBuffer = mBuffer;
        reader.mInputPtr = mOffset;
        reader.mInputLen = mInputLen;
        // Also, need to note the fact we're done
        mOffset = mInputLen;
        return len;
    }

    public boolean readMore(WstxInputData reader, int minAmount)
    {
        /* Only case where this may work is if we haven't yet been
         * read from at all. And that should mean caller also has no
         * existing input...
         */
        if (reader.mInputPtr >= reader.mInputLen) {
            int len = mInputLen - mOffset;
            if (len >= minAmount) {
                return (readInto(reader) > 0);
            }
        }
        return false;
    }

    public void close()
    {
        /* Let's help GC a bit, in case there might be back references
         * to this Onject from somewhere...
         */
        mBuffer = null;
    }

    /*
    public String toString() {
        return "[CharArraySource #"+Integer.toHexString(System.identityHashCode(this))
            +", start: "+mOffset+" < ptr: "+mInputPtr+" < last: "+mInputLen+"]";
    }
    */
}
