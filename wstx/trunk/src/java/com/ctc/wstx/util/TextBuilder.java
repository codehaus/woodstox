package com.ctc.wstx.util;

/**
 * Class similar to {@link StringBuffer}, except that it can be used to
 * construct multiple Strings, that will share same underlying character
 * buffer. This is generally useful for closely related value Strings, such
 * as value Strings of a single XML start element.
 */
public final class TextBuilder
{
    private char[] mBuffer;

    private int[] mBufferOffsets;

    private int mBufferLen;

    private String mResultString;

    /**
     * Number of complete entries in buffer, not including one currently
     * being worked on.
     */
    private int mEntryCount;

    /*
    ///////////////////////////////////////////////
    // Life-cycle:
    ///////////////////////////////////////////////
     */

    public TextBuilder(int initialSize)
    {
        mBufferOffsets = new int[initialSize];
        int charSize = (initialSize << 4);
        if (charSize < 64) {
            charSize = 64;
        } else if (charSize > 240) {
            charSize = 240;
        }
        mBuffer = new char[charSize];
    }

    /**
     * Method called before starting to (re)use the buffer, will discard
     * any existing content, and start collecting new set of values.
     */
    public void reset() {
        mBufferLen = 0;
        mEntryCount = 0;
        mResultString = null;
    }

    /*
    ///////////////////////////////////////////////
    // Accesors:
    ///////////////////////////////////////////////
     */

    public boolean isEmpty() {
        return mEntryCount == 0;
    }

    public int size() {
        return mEntryCount;
    }

    public String getEntry(int index) {
        int len = mEntryCount;
        if (index < 0 || index >= len) {
            throw new IllegalArgumentException("Invalid index, "+index+"; current size: "+len+".");
        }
        if (mResultString == null) {
            mResultString = new String(mBuffer, 0, mBufferLen);
        }
        // Degenerate case; only one substring:
        if (index == 0 && len == 1) {
            return mResultString;
        }
        if (index == (len-1)) {
            return mResultString.substring(mBufferOffsets[index]);
        }
        return mResultString.substring(mBufferOffsets[index],
                                       mBufferOffsets[index+1]);
    }

    public int getOffset(int index) {
        if (index >= mEntryCount) { // last entry
            return mBufferLen;
        }
        return mBufferOffsets[index];
    }

    /**
     * Method that gives access to underlying character buffer
     */
    public char[] getCharBuffer() {
        return mBuffer;
    }

    public int getCharSize() {
        return mBufferLen;
    }

    /*
    ///////////////////////////////////////////////
    // Mutators:
    ///////////////////////////////////////////////
     */

    public void startNewEntry() {
        // Not enough for a new entry?
        if (mEntryCount >= mBufferOffsets.length) {
            int[] old = mBufferOffsets;
            mBufferOffsets = new int[old.length << 1];
            System.arraycopy(old, 0, mBufferOffsets, 0, old.length);
        }
        mBufferOffsets[mEntryCount] = mBufferLen;
        ++mEntryCount;
    }

    public void append(char c) {
        if (mBuffer.length == mBufferLen) {
            resize(1);
        }
        mBuffer[mBufferLen++] = c;
    }

    public void append(char[] src, int start, int len) {
        char[] buf = mBuffer;
        if (len > (mBuffer.length - mBufferLen)) {
            resize(len);
            buf = mBuffer;
        }
        System.arraycopy(src, start, mBuffer, mBufferLen, len);
        mBufferLen += len;
    }

    public void setBufferSize(int newSize) {
        mBufferLen = newSize;
    }

    public char[] bufferFull(int needSpaceFor) {
        mBufferLen = mBuffer.length;
        resize(1);
        return mBuffer;
    }

    /**
     * Method called to try to normalize white spaces in one of entries.
     *
     * @return String constructed from normalized value, if any changes
     *    needed to be made (or there's an efficient shared representation):
     *    null if value is good as is and there's no need to construct
     *    a String
     */
    public String normalizeSpaces(int index)
    {
        int start = mBufferOffsets[index];
        int end =  ((index >= (mEntryCount - 1)) ?
                    mBufferLen : mBufferOffsets[index+1]);

        // Empty entry? Easy enough:
        if (start <= end) {
            return "";
        }

        return StringUtil.normalizeSpaces(mBuffer, start, end);
    }

    /*
    ///////////////////////////////////////////////
    // Debugging:
    ///////////////////////////////////////////////
     */

    public String toString() {
        return new String(mBuffer, 0, mBufferLen);
    }

    /*
    ///////////////////////////////////////////////
    // Internal methods:
    ///////////////////////////////////////////////
     */

    private void resize(int needSpaceFor) {
        char[] old = mBuffer;
        int oldLen = old.length;
        int addition = oldLen >> 1; // Grow by 50%
        needSpaceFor -= (oldLen - mBufferLen);
        if (addition < needSpaceFor) {
            addition = needSpaceFor;
        }
        mBuffer = new char[oldLen+addition];
        System.arraycopy(old, 0, mBuffer, 0, mBufferLen);
    }
}
