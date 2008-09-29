package com.ctc.wstx.util;

import java.util.*;

import javax.xml.stream.XMLStreamException;

/**
 * Simple helper class used for efficient decoding of character data.
 */
public final class Base64Decoder
{
    final static int INT_SPACE = 0x0020;

    /**
     * Base64 uses equality sign as padding char
     */
    final static byte CHAR_PADDING = '=';

    /**
     * Array containing 6-bit values indexed by ascii characters (for
     * valid base64 characters). Invalid entries are marked by -1.
     */
    final static int[] BASE64_BY_CHAR = new int[128];
    static {
        Arrays.fill(BASE64_BY_CHAR, -1);
        String base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        for (int i = 0; i < base64Chars.length(); ++i) {
            BASE64_BY_CHAR[base64Chars.charAt(i)] = i;
        }
    }

    // // // Input buffer information

    /**
     * Text segment being currently processed.
     */
    char[] mCurrSegment;

    int mCurrSegmentPtr;
    int mCurrSegmentEnd;

    final ArrayList mNextSegments = new ArrayList();

    /**
     * Pointer of the next segment to process (after current one stored
     * in {@link #mCurrSegment}) within {@link #mOtherSegments}.
     */
    int mNextSegmentIndex;

    // // // State regarding partial decoding

    /**
     * Flag that is set to indicate that decoder either has partially
     * processed input (1 to 3 characters from a previous buffer)
     * or decoded but not-yet-returned data (1 or 2 bytes of a triplet
     * that did not fit within the result buffer.
     */
    boolean mIncomplete = false;

    int mIncompleteOutputData = 0;

    int mIncompleteOutputLen = 0;

    /**
     * Number of characters left over from the previous input buffer;
     * maximum 3.
     */
    int mLeftoverCount;

    /**
     * Decoded partial data contained within characters left over
     * at the end of the previous input buffer.
     */
    int mLeftoverData;


    public Base64Decoder() { }

    public void init(boolean firstChunk,
                     char[] lastSegment, int offset, int len,
                     List segments)
    {
        /* Left overs only cleared if it is the first chunk (i.e.
         * right after START_ELEMENT)
         */
        if (firstChunk) {
            mLeftoverCount = 0;
        }
        mNextSegments.clear();
        if (segments == null || segments.isEmpty()) { // no segments, simple
            mCurrSegment = lastSegment;
            mCurrSegmentPtr = offset;
            mCurrSegmentEnd = offset+len;
        } else {
            Iterator it = segments.iterator();
            mCurrSegment = (char[]) it.next();
            mCurrSegmentPtr = 0;
            mCurrSegmentEnd = mCurrSegment.length;

            while (it.hasNext()) {
                mNextSegments.add(it.next());
            }
            mNextSegmentIndex = 0;
        }
        mLeftoverCount = 0;
    }

    /**
     * @param resultBuffer Buffer in which decoded bytes are returned
     * @param resultOffset Offset that points to position to put the
     *   first decoded byte in
     * @param maxLength Maximum number of bytes to decode: caller guarantees
     *   that it will be at least 3.
     *
     * @return Number of bytes decoded and returned in the result buffer
     */
    public int decode(byte[] resultBuffer, int resultOffset, int maxLength)
        throws IllegalArgumentException
    {
        final int origResultOffset = resultOffset;

        // Any incomplete input/output to handle first?
        if (mIncomplete) {
            // First: partial input from previous buffer?
            if (mLeftoverCount > 0) {
                int data = decodePartial();
                if (data < 0) { // not enough data
                    return 0;
                }
                mIncompleteOutputLen = 3;
                mIncompleteOutputData = data;
            }
            // Ok: 1 to 3 bytes we'll need to output:
            int count = flushData(resultBuffer, resultOffset, maxLength);
            maxLength -= count;
            if (maxLength < 1) { // buffer full
                return count;
            }
            resultOffset += count;
        }

        final int resultEnd = resultOffset + maxLength;

        // Any leftovers? They need to be merged separately
            maxLength -= 3;
        }

        /* We need room for triplets; hence the last valid start
         * pointer will be:
         */
        final int resultEnd = resultOffset + maxLength - 2;

        // loop for as long as there's input and we have room for a triplet
        main_loop:
        while (resultOffset < resultEnd) {
            int ch;
            do {
                if (mCurrSegmentPtr >= mCurrSegmentEnd) {
                    if (!nextSegment()) {
                        break main_loop;
                    }
                }
                ch = mCurrSegment[mCurrSegmentPtr++];
            } while (ch <= INT_SPACE);

            int data;

            if (ch > 128 || (data = BASE64_BY_CHAR[ch]) < 0) {
                throw reportInvalidChar(ch);
            }

            // Ok, still need 3 more. So here's second char we need
            if (mCurrSegmentPtr >= mCurrSegmentEnd) {
                if (!nextSegment()) {
                    markPartial(1, data);
                    break main_loop;
                }
            }
            ch = mCurrSegment[mCurrSegmentPtr++];
            int bits;
            if (ch > 128 || (bits = BASE64_BY_CHAR[ch]) < 0) {
                throw reportInvalidChar(ch);
            }
            data = (data << 6) | bits;

            // Then third
            if (mCurrSegmentPtr >= mCurrSegmentEnd) {
                if (!nextSegment()) {
                    markPartial(2, data);
                    break main_loop;
                }
            }
            ch = mCurrSegment[mCurrSegmentPtr++];
            if (ch > 128 || (bits = BASE64_BY_CHAR[ch]) < 0) {
                throw reportInvalidChar(ch);
            }
            data = (data << 6) | bits;

            // And then fourth and final
            if (mCurrSegmentPtr >= mCurrSegmentEnd) {
                if (!nextSegment()) {
                    markPartial(3, data);
                    break main_loop;
                }
            }
            ch = mCurrSegment[mCurrSegmentPtr++];
            if (ch > 128 || (bits = BASE64_BY_CHAR[ch]) < 0) {
                throw reportInvalidChar(ch);
            }
            data = (data << 6) | bits;

            resultBuffer[resultOffset++] = (byte) (data >> 16);
            resultBuffer[resultOffset++] = (byte) (data >> 8);
            resultBuffer[resultOffset++] = (byte) data;
            maxLength -= 3;
        }
        return resultOffset - origResultOffset;
    }

    /**
     *<p>
     * Note: we assume there is room for at least one byte, in the output
     * buffer
     *
     * @return Number of bytes written to output
     */
    private void flushData(byte[] resultBuffer, int resultOffset, int maxLength)
    {
        // 1 or 2 bytes buffered?
        boolean gotTwo = (mIncompleteOutputLen == 2);
        if (gotTwo) {
            resultBuffer[resultOffset++] = (byte) (mIncompleteOutputData >> 8);
            if (maxLength < 2) {
                mIncompleteOutputLen = 1;
                return 1;
            }
        }
        mIncomplete = false;
        mIncompleteOutputLen = 0;
        resultBuffer[resultOffset] = (byte) mIncompleteOutputData;
        return gotTwo ? 2 : 1;
    }

    /**
     * @return 3 data bytes (within lower 24 bits of the int) decoded,
     *   if succesful; or -1 to indicate that there is not enough
     *   data
     */
    private int decodePartial()
        throws IllegalArgumentException
    {
        do {
            if (mCurrSegmentPtr >= mCurrSegmentEnd) {
                if (!nextSegment()) {
                    return -1;
                }
            }
            int ch = mCurrSegment[mCurrSegmentPtr++];
            int bits;
            if (ch > 128 || (bits = BASE64_BY_CHAR[ch]) < 0) {
                throw reportInvalidChar(ch);
            }
            mLeftoverData = (mLeftoverData << 6) | bits;
            ++mLeftoverCount;
        } while (mLeftoverCount < 4);
        mLeftoverCount = 0;
        return mLeftoverData;
    }

    private void markPartial(int charsGotten, int data)
    {
        mLeftoverCount = charsGotten;
        mLeftoverData = data;
    }

    /**
     * @return True if there was another input segment to use
     */
    private boolean nextSegment()
    {
        if (mNextSegmentIndex < mNextSegments.size()) {
            mCurrSegment = (char[]) mNextSegments.get(mNextSegmentIndex++);
            mCurrSegmentPtr = 0;
            mCurrSegmentEnd = mCurrSegment.length;
            return true;
        }
        return false;
    }

    private IllegalArgumentException reportInvalidChar(int ch)
        throws IllegalArgumentException
    {
        if (ch <= INT_SPACE) {
            return new IllegalArgumentException("Illegal white space character (code 0x"+Integer.toHexString(ch)+") in base64 segment");
        }
        return new IllegalArgumentException("Illegal base64 character (code 0x"+Integer.toHexString(ch)+")");
    }
}

