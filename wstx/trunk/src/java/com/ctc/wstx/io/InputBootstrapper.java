package com.ctc.wstx.io;

import java.io.*;

import javax.xml.stream.Location;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLStreamException;

import com.ctc.wstx.exc.*;

/**
 * Abstract base class that defines common API used with both stream and
 * reader-based input sources. Class is responsible for opening the physical
 * input source, figure out encoding (if necessary; only for streams), and
 * then handle (optional) XML declaration.
 */
public abstract class InputBootstrapper
{
    /*
    ////////////////////////////////////////////////////////////
    // Shared string consts
    ////////////////////////////////////////////////////////////
     */

    private final static String KW_ENC = "encoding";
    private final static String KW_VERS = "version";
    private final static String KW_SA = "standalone";

    protected final static String ERR_XMLDECL_KW_VERSION = "; expected keyword 'version'";
    protected final static String ERR_XMLDECL_KW_ENCODING = "; expected keyword 'encoding'";
    protected final static String ERR_XMLDECL_KW_STANDALONE = "; expected keyword 'standalone'";

    protected final static String ERR_XMLDECL_END_MARKER = "; expected \"?>\" end marker";

    protected final static String ERR_XMLDECL_EXP_SPACE = "; expected a white space";
    protected final static String ERR_XMLDECL_EXP_EQ = "; expected '=' after ";
    protected final static String ERR_XMLDECL_EXP_ATTRVAL = "; expected a quote character enclosing value for ";

    /*
    ////////////////////////////////////////////////////////////
    // Other consts
    ////////////////////////////////////////////////////////////
     */

    public final static char CHAR_NULL = (char) 0;
    public final static char CHAR_SPACE = (char) 0x0020;

    public final static byte CHAR_CR = '\r';
    public final static byte CHAR_LF = '\n';

    public final static byte BYTE_NULL = (byte) 0;

    public final static byte BYTE_CR = (byte) '\r';
    public final static byte BYTE_LF = (byte) '\n';

    /*
    ////////////////////////////////////////////////////////////
    // Input source info
    ////////////////////////////////////////////////////////////
     */

    protected final String mPublicId;

    protected final String mSystemId;

    /*
    ////////////////////////////////////////////////////////////
    // Input location data (similar to one in WstxInputData)
    ////////////////////////////////////////////////////////////
     */

    /**
     * Current number of characters that were processed in previous blocks,
     * before contents of current input buffer.
     */
    protected int mInputProcessed = 0;

    /**
     * Current row location of current point in input buffer, starting
     * from 1
     */
    protected int mInputRow = 1;

    /**
     * Current index of the first character of the current row in input
     * buffer. Needed to calculate column position, if necessary; benefit
     * of not having column itself is that this only has to be updated
     * once per line.
     */
    protected int mInputRowStart = 0;

    /*
    ////////////////////////////////////////
    // Info from XML declaration
    ////////////////////////////////////////
    */

    boolean mHadDeclaration = false;

    String mVersion;

    String mFoundEncoding;

    String mStandalone;

    /*
    ////////////////////////////////////////
    // Temporary data
    ////////////////////////////////////////
    */

    /**
     * Need a short buffer to read in values of pseudo-attributes (version,
     * encoding, standalone). Don't really need tons of space; just enough
     * for the longest anticipated encoding id... and maybe few chars just
     * in case
     */
    final char[] mKeyword = new char[32];

    /*
    ////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////
    */

    protected InputBootstrapper(String pubId, String sysId)
    {
        mPublicId = pubId;
        mSystemId = sysId;
    }

    /*
    ////////////////////////////////////////
    // Public API
    ////////////////////////////////////////
    */

    public abstract Reader bootstrapInput(boolean mainDoc, XMLReporter rep)
        throws IOException, WstxException;

    // // // Source information:

    public String getPublicId() { return mPublicId; }
    public String getSystemId() { return mSystemId; }

    // // // XML declaration data:

    public String getVersion() {
        return mVersion;
    }

    public String getStandalone() {
        return mStandalone;
    }

    /**
     * @return Encoding declaration found from the xml declaration, if any;
     *    null if none.
     */
    public String getDeclaredEncoding() {
        return mFoundEncoding;
    }

    // // // Location/position info:

    /**
     * @return Total number of characters read from bootstrapped input
     *   (stream, reader)
     */
    public abstract int getInputTotal();

    public int getInputRow() {
        return mInputRow;
    }

    public abstract int getInputColumn();


    // // // Misc other info

    /**
     * @return Application specified input-encoding used, if any; null
     *   if no information was passed.
     */
    public abstract String getAppEncoding();

    /*
    ////////////////////////////////////////
    // Package methods, parsing
    ////////////////////////////////////////
    */

    protected void readXmlDecl(boolean isMainDoc)
        throws IOException, WstxException
    {
        int c = getNextAfterWs(false);

        // First, version pseudo-attribute:

        if (c != 'v') { // version info obligatory for main docs
            if (isMainDoc) {
                reportUnexpectedChar(c, ERR_XMLDECL_KW_VERSION);
            }
        } else { // ok, should be version
            mVersion = readXmlVersion();
            c = getWsOrChar('?');
        }

        // Then, 'encoding'
        if (c != 'e') { // obligatory for external entities
            if (!isMainDoc) {
                reportUnexpectedChar(c, ERR_XMLDECL_KW_ENCODING);
            }
        } else {
            mFoundEncoding = readXmlEncoding();
            c = getWsOrChar('?');
        }

        // Then, 'standalone' (for main doc)
        if (isMainDoc && c == 's') {
            mStandalone = readXmlStandalone();
            c = getWsOrChar('?');
        }

        // And finally, need to have closing markers

        if (c != '?') {
            reportUnexpectedChar(c, ERR_XMLDECL_END_MARKER);
        }
        c = getNext();
        if (c != '>') {
            reportUnexpectedChar(c, ERR_XMLDECL_END_MARKER);
        }
    }

    private final String readXmlVersion()
        throws IOException, WstxException
    {
        int c = checkKeyword(KW_VERS);
        if (c != CHAR_NULL) {
            reportUnexpectedChar(c, ERR_XMLDECL_KW_VERSION);
        }
        c = handleEq(KW_VERS);
        int len = readQuotedValue(mKeyword, c, false);

        if (len == 3) {
            if (mKeyword[0] == '1' && mKeyword[1] == '.') {
                c = mKeyword[2];
                if (c == '0') {
                    return "1.0";
                }
                if (c == '1') {
                    return "1.1";
                }
            }
        }

        // Nope; error. -1 indicates run off...
        String got;

        if (len < 0) {
            got = "'"+new String(mKeyword)+"[..]'";
        } else if (len == 0) {
            got = "<empty>";
        } else {
            got = "'"+new String(mKeyword, 0, len)+"'";
        }

        throw new WstxParsingException("Invalid XML version value "+got+"; expected '1.0' or '1.1'",
                                       getLocation());
    }

    private final String readXmlEncoding()
        throws IOException, WstxException
    {
        int c = checkKeyword(KW_ENC);
        if (c != CHAR_NULL) {
            reportUnexpectedChar(c, ERR_XMLDECL_KW_VERSION);
        }
        c = handleEq(KW_ENC);

        // Let's request 'normalization', upper-casing and some substitutions
	/* 22-Mar-2005, TSa: No, better not do modifications, since we do
	 *   need to be able to return the original String via API. We
	 *   can do loose comparison/substitutions when checking their
	 *   validity.
	 */
        //int len = readQuotedValue(mKeyword, c, true);
        int len = readQuotedValue(mKeyword, c, false);

        /* Hmmh. How about "too long" encodings? Maybe just truncate them,
         * for now?
         */
        if (len == 0) { // let's still detect missing value...
            throw new WstxParsingException("Missing XML encoding value for encoding pseudo-attribute",
                                           getLocation());
        }

        if (len < 0) {
            return new String(mKeyword);
        }
        return new String(mKeyword, 0, len);
    }

    private final String readXmlStandalone()
        throws IOException, WstxException
    {
        int c = checkKeyword(KW_SA);
        if (c != CHAR_NULL) {
            reportUnexpectedChar(c, ERR_XMLDECL_KW_STANDALONE);
        }
        c = handleEq(KW_SA);
        int len = readQuotedValue(mKeyword, c, false);

        if (len == 2) {
            if (mKeyword[0] == 'n' && mKeyword[1] == 'o') {
                return "no";
            }
        } else if (len == 3) {
            if (mKeyword[0] == 'y' && mKeyword[1] == 'e'
                && mKeyword[2] == 's') {
                return "yes";
            }
        }

        // Nope; error. -1 indicates run off...
        String got;

        if (len < 0) {
            got = "'"+new String(mKeyword)+"[..]'";
        } else if (len == 0) {
            got = "<empty>";
        } else {
            got = "'"+new String(mKeyword, 0, len)+"'";
        }

        throw new WstxParsingException("Invalid XML 'standalone' value "+got+"; expected 'yes' or 'no'",
                                       getLocation());
    }

    private final int handleEq(String attr)
        throws IOException, WstxException
    {
        int c = getNextAfterWs(false);
        if (c != '=') {
            reportUnexpectedChar(c, ERR_XMLDECL_EXP_EQ+"'"+attr+"'");
        }

        c = getNextAfterWs(false);
        if (c != '"' && c != '\'') {
            reportUnexpectedChar(c, ERR_XMLDECL_EXP_ATTRVAL+"'"+attr+"'");
        }
        return c;
    }

    /**
     * Method that should get next character, which has to be either specified
     * character (usually end marker), OR, any character as long as there'
     * at least one space character before it.
     */
    private final int getWsOrChar(int ok)
        throws IOException, WstxException
    {
        int c = getNext();
        if (c == ok) {
            return c;
        }
        if (c > CHAR_SPACE) {
            reportUnexpectedChar(c, "; expected either '"+((char) ok)+"' or white space");
        }
        if (c == CHAR_LF || c == CHAR_CR) {
            // Need to push it back to be processed properly
            pushback();
        }
        return getNextAfterWs(false);
    }

    /*
    ////////////////////////////////////////////////////////
    // Abstract parsing methods for sub-classes to implement
    ////////////////////////////////////////////////////////
    */

    protected abstract void pushback();

    protected abstract int getNext()
        throws IOException, WstxException;

    protected abstract int getNextAfterWs(boolean reqWs)
        throws IOException, WstxException;

    /**
     * @return First character that does not match expected, if any;
     *    CHAR_NULL if match succeeded
     */
    protected abstract int checkKeyword(String exp)
        throws IOException, WstxException;

    protected abstract int readQuotedValue(char[] kw, int quoteChar, boolean norm)
        throws IOException, WstxException;

    protected abstract Location getLocation();

    /*
    ////////////////////////////////////////////////////////
    // Package methods available to sub-classes:
    ////////////////////////////////////////////////////////
    */

    protected void reportNull()
        throws WstxException
    {
        Location loc = getLocation();
        throw new WstxException("Illegal null byte in input stream",
                                getLocation());
    }

    protected void reportXmlProblem(String msg)
        throws WstxException
    {
        throw new WstxParsingException(msg, getLocation());
    }

    protected void reportUnexpectedChar(int i, String msg)
        throws WstxException
    {
        char c = (char) i;
        String excMsg;

        // WTF? JDK thinks null char is just fine as?!
        if (Character.isISOControl(c)) {
            excMsg = "Unexpected character (CTRL-CHAR, code "+i+")"+msg;
        } else {
            excMsg = "Unexpected character '"+c+"' (code "+i+")"+msg;
        }
        Location loc = getLocation();
        throw new WstxUnexpectedCharException(excMsg, loc, c);
    }

    /*
    ////////////////////////////////////////
    // Other private methods:
    ////////////////////////////////////////
    */

    /*
    public static void main(String[] args)
        throws Exception
    {
    }
    */
}


