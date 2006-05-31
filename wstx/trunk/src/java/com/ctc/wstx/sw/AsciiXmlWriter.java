/* Woodstox XML processor
 *
 * Copyright (c) 2004- Tatu Saloranta, tatu.saloranta@iki.fi
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

package com.ctc.wstx.sw;

import java.io.*;

import javax.xml.stream.XMLStreamException;

import com.ctc.wstx.api.WriterConfig;

/**
 * Concrete implementation of {@link EncodingXmlWriter} used when output
 * is to be encoded using 7-bit ascii (US-ASCII) encoding.
 */
public final class AsciiXmlWriter
    extends EncodingXmlWriter
{
    public AsciiXmlWriter(OutputStream out, WriterConfig cfg, String encoding)
        throws IOException
    {
        super(out, cfg, encoding);
    }

    public void writeRaw(char[] cbuf, int offset, int len)
        throws IOException
    {
        int ptr = mOutputPtr;
        byte[] outBuf = mOutputBuffer;
        while (len > 0) {
            int max = outBuf.length - mOutputPtr;
            if (max < 1) { // output buffer full?
                flushBuffer();
                ptr = 0;
                max = outBuf.length;
            }
            // How much can we output?
            if (max > len) {
                max = len;
            }
            if (mCheckContent) {
                for (int inEnd = offset + max; offset < inEnd; ++offset) {
                    int c = cbuf[offset];
                    if (c < 32) {
                        if (c != '\n' && c != '\r' && c != '\t') {
                            throwInvalidChar(c);
                        }
                    } else if (c > 0x7E) {
                        if (c != 0x7F) { // del ok in xml1.0
                            mOutputPtr = ptr;
                            throwInvalidAsciiChar(c);
                        } else if (mXml11) {
                            mOutputPtr = ptr;
                            throwInvalidChar(c);
                        }
                    }
                    outBuf[ptr++] = (byte) c;
                }
            } else {
                for (int inEnd = offset + max; offset < inEnd; ++offset) {
                    outBuf[ptr++] = (byte) cbuf[offset];
                }
            }
            len -= max;
        }
        mOutputPtr = ptr;
    }

    public void writeRaw(String str, int offset, int len)
        throws IOException
    {
            int ptr = mOutputPtr;
        byte[] outBuf = mOutputBuffer;
        while (len > 0) {
            int max = outBuf.length - mOutputPtr;
            if (max < 1) { // output buffer full?
                flushBuffer();
                ptr = 0;
                max = outBuf.length;
            }
            // How much can we output?
            if (max > len) {
                max = len;
            }
            if (mCheckContent) {
                for (int inEnd = offset + max; offset < inEnd; ++offset) {
                    int c = str.charAt(offset);
                    if (c < 32) {
                        if (c != '\n' && c != '\r' && c != '\t') {
                            throwInvalidChar(c);
                        }
                    } else if (c > 0x7E) {
                        if (c != 0x7F) { // del ok in xml1.0
                            mOutputPtr = ptr;
                            throwInvalidAsciiChar(c);
                        } else if (mXml11) {
                            mOutputPtr = ptr;
                            throwInvalidChar(c);
                        }
                    }
                    outBuf[ptr++] = (byte) c;
                }
            } else {
                for (int inEnd = offset + max; offset < inEnd; ++offset) {
                    outBuf[ptr++] = (byte) str.charAt(offset);
                }
            }
            len -= max;
        }
        mOutputPtr = ptr;
    }

    protected void writeAttrValue(String data)
        throws IOException, XMLStreamException
    {
        // !!! TBI
    }

    protected int writeCDataContent(String data)
        throws IOException
    {
        // !!! TBI
        return -1;
    }

    protected int writeCDataContent(char[] cbuf, int start, int len)
        throws IOException
    {
        // !!! TBI
        return -1;
    }

    protected int writeCommentContent(String data)
        throws IOException
    {
        // !!! TBI
        return -1;
    }

    protected int writePIData(String data)
        throws IOException, XMLStreamException
    {
        // !!! TBI
        return -1;
    }

    protected void writeTextContent(String data)
        throws IOException
    {
        // !!! TBI
    }

    protected void writeTextContent(char[] cbuf, int start, int len)
        throws IOException
    {
        // !!! TBI
    }

    /*
    ////////////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////////////
     */

    protected void throwInvalidAsciiChar(int c)
        throws IOException
    {
        // First, let's flush any output we may have, to make debugging easier
        flush();
        
        /* 17-May-2006, TSa: Would really be useful if we could throw
         *   XMLStreamExceptions; esp. to indicate actual output location.
         *   However, this causes problem with methods that call us and
         *   can only throw IOExceptions (when invoked via Writer proxy).
         *   Need to figure out how to resolve this.
         */
        throw new IOException("Invalid XML character (0x"+Integer.toHexString(c)+"); can only be output using character entity when using Ascii encoding");
    }
}
