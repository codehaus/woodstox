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
            // No need to load more, already got one char
        } else {
            /* 04-Dec-2004, TSa: Ok; now, a change was needed so that we
             *   would only try to read more stuff from the input stream
             *   when we absolutely have to. If not, we might be blocking
             *   "too early", possibly without higher-level functionality
             *   having had a chance to parse and return something that
             *   could be parsed from what was already decoded. To change
             *   this, need to only load things before looping. This will
             *   also slightly simplify loop itself, for multi-byte chars.
             */

            int left = (mLength - mPtr);

            /* So; only need to load more if we can't provide at least
             * one more character. We need not do thorough check here,
             * but let's check the common cases here: either completely
             * empty buffer (left == 0), or one with less than max. byte
             * count for a single char, and starting of a multi-byte
             * encoding (this leaves possibility of a 2/3-byte char
             * that is still fully accessible... but that can be checked
             * by the load method)
             */

            if (left < 4) {
                // Need to load more?
                if (left < 1 || mBuffer[mPtr] < 0) {
                    if (!loadMore(left)) { // (legal) EOF?
                        return -1;
                    }
                }
            }
        }

        byte[] buf = mBuffer;

        main_loop:
        while (outPtr < len) {
            // At this point we have at least one byte available
            byte b = buf[mPtr++];

            /* Let's first do the quickie loop for common case; 7-bit
             * ascii:
             */
            //while (b >= NULL_BYTE) { // still 7-bit?
            while (b >= 0) { // still 7-bit?
                cbuf[outPtr] = (char) b; // ok since MSB is never on
                if (++outPtr >= len) { // output buffer full, let's leave
                    break main_loop;
                }
                if (mPtr >= mLength) { // out of input, let's quit as well
                    break main_loop;
                }
                b = buf[mPtr++];
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

            /* Do we have enough bytes? If not, let's just push back the
             * byte and leave, since we have already gotten at least one
             * char decoded. This way we will only block (with read from
             * input stream) when absolutely necessary.
             */
            if ((mLength - mPtr) < needed) {
                --mPtr;
                break main_loop;
            }

            int d = buf[mPtr++];
            if ((d & 0xC0) != 0x080) {
                reportInvalidOther(d & 0xFF, outPtr-start);
            }
            c = (c << 6) | (d & 0x3F);

            if (needed > 1) {
                d = buf[mPtr++];
                if ((d & 0xC0) != 0x080) {
                    reportInvalidOther(d & 0xFF, outPtr-start);
                }
                c = (c << 6) | (d & 0x3F);
                if (needed > 2) {
                    d = buf[mPtr++];
                    if ((d & 0xC0) != 0x080) {
                        reportInvalidOther(d & 0xFF, outPtr-start);
                    }
                    c = (c << 6) | (d & 0x3F);
                    if (needed > 3) { // weird case! (surrogates)
                        d = buf[mPtr++];
                        if ((d & 0xC0) != 0x080) {
                            reportInvalidOther(d & 0xFF, outPtr-start);
                        }
                        c = (c << 6) | (d & 0x3F);
                        /* Ugh. Need to mess with surrogates. Ok; let's inline them
                         * there, then, if there's room: if only room for one,
                         * need to save the surrogate for the rainy day...
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
            if (mPtr >= mLength) {
                break main_loop;
            }
        }

        len = outPtr - start;
        mCharCount += len;
        return len;
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

    private void reportUnexpectedEOF(int gotBytes, int needed)
        throws IOException
    {
        int bytePos = mByteCount + gotBytes;
        int charPos = mCharCount;

        throw new CharConversionException("Unexpected EOF in the middle of a multi-byte char: got "
                                          +gotBytes+", needed "+needed
                                          +", at char #"+charPos+", byte #"+bytePos+")");
    }

    /**
     * @param available Number of "unused" bytes in the input buffer
     *
     * @return Character we read, if any; or -1 to signal an EOF encountered
     *   that prevented us reading one more full char (not including cases
     *   where such EOF signals a broken char, which are signaled by
     *   proper IOException)
     */
    private boolean loadMore(int available)
        throws IOException
    {
        mByteCount += (mLength - available);

        // Bytes that need to be moved to the beginning of buffer?
        if (available > 0) {
            if (mPtr > 0) {
                for (int i = 0; i < available; ++i) {
                    mBuffer[i] = mBuffer[mPtr+i];
                }
                mPtr = 0;
                mLength = available;
            }
        } else {
            /* Ok; here we can actually reasonably expect an EOF,
             * so let's do a separate read right away:
             */
            mPtr = 0;
            int count = mIn.read(mBuffer);
            if (count < 1) {
                mLength = 0;
                if (count < 0) { // -1
                    mBuffer = null; // to help GC?
                    return false;
                }
                // 0 count is no good; let's err out
                reportStrangeStream();
            }
            mLength = count;
        }

        /* We now have at least one byte... and that allows us to
         * calculate exactly how many bytes we need!
         */
        int c = (int) mBuffer[0];
        if (c >= 0) { // single byte... cool, can return
            return true;
        }

        // Ok, a multi-byte char, let's check how many bytes we'll need:
        int needed;
        if ((c & 0xE0) == 0xC0) { // 2 bytes (0x0080 - 0x07FF)
            needed = 2;
        } else if ((c & 0xF0) == 0xE0) { // 3 bytes (0x0800 - 0xFFFF)
            needed = 3;
        } else if ((c & 0xF8) == 0xF0) {
            // 4 bytes; double-char BS, with surrogates and all...
            needed = 4;
        } else {
            reportInvalidInitial(c & 0xFF, 0);
            // never gets here...
            needed = 1;
        }

        /* And then we'll just need to load up to that many bytes;
         * if an EOF is hit, that'll be an error. But we need not do
         * actual decoding here, just load enough bytes.
         */
        do {
            int count = mIn.read(mBuffer, mLength, mBuffer.length - mLength);
            if (count < 1) {
                if (count < 0) { // -1, EOF... no good!
                    mBuffer = null; // to help GC?
                    reportUnexpectedEOF(mLength, needed);
                }
                // 0 count is no good; let's err out
                reportStrangeStream();
            }
            mLength += count;
        } while (mLength < needed);
        return true;
    }

    /*
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
                // Let's actually then free the buffer right away; shouldn't
                // yet close the underlying stream though?
                mBuffer = null;
                if (count == 0) {
                    reportStrangeStream();
                }
                throw new IOException("Unexpected EOF in the middle of multi-byte UTF-8 character");
            }
            mLength += count;
        }
    }
    */
}

