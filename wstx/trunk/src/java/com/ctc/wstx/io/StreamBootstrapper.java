package com.ctc.wstx.io;

import java.io.*;

import javax.xml.stream.Location;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLStreamException;

import com.ctc.wstx.cfg.ParsingErrorMsgs;
import com.ctc.wstx.exc.*;
import com.ctc.wstx.util.StringUtil;

/**
 * Input bootstrap class used with streams, when encoding is not known
 * (when encoding is specified by application, a reader is constructed,
 * and then reader-based bootstrapper is used).
 *<p
 * Encoding used for an entity (including
 * main document entity) is detrmined using algorithms suggested in
 * XML 1.0#3 spec, appendix F
 */
public final class StreamBootstrapper
    extends InputBootstrapper
{
    /**
     * Let's size buffer at least big enough to contain the longest possible
     * prefix of a document needed to positively identify it starts with
     * the XML declaration. That means having (optional) BOM, and then first
     * 6 characters ("<?xml "), in whatever encoding. With 4-byte encodings
     * (UCS-4), that comes to 28 bytes. And for good measure, let's pad
     * that a bit as well....
     */
    final static int MIN_BUF_SIZE = 64;

    /*
    ////////////////////////////////////////
    // Configuration
    ////////////////////////////////////////
    */

    /**
     * Underlying InputStream to use for reading content.
     */
    final InputStream mIn;

    /*
    ///////////////////////////////////////////////////////////////
    // Input buffering
    ///////////////////////////////////////////////////////////////
    */

    final byte[] mByteBuffer;

    private int mInputPtr;

    private int mInputLen;

    /*
    ///////////////////////////////////////////////////////////////
    // Physical encoding properties found so far
    ///////////////////////////////////////////////////////////////
    */

    boolean mBigEndian = true;

    boolean mHadBOM = false;

    boolean mByteSizeFound = false;

    int mBytesPerChar; // minimum, ie. 1 for UTF-8

    /*
    ////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////
    */

    private StreamBootstrapper(InputStream in, String pubId, String sysId,
                               int bufSize)
    {
        super(pubId, sysId);
        mIn = in;
        if (bufSize < MIN_BUF_SIZE) {
            bufSize = MIN_BUF_SIZE;
        }
        mByteBuffer = new byte[bufSize];
    }

    /*
    ////////////////////////////////////////
    // Public API
    ////////////////////////////////////////
    */

    public static StreamBootstrapper getInstance(InputStream in, String pubId, String sysId, int bufSize)
    {
        return new StreamBootstrapper(in, pubId, sysId, bufSize);
    }

    public Reader bootstrapInput(boolean mainDoc, XMLReporter rep)
        throws IOException, WstxException
    {
        resolveStreamEncoding();
        if (hasXmlDecl()) {
            readXmlDecl(mainDoc);
            if (mFoundEncoding != null) {
                mFoundEncoding = verifyXmlEncoding(mFoundEncoding);
            }
        }

        // Now, have we figured out the encoding?
        String enc = mFoundEncoding;

        if (enc == null) { // not via xml declaration
            if (mBytesPerChar == 2) { // UTF-16, BE/LE
                enc = mBigEndian ? "UTF-16BE" : "UTF-16LE";
            } else if (mBytesPerChar == 4) { // UCS-4... ?
                /* 22-Mar-2005, TSa: JDK apparently has no way of dealing
                 *   with these encodings... not sure if and how it should
                 *   be dealt with, really. Name could be UCS-4xx... or
                 *   perhaps UTF-32xx
                 */
                enc = mBigEndian ? "UTF-32BE" : "UTF-32LE";
            } else {
                // Ok, default has to be UTF-8, as per XML specs
                enc = "UTF-8";
            }
        }
        
        /* And then the reader. Let's figure out if we can use our own fast
         * implementations first:
         */
        Reader r = null;
        
        char c = (enc.length() > 0) ? enc.charAt(0) : ' ';
        
        if (c == 'u' || c == 'U') {
            if (StringUtil.equalEncodings(enc, "UTF-8")) {
                r = new UTF8Reader(mIn, mByteBuffer, mInputPtr, mInputLen);
            } else if (StringUtil.equalEncodings(enc, "US-ASCII")) {
                r = new AsciiReader(mIn, mByteBuffer, mInputPtr, mInputLen);
            } else if (StringUtil.equalEncodings(enc, "UTF-16BE")) {
                // let's just make sure they're using canonical name...
                enc = "UTF-16BE";
            } else if (StringUtil.equalEncodings(enc, "UTF-16LE")) {
                enc = "UTF-16LE";
            } else if (StringUtil.equalEncodings(enc, "UTF")) {
                enc = "UTF";
            }
        } else if (c == 'i' || c== 'I') {
            if (StringUtil.equalEncodings(enc, "ISO-8859-1")) {
                r = new ISOLatinReader(mIn, mByteBuffer, mInputPtr, mInputLen);
            }
        }
        
        if (r == null) {
            // Nah, JDK needs to try it
            // Ok; first, do we need to merge stuff back?
            InputStream in = mIn;
            if (mInputPtr < mInputLen) {
                in = new MergedStream(in, mByteBuffer, mInputPtr, mInputLen);
            }
            r = new InputStreamReader(in, enc);
        }
        return r;
    }
    
    /**
     * By definition, when this bootstrapper is used, encoding is not
     * known...
     */
    public String getAppEncoding() {
        return null;
    }

    public int getInputTotal() {
        int total = mInputProcessed + mInputPtr;
        return (mBytesPerChar > 1) ? (total / mBytesPerChar) : total;
    }

    public int getInputColumn() {
        int col = mInputPtr - mInputRowStart;
        return (mBytesPerChar > 1) ? (col / mBytesPerChar) : col;
    }

    /*
    ////////////////////////////////////////
    // Internal methods, parsing
    ////////////////////////////////////////
    */

    /**
     * Method called to try to figure out physical encoding the underlying
     * input stream uses.
     */
    protected void resolveStreamEncoding()
        throws IOException, WstxException
    {
        // Let's first set defaults:
        mBytesPerChar = 0;
        mBigEndian = true;

        /* Let's try to read enough data for the longest possible XML
         * declaration (and leading BOM if any).
         */
        initialLoad(MIN_BUF_SIZE);

        // However, for these first checks, we just need first 4 bytes:
        if (mInputLen >= 4) {
            do { // BOM/auto-detection block
                int quartet = (mByteBuffer[0] << 24)
                    | ((mByteBuffer[1] & 0xFF) << 16)
                    | ((mByteBuffer[2] & 0xFF) << 8)
                    | (mByteBuffer[3] & 0xFF);
                
                /* First, handling of (usually) optional BOM (required for
                 * multi-byte formats)
                 */
                if (quartet == 0x0000FEFF) { // UCS-4, BE?
                    mBigEndian = true;
                    mInputPtr = mBytesPerChar = 4;
                    break;
                }
                if (quartet == 0xFFFE0000) { // UCS-4, LE?
                    mInputPtr = mBytesPerChar = 4;
                    mBigEndian = false;
                    break;
                }
                if (quartet == 0x0000FFFE) { // UCS-4, in-order...
                    reportWeirdUCS4("2143");
                }
                if (quartet == 0x0FEFF0000) { // UCS-4, in-order...
                    reportWeirdUCS4("3412");
                }

                int msw = quartet >>> 16;
                if (msw == 0xFEFF) { // UTF-16, BE
                    mInputPtr = mBytesPerChar = 2;
                    mBigEndian = true;
                    break;
                }
                if (msw == 0xFFFE) { // UTF-16, LE
                    mInputPtr = mBytesPerChar = 2;
                    mBigEndian = false;
                    break;
                }

                if ((quartet >>> 8) == 0xEFBBBF) { // UTF-8
                    mInputPtr = 3;
                    mBytesPerChar = 1;
                    mBigEndian = true; // doesn't really matter
                    break;
                }

		/* And if that wasn't succesful, how about auto-detection
		 * for '<?xm' (or subset for multi-byte encodings) marker?
		 */
		// Note: none of these consume bytes... so ptr remains at 0
                if (quartet == 0x0000003c) { // UCS-4, BE?
                    mBigEndian = true;
                    mBytesPerChar = 4;
                    break;
                }
                if (quartet == 0x3c000000) { // UCS-4, LE?
                    mBytesPerChar = 4;
                    mBigEndian = false;
                    break;
                }
                if (quartet == 0x00003c00) { // UCS-4, in-order...
                    reportWeirdUCS4("2143");
                }
                if (quartet == 0x003c0000) { // UCS-4, in-order...
                    reportWeirdUCS4("3412");
                }

                if (quartet == 0x003c003f) { // UTF-16, BE
		    mBytesPerChar = 2;
                    mBigEndian = true;
                    break;
                }
                if (quartet == 0x3c003f00) { // UTF-16, LE
                    mBytesPerChar = 2;
                    mBigEndian = false;
                    break;
                }

                if (quartet == 0x3c3f786d) { // UTF-8, Ascii, ISO-Latin
                    mBytesPerChar = 1;
                    mBigEndian = true; // doesn't really matter
                    break;
                }

		/* Otherwise it's either single-byte doc without xml
		 * declaration, or corrupt input...
		 */
            } while (false); // BOM/auto-detection block

            mHadBOM = (mBytesPerChar > 0);

            // Let's update location markers to ignore BOM.
            mInputProcessed = -mInputPtr;
            mInputRowStart = mInputPtr;

            /* Well, if there was no BOM, we can potentially still figure
             * out encoding with first 4 bytes; assuming there is a
             * valid XML declaration...
             */

            if (!mHadBOM) {
                int quartet = (mByteBuffer[0] << 24)
                    | ((mByteBuffer[1] & 0xFF) << 16)
                    | ((mByteBuffer[2] & 0xFF) << 8)
                    | (mByteBuffer[3] & 0xFF);
                
                /* Note: should not update input pointer since the full
                 * XML declaration needs to be read later on to verify
                 * it's correct; here it's just used as the marker
                 */
                
                switch (quartet) {
                case 0x0000003C: // UCS-4, BE
                    mBytesPerChar = 4;
                    mBigEndian = true;
                    break;
                case 0x3C000000: // UCS-4, LE
                    mBytesPerChar = 4;
                    mBigEndian = false;
                    break;
                case 0x00003C00: // UCS, mixed
                    reportWeirdUCS4("2143");
		    break; // to keep jikes happy
                case 0x003C0000: // UCS, mixed
                    reportWeirdUCS4("3412");
		    break; // to keep jikes happy
                case 0x003C003F: // UTF-16 BE etc
                    mBytesPerChar = 2;
                    mBigEndian = false;
                    break;
                case 0x3C003F00: // UTF-16 LE etc
                    mBytesPerChar = 2;
                    mBigEndian = false;
                    break;
                case 0x3C3F786D: // UTF-8, ISO-8859-x, Ascii etc
                    mBytesPerChar = 1;
                    mBigEndian = false;
                    break;
                case 0x4C6FA794: // EBCDIC... if/how can we handle that?
                    // !!! 10-Aug-2004, TSa: not (yet?) supported
                    reportEBCDIC();
                }
            }
        }

        /* Hmmh. If we haven't figured it out, let's just assume
         * UTF-8 as per XML specs:
         */
        mByteSizeFound = (mBytesPerChar > 0);
        if (!mByteSizeFound) {
            mBytesPerChar = 1;
            mBigEndian = true; // doesn't matter
        }
    }

    protected String verifyXmlEncoding(String enc)
        throws WstxException
    {
        // Let's actually verify we got matching information:
        if (enc.startsWith("UTF")) {
            String s = (enc.charAt(3) == '-') ?
                enc.substring(4) : enc.substring(3);
            if (s.equals("8")) {
                verifyEncoding(enc, 1);
                enc = "UTF-8";
            } else if (s.startsWith("16")) {
                if (s.length() == 2) {
                    // BOM is obligatory, to know the ordering
		    /* 22-Mar-2005, TSa: Actually, since we don't have a
		     *   custom decoder, so the underlying JDK Reader may
		     *   have dealt with it transparently... so we can not
		     *   really throw an exception here.
		     */
                    if (!mHadBOM) {
                        //reportMissingBOM(enc);
                    }
                    verifyEncoding(enc, 2);
		    enc = "UTF-16";
                } else if (s.equals("16BE")) {
                    verifyEncoding(enc, 2, true);
		    enc = "UTF-16BE";
                } else if (s.equals("16LE")) {
                    verifyEncoding(enc, 2, false);
		    enc = "UTF-16LE";
                }
            }
        } else if (enc.startsWith("ISO")) {
            String s = (enc.charAt(3) == '-') ?
                enc.substring(4) : enc.substring(3);
            if (s.startsWith("8859")) { // various code pages, incl. ISO-Latin
                verifyEncoding(enc, 1);
		// enc should be good as is...
            } else if (s.startsWith("10646")) { // alias(es) for UTF...?
                if (s.equals("10646-UCS-2")) {
                    /* JDK doesn't seem to have direct match, but shouldn't
                     * following mapping work? (see after checks)
                     */
                    if (!mHadBOM) { // needs BOM
                        reportMissingBOM(enc);
                    }
                    verifyEncoding(enc, 2);
                    enc = "UTF-16";
                } else if (s.equals("10646-UCS-4")) {
                    /* Does JDK even support this encoding? Hmmh...
                     * didn't see one in the list Charset returns?
                     * Let's try it, however; may fail later on.
                     */
                    if (!mHadBOM) { // needs BOM
                        reportMissingBOM(enc);
                    }
                    verifyEncoding(enc, 4);
                    enc = "UTF-32";
                }
            } else if (s.equals("646-US")) {
                enc = "US-ASCII";
                verifyEncoding(enc, 1);
            }
        } else if (enc.endsWith("ASCII")) {
            enc = "US-ASCII";
            verifyEncoding(enc, 1);
        }
        return enc;
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
            int count = mIn.read(mByteBuffer, mInputLen,
                                 mByteBuffer.length - mInputLen);
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
        /* Note: at this point these are all in bytes, not chars (for multibyte
         * encodings)
         */
        mInputProcessed += mInputLen;
        mInputRowStart -= mInputLen;

        mInputPtr = 0;
        mInputLen = mIn.read(mByteBuffer, 0, mByteBuffer.length);
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
        mInputPtr -= mBytesPerChar;
    }

    protected int getNext()
        throws IOException, WstxException
    {
        if (mBytesPerChar > 1) {
            return nextMultiByte();
        }
        byte b = (mInputPtr < mInputLen) ?
            mByteBuffer[mInputPtr++] : nextByte();
        return (b & 0xFF);
    }


    protected int getNextAfterWs(boolean reqWs)
        throws IOException, WstxException
    {
        int count;

        if (mBytesPerChar > 1) { // multi-byte
            count = skipMbWs();
        } else {
            count = skipSbWs();
        }

        if (reqWs && count == 0) {
            reportUnexpectedChar(getNext(), ERR_XMLDECL_EXP_SPACE);
        }

        // inlined getNext()
        if (mBytesPerChar > 1) {
            return nextMultiByte();
        }
        byte b = (mInputPtr < mInputLen) ?
            mByteBuffer[mInputPtr++] : nextByte();
        return (b & 0xFF);
    }

    /**
     * @return First character that does not match expected, if any;
     *    CHAR_NULL if match succeeded
     */
    protected int checkKeyword(String exp)
        throws IOException, WstxException
    {
        if (mBytesPerChar > 1) {
            return checkMbKeyword(exp);
        }
        return checkSbKeyword(exp);
    }

    protected int readQuotedValue(char[] kw, int quoteChar, boolean norm)
        throws IOException, WstxException
    {
        int i = 0;
        int len = kw.length;
        boolean mb = (mBytesPerChar > 1);

        while (i < len) {
            int c;

            if (mb) {
                c = nextMultiByte();
            } else {
                byte b = (mInputPtr < mInputLen) ?
                    mByteBuffer[mInputPtr++] : nextByte();
                if (b == BYTE_NULL) {
                    reportNull();
                }
                c = (b & 0xFF);
            }

            if (c == quoteChar) {
                return i;
            }

            /* Normalization used for encodings; for some reason encoding
             * names are all upper-case...
             */
            char d = (char) c;
            if (norm) {
                if (d <= CHAR_SPACE || d == '_') {
                    d = '-';
                } else {
                    d = Character.toUpperCase(d);
                }
            }

            kw[i++] = d;
        }

        /* If we end up this far, we ran out of buffer space... let's let
         * caller figure that out, though
         */
        return -1;
    }

    protected boolean hasXmlDecl()
        throws IOException, WstxException
    {
        /* Separate handling for common and fast case; 1/variable byte
         * encodings that have ASCII subset:
         */
        if (mBytesPerChar == 1) {
            /* However... there has to be at least 6 bytes available; and if
             * so, can check the 'signature' easily:
             */
            if ((mInputLen - mInputPtr) >= 6) {
                if (mByteBuffer[mInputPtr] == '<'
                    && mByteBuffer[mInputPtr+1] == '?'
                    && mByteBuffer[mInputPtr+2] == 'x'
                    && mByteBuffer[mInputPtr+3] == 'm'
                    && mByteBuffer[mInputPtr+4] == 'l'
                    && ((mByteBuffer[mInputPtr+5] & 0xFF) <= CHAR_SPACE)) {

                    // Let's skip stuff so far:
                    mInputPtr += 6;
                    return true;
                }
            }
        } else {
            // ... and then for slower fixed-multibyte encodings:

            // Is there enough data for checks?
            int last = mInputPtr + (6 * mBytesPerChar);

            if (last <= mInputLen) { // yup
                int start = mInputPtr; // if we have to 'unread' chars
                if (nextMultiByte() == '<'
                    && nextMultiByte() == '?'
                    && nextMultiByte() == 'x'
                    && nextMultiByte() == 'm'
                    && nextMultiByte() == 'l'
                    && nextMultiByte() <= CHAR_SPACE) {
                    return true;
                }
                mInputPtr = start; // push data back
            }
        }

        return false;
    }

    protected Location getLocation()
    {
        /* Ok; for fixed-size multi-byte encodings, need to divide numbers
         * to get character locations. For variable-length encodings the
         * good thing is that xml declaration only uses shortest codepoints,
         * ie. char count == byte count.
         */
        int total = mInputProcessed + mInputPtr;
        int col = mInputPtr - mInputRowStart;

        if (mBytesPerChar > 1) {
            total /= mBytesPerChar;
            col /= mBytesPerChar;
        }

        return new WstxInputLocation(null, mPublicId, mSystemId,
                                     total - 1, // 0-based
                                     mInputRow, col);
    }

    /*
    /////////////////////////////////////////////////////
    // Internal methods, single-byte access methods
    /////////////////////////////////////////////////////
    */

    protected byte nextByte()
        throws IOException, WstxException
    {
        if (mInputPtr >= mInputLen) {
            loadMore();
        }
        return mByteBuffer[mInputPtr++];
    }

    protected int skipSbWs()
        throws IOException, WstxException
    {
        int count = 0;

        while (true) {
            byte b = (mInputPtr < mInputLen) ?
                mByteBuffer[mInputPtr++] : nextByte();

            if ((b & 0xFF) > CHAR_SPACE) {
                --mInputPtr;
                break;
            }
            if (b == BYTE_CR || b == BYTE_LF) {
                skipSbLF(b);
            } else if (b == BYTE_NULL) {
                reportNull();
            }
            ++count;
        }
        return count;
    }

    protected void skipSbLF(byte lfByte)
        throws IOException, WstxException
    {
        if (lfByte == BYTE_CR) {
            byte b = (mInputPtr < mInputLen) ?
                mByteBuffer[mInputPtr++] : nextByte();
            if (b != BYTE_LF) {
                --mInputPtr; // pushback if not 2-char/byte lf
            }
        }
        ++mInputRow;
        mInputRowStart = mInputPtr;
    }

    /**
     * @return First character that does not match expected, if any;
     *    CHAR_NULL if match succeeded
     */
    protected int checkSbKeyword(String expected)
        throws IOException, WstxException
    {
        int len = expected.length();
        
        for (int ptr = 1; ptr < len; ++ptr) {
            byte b = (mInputPtr < mInputLen) ?
                mByteBuffer[mInputPtr++] : nextByte();
            
            if (b == BYTE_NULL) {
                reportNull();
            }
            if ((b & 0xFF) != expected.charAt(ptr)) {
                return (b & 0xFF);
            }
        }

        return CHAR_NULL;
    }

    /*
    /////////////////////////////////////////////////////
    // Internal methods, multi-byte access/checks
    /////////////////////////////////////////////////////
    */

    protected int nextMultiByte()
        throws IOException, WstxException
    {
        byte b = (mInputPtr < mInputLen) ?
            mByteBuffer[mInputPtr++] : nextByte();
        byte b2 = (mInputPtr < mInputLen) ?
            mByteBuffer[mInputPtr++] : nextByte();
        int c;

        if (mBytesPerChar == 2) {
            if (mBigEndian) {
                c = ((b & 0xFF) << 8) | (b2 & 0xFF);
            } else {
                c = (b & 0xFF) | ((b2 & 0xFF) << 8);
            }
        } else {
            // Has to be 4 bytes
            byte b3 = (mInputPtr < mInputLen) ?
                mByteBuffer[mInputPtr++] : nextByte();
            byte b4 = (mInputPtr < mInputLen) ?
                mByteBuffer[mInputPtr++] : nextByte();
            
            if (mBigEndian) {
                c = (b  << 24) | ((b2 & 0xFF) << 16)
                    | ((b3 & 0xFF) << 8) | (b4 & 0xFF);
            } else {
                c = (b4  << 24) | ((b3 & 0xFF) << 16)
                    | ((b2 & 0xFF) << 8) | (b & 0xFF);
            }
        }

        // Let's catch null chars early
        if (c == 0) {
            reportNull();
        }
        return c;
    }

    protected int skipMbWs()
        throws IOException, WstxException
    {
        int count = 0;

        while (true) {
            int c = nextMultiByte();

            if (c > CHAR_SPACE) {
                mInputPtr -= mBytesPerChar;
                break;
            }
            if (c == CHAR_CR || c == CHAR_LF) {
                skipMbLF(c);
            } else if (c == CHAR_NULL) {
                reportNull();
            }
            ++count;
        }
        return count;
    }

    protected void skipMbLF(int lf)
        throws IOException, WstxException
    {
        if (lf == CHAR_CR) {
            int c = nextMultiByte();
            if (c != CHAR_LF) {
                mInputPtr -= mBytesPerChar;
            }
        }
        ++mInputRow;
        mInputRowStart = mInputPtr;
    }

    /**
     * @return First character that does not match expected, if any;
     *    CHAR_NULL if match succeeded
     */
    protected int checkMbKeyword(String expected)
        throws IOException, WstxException
    {
        int len = expected.length();
        
        for (int ptr = 1; ptr < len; ++ptr) {
	    int c = nextMultiByte();
            if (c == BYTE_NULL) {
                reportNull();
            }
            if (c != expected.charAt(ptr)) {
              return c;
            }
        }

        return CHAR_NULL;
    }

    /*
    ////////////////////////////////////////
    // Other private methods:
    ////////////////////////////////////////
    */

    private void verifyEncoding(String id, int bpc)
        throws WstxException
    {
        if (mByteSizeFound) {
            /* Let's verify that if we matched an encoding, it's the same
             * as what was declared...
             */
            if (bpc != mBytesPerChar) {
                reportXmlProblem("Declared encoding '"+id+"' uses "+bpc
                                 +" bytes per character; but physical encoding appeared to use "+mBytesPerChar+"; cannot decode");
            }
        }
    }

    private void verifyEncoding(String id, int bpc, boolean bigEndian)
        throws WstxException
    {
        if (mByteSizeFound) {
            verifyEncoding(id, bpc);

            if (bigEndian != mBigEndian) {
                String bigStr = bigEndian ? "big" : "little";
                reportXmlProblem
                    ("Declared encoding '"+id+"' has different endianness ("
                     +bigStr+" endian) than what physical ordering appeared to be; cannot decode");
            }
        }
    }

    private void reportWeirdUCS4(String type)
        throws IOException
    {
        throw new CharConversionException("Unsupported UCS-4 endianness ("+type+") detected");
    }

    private void reportEBCDIC()
        throws IOException
    {
        throw new CharConversionException("Unsupported encoding (EBCDIC)");
    }

    private void reportMissingBOM(String enc)
        throws WstxException
    {
        throw new WstxException("Missing BOM for encoding '"+enc+"'; can not be omitted",
                                getLocation());
    }
}
