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

import com.ctc.wstx.cfg.XmlConsts;

/**
 * Optimized Reader that reads ascii content from an input stream.
 * In addition to doing (hopefully) optimal conversion, it can also take
 * array of "pre-read" (leftover) bytes; this is necessary when preliminary
 * stream/reader is trying to figure out XML encoding.
 */
public final class AsciiReader
    extends BaseReader
{
    boolean mXml11 = false;

   /**
     * Total read character count; used for error reporting purposes
     * (note: byte count is the same, due to fixed one-byte-per char mapping)
     */
    int mCharCount = 0;

    /*
    ////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////
    */

    public AsciiReader(InputStream in, byte[] buf, int ptr, int len)
    {
        super(in, buf, ptr, len);
    }

    public void setXmlCompliancy(int xmlVersion)
    {
        mXml11 = (xmlVersion == XmlConsts.XML_V_11);
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
        // Let's then ensure there's enough room...
        if (start < 0 || (start+len) > cbuf.length) {
            reportBounds(cbuf, start, len);
        }

        // Need to load more data?
        int avail = mLength - mPtr;
        if (avail <= 0) {
            // Let's always try to read full buffers, actually...
            int count = mIn.read(mBuffer);
            if (count <= 0) {
                if (count == 0) {
                    reportStrangeStream();
                }
                /* Let's actually then free the buffer right away; shouldn't
                 * yet close the underlying stream though?
                 */
                mBuffer = null;
                return -1;
            }
            mLength = avail = count;
            mCharCount += count;
            mPtr = 0;
        }

        // K, have at least one byte == char, good enough:

        if (len > avail) {
            len = avail;
        }
        int i = mPtr;
        int last = i + len;

        for (; i < last; ) {
            char c = (char) mBuffer[i++];
            if (c >= CHAR_DEL) {
                if (c > CHAR_DEL) {
                    reportInvalidAscii(c);
                } else {
                    if (mXml11) {
                        int pos = mCharCount + mPtr;
                        reportInvalidXml11(c, pos, pos);
                    }
                }
            }
            cbuf[start++] = c;
        }

        mPtr = last;
        return len;
    }

    /*
    ////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////
    */

    private void reportInvalidAscii(char c)
        throws IOException
    {
        throw new CharConversionException("Invalid ascii byte; value above 7-bit ascii range ("+((int) c)+"; at pos #"+(mCharCount + mPtr)+")");
    }
}

