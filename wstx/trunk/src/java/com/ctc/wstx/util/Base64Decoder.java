package com.ctc.wstx.util;

import java.util.*;

import javax.xml.stream.XMLStreamException;

/**
 * Simple helper class used for efficient decoding of character data.
 */
public final class Base64Decoder
{
    /**
     * Text segment being currently processed
     */
    char[] mCurrSegment;

    int mOffset;
    int mEnd;

    final ArrayList mOtherSegments = new ArrayList();

    public Base64Decoder() { }

    public void init(char[] lastSegment, int offset, int len,
                     List segments)
    {
        mOtherSegments.clear();
        if (segments == null || segments.isEmpty()) { // no segments, simple
            mCurrSegment = lastSegment;
            mOffset = offset;
            mEnd = offset+len;
        } else {
            Iterator it = segments.iterator();
            mCurrSegment = (char[]) it.next();
            mOffset = 0;
            mEnd = mCurrSegment.length;

            while (it.hasNext()) {
                mOtherSegments.add(it.next());
            }
        }
    }

    public int decode(byte[] resultBuffer, int offset, int maxLength)
    {
        /* We need room for triplets; hence the last valid start
         * pointer will be:
         */
        final int origStart = offset;
        final int lastOffset = offset + maxLength - 2;
        // !!! TBI
        return -1;
    }
}

