/* Woodstox XML processor
 *
 * Copyright (c) 2004 Tatu Saloranta, tatu.saloranta@iki.fi
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

package com.ctc.wstx.io;

import java.io.*;

/**
 * Optimized Reader that reads UTF-8 encoded content from an input stream.
 * In addition to doing (hopefully) optimal conversion, it can also take
 * array of "pre-read" (leftover) bytes; this is necessary when preliminary
 * stream/reader is trying to figure out XML encoding.
 */
public final class UTF8Reader
    extends BaseReader
{
    final static char NULL_CHAR = (char) 0;
    final static char NULL_BYTE = (byte) 0;

    char mSurrogate = NULL_CHAR;

    /**
     * Total read character count; used for error reporting purposes
     */
    int mCharCount = 0;

    /**
     * Total read byte count; used for error reporting purposes
     */
    int mByteCount = 0;

    /*
    ////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////
    */

    public UTF8Reader(InputStream in, byte[] buf, int ptr, int len)
    {
        super(in, buf, ptr, len);
    }

    /*
    ////////////////////////////////////////
    // Public API
    ////////////////////////////////////////
    */

    public int read(char[] cbuf, int start, int len)
        throws IOException
    {
        // Already EOF?
        if (mBuffer == null) {
            return -1;
        }
        if (len < 1) {
            return len;
        }
        // Let's then ensure there's enough room...
        if (start < 0 || (start+len) > cbuf.length) {
            reportBounds(cbuf, start, len);
        }

        len += start;
        int outPtr = start;

        // Ok, first; do we have a surrogate from last round?
        if (mSurrogate != NULL_CHAR) {
            cbuf[outPtr++] = mSurrogate;
            mSurrogate = NULL_CHAR;
        }

        main_loop:
        while (outPtr < len) {
            if (mPtr >= mLength) {
                if (!loadMore()) { // EOF is ok here
                    break main_loop;
                }
            }

            byte b = mBuffer[mPtr++];

            /* Let's first do the quickie loop for common case; 7-bit
             * ascii:
             */
            //while (b >= NULL_BYTE) { // still 7-bit?
            while (b >= 0) { // still 7-bit?
                cbuf[outPtr] = (char) b; // ok since MSB is never on
                if (++outPtr >= len) {
                    break main_loop;
                }
                if (mPtr >= mLength) {
                    continue main_loop;
                }
                b = mBuffer[mPtr++];
            }

            int c = (int) b; // it's ok to get sign extension
            int needed;

            // Ok; if we end here, we got multi-byte combination
            if ((c & 0xE0) == 0xC0) { // 2 bytes (0x0080 - 0x07FF)
                c = (b & 0x1F);
                needed = 1;
            } else if ((c & 0xF0) == 0xE0) { // 3 bytes (0x0800 - 0xFFFF)
                c = (b & 0x0F);
                needed = 2;
            } else if ((c & 0xF8) == 0xF0) {
                // 4 bytes; double-char BS, with surrogates and all...
                c = (b & 0x0F);
                needed = 3;
            } else {
                reportInvalidInitial(c & 0xFF, outPtr-start);
                // never gets here...
                needed = 1;
            }

            /* Ok, so far so good; need to ensure we have enough stuff in
             * the buffer, and then check that bit patterns look good. Plus,
             * for the weird-ass 4 byte case, need to do more too...
             */
            if ((mLength - mPtr) < needed) {
                loadMore(needed);
            }
            int d = mBuffer[mPtr++];
            if ((d & 0xC0) != 0x080) {
                reportInvalidOther(d & 0xFF, outPtr-start);
            }
            c = (c << 6) | (d & 0x3F);

            if (needed > 1) {
                d = mBuffer[mPtr++];
                if ((d & 0xC0) != 0x080) {
                    reportInvalidOther(d & 0xFF, outPtr-start);
                }
                c = (c << 6) | (d & 0x3F);
                if (needed > 2) {
                    d = mBuffer[mPtr++];
                    if ((d & 0xC0) != 0x080) {
                        reportInvalidOther(d & 0xFF, outPtr-start);
                    }
                    c = (c << 6) | (d & 0x3F);
                    if (needed > 3) { // weird-ass case!
                        d = mBuffer[mPtr++];
                        if ((d & 0xC0) != 0x080) {
                            reportInvalidOther(d & 0xFF, outPtr-start);
                        }
                        c = (c << 6) | (d & 0x3F);
                        /* Ugh. Need to mess with surrogates. Ok; let's inline them
                         * there, then, if there's room: if only room for one,
                         * need to save the surrogate for rainy day...
                         */
                        c -= 0x10000; // to normalize it starting with 0x0
                        cbuf[outPtr++] = (char) (0xD800 + (c >> 10));
                        // hmmh. can this ever be 0? (not legal, at least?)
                        c = (0xDC00 | (c & 0x03FF));
                        // Room for second part?
                        if (outPtr >= len) { // nope
                            mSurrogate = (char) c;
                            break main_loop;
                        }
                        // sure, let's fall back to normal processing:
                    }
                }
            }

            cbuf[outPtr++] = (char) c;
        }

        len = outPtr - start;
        mCharCount += len;
        return (len == 0) ? -1 : len; // to signal EOF
    }

    /*
    ////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////
    */

    private void reportInvalidInitial(int mask, int offset)
        throws IOException
    {
        // input (byte) ptr has been advanced by one, by now:
        int bytePos = mByteCount + mPtr - 1;
        int charPos = mCharCount + offset + 1;

        throw new CharConversionException("Invalid UTF-8 start byte 0x"
                                          +Integer.toHexString(mask)
                                          +" (at char #"+charPos+", byte #"+bytePos+")");
    }

    private void reportInvalidOther(int mask, int offset)
        throws IOException
    {
        int bytePos = mByteCount + mPtr - 1;
        int charPos = mCharCount + offset;

        throw new CharConversionException("Invalid UTF-8 middle byte 0x"
                                          +Integer.toHexString(mask)
                                          +" (at char #"+charPos+", byte #"+bytePos+")");
    }

    private boolean loadMore()
        throws IOException
    {
        mByteCount += mLength;
        int count = mIn.read(mBuffer);
        if (count <= 0) {
            /* Let's actually then free the buffer right away; shouldn't
             * yet close the underlying stream though?
             */
            mBuffer = null;
            if (count == 0) {
                reportStrangeStream();
            }
            return false;
        }
        mLength = count;
        mPtr = 0;
        return true;
    }

    private void loadMore(int needed)
        throws IOException
    {
        // let's first move existing bytes back
        for (int i = mPtr; i < mLength; ++i) {
            mBuffer[i - mPtr] = mBuffer[i];
        }
        mLength = mLength - mPtr;
        mPtr = 0;

        // and then load more:
        while (mLength < needed) {
            int count = mIn.read(mBuffer, mLength, mBuffer.length - mLength);
            if (count < 1) {
                /* Let's actually then free the buffer right away; shouldn't
                 * yet close the underlying stream though?
                 */
                mBuffer = null;
                if (count == 0) {
                    reportStrangeStream();
                }
                throw new IOException("Unexpected EOF in the middle of multi-byte UTF-8 character");
            }
            mLength += count;
        }
    }
}

