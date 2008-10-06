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
    final static char CHAR_PADDING = '=';

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

    /**
     * Flag that indicates that a padding character is expected
     * after left over characters already seen (of which there must
     * be exactly 3, and hence just one byte to output)
     */
    boolean mLeftoverPadding;

    public Base64Decoder() { }

    public void init(boolean firstChunk,
                     char[] lastSegment, int offset, int len,
                     List segments)
    {
        int size = (segments == null) ? 0 : segments.size();

        /* Left overs only cleared if it is the first chunk (i.e.
         * right after START_ELEMENT)
         */
        if (firstChunk) {
            mLeftoverCount = 0;
            mIncompleteOutputLen = 0;
            mIncomplete = false;
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
    }

    /**
     * @param resultBuffer Buffer in which decoded bytes are returned
     * @param resultOffset Offset that points to position to put the
     *   first decoded byte in maxLength Maximum number of bytes that can be returned
     *   in given buffer
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
                if (!decodePartial()) { // not enough data
                    return 0;
                }
                // otherwise we'll have incomplete output data now
            }
            // Ok: 1 to 3 bytes we'll need to output:
            int count = flushData(resultBuffer, resultOffset, maxLength);
            maxLength -= count;
            if (maxLength < 1) { // buffer full
                return count;
            }
            resultOffset += count;
        }

        // Ok: now we can work with one triplet at a time
        final int resultFullEnd = resultOffset + maxLength - 3;

        // loop for as long as there's input and we have room for a triplet
        main_loop:
        while (true) {
            int ch;
            // first, we'll skip preceding white space, if any
            do {
                if (mCurrSegmentPtr >= mCurrSegmentEnd) {
                    if (!nextSegment()) {
                        break main_loop;
                    }
                }
                ch = mCurrSegment[mCurrSegmentPtr++];
            } while (ch <= INT_SPACE);

            int data;

            if (ch > 127 || (data = BASE64_BY_CHAR[ch]) < 0) {
                throw reportInvalidChar(ch);
            }
            // Ok, still need 3 more. So here's second char we need
            if (mCurrSegmentPtr >= mCurrSegmentEnd) {
                if (!nextSegment()) {
                    markPartialInput(1, data, false);
                    break main_loop;
                }
            }
            ch = mCurrSegment[mCurrSegmentPtr++];
            int bits;
            if (ch > 127 || (bits = BASE64_BY_CHAR[ch]) < 0) {
                throw reportInvalidChar(ch);
            }
            data = (data << 6) | bits;

            // Then third, can be '=' at the end
            if (mCurrSegmentPtr >= mCurrSegmentEnd) {
                if (!nextSegment()) {
                    markPartialInput(2, data, false);
                    break main_loop;
                }
            }
            ch = mCurrSegment[mCurrSegmentPtr++];
            if (ch > 127 || (bits = BASE64_BY_CHAR[ch]) < 0) {
                if (ch == CHAR_PADDING) {
                    // can we decode tripled and output results?
                    if (decodePadded1(resultBuffer, resultOffset, resultFullEnd+3, data)) {
                        // if yes, can continue
                        ++resultOffset;
                        continue main_loop;
                    }
                    // if not, need to bail out
                    break main_loop;
                }
                throw reportInvalidChar(ch);
            }
            data = (data << 6) | bits;

            // And then fourth and final
            if (mCurrSegmentPtr >= mCurrSegmentEnd) {
                if (!nextSegment()) {
                    markPartialInput(3, data, false);
                    break main_loop;
                }
            }
            ch = mCurrSegment[mCurrSegmentPtr++];
            if (ch > 127 || (bits = BASE64_BY_CHAR[ch]) < 0) {
                if (ch == CHAR_PADDING) {
                    // 2 bytes already in, but need to re-align
                    data >>= 2; // 3x6 bits == 18, 2 zeroes at the end
                    int end = resultFullEnd + 3;
                    if (resultOffset < end) {
                        resultBuffer[resultOffset++] = (byte) (data >> 8);
                        if (resultOffset < end) {
                            resultBuffer[resultOffset++] = (byte) data;
                            continue main_loop;
                        } else {
                            markPartialOutput(1, data);
                        }
                    } else {
                        markPartialOutput(2, data);
                    }
                    break main_loop;
                }
                throw reportInvalidChar(ch);
            }
            data = (data << 6) | bits;

            // Room in output buffer?
            if (resultOffset <= resultFullEnd) { // yes, simple
                resultBuffer[resultOffset++] = (byte) (data >> 16);
                resultBuffer[resultOffset++] = (byte) (data >> 8);
                resultBuffer[resultOffset++] = (byte) data;
                continue;
            }

            // Nope: need to do partial output
            int roomFor = (resultFullEnd+3) - resultOffset;

            if (roomFor > 0) {
                resultBuffer[resultOffset++] = (byte) (data >> 16);
                if (roomFor > 1) {
                    resultBuffer[resultOffset++] = (byte) (data >> 8);
                }
            }
            markPartialOutput(3 - roomFor, data);
            break main_loop;
        }
        return resultOffset - origResultOffset;
    }

    /**
     * Method used to decode 'partial' base64 triplet, which contains
     * information for just one byte. This is the case when there are
     * 2 base64 data characters followed by 2 padding characters.
     *
     * @return True if decoding and outputting succeeded; false if at
     *   least one failed due to lacking input or room for output.
     */
    public boolean decodePadded1(byte[] resultBuffer, int resultOffset, int resultEnd,
                             int data)
        throws IllegalArgumentException
    {
        if (mCurrSegmentPtr >= mCurrSegmentEnd) {
            if (!nextSegment()) {
                markPartialInput(3, data, true);
                return false;
            }
        }
        char ch = mCurrSegment[mCurrSegmentPtr++];
        if (ch != CHAR_PADDING) {
            throw reportInvalidChar(ch, "expected padding character '='");
        }
        // Ok, just a single byte to output; but it needs re-alignment
        data >>= 4; // need to unwind last 4 zero bits
        if (resultOffset >= resultEnd) {
            markPartialOutput(1, data);
            return false;
        }
        resultBuffer[resultOffset] = (byte) data;
        return true;
    }

    /**
     * Method that can be called to check whether this decoder has
     * used up all available input data or not.
     *
     * @return True if this decoder has no more input it can use
     *   for decoding.
     */
    public boolean isEmpty()
    {
        return (mNextSegmentIndex >= mNextSegments.size());
    }

    /**
     *<p>
     * Note: we assume that the output buffer has room for at least one byte.
     *
     * @return Number of bytes written to output
     */
    private int flushData(byte[] resultBuffer, int resultOffset, int maxLength)
    {
        // Can have 1, 2 or 3 bytes to output
        int origOffset = resultOffset;
        if (--mIncompleteOutputLen > 0) {
            if (mIncompleteOutputLen > 1) { // 3 bytes
                resultBuffer[resultOffset++] = (byte) (mIncompleteOutputData >> 16);
                if (--maxLength < 1) {
                    return 1;
                }
                --mIncompleteOutputLen;
            }
            resultBuffer[resultOffset++] = (byte) (mIncompleteOutputData >> 8);
            if (--maxLength < 1) {
                return (resultOffset - origOffset);
            }
            --mIncompleteOutputLen;
        }
        resultBuffer[resultOffset++] = (byte) mIncompleteOutputData;
        mIncomplete = false;
        mIncompleteOutputLen = 0;
        return (resultOffset - origOffset);
    }

    /**
     * @return True if we managed to decode a full triplet; false if
     *    there wasn't enough data for decoding
     */
    private boolean decodePartial()
        throws IllegalArgumentException
    {
        // !!! TBI: ensure padding

        do {
            while (mCurrSegmentPtr >= mCurrSegmentEnd) {
                if (!nextSegment()) {
                    return false;
                }
            }
            int ch = mCurrSegment[mCurrSegmentPtr++];
            int bits;
            if (ch > 127 || (bits = BASE64_BY_CHAR[ch]) < 0) {
                throw reportInvalidChar(ch);
            }
            mLeftoverData = (mLeftoverData << 6) | bits;
            ++mLeftoverCount;
        } while (mLeftoverCount < 4);
        // Let's temporarily buffer output, too; will usually be written right away
        mIncompleteOutputLen = 3;
        mIncompleteOutputData = mLeftoverData;
        mLeftoverCount = 0;
        return true;
    }

    private void markPartialInput(int charsGotten, int data, boolean needPadding)
    {
        mLeftoverCount = charsGotten;
        mLeftoverData = data;
        mIncomplete = true;
        mLeftoverPadding = needPadding;
    }

    private void markPartialOutput(int bytesBuffered, int data)
    {
        mIncompleteOutputLen = bytesBuffered;
        mIncompleteOutputData = data;
        mIncomplete = true;
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
        return reportInvalidChar(ch, null);
    }

    private IllegalArgumentException reportInvalidChar(int ch, String msg)
        throws IllegalArgumentException
    {
        String base;
        if (ch <= INT_SPACE) {
             base = "Illegal white space character (code 0x"+Integer.toHexString(ch)+") in base64 segment";
        } else {
            base = "Illegal base64 character (code 0x"+Integer.toHexString(ch)+")";
        }
        if (msg != null) {
            base = base + ": " + msg;
        }
        return new IllegalArgumentException(base);
    }
}

