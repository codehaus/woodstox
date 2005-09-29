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
import java.text.MessageFormat;

import javax.xml.stream.Location;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLStreamException;

import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.cfg.ParsingErrorMsgs;
import com.ctc.wstx.exc.*;

/**
 * Input bootstrap class used when input comes from a Reader; in this case,
 * encoding is already known, and thus encoding from XML declaration (if
 * any) is only double-checked, not really used.
 */
public final class ReaderBootstrapper
    extends InputBootstrapper
{
    final static char CHAR_BOM_MARKER = (char) 0xFEFF;

    /*
    ////////////////////////////////////////
    // Configuration
    ////////////////////////////////////////
    */

    /**
     * Underlying Reader to use for reading content.
     */
    final Reader mIn;

    /**
     * Encoding identifier processing application passed in; if not null,
     * will be compared to actual xml declaration based encoding (if
     * declaration found)
     */
    final String mAppEncoding;

    /*
    ///////////////////////////////////////////////////////////////
    // Input buffering
    ///////////////////////////////////////////////////////////////
    */

    final char[] mCharBuffer;

    private int mInputPtr;

    private int mInputLen;

    /*
    ////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////
    */

    private ReaderBootstrapper(Reader r, String pubId, String sysId,
                               int bufSize, String appEncoding)
    {
        super(pubId, sysId);
        mIn = r;

        /* Let's make sure buffer is at least 6 chars (to know '<?xml '
         * prefix), but not too long to waste space -- it won't be reused
         * by the real input reader.
         */
        /*
        if (bufSize < MIN_BUF_SIZE) {
            bufSize = MIN_BUF_SIZE;
        }
        */
        bufSize = 128;
        mCharBuffer = new char[bufSize];
        mAppEncoding = appEncoding;
    }

    /*
    ////////////////////////////////////////
    // Public API
    ////////////////////////////////////////
    */

    /**
     * @param appEncoding Encoding that application declared; may be null.
     *   If not null, will be compared to actual declaration found; and
     *   incompatibility reported as a potential (but not necessarily fatal)
     *   problem.
     */
    public static ReaderBootstrapper getInstance(Reader r, String pubId, String sysId,
                                                 int bufSize, String appEncoding)
    {
        return new ReaderBootstrapper(r, pubId, sysId, bufSize, appEncoding);
    }

    /**
     *
     */
    public Reader bootstrapInput(boolean mainDoc, XMLReporter rep)
        throws IOException, WstxException
    {
        initialLoad(mCharBuffer.length);
	/* Only need 6 for signature ("<?xml\s"), but there may be a leading
	 * BOM in there... and a valid xml declaration has to be longer
	 * than 7 chars anyway:
	 */
        if (mInputLen >= 7) {
	    char c = mCharBuffer[mInputPtr];

	    // BOM to skip?
	    if (c == CHAR_BOM_MARKER) {
		c = mCharBuffer[++mInputPtr];
	    }
            if (c == '<') {
		if (mCharBuffer[mInputPtr+1] == '?'
		    && mCharBuffer[mInputPtr+2] == 'x'
		    && mCharBuffer[mInputPtr+3] == 'm'
		    && mCharBuffer[mInputPtr+4] == 'l'
		    && mCharBuffer[mInputPtr+5] <= CHAR_SPACE) {
		    // Yup, got the declaration ok!
		    mInputPtr += 6; // skip declaration
		    readXmlDecl(mainDoc);

		    // !!! TBI: Check that xml encoding is compatible?
		    if (mFoundEncoding != null && mAppEncoding != null) {
			verifyXmlEncoding(rep);
		    }
		}
            }
        }

        /* Ok, now; do we have unused chars we have read that need to
         * be merged in?
         */
        if (mInputPtr < mInputLen) {
            return new MergedReader(mIn, mCharBuffer, mInputPtr, mInputLen);
        }

        return mIn;
    }

    public String getAppEncoding() {
        return mAppEncoding;
    }

    public int getInputTotal() {
        return mInputProcessed + mInputPtr;
    }

    public int getInputColumn() {
        return (mInputPtr - mInputRowStart);
    }

    /*
    ////////////////////////////////////////
    // Internal methods, parsing
    ////////////////////////////////////////
    */

    protected void verifyXmlEncoding(XMLReporter rep)
        throws WstxException
    {
        String appEnc = mAppEncoding;

        if (appEnc.equalsIgnoreCase(mFoundEncoding)) {
            return;
        }

        // UTF-8 has alias UTF8
        //appEnc = appEnc.toUpperCase();
        if (appEnc.equalsIgnoreCase("UTF8")) {
            if (mFoundEncoding.equalsIgnoreCase("UTF-8")) {
                return;
            }
        }

        // ... probably need to add others too...

        Location loc = getLocation();
        try {
            rep.report(MessageFormat.format(ErrorConsts.W_MIXED_ENCODINGS,
                                            new Object[] { mFoundEncoding,
                                                           mAppEncoding }),
                       ErrorConsts.WT_XML_DECL,
                       this, loc);
        } catch (XMLStreamException ex) {
            throw new WstxException(ex.toString(), loc);
        }
    }

    /*
    /////////////////////////////////////////////////////
    // Internal methods, loading input data
    /////////////////////////////////////////////////////
    */

    protected boolean initialLoad(int minimum)
        throws IOException
    {
        mInputPtr = 0;
        mInputLen = 0;

        while (mInputLen < minimum) {
            int count = mIn.read(mCharBuffer, mInputLen,
                                 mCharBuffer.length - mInputLen);
            if (count < 1) {
                return false;
            }
            mInputLen += count;
        }
        return true;
    }

    protected void loadMore()
        throws IOException, WstxException
    {
        /* Need to make sure offsets are properly updated for error
         * reporting purposes, and do this now while previous amounts
         * are still known.
         */
        mInputProcessed += mInputLen;
        mInputRowStart -= mInputLen;

        mInputPtr = 0;
        mInputLen = mIn.read(mCharBuffer, 0, mCharBuffer.length);
        if (mInputLen < 1) {
            throw new WstxEOFException(ParsingErrorMsgs.SUFFIX_IN_XML_DECL,
                                       getLocation());
        }
    }

    /*
    /////////////////////////////////////////////////////
    // Implementations of abstract parsing methods
    /////////////////////////////////////////////////////
    */

    protected void pushback() {
        --mInputPtr;
    }

    protected int getNext()
        throws IOException, WstxException
    {
        return (mInputPtr < mInputLen) ?
            mCharBuffer[mInputPtr++] : nextChar();
    }


    protected int getNextAfterWs(boolean reqWs)
        throws IOException, WstxException
    {
        int count = 0;

        while (true) {
            char c = (mInputPtr < mInputLen) ?
                mCharBuffer[mInputPtr++] : nextChar();

            if (c > CHAR_SPACE) {
                if (reqWs && count == 0) {
                    reportUnexpectedChar(c, ERR_XMLDECL_EXP_SPACE);
                }
                return c;
            }
            if (c == CHAR_CR || c == CHAR_LF) {
                skipCRLF(c);
            } else if (c == CHAR_NULL) {
                reportNull();
            }
            ++count;
        }
    }

    /**
     * @return First character that does not match expected, if any;
     *    CHAR_NULL if match succeeded
     */
    protected int checkKeyword(String exp)
        throws IOException, WstxException
    {
        int len = exp.length();
        
        for (int ptr = 1; ptr < len; ++ptr) {
            char c = (mInputPtr < mInputLen) ?
                mCharBuffer[mInputPtr++] : nextChar();
            
            if (c != exp.charAt(ptr)) {
                return c;
            }
            if (c == CHAR_NULL) {
                reportNull();
            }
        }

        return CHAR_NULL;
    }

    protected int readQuotedValue(char[] kw, int quoteChar, boolean norm)
        throws IOException, WstxException
    {
        int i = 0;
        int len = kw.length;

        while (i < len) {
            char c = (mInputPtr < mInputLen) ?
                mCharBuffer[mInputPtr++] : nextChar();
            if (c == CHAR_NULL) {
                reportNull();
            }
            if (c == quoteChar) {
                return i;
            }

            /* Normalization used for encodings; for some reason encoding
             * names are all upper-case...
             */
            if (norm) {
                if (c <= CHAR_SPACE || c == '_') {
                    c = '-';
                } else {
                    c = Character.toUpperCase(c);
                }
            }

            kw[i++] = c;
        }

        /* If we end up this far, we ran out of buffer space... let's let
         * caller figure that out, though
         */
        return -1;
    }

    protected Location getLocation()
    {
        return new WstxInputLocation(null, mPublicId, mSystemId,
                                     mInputProcessed + mInputPtr - 1,
                                     mInputRow, mInputPtr - mInputRowStart);
    }

    /*
    /////////////////////////////////////////////////////
    // Internal methods, single-byte access methods
    /////////////////////////////////////////////////////
    */

    protected char nextChar()
        throws IOException, WstxException
    {
        if (mInputPtr >= mInputLen) {
            loadMore();
        }
        return mCharBuffer[mInputPtr++];
    }

    protected void skipCRLF(char lf)
        throws IOException, WstxException
    {
        if (lf == CHAR_CR) {
            char c = (mInputPtr < mInputLen) ?
                mCharBuffer[mInputPtr++] : nextChar();
            if (c != BYTE_LF) {
                --mInputPtr; // pushback if not 2-char/byte lf
            }
        }
        ++mInputRow;
        mInputRowStart = mInputPtr;
    }

    /*
    ////////////////////////////////////////
    // Other private methods:
    ////////////////////////////////////////
    */
}


