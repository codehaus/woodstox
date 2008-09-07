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

import com.ctc.wstx.api.ReaderConfig;
import com.ctc.wstx.cfg.XmlConsts;
import com.ctc.wstx.util.StringUtil;

/**
 * Simple baseline Reader for reading EBCDIC encoded content to be
 * able to boostrap xml document. It is based on codepage 037 (one
 * used in US), but will hopefully be applicable for bootstrapping
 * other codepages as well (but not beyond).
 */
public final class EBCDICReader
    extends BaseReader
{
    boolean mXml11 = false;

   /**
     * Total read character count; used for error reporting purposes
     * (note: byte count is the same, due to fixed one-byte-per char mapping)
     */
    int mByteCount = 0;

    final int[] mToUnicode;

    /*
    ////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////
    */

    public EBCDICReader(ReaderConfig cfg, InputStream in, byte[] buf, int ptr, int len)
    {
        super(cfg, in, buf, ptr, len);
        // Should determine actual real code page...
        mToUnicode = EBCDICCodec.getCp037Mapping();
    }

    public void setXmlCompliancy(int xmlVersion)
    {
        mXml11 = (xmlVersion == XmlConsts.XML_V_11);
    }

    /* OUCH! This is very ugly (fugly, even!)
     * But for now it'll do: we need to be able to switch from this
     * 'bootstrapping' EBCDIC handler to a real one, provided by JDK
     */

    public Reader switchIfNecessary(String realEncoding, char[] charBuffer, int ptr, int len)
        throws IOException
    {
        String norm = StringUtil.trimEncoding(realEncoding, false);
        Reader r;

        if (norm.endsWith("037")) {
            // good enough, may just need to merge stuff read
            r = this;
        } else {
            // Nope, different EBCDIC variant
            r = new InputStreamReader(getStream(), realEncoding);
        }
        if (ptr < len) {
            return new MergedReader(mConfig, r, charBuffer, ptr, len);
        }
        return this;
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
            mByteCount += mLength;
            // Let's always try to read full buffers, actually...
            int count = readBytes();
            if (count <= 0) {
                if (count == 0) {
                    reportStrangeStream();
                }
                /* Let's actually then free the buffer right away; shouldn't
                 * yet close the underlying stream though?
                 */
                freeBuffers(); // to help GC?
                return -1;
            }
            avail = count;
        }

        // K, have at least one byte == char, good enough:

        if (len > avail) {
            len = avail;
        }
        int i = mPtr;
        int last = i + len;

        final int[] mapping = mToUnicode;

        for (; i < last; ) {
            int ch = mapping[mBuffer[i++] & 0xFF];
//System.out.println("Char #"+(i-1)+", 0x"+Integer.toHexString(mBuffer[i-1])+" -> 0x"+Integer.toHexString(ch));
            if (ch < 0) { // special) {
                ch = -ch;
                // With xml 1.0 they are ok; with 1.1 not necessarily
                if (mXml11) {
                    if (ch == 0x85) {
                        ch = CONVERT_NEL_TO;
                    } else {
                        int pos = mByteCount + i;
                        reportInvalidXml11(ch, pos, pos);
                    }
                }
            }
            cbuf[start++] = (char) ch;
        }

        mPtr = last;
        return len;
    }
}

