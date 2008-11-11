/* StAX2 extension for StAX API (JSR-173).
 *
 * Copyright (c) 2005- Tatu Saloranta, tatu.saloranta@iki.fi
 *
 * Licensed under the License specified in file LICENSE, included with
 * the source code.
 * You may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.stax2.ri.typed;

import java.util.*;

/**
 * Base64 decoder that can be used to decode base64 encoded content that
 * is passed as char arrays.
 */
public final class CharArrayBase64Decoder
    extends Base64DecoderBase
{
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

    public CharArrayBase64Decoder() { super(); }

    public void init(boolean firstChunk,
                     char[] lastSegment, int offset, int len,
                     List segments)
    {
        /* Left overs only cleared if it is the first chunk (i.e.
         * right after START_ELEMENT)
         */
        if (firstChunk) {
            mState = STATE_INITIAL;
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
        final int resultBufferEnd = resultOffset + maxLength;

        main_loop:
        while (true) {
            switch (mState) {
            case STATE_INITIAL:
                // first, we'll skip preceding white space, if any
                {
                    char ch;
                    do {
                        if (mCurrSegmentPtr >= mCurrSegmentEnd) {
                            if (!nextSegment()) {
                                break main_loop;
                            }
                        }
                        ch = mCurrSegment[mCurrSegmentPtr++];
                    } while (ch <= INT_SPACE);
                    if (ch > 127 || (mDecodedData = BASE64_BY_CHAR[ch]) < 0) {
                        throw reportInvalidChar(ch, 0);
                    }
                }
                // fall through, "fast" path

            case STATE_VALID_1:
                // then second base64 char; can't get padding yet, nor ws
                {
                    if (mCurrSegmentPtr >= mCurrSegmentEnd) {
                        if (!nextSegment()) {
                            mState = STATE_VALID_1; // to cover fall-through case
                            break main_loop;
                        }
                    }
                    char ch = mCurrSegment[mCurrSegmentPtr++];
                    int bits;
                    if (ch > 127 || (bits = BASE64_BY_CHAR[ch]) < 0) {
                        throw reportInvalidChar(ch, 1);
                    }
                    mDecodedData = (mDecodedData << 6) | bits;
                }
                // fall through, "fast path"

            case STATE_VALID_2:
                // third base64 char; can be padding, but not ws
                {
                    if (mCurrSegmentPtr >= mCurrSegmentEnd) {
                        if (!nextSegment()) {
                            mState = STATE_VALID_2; // to cover fall-through case
                            break main_loop;
                        }
                    }
                    char ch = mCurrSegment[mCurrSegmentPtr++];
                    int bits;
                    if (ch > 127 || (bits = BASE64_BY_CHAR[ch]) < 0) {
                        if (ch != CHAR_PADDING) {
                            throw reportInvalidChar(ch, 2);
                        }
                        // Padding is off the "fast path", so:
                        mState = STATE_VALID_2_AND_PADDING;
                        continue main_loop;
                    }
                    mDecodedData = (mDecodedData << 6) | bits;
                }
                // fall through, "fast path"

            case STATE_VALID_3:
                // fourth and last base64 char; can be padding, but not ws
                {
                    if (mCurrSegmentPtr >= mCurrSegmentEnd) {
                        if (!nextSegment()) {
                            mState = STATE_VALID_3; // to cover fall-through case
                            break main_loop;
                        }
                    }
                    char ch = mCurrSegment[mCurrSegmentPtr++];
                    int bits;
                    if (ch > 127 || (bits = BASE64_BY_CHAR[ch]) < 0) {
                        if (ch != CHAR_PADDING) {
                            throw reportInvalidChar(ch, 3);
                        }
                        /* With padding we only get 2 bytes; but we have
                         * to shift it a bit so it is identical to triplet
                         * case with partial output.
                         * 3 chars gives 3x6 == 18 bits, of which 2 are
                         * dummies, need to discard:
                         */
                        mDecodedData >>= 2;
                        mState = STATE_OUTPUT_2;
                        continue main_loop;
                    }
                    // otherwise, our triple is now complete
                    mDecodedData = (mDecodedData << 6) | bits;
                }
                // still along fast path

            case STATE_OUTPUT_3:
                if (resultOffset >= resultBufferEnd) { // no room
                    mState = STATE_OUTPUT_3;
                    break main_loop;
                }
                resultBuffer[resultOffset++] = (byte) (mDecodedData >> 16);
                // fall through

            case STATE_OUTPUT_2:
                if (resultOffset >= resultBufferEnd) { // no room
                    mState = STATE_OUTPUT_2;
                    break main_loop;
                }
                resultBuffer[resultOffset++] = (byte) (mDecodedData >> 8);
                // fall through

            case STATE_OUTPUT_1:
                if (resultOffset >= resultBufferEnd) { // no room
                    mState = STATE_OUTPUT_1;
                    break main_loop;
                }
                resultBuffer[resultOffset++] = (byte) mDecodedData;
                mState = STATE_INITIAL;
                continue main_loop;

            case STATE_VALID_2_AND_PADDING:
                {
                    if (mCurrSegmentPtr >= mCurrSegmentEnd) {
                        if (!nextSegment()) {
                            // must have valid state already (can't get in via fall-through)
                            break main_loop;
                        }
                    }
                    char ch = mCurrSegment[mCurrSegmentPtr++];
                    if (ch != CHAR_PADDING) {
                        throw reportInvalidChar(ch, 3, "expected padding character '='");
                    }
                    // Got 12 bits, only need 8, need to shift
                    mState = STATE_OUTPUT_1;
                    mDecodedData >>= 4;
                }
                continue main_loop;

            default:
                // sanity check: should never happen
                throw new IllegalStateException("Illegal internal state "+mState);
            }
        }
        return resultOffset - origResultOffset;
    }

    /*
    //////////////////////////////////////////////////////////////
    // Internal helper methods for input access:
    //////////////////////////////////////////////////////////////
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
}

