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

package com.ctc.wstx.sr;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Map;

import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.XMLStreamLocation2;
import org.codehaus.stax2.validation.XMLValidationException;
import org.codehaus.stax2.validation.XMLValidationProblem;

import com.ctc.wstx.api.ReaderConfig;
import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.cfg.InputConfigFlags;
import com.ctc.wstx.cfg.ParsingErrorMsgs;
import com.ctc.wstx.compat.JdkFeatures;
import com.ctc.wstx.ent.EntityDecl;
import com.ctc.wstx.exc.*;
import com.ctc.wstx.io.*;
import com.ctc.wstx.util.ExceptionUtil;
import com.ctc.wstx.util.SymbolTable;
import com.ctc.wstx.util.TextBuffer;

/**
 * Abstract base class that defines some basic functionality that all
 * Woodstox reader classes (main XML reader, DTD reader) extend from.
 */

public abstract class StreamScanner
    extends WstxInputData
    implements InputProblemReporter,
        InputConfigFlags, ParsingErrorMsgs
{
    // // // Some well-known chars:

    /**
     * Last (highest) char code of the three, LF, CR and NULL
     */
    public final static char CHAR_CR_LF_OR_NULL = (char) 13;

    public final static int INT_CR_LF_OR_NULL = 13;

    /**
     * Character that allows quick check of whether a char can potentially
     * be some kind of markup, WRT input stream processing;
     * has to contain linefeeds, &, < and > (">" only matters when
     * quoting text, as part of "]]>")
     */
    protected final static char CHAR_FIRST_PURE_TEXT = (char) ('>' + 1);


    /**
     * First character in Unicode (ie one with lowest id) that is legal
     * as part of a local name (all valid name chars minus ':'). Used
     * for doing quick check for local name end; usually name ends in
     * a whitespace or equals sign.
     */
    protected final static char CHAR_LOWEST_LEGAL_LOCALNAME_CHAR = '-';

    /*
    ////////////////////////////////////////////////////
    // Character validity constants, structs
    ////////////////////////////////////////////////////
     */

    /**
     * We will only use validity array for first 256 characters, mostly
     * because after those characters it's easier to do fairly simple
     * block checks.
     */
    private final static int VALID_CHAR_COUNT = 0x100;

    private final static byte NAME_CHAR_INVALID_B = (byte) 0;
    private final static byte NAME_CHAR_ALL_VALID_B = (byte) 1;
    private final static byte NAME_CHAR_VALID_NONFIRST_B = (byte) -1;

    private final static int NAME_CHAR_INVALID_I = (byte) 0;
    private final static int NAME_CHAR_ALL_VALID_I = (byte) 1;
    private final static int NAME_CHAR_VALID_NONFIRST_I = (byte) -1;

    private final static byte[] sCharValidity = new byte[VALID_CHAR_COUNT];

    static {
        /* First, since all valid-as-first chars are also valid-as-other chars,
         * we'll initialize common chars:
         */
        sCharValidity['_'] = NAME_CHAR_ALL_VALID_B;
        for (int i = 0, last = ('z' - 'a'); i <= last; ++i) {
            sCharValidity['A' + i] = NAME_CHAR_ALL_VALID_B;
            sCharValidity['a' + i] = NAME_CHAR_ALL_VALID_B;
        }
        for (int i = 0xC0; i < 0xF6; ++i) { // not all are fully valid, but
            sCharValidity[i] = NAME_CHAR_ALL_VALID_B;
        }
        // ... now we can 'revert' ones not fully valid:
        sCharValidity[0xD7] = NAME_CHAR_INVALID_B;
        sCharValidity[0xF7] = NAME_CHAR_INVALID_B;

        /* And then we can proceed with ones only valid-as-other.
         */
        sCharValidity['-'] = NAME_CHAR_VALID_NONFIRST_B;
        sCharValidity['.'] = NAME_CHAR_VALID_NONFIRST_B;
        sCharValidity[0xB7] = NAME_CHAR_VALID_NONFIRST_B;
        for (int i = '0'; i <= '9'; ++i) {
            sCharValidity[i] = NAME_CHAR_VALID_NONFIRST_B;
        }
    }

    /**
     * Public identifiers only use 7-bit ascii range.
     */
    private final static int VALID_PUBID_CHAR_COUNT = 0x80;
    private final static byte[] sPubidValidity = new byte[VALID_PUBID_CHAR_COUNT];
    private final static byte PUBID_CHAR_INVALID_B = (byte) 0;
    private final static byte PUBID_CHAR_VALID_B = (byte) 1;
    static {
        for (int i = 0, last = ('z' - 'a'); i <= last; ++i) {
            sPubidValidity['A' + i] = PUBID_CHAR_VALID_B;
            sPubidValidity['a' + i] = PUBID_CHAR_VALID_B;
        }
        for (int i = '0'; i <= '9'; ++i) {
            sPubidValidity[i] = PUBID_CHAR_VALID_B;
        }

        // 3 main white space types are valid
        sPubidValidity[0x0A] = PUBID_CHAR_VALID_B;
        sPubidValidity[0x0D] = PUBID_CHAR_VALID_B;
        sPubidValidity[0x20] = PUBID_CHAR_VALID_B;

        // And many of punctuation/separator ascii chars too:
        sPubidValidity['-'] = PUBID_CHAR_VALID_B;
        sPubidValidity['\''] = PUBID_CHAR_VALID_B;
        sPubidValidity['('] = PUBID_CHAR_VALID_B;
        sPubidValidity[')'] = PUBID_CHAR_VALID_B;
        sPubidValidity['+'] = PUBID_CHAR_VALID_B;
        sPubidValidity[','] = PUBID_CHAR_VALID_B;
        sPubidValidity['.'] = PUBID_CHAR_VALID_B;
        sPubidValidity['/'] = PUBID_CHAR_VALID_B;
        sPubidValidity[':'] = PUBID_CHAR_VALID_B;
        sPubidValidity['='] = PUBID_CHAR_VALID_B;
        sPubidValidity['?'] = PUBID_CHAR_VALID_B;
        sPubidValidity[';'] = PUBID_CHAR_VALID_B;
        sPubidValidity['!'] = PUBID_CHAR_VALID_B;
        sPubidValidity['*'] = PUBID_CHAR_VALID_B;
        sPubidValidity['#'] = PUBID_CHAR_VALID_B;
        sPubidValidity['@'] = PUBID_CHAR_VALID_B;
        sPubidValidity['$'] = PUBID_CHAR_VALID_B;
        sPubidValidity['_'] = PUBID_CHAR_VALID_B;
        sPubidValidity['%'] = PUBID_CHAR_VALID_B;
    }

    /*
    ////////////////////////////////////////////////////
    // Basic configuration
    ////////////////////////////////////////////////////
     */

    /**
     * Copy of the configuration object passed by the factory.
     * Contains immutable settings for this reader (or in case
     * of DTD parsers, reader that uses it)
     */
    protected final ReaderConfig mConfig;

    // // // Various extracted settings:

    /**
     * If true, Reader is namespace aware, and should do basic checks
     * (usually enforcing limitations on having colons in names)
     */
    protected final boolean mCfgNsEnabled;

    // Extracted standard on/off settings:

    /**
     * note: left non-final on purpose: sub-class may need to modify
     * the default value after construction.
     */
    protected boolean mCfgReplaceEntities;

    /*
    ////////////////////////////////////////////////////
    // Symbol handling, if applicable
    ////////////////////////////////////////////////////
     */

    final SymbolTable mSymbols;

    /**
     * Local full name for the event, if it has one (note: element events
     * do NOT use this variable; those names are stored in element stack):
     * target for processing instructions.
     *<p>
     * Currently used for proc. instr. target, and entity name (at least
     * when current entity reference is null).
     *<p>
     * Note: this variable is generally not cleared, since it comes from
     * a symbol table, ie. this won't be the only reference.
     */
    protected String mCurrName;

    /*
    ////////////////////////////////////////////////////
    // Input handling
    ////////////////////////////////////////////////////
     */

    /**
     * Currently active input source; contains link to parent (nesting) input
     * sources, if any.
     */
    protected WstxInputSource mInput;

    /**
     * Top-most input source this reader can use; due to input source
     * chaining, this is not necessarily the root of all input; for example,
     * external DTD subset reader's root input still has original document
     * input as its parent.
     */
    protected final WstxInputSource mRootInput;

    /**
     * Custom resolver used to handle external entities that are to be expanded
     * by this reader (external param/general entity expander)
     */
    XMLResolver mEntityResolver = null;

    /*
    ////////////////////////////////////////////////////
    // Buffer(s) for local name(s) and text content
    ////////////////////////////////////////////////////
     */

    /**
     * Temporary buffer used if local name can not be just directly
     * constructed from input buffer (name is on a boundary or such).
     */
    protected char[] mNameBuffer = null;

    /**
     * TextBuffer mostly used to collect non-element text content; needs
     * to be accessible here to make sure synchronized sharing with input
     * is safe.
     */
    protected TextBuffer mTextBuffer;

    /*
    ////////////////////////////////////////////////////
    // Information about starting location of event
    // Reader is pointing to; updated on-demand
    ////////////////////////////////////////////////////
     */

    // // // Location info at point when current token was started

    /**
     * Total number of characters read before start of current token.
     * For big (gigabyte-sized) sizes are possible, needs to be long,
     * unlike pointers and sizes related to in-memory buffers.
     */
    protected long mTokenInputTotal = 0; 

    /**
     * Input row on which current token starts, 1-based
     */
    protected int mTokenInputRow = 1;

    /**
     * Column on input row that current token starts; 0-based (although
     * in the end it'll be converted to 1-based)
     */
    protected int mTokenInputCol = 0;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////////////////
     */

    /**
     * Constructor used when creating a complete new (main-level) reader that
     * does not share its input buffers or state with another reader.
     */
    protected StreamScanner(WstxInputSource input, ReaderConfig cfg,
                            XMLResolver res)
    {
        super();
        mInput = input;
        // 17-Jun-2004, TSa: Need to know root-level input source
        mRootInput = input;

        mConfig = cfg;
        mSymbols = cfg.getSymbols();
        int cf = cfg.getConfigFlags();
        mCfgNsEnabled = (cf & CFG_NAMESPACE_AWARE) != 0;
        mCfgReplaceEntities = (cf & CFG_REPLACE_ENTITY_REFS) != 0;

        mInputBuffer = null;
        mInputPtr = mInputLen = 0;
        mEntityResolver = res;
    }

    /*
    ////////////////////////////////////////////////////
    // Package API
    ////////////////////////////////////////////////////
     */

    /**
     * Method that returns location of the last character returned by this
     * reader; that is, location "one less" than the currently pointed to
     * location.
     */
    protected WstxInputLocation getLastCharLocation()
    {
        return mInput.getLocation(mCurrInputProcessed + mInputPtr - 1,
                                  mCurrInputRow,
                                  mInputPtr - mCurrInputRowStart);
    }

    protected URL getSource() {
        return mInput.getSource();
    }

    protected String getSystemId() {
        return mInput.getSystemId();
    }

    /*
    ///////////////////////////////////////////////////////
    // Partial LocationInfo implementation (not implemented
    // by this base class, but is by some sub-classes)
    ///////////////////////////////////////////////////////
     */

    /**
     * Returns location of last properly parsed token; as per StAX specs,
     * apparently needs to be the end of current event, which is the same
     * as the start of the following event (or EOF if that's next).
     */
    public abstract Location getLocation();

    public XMLStreamLocation2 getStartLocation()
    {
        // note: +1 is used as columns are 1-based...
        return mInput.getLocation(mTokenInputTotal, mTokenInputRow,
                                  mTokenInputCol + 1);
    }

    public XMLStreamLocation2 getCurrentLocation()
    {
        return mInput.getLocation(mCurrInputProcessed + mInputPtr,
                                  mCurrInputRow,
                                  mInputPtr - mCurrInputRowStart + 1);
    }

    /*
    ////////////////////////////////////////////////////
    // InputProblemReporter implementation
    ////////////////////////////////////////////////////
     */

    /**
     * Throws generic parse error with specified message and current parsing
     * location.
     *<p>
     * Note: public access only because core code in other packages needs
     * to access it.
     */
    public void throwParseError(String msg)
        throws WstxException
    {
        throw new WstxParsingException(msg, getLastCharLocation());
    }

    public void throwParseError(String format, Object arg)
        throws WstxException
    {
        String msg = MessageFormat.format(format, new Object[] { arg });
        throw new WstxParsingException(msg, getLastCharLocation());
    }

    public void throwParseError(String format, Object arg, Object arg2)
        throws WstxException
    {
        String msg = MessageFormat.format(format, new Object[] { arg, arg2 });
        throw new WstxParsingException(msg, getLastCharLocation());
    }

    public void reportProblem(String probType, String msg)
    {
        doReportProblem(mConfig.getXMLReporter(), probType, msg, null);
    }

    public void reportProblem(String probType, String format, Object arg)
    {
        XMLReporter rep = mConfig.getXMLReporter();
        if (rep != null) {
            doReportProblem(rep, probType,
                            MessageFormat.format(format, new Object[] { arg }),
                            null);
        }
    }

    public void reportProblem(String probType, String format, Object arg,
                              Object arg2)
    {
        XMLReporter rep = mConfig.getXMLReporter();
        if (rep != null) {
            doReportProblem(rep, probType,
                            MessageFormat.format(format, new Object[] { arg, arg2 }),
                            null);
        }
    }

    public void reportProblem(String probType, String format, Object arg,
                              Object arg2, Location loc)
    {
        XMLReporter rep = mConfig.getXMLReporter();
        if (rep != null) {
            doReportProblem(rep, probType,
                            MessageFormat.format(format, new Object[] { arg, arg2 }),
                            loc);
        }
    }

    /**
     *<p>
     * Note: this is the base implementation used for implementing
     * <code>ValidationContext</code>
     */
    public void reportValidationProblem(XMLValidationProblem prob)
        throws XMLValidationException
    {
        // !!! TBI: Fail-fast vs. deferred modes

        /* For now let's implement basic functionality: warnings get
         * reported via XMLReporter, errors and fatal errors result in
         * immediate exceptions.
         */
        if (prob.getSeverity() >= XMLValidationProblem.SEVERITY_ERROR) {
            throw WstxValidationException.create(prob);
        }
        XMLReporter rep = mConfig.getXMLReporter();
        if (rep != null) {
            doReportProblem(rep, ErrorConsts.WT_VALIDATION, prob.getMessage(),
                            prob.getLocation());
        }
    }

    public void reportValidationProblem(String msg, Location loc, int severity)
        throws XMLValidationException
    {
        reportValidationProblem(new XMLValidationProblem(loc, msg, severity));
    }

    public void reportValidationProblem(String msg, int severity)
        throws XMLValidationException
    {
        reportValidationProblem(new XMLValidationProblem(getLastCharLocation(),
                                                         msg, severity));
    }

    public void reportValidationProblem(String msg)
        throws XMLValidationException
    {
        reportValidationProblem(new XMLValidationProblem(getLastCharLocation(),
                                                         msg,
                                                         XMLValidationProblem.SEVERITY_ERROR));
    }

    public void reportValidationProblem(Location loc, String msg)
        throws XMLValidationException
    {
        reportValidationProblem(new XMLValidationProblem(getLastCharLocation(),
                                                         msg));
    }

    public void reportValidationProblem(String format, Object arg)
        throws XMLValidationException
    {
        String msg = MessageFormat.format(format, new Object[] { arg });
        reportValidationProblem(new XMLValidationProblem(getLastCharLocation(),
                                                         msg));
    }

    public void reportValidationProblem(String format, Object arg, Object arg2)
        throws XMLValidationException
    {
        String msg = MessageFormat.format(format, new Object[] { arg, arg2 });
        reportValidationProblem(new XMLValidationProblem(getLastCharLocation(),
                                                         msg));
    }

    protected final void doReportProblem(XMLReporter rep, String probType,
                                         String msg, Location loc)
    {
        if (rep != null) {
            if (loc == null) {
                loc = getLastCharLocation();
            }
            try {
                rep.report(msg, probType, null, loc);
            } catch (XMLStreamException e) {
                // Hmmh. Weird that a reporter is allowed to do this...
                System.err.println("Problem reporting a problem: "+e);
            }
        }
    }

    /*
    ////////////////////////////////////////////////////
    // Other error reporting methods
    ////////////////////////////////////////////////////
     */

    protected void throwUnexpectedChar(int i, String msg)
        throws WstxException
    {
        char c = (char) i;
        String excMsg = "Unexpected character "+getCharDesc(c)+msg;
        WstxInputLocation loc = getLastCharLocation();
        throw new WstxUnexpectedCharException(excMsg, loc, c);
    }

    protected void throwNullChar()
        throws WstxException
    {
        WstxInputLocation loc = getLastCharLocation();
        throw new WstxUnexpectedCharException("Illegal character (NULL, unicode 0) encountered: not valid in any context", loc, CHAR_NULL);
    }

    protected void throwUnexpectedEOF(String msg)
        throws WstxException
    {
        throw new WstxEOFException("Unexpected EOF"
                                   +(msg == null ? "" : msg),
                                   getLastCharLocation());
    }

    /**
     * Similar to {@link #throwUnexpectedEOF}, but only indicates ending
     * of an input block. Used when reading a token that can not span
     * input block boundaries (ie. can not continue past end of an
     * entity expansion).
     */
    protected void throwUnexpectedEOB(String msg)
        throws WstxException
    {
        throw new WstxEOFException("Unexpected end of input block"
                                   +(msg == null ? "" : msg),
                                   getLastCharLocation());
    }

    protected void throwFromIOE(IOException ioe)
        throws WstxException
    {
        throw new WstxIOException(ioe);
    }

    protected void throwFromStrE(XMLStreamException strex)
        throws WstxException
    {
        if (strex instanceof WstxException) {
            throw (WstxException) strex;
        }
        WstxException newEx = new WstxException(strex);
        JdkFeatures.getInstance().setInitCause(newEx, strex);
        throw newEx;
    }

    /**
     * Method called to report an error, when caller's signature only
     * allows runtime exceptions to be thrown.
     */
    protected void throwLazyError(Exception e)
    {
        if (e instanceof XMLStreamException) {
            WstxLazyException.throwLazily((XMLStreamException) e);
        }
        ExceptionUtil.throwRuntimeException(e);
    }

    protected String tokenTypeDesc(int type)
    {
        return ErrorConsts.tokenTypeDesc(type);
    }

    /*
    ////////////////////////////////////////////////////
    // Input buffer handling
    ////////////////////////////////////////////////////
     */

    /**
     * Returns current input source this source uses.
     *<p>
     * Note: public only because some implementations are on different
     * package.
     */
    public final WstxInputSource getCurrentInput() {
        return mInput;
    }

    protected final int inputInBuffer() {
        return mInputLen - mInputPtr;
    }

    protected final int getNext()
        throws IOException, WstxException
    {
        if (mInputPtr >= mInputLen) {
            if (!loadMore()) {
                return -1;
            }
        }
        return (int) mInputBuffer[mInputPtr++];
    }

    protected final char getNextChar(String errorMsg)
        throws IOException, WstxException
    {
        if (mInputPtr >= mInputLen) {
            loadMore(errorMsg);
        }
        return mInputBuffer[mInputPtr++];
    }

    /**
     * Similar to {@link #getNextChar}, but will not read more characters
     * from parent input source(s) if the current input source doesn't
     * have more content. This is often needed to prevent "runaway" content,
     * such as comments that start in an entity but do not have matching
     * close marker inside entity; XML specification specifically states
     * such markup is not legal.
     */
    protected final char getNextCharFromCurrent(String errorMsg)
        throws IOException, WstxException
    {
        if (mInputPtr >= mInputLen) {
            loadMoreFromCurrent(errorMsg);
        }
        return mInputBuffer[mInputPtr++];
    }

    /**
     * Similar to {@link #getNext}, but does not advance pointer
     * in input buffer.
     *<p>
     * Note: this method only peeks within current input source;
     * it does not close it and check nested input source (if any).
     * This because that's never the desired behaviour (if such
     * behaviour is needed, have to create a new method).
     */
    protected final int peekNext()
        throws IOException, WstxException
    {
        if (mInputPtr >= mInputLen) {
            if (!loadMoreFromCurrent()) {
                return -1;
            }
        }
        return (int) mInputBuffer[mInputPtr];
    }

    /**
     * Method that will completely skip and ignore zero or more white space
     * characters, and return next character (or EOF marker) after white
     * space.
     */
    protected final int getNextAfterWS(char c)
        throws IOException, WstxException
    {
        do {
            // Linefeed?
            if (c == '\n' || c == '\r') {
                skipCRLF(c);
            } else if (c == CHAR_NULL) {
                throwNullChar();
            }

            // Still a white space?
            if (mInputPtr >= mInputLen) {
                if (!loadMore()) {
                    return -1;
                }
            }
            c = mInputBuffer[mInputPtr++];
        } while (c <= CHAR_SPACE);

        return (int) c;
    }

    protected final char getNextCharAfterWS(char c, String errorMsg)
        throws IOException, WstxException
    {
        do {
            // Linefeed?
            if (c == '\n' || c == '\r') {
                skipCRLF(c);
            } else if (c == CHAR_NULL) {
                throwNullChar();
            }

            // Still a white space?
            if (mInputPtr >= mInputLen) {
                if (!loadMore()) {
                    throwUnexpectedEOF(errorMsg);
                }
            }
            c = mInputBuffer[mInputPtr++];
        } while (c <= CHAR_SPACE);

        return c;
    }

    protected final int getNextAfterWS()
        throws IOException, WstxException
    {
        if (mInputPtr >= mInputLen) {
            if (!loadMore()) {
                return -1;
            }
        }
        char c = mInputBuffer[mInputPtr++];
        if (c <= CHAR_SPACE) {
            return getNextAfterWS(c);
        }
        return (int) c;
    }

    protected final char getNextCharAfterWS(String errorMsg)
        throws IOException, WstxException
    {
        if (mInputPtr >= mInputLen) {
            loadMore(errorMsg);
        }

        char c = mInputBuffer[mInputPtr++];
        while (c <= CHAR_SPACE) {
            // Linefeed?
            if (c == '\n' || c == '\r') {
                skipCRLF(c);
            } else if (c == CHAR_NULL) {
                throwNullChar();
            }

            // Still a white space?
            if (mInputPtr >= mInputLen) {
                loadMore(errorMsg);
            }
            c = mInputBuffer[mInputPtr++];
        }
        return c;
    }

    protected final char getNextInCurrAfterWS(String errorMsg)
        throws IOException, WstxException
    {
        return getNextInCurrAfterWS(errorMsg, getNextCharFromCurrent(errorMsg));
    }

    protected final char getNextInCurrAfterWS(String errorMsg, char c)
        throws IOException, WstxException
    {
        while (c <= CHAR_SPACE) {
            // Linefeed?
            if (c == '\n' || c == '\r') {
                skipCRLF(c);
            } else if (c == CHAR_NULL) {
                throwNullChar();
            }

            // Still a white space?
            if (mInputPtr >= mInputLen) {
                loadMoreFromCurrent(errorMsg);
            }
            c = mInputBuffer[mInputPtr++];
        }
        return c;
    }
    
    /**
     * Method that will skip any white space potentially coming from the
     * current input source, without returning next character. Note that
     * it does NOT continue to the next input source, in case of a
     * nested input source (like entity expansion).
     */
    protected final void skipWS() 
        throws IOException, WstxException
    {
        while (true) {
            if (mInputPtr >= mInputLen) {
                // Let's see if current source has more
                if (!loadMoreFromCurrent()) {
                    return;
                }
            }
            char c = mInputBuffer[mInputPtr];
            if (c > CHAR_SPACE) { // not WS? Need to return
                break;
            }
            ++mInputPtr;

            // Linefeed?
            if (c == '\n' || c == '\r') {
                skipCRLF(c);
            } else if (c == CHAR_NULL) {
                throwNullChar();
            }
        }
    }

    /**
     * Method called when a CR has been spotted in input; checks if next
     * char is LF, and if so, skips it. Note that next character has to
     * come from the current input source, to qualify; it can never come
     * from another (nested) input source.
     *
     * @return True, if passed in char is '\r' and next one is '\n'.
     */
    protected final boolean skipCRLF(char c) 
        throws IOException, WstxException
    {
        boolean result;

        if (c == '\r' && peekNext() == '\n') {
            ++mInputPtr;
            result = true;
        } else {
            result = false;
        }
        ++mCurrInputRow;
        mCurrInputRowStart = mInputPtr;
        return result;
    }

    protected final void markLF() {
        ++mCurrInputRow;
        mCurrInputRowStart = mInputPtr;
    }

    protected final void markLF(int inputPtr) {
        ++mCurrInputRow;
        mCurrInputRowStart = inputPtr;
    }

    /**
     * Method to push back last character read; can only be called once,
     * that is, no more than one char can be guaranteed to be succesfully
     * returned.
     */
    protected final void pushback() { --mInputPtr; }

    /*
    ////////////////////////////////////////////////////
    // Sub-class overridable input handling methods
    ////////////////////////////////////////////////////
     */

    /**
     * Method called when an entity has been expanded (new input source
     * has been created). Needs to initialize location information and change
     * active input source.
     */
    protected void initInputSource(WstxInputSource newInput, boolean isExt)
        throws IOException, XMLStreamException
    {
        mInput = newInput;
        // Let's make sure new input will be read next time input is needed:
        mInputPtr = 0;
        mInputLen = 0;
        /* Plus, reset the input location so that'll be accurate for
         * error reporting etc.
         */
        mInput.initInputLocation(this);

        /* Then, for external (parsed) entities, may need to skip the xml
         * declaration; this can and should be done before calling init, 
         * since init will update
         */
        if (isExt) {
            // 13-Aug-2004, TSa: Nope; bootstrappers get rid of them...
        }
    }

    /**
     * @return true if reading succeeded (or may succeed), false if
     *   we reached EOF.
     */
    protected boolean loadMore()
        throws IOException, WstxException
    {
        WstxInputSource input = mInput;
        do {
            /* Need to make sure offsets are properly updated for error
             * reporting purposes, and do this now while previous amounts
             * are still known.
             */
            mCurrInputProcessed += mInputLen;
            mCurrInputRowStart -= mInputLen;
            int count = input.readInto(this);
            if (count > 0) {
                return true;
            }
            input.close();
            if (input == mRootInput) {
                return false;
            }
            WstxInputSource parent = input.getParent();
            if (parent == null) { // sanity check!
                throw new Error("Internal error: null parent for input source '"
                                +input+"'; should never occur (should have stopped at root input '"+mRootInput+"'.");
            }

            mInput = input = parent;
            input.restoreContext(this);
            // Maybe there are leftovers from that input in buffer now?
        } while (mInputPtr >= mInputLen);

        return true;
    }

    protected final boolean loadMore(String errorMsg)
        throws WstxException, IOException
    {
        if (!loadMore()) {
            throwUnexpectedEOF(errorMsg);
        }
        return true;
    }

    protected boolean loadMoreFromCurrent()
        throws IOException, WstxException
    {
        // Need to update offsets properly
        mCurrInputProcessed += mInputLen;
        mCurrInputRowStart -= mInputLen;
        int count = mInput.readInto(this);
        return (count > 0);
    }

    protected final boolean loadMoreFromCurrent(String errorMsg)
        throws WstxException, IOException
    {
        if (!loadMoreFromCurrent()) {
            throwUnexpectedEOB(errorMsg);
        }
        return true;
    }

    /**
     * Method called to make sure current main-level input buffer has at
     * least specified number of characters available consequtively,
     * without having to call {@link #loadMore}. It can only be called
     * when input comes from main-level buffer; further, call can shift
     * content in input buffer, so caller has to flush any data still
     * pending. In short, caller has to know exactly what it's doing. :-)
     *<p>
     * Note: method does not check for any other input sources than the
     * current one -- if current source can not fulfill the request, a
     * failure is indicated.
     *
     * @return true if there's now enough data; false if not (EOF)
     */
    protected boolean ensureInput(int minAmount)
        throws IOException
    {
        int currAmount = mInputLen - mInputPtr;
        if (currAmount >= minAmount) {
            return true;
        }
        return mInput.readMore(this, minAmount);
    }

    /*
    ////////////////////////////////////////////////////
    // Entity resolution
    ////////////////////////////////////////////////////
     */

    /**
     * Method that tries to resolve a character entity, or (if caller so
     * specifies), a pre-defined internal entity (lt, gt, amp, apos, quot).
     * It will succeed iff:
     * <ol>
     *  <li>Entity in question is a simple character entity (either one of
     *    5 pre-defined ones, or using decimal/hex notation), AND
     *   <li>
     *  <li>Entity fits completely inside current input buffer.
     *   <li>
     * </ol>
     * If so, character value of entity is returned. Character 0 is returned
     * otherwise; if so, caller needs to do full resolution.
     *<p>
     * Note: On entry we are guaranteed there are at least 3 more characters
     * in this buffer; otherwise we shouldn't be called.
     *
     * @param checkStd If true, will check pre-defined internal entities
     *   (gt, lt, amp, apos, quot); if false, will only check actual
     *   character entities.
     *
     * @return (Valid) character value, if entity is a character reference,
     *   and could be resolved from current input buffer (does not span
     *   buffer boundary); null char (code 0) if not (either non-char
     *   entity, or spans input buffer boundary).
     */
    protected char resolveSimpleEntity(boolean checkStd)
        throws WstxException
    {
        char[] buf = mInputBuffer;
        int ptr = mInputPtr;
        char c = buf[ptr++];

        // Numeric reference?
        if (c == '#') {
            c = buf[ptr++];
            int value = 0;
            int inputLen = mInputLen;
            if (c == 'x') { // hex
                while (ptr < inputLen) {
                    c = buf[ptr++];
                    if (c == ';') {
                        break;
                    }
                    value = value << 4;
                    if (c <= '9' && c >= '0') {
                        value += (c - '0');
                    } else if (c >= 'a' && c <= 'z') {
                        value += (10 + (c - 'a'));
                    } else if (c >= 'A' && c <= 'Z') {
                        value += (10 + (c - 'A'));
                    } else {
                        mInputPtr = ptr; // so error points to correct char
                        throwUnexpectedChar(c, "; expected a hex number (0-9a-zA-Z).");
                    }
                }
            } else { // numeric (decimal)
                while (c != ';') {
                    if (c <= '9' && c >= '0') {
                        value = (value * 10) + (c - '0');
                    } else {
                        mInputPtr = ptr; // so error points to correct char
                        throwUnexpectedChar(c, "; expected a decimal number.");
                    }
                    if (ptr >= inputLen) {
                        break;
                    }
                    c = buf[ptr++];
                }
            }
            /* We get here either if we got it all, OR if we ran out of
             * input in current buffer.
             */
            if (c == ';') { // got the full thing
                if (value == 0) {
                    throwParseError("Invalid character reference -- null character not allowed in XML content.");
                }
                mInputPtr = ptr;
                return (char) value;
            }

            /* If we ran out of input, need to just fall back, gets
             * resolved via 'full' resolution mechanism.
             */

        } else if (checkStd) {
            /* Caller may not want to resolve these quite yet...
             * (when it wants separate events for non-char entities)
             */
            if (c == 'a') { // amp or apos?
                c = buf[ptr++];
                
                if (c == 'm') { // amp?
                    if (buf[ptr++] == 'p') {
                        if (ptr < mInputLen && buf[ptr++] == ';') {
                            mInputPtr = ptr;
                            return '&';
                        }
                    }
                } else if (c == 'p') { // apos?
                    if (buf[ptr++] == 'o') {
                        int len = mInputLen;
                        if (ptr < len && buf[ptr++] == 's') {
                            if (ptr < len && buf[ptr++] == ';') {
                                mInputPtr = ptr;
                                return '\'';
                            }
                        }
                    }
                }
            } else if (c == 'g') { // gt?
                if (buf[ptr++] == 't' && buf[ptr++] == ';') {
                    mInputPtr = ptr;
                    return '>';
                }
            } else if (c == 'l') { // lt?
                if (buf[ptr++] == 't' && buf[ptr++] == ';') {
                    mInputPtr = ptr;
                    return '<';
                }
            } else if (c == 'q') { // quot?
                if (buf[ptr++] == 'u' && buf[ptr++] == 'o') {
                    int len = mInputLen;
                    if (ptr < len && buf[ptr++] == 't') {
                        if (ptr < len && buf[ptr++] == ';') {
                            mInputPtr = ptr;
                            return '"';
                        }
                    }
                }
            }
        }
        return CHAR_NULL;
    }

    /**
     * Method called to resolve character entities, and only character
     * entities (except that pre-defined char entities -- amp, apos, lt,
     * gt, quote -- MAY be "char entities" in this sense, depending on
     * arguments).
     * Otherwise it is to return the null char; if so,
     * the input pointer will point to the same point as when method
     * entered (char after ampersand), plus the ampersand itself is
     * guaranteed to be in the input buffer (so caller can just push
     * back it if necessary).
     *<p>
     * Most often this method is called when reader is not to expand
     * non-char entities automatically, but to return them as separate
     * events.
     *<p>
     * Main complication here is that we need to do 5-char lookahead. This
     * is problematic if chars are on input buffer boundary. This is ok
     * for the root level input buffer, but not for some nested buffers.
     * However, according to XML specs, such split entities are actually
     * illegal... so we can throw an exception in those cases.
     *
     * @param checkStd If true, will check pre-defined internal entities
     *   (gt, lt, amp, apos, quot) as character entities; if false, will only
     *   check actual 'real' character entities.
     *
     * @return (Valid) character value, if entity is a character reference,
     *   and could be resolved from current input buffer (does not span
     *   buffer boundary); null char (code 0) if not (either non-char
     *   entity, or spans input buffer boundary).
     */
    protected char resolveCharOnlyEntity(boolean checkStd)
        throws IOException, WstxException
    {
        //int avail = inputInBuffer();
        int avail = mInputLen - mInputPtr;
        if (avail < 6) {
            // split entity, or buffer boundary
            /* Don't want to lose leading '&' (in case we can not expand
             * the entity), so let's push it back first
             */
            --mInputPtr;
            /* Shortest valid reference would be 3 chars ('&a;'); which
             * would only be legal from an expanded entity...
             */
            if (!ensureInput(6)) {
                avail = inputInBuffer();
                if (avail < 3) {
                    throwUnexpectedEOF(SUFFIX_IN_ENTITY_REF);
                }
            } else {
                avail = 6;
            }
            // ... and now we can move pointer back as well:
            ++mInputPtr;
        }

        /* Ok, now we have one more character to check, and that's enough
         * to determine type decisively.
         */
        char c = mInputBuffer[mInputPtr];

        // A char reference?
        if (c == '#') { // yup
            ++mInputPtr;
            return resolveCharEnt();
        }

        // nope... except may be a pre-def?
        if (checkStd) {
            if (c == 'a') {
                char d = mInputBuffer[mInputPtr+1];
                if (d == 'm') {
                    if (avail >= 4
                        && mInputBuffer[mInputPtr+2] == 'p'
                        && mInputBuffer[mInputPtr+3] == ';') {
                        mInputPtr += 4;
                        return '&';
                    }
                } else if (d == 'p') {
                    if (avail >= 5
                        && mInputBuffer[mInputPtr+2] == 'o'
                        && mInputBuffer[mInputPtr+3] == 's'
                        && mInputBuffer[mInputPtr+4] == ';') {
                        mInputPtr += 5;
                        return '\'';
                    }
                }
            } else if (c == 'l') {
                if (avail >= 3
                    && mInputBuffer[mInputPtr+1] == 't'
                    && mInputBuffer[mInputPtr+2] == ';') {
                    mInputPtr += 3;
                    return '<';
                }
            } else if (c == 'g') {
                if (avail >= 3
                    && mInputBuffer[mInputPtr+1] == 't'
                    && mInputBuffer[mInputPtr+2] == ';') {
                    mInputPtr += 3;
                    return '>';
                }
            } else if (c == 'q') {
                if (avail >= 5
                    && mInputBuffer[mInputPtr+1] == 'u'
                    && mInputBuffer[mInputPtr+2] == 'o'
                    && mInputBuffer[mInputPtr+3] == 't'
                    && mInputBuffer[mInputPtr+4] == ';') {
                    mInputPtr += 5;
                    return '"';
                }
            }
        }
        return CHAR_NULL;
    }

    /**
     * Reverse of {@link #resolveCharOnlyEntity}; will only resolve entity
     * if it is NOT a character entity (or pre-defined 'generic' entity;
     * amp, apos, lt, gt or quot). Only used in cases where entities
     * are to be separately returned unexpanded (in non-entity-replacing
     * mode); which means it's never called from dtd handler.
     */
    protected EntityDecl resolveNonCharEntity(Map ent1, Map ent2)
        throws IOException, WstxException
    {
        //int avail = inputInBuffer();
        int avail = mInputLen - mInputPtr;
        if (avail < 6) {
            // split entity, or buffer boundary
            /* Don't want to lose leading '&' (in case we can not expand
             * the entity), so let's push it back first
             */
            --mInputPtr;

            /* !!! Hmmh, need to rewrite:
            if (mInput.getParent() != null) { // main level, ok
                throwParseError("Entity not completely defined in included entity (starting '&' in entity expansion; entity identifier outside expansion).");
            }
            */

            /* Shortest valid reference would be 3 chars ('&a;'); which
             * would only be legal from an expanded entity...
             */
            if (!ensureInput(6)) {
                avail = inputInBuffer();
                if (avail < 3) {
                    throwUnexpectedEOF(SUFFIX_IN_ENTITY_REF);
                }
            } else {
                avail = 6;
            }
            // ... and now we can move pointer back as well:
            ++mInputPtr;
        }

        // We don't care about char entities:
        char c = mInputBuffer[mInputPtr];
        if (c == '#') {
            return null;
        }

        EntityDecl ed;

        /* 19-Aug-2004, TSa: Need special handling for pre-defined
         *   entities; they are not counted as 'real' general parsed
         *   entities, but more as character entities...
         */

        // have chars at least up to mInputPtr+4 by now
        if (c == 'a') {
            char d = mInputBuffer[mInputPtr+1];
            if (d == 'm') {
                if (avail >= 4
                    && mInputBuffer[mInputPtr+2] == 'p'
                    && mInputBuffer[mInputPtr+3] == ';') {
                    // If not automatically expanding:
                    //return sEntityAmp;
                    // mInputPtr += 4;
                    return null;
                }
            } else if (d == 'p') {
                if (avail >= 5
                    && mInputBuffer[mInputPtr+2] == 'o'
                    && mInputBuffer[mInputPtr+3] == 's'
                    && mInputBuffer[mInputPtr+4] == ';') {
                    return null;
                }
            }
        } else if (c == 'l') {
            if (avail >= 3
                && mInputBuffer[mInputPtr+1] == 't'
                && mInputBuffer[mInputPtr+2] == ';') {
                return null;
            }
        } else if (c == 'g') {
            if (avail >= 3
                && mInputBuffer[mInputPtr+1] == 't'
                && mInputBuffer[mInputPtr+2] == ';') {
                return null;
            }
        } else if (c == 'q') {
            if (avail >= 5
                && mInputBuffer[mInputPtr+1] == 'u'
                && mInputBuffer[mInputPtr+2] == 'o'
                && mInputBuffer[mInputPtr+3] == 't'
                && mInputBuffer[mInputPtr+4] == ';') {
                return null;
            }
        }

        // Otherwise, let's just parse in generic way:
        ++mInputPtr; // since we already read the first letter
        String id = parseEntityName(c);
        mCurrName = id;

        if (ent1 != null) {
            ed = (EntityDecl) ent1.get(id);
        } else {
            ed = null;
        }
        if (ed == null) {
            if (ent2 != null) {
                ed = (EntityDecl) ent2.get(id);
            }
        }
        /* No need for null checks -- only called in non-expanding mode,
         * when it's ok to return null to signal an undeclared entity
         */

        return ed;
    }

    /**
     * Method that does full resolution of an entity reference, be it
     * character entity, internal entity or external entity, including
     * updating of input buffers, and depending on whether result is
     * a character entity (or one of 5 pre-defined entities), returns
     * char in question, or null character (code 0) to indicate it had
     * to change input source.
     *
     * @param ent1
     * @param ent2
     * @param allowExt If true, is allowed to expand external entities
     *   (expanding text); if false, is not (expanding attribute value).
     *
     * @return Either single-character replacement (which is NOT to be
     *    reparsed), or null char (0) to indicate expansion is done via
     *    input source.
     */
    protected char fullyResolveEntity(Map ent1, Map ent2, boolean allowExt)
        throws IOException, XMLStreamException
    {
        char c = getNextChar(SUFFIX_IN_ENTITY_REF);
        
        // Do we have a (numeric) character entity reference?
        if (c == '#') { // numeric
            return resolveCharEnt();
        }

        String id = parseEntityName(c);
            
        // Perhaps we have a pre-defined char reference?
        c = id.charAt(0);
        /* 16-May-2004, TSa: Should custom entities (or ones defined in
         *   int/ext subset) override pre-defined settings for these?
         */
        if (c == 'a') { // amp or apos?
            if (id.equals("amp")) {
                return '&';
            }
            if (id.equals("apos")) {
                return '\'';
            }
        } else if (c == 'g') { // gt?
            if (id.length() == 2 && id.charAt(1) == 't') {
                return '>';
            }
        } else if (c == 'l') { // lt?
            if (id.length() == 2 && id.charAt(1) == 't') {
                return '<';
            }
        } else if (c == 'q') { // quot?
            if (id.equals("quot")) {
                return '"';
            }
        }
        expandEntity(id, ent1, ent2, allowExt);
        return CHAR_NULL;
    }

    /**
     * Helper method that will try to expand a parsed entity (parameter or
     * generic entity).
     *<p>
     * note: called by sub-classes (dtd parser), needs to be protected.
     *
     * @return 1, if entity was found from the first Map passed in; 2,
     *   if from second, 0 if neither (only for non-entity-replacing mode)
     */
    protected int expandEntity(String id, Map ent1, Map ent2, boolean allowExt)
        throws IOException, XMLStreamException
    {
        EntityDecl ed;
        
        mCurrName = id;

        if (ent1 == null) {
            ed = null;
        } else {
            ed = (EntityDecl) ent1.get(id);
        }

        int result;
        if (ed == null) {
            result = 2;
            if (ent2 != null) {
                ed = (EntityDecl) ent2.get(id);
            }
        } else {
            result = 1;
        }

        if (ed == null) {
            /* 30-Sep-2005, TSa: As per [WSTX-5], let's only throw exception
             *   if we have to resolve it (otherwise it's just best-effort, 
             *   and null is ok)
             */
            /* 02-Oct-2005, TSa: Plus, [WSTX-4] adds "undeclared entity
             *    resolver"
             */
            if (mCfgReplaceEntities) {
                expandUnresolvedEntity(id);
            }
            return 0;
        }
        expandEntity(ed, allowExt);
        return result;
    }

    /**
     *
     *<p>
     * note: defined as private for documentation, ie. it's just called
     * from within this class (not sub-classes), from one specific method
     * (see above)
     *
     * @param ed Entity to be expanded
     * @param allowExt Whether external entities are allowed or not.
     */
    private void expandEntity(EntityDecl ed, boolean allowExt)
        throws IOException, XMLStreamException
    {
        /* Should not refer unparsed entities from attribute values
         * or text content (except via notation mechanism, but that's
         * not parsed here)
         */
        if (!ed.isParsed()) {
            throwParseError("Illegal reference to unparsed external entity '"
                            +ed.getName()+"'.");
        }

        // 28-Jun-2004, TSa: Do we support external entity expansion?
        boolean isExt = ed.isExternal();
        if (isExt) {
            if (!allowExt) { // never ok in attribute value...
                throwParseError("Encountered a reference to external parsed entity '"
                                +ed.getName()+"' when expanding attribute value: not legal as per XML 1.0/1.1 #3.1.");
            }
            if (!mConfig.hasConfigFlags(CFG_SUPPORT_EXTERNAL_ENTITIES)) {
                throwParseError("Encountered a reference to external entity '"
                                +ed.getName()+"', but Reader has feature '"
                                +XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES
                                +"' disabled.");
            }
        }

        // First, let's give current context chance to save its stuff
        WstxInputSource oldInput = mInput;
        oldInput.saveContext(this);
        WstxInputSource newInput = null;
        try {
            newInput = ed.expand(oldInput, mEntityResolver,
                                 mConfig.getXMLReporter());
        } catch (FileNotFoundException fex) {
            /* Let's catch and rethrow this just so we get more meaningful
             * description (with input source position etc)
             */
            throwParseError("(was "+fex.getClass().getName()+") "+fex.getMessage());
        }

        // Let's check there's no recursion (-> infinite loops)
        if (newInput.hasRecursion()) {
            throwRecursionError(ed.getName());
        }
        /* And then we'll need to make sure new input comes from the new
         * input source
         */
        initInputSource(newInput, isExt);
    }

    /**
     *<p>
     * note: only from the local expandEntity() method
     */
    private void expandUnresolvedEntity(String id)
        throws IOException, XMLStreamException
    {
        XMLResolver resolver = mConfig.getUndeclaredEntityResolver();
        if (resolver != null) {
            WstxInputSource oldInput = mInput;
            oldInput.saveContext(this);
            // null, null -> no public or system ids
            WstxInputSource newInput = DefaultInputResolver.resolveEntityUsing
                (oldInput, id, null, null, resolver, mConfig.getXMLReporter());
            if (newInput != null) {
                // Not 100% sure if recursion check is needed... but let's be safe?
                if (newInput.hasRecursion()) {
                    throwRecursionError(id);
                }
                initInputSource(newInput, true); // true -> is external
                return;
            }
        }
        throwParseError("Undeclared entity '"+id+"'.");
    }
  
    /*
    ////////////////////////////////////////////////////
    // Basic tokenization
    ////////////////////////////////////////////////////
     */


    /**
     * Method that will parse name token (roughly equivalent to XML specs;
     * although bit lenier for more efficient handling); either uri prefix,
     * or local name.
     *<p>
     * Much of complexity in this method has to do with the intention to 
     * try to avoid any character copies. In this optimal case algorithm
     * would be fairly simple. However, this only works if all data is
     * already in input buffer... if not, copy has to be made halfway
     * through parsing, and that complicates things.
     *<p>
     * One thing to note is that String returned has been canonicalized
     * and (if necessary) added to symbol table. It can thus be compared
     * against other such (usually id) Strings, with simple equality operator.
     *
     * @param c First character of the name; not yet checked for validity
     *
     * @return Canonicalized name String (which may have length 0, if
     *    EOF or non-name-start char encountered)
     */
    protected String parseLocalName(char c)
        throws IOException, WstxException
    {
        /* Has to start with letter, or '_' (etc); we won't allow ':' as that
         * is taken as namespace separator; no use trying to optimize
         * heavily as it's 98% likely it is a valid char...
         */
        if (!is11NameStartChar(c)) {
            if (c == ':') {
                throwUnexpectedChar(c, " (missing namespace prefix?)");
            }
            throwUnexpectedChar(c, " (expected a name start character)");
        }

        int ptr = mInputPtr;
        int inputLen = mInputLen;
        //char[] inputBuf = mInputBuffer;
        int startPtr = ptr-1; // already read previous char
        int hash = (int) c;

        /* After which there may be zero or more name chars
         * we have to consider
         */
        while (ptr < inputLen) {
            //c = inputBuf[ptr];
            c = mInputBuffer[ptr];
            if ((c < CHAR_LOWEST_LEGAL_LOCALNAME_CHAR) || !is11NameChar(c)) {
                mInputPtr = ptr;
                return mSymbols.findSymbol(mInputBuffer, startPtr, ptr - startPtr, hash);
            }
            hash = (hash * 31) + (int) c;
            ++ptr;
        }

        
        /* Ok, identifier may continue past buffer end, need
         * to continue with part 2 (separate method, as this is
         * not as common as having it all in buffer)
         */
        mInputPtr = ptr;
        return parseLocalName2(startPtr, hash);
    }

    /**
     * Second part of name token parsing; called when name can continue
     * past input buffer end (so only part was read before calling this
     * method to read the rest).
     *<p>
     * Note that this isn't heavily optimized, on assumption it's not
     * called very often.
     */
    protected String parseLocalName2(int start, int hash)
        throws IOException, WstxException
    {
        int ptr = mInputLen - start;
        // Let's assume fairly short names
        char[] outBuf = getNameBuffer(ptr+8);

        if (ptr > 0) {
            System.arraycopy(mInputBuffer, start, outBuf, 0, ptr);
        }

        int outLen = outBuf.length;
        while (true) {
            char c = (mInputPtr < mInputLen) ? mInputBuffer[mInputPtr++]
                : getNextChar(SUFFIX_IN_NAME);
            if ((c < CHAR_LOWEST_LEGAL_LOCALNAME_CHAR) || !is11NameChar(c)) {
                --mInputPtr; // pusback
                break;
            }
            if (ptr >= outLen) {
              mNameBuffer = outBuf = expandBy50Pct(outBuf);
              outLen = outBuf.length;
            }
            outBuf[ptr++] = c;
            hash = (hash * 31) + (int) c;
        }
        // Still need to canonicalize the name:
        return mSymbols.findSymbol(outBuf, 0, ptr, hash);
    }

    /**
     * Method that will parse 'full' name token; what full means depends on
     * whether reader is namespace aware or not. If it is, full name means
     * local name with no namespace prefix (PI target, entity/notation name);
     * if not, name can contain arbitrary number of colons. Note that
     * element and attribute names are NOT parsed here, so actual namespace
     * prefix separation can be handled properly there.
     *<p>
     * Similar to {@link #parseLocalName}, much of complexity stems from
     * trying to avoid copying name characters from input buffer.
     *<p>
     * Note that returned String will be canonicalized, similar to
     * {@link #parseLocalName}, but without separating prefix/local name.
      *
     * @return Canonicalized name String (which may have length 0, if
     *    EOF or non-name-start char encountered)
     */
    protected String parseFullName()
        throws IOException, WstxException
    {
        char c;

        if (mInputPtr >= mInputLen) {
            loadMoreFromCurrent();
        }
        return parseFullName(mInputBuffer[mInputPtr++]);
    }

    protected String parseFullName(char c)
        throws IOException, WstxException
    {
        // First char has special handling:
        if (!is11NameStartChar(c)) {
            if (c == ':') { // no name.... generally an error:
                if (mCfgNsEnabled) {
                    throwNsColonException(parseFNameForError());
                }
                // Ok, that's fine actually
            } else {
                if (c <= CHAR_SPACE) {
                    throwUnexpectedChar(c, " (missing name?)");
                }
                throwUnexpectedChar(c, " (expected a name start character)");
            }
        }

        int hash = (int) c;
        int ptr = mInputPtr;
        int inputLen = mInputLen;
        char[] inputBuf = mInputBuffer;
        int startPtr = ptr-1; // to account for the first char

        /* After which there may be zero or more name chars
         * we have to consider
         */
        while (true) {
            if (ptr >= inputLen) {
                /* Ok, identifier may continue past buffer end, need
                 * to continue with part 2 (separate method, as this is
                 * not as common as having it all in buffer)
                 */
                mInputPtr = ptr;
                return parseFullName2(startPtr, hash);
            }
            c = inputBuf[ptr];
            if (c == ':') { // colon only allowed in non-NS mode
                if (mCfgNsEnabled) {
                    mInputPtr = ptr;
                    throwNsColonException(new String(inputBuf, startPtr, ptr - startPtr) + parseFNameForError());
                }
            } else if (!is11NameChar(c)) {
                break;
            }
            hash = (hash * 31) + (int) c;
            ++ptr;
        }
        mInputPtr = ptr;
        return mSymbols.findSymbol(inputBuf, startPtr, ptr - startPtr, hash);
    }

    protected String parseFullName2(int start, int hash)
        throws IOException, WstxException
    {
        int ptr = mInputLen - start;
        // Let's assume fairly short names
        char[] outBuf = getNameBuffer(ptr+8);

        if (ptr > 0) {
            System.arraycopy(mInputBuffer, start, outBuf, 0, ptr);
        }

        int outLen = outBuf.length;
        while (true) {
            /* 06-Sep-2004, TSa: Name tokens are not allowed to continue
             *   past entity expansion ranges... that is, all characters
             *   have to come from the same input source. Thus, let's only
             *   load things from same input level
             */
            char c;

            if (mInputPtr >= mInputLen) {
                /* Should only load more input from the current input
                 * source... and usually it'd also be an error to hit
                 * an EOB, but not always. Thus, let's just let the caller
                 * deal with such situations.
                 */
                if (!loadMoreFromCurrent()) {
                    break;
                }
            }
            c = mInputBuffer[mInputPtr];
            if (c == ':') { // colon only allowed in non-NS mode
                ++mInputPtr;
                if (mCfgNsEnabled) {
                    throwNsColonException(new String(outBuf, 0, ptr) + c + parseFNameForError());
                }
            } else if (is11NameChar(c)) {
                ++mInputPtr;
            } else {
                break;
            }

            if (ptr >= outLen) {
                mNameBuffer = outBuf = expandBy50Pct(outBuf);
                outLen = outBuf.length;
            }
            outBuf[ptr++] = c;
            hash = (hash * 31) + (int) c;
        }

        // Still need to canonicalize the name:
        return mSymbols.findSymbol(outBuf, 0, ptr, hash);
    }

    /**
     * Method called to read in full name, including unlimited number of
     * namespace separators (':'), for the purpose of displaying name in
     * an error message. Won't do any further validations, and parsing
     * is not optimized: main need is just to get more meaningful error
     * messages.
     */
    protected String parseFNameForError()
        throws IOException, WstxException
    {
        StringBuffer sb = new StringBuffer(100);
        while (true) {
            char c;

            if (mInputPtr < mInputLen) {
                c = mInputBuffer[mInputPtr++];
            } else { // can't error here, so let's accept EOF for now:
                int i = getNext();
                if (i < 0) {
                    break;
                }
                c = (char) i;
            }
            if (c != ':' && !is11NameChar(c)) {
                --mInputPtr;
                break;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    protected final String parseEntityName(char c)
        throws IOException, WstxException
    {
        String id = parseFullName(c);
        // Needs to be followed by a semi-colon, too.. from same input source:
        if (mInputPtr >= mInputLen) {
            if (!loadMoreFromCurrent()) {
                throwParseError("Missing semicolon after reference for entity '"+id+"'");
            }
        }
        c = mInputBuffer[mInputPtr++];
        if (c != ';') {
            throwUnexpectedChar(c, "; expected a semi-colon after the reference for entity '"+id+"'");
        }
        return id;
    }
    
    /**
     * Note: does not check for number of colons, amongst other things.
     * Main idea is to skip through what superficially seems like a valid
     * id, nothing more. This is only done when really skipping through
     * something we do not care about at all: not even whether names/ids
     * would be valid (for example, when ignoring internal DTD subset).
     *
     * @return Length of skipped name.
     */
    protected int skipFullName(char c)
        throws IOException, WstxException
    {
        if (!is11NameStartChar(c)) {
            --mInputPtr;
            return 0;
        }

        /* After which there may be zero or more name chars
         * we have to consider
         */
        int count = 1;
        while (true) {
            c = (mInputPtr < mInputLen) ?
                mInputBuffer[mInputPtr++] : getNextChar(SUFFIX_EOF_EXP_NAME);
            if (c != ':' && !is11NameChar(c)) {
                break;
            }
            ++count;
        }
        return count;
    }

    /**
     * Simple parsing method that parses system ids, which are generally
     * used in entities (from DOCTYPE declaration to internal/external
     * subsets).
     *<p>
     * NOTE: returned String is not canonicalized, on assumption that
     * external ids may be longish, and are not shared all that often, as
     * they are generally just used for resolving paths, if anything.
     *<br />
     * Also note that this method is not heavily optimized, as it's not
     * likely to be a bottleneck for parsing.
     */
    protected final String parseSystemId(char quoteChar, boolean convertLFs,
					 String errorMsg)
        throws IOException, WstxException
    {
        char[] buf = getNameBuffer(-1);
        int ptr = 0;

        while (true) {
            char c = (mInputPtr < mInputLen) ?
                mInputBuffer[mInputPtr++] : getNextChar(errorMsg);
            if (c == quoteChar) {
                break;
            }
            /* ??? 14-Jun-2004, TSa: Should we normalize linefeeds or not?
             *   It seems like we should, for all input... so that's the way it
             *   works.
             */
            if (c == '\n') {
                markLF();
            } else if (c == '\r') {
                if (peekNext() == '\n') {
                    ++mInputPtr;
                    if (!convertLFs) {
                        /* The only tricky thing; need to preserve 2-char LF; need to
                         * output one char from here, then can fall back to default:
                         */
                        if (ptr >= buf.length) {
                            buf = expandBy50Pct(buf);
                        }
                        buf[ptr++] = '\r';
                    }
                    c = '\n';
                } else if (convertLFs) {
                    c = '\n';
                }
            }

            // Other than that, let's just append it:
            if (ptr >= buf.length) {
                buf = expandBy50Pct(buf);
            }
            buf[ptr++] = c;
        }

        return (ptr == 0) ? "" : new String(buf, 0, ptr);
    }

    /**
     * Simple parsing method that parses system ids, which are generally
     * used in entities (from DOCTYPE declaration to internal/external
     * subsets).
     *<p>
     * NOTE: returned String is not canonicalized, on assumption that
     * external ids may be longish, and are not shared all that often, as
     * they are generally just used for resolving paths, if anything.
     *<br />
     * Also note that this method is not heavily optimized, as it's not
     * likely to be a bottleneck for parsing.
     */
    protected final String parsePublicId(char quoteChar, boolean convertLFs,
                                         String errorMsg)
        throws IOException, WstxException
    {
        char[] buf = getNameBuffer(-1);
        int ptr = 0;

        while (true) {
            char c = (mInputPtr < mInputLen) ?
                mInputBuffer[mInputPtr++] : getNextChar(errorMsg);
            if (c == quoteChar) {
                break;
            }
            if (c == '\n') {
                markLF();
            } else if (c == '\r') {
                if (peekNext() == '\n') {
                    ++mInputPtr;
                    if (!convertLFs) {
                        if (ptr >= buf.length) {
                            buf = expandBy50Pct(buf);
                        }
                        buf[ptr++] = '\r';
                    }
                    c = '\n';
                } else if (convertLFs) {
                    c = '\n';
                }
            } else {
                // Verify it's a legal pubid char (see XML spec, #13, from 2.3)
                if ((c >= VALID_PUBID_CHAR_COUNT)
                    || sPubidValidity[c] != PUBID_CHAR_VALID_B) {
                    throwUnexpectedChar(c, " in public identifier");
                }
            }
        
            // Other than that, let's just append it:
            if (ptr >= buf.length) {
                buf = expandBy50Pct(buf);
            }
            buf[ptr++] = c;
        }
      
        return (ptr == 0) ? "" : new String(buf, 0, ptr);
    }

    protected final void parseUntil(TextBuffer tb, char endChar, boolean convertLFs,
                                    String errorMsg)
        throws IOException, WstxException
    {
        // Let's first ensure we have some data in there...
        if (mInputPtr >= mInputLen) {
            loadMore(errorMsg);
        }
        while (true) {
            // Let's loop consequtive 'easy' spans:
            char[] inputBuf = mInputBuffer;
            int inputLen = mInputLen;
            int ptr = mInputPtr;
            int startPtr = ptr;
            while (ptr < inputLen) {
                char c = inputBuf[ptr++];
                if (c == endChar) {
                    int thisLen = ptr - startPtr - 1;
                    if (thisLen > 0) {
                        tb.append(inputBuf, startPtr, thisLen);
                    }
                    mInputPtr = ptr;
                    return;
                }
                if (c == '\n') {
                    mInputPtr = ptr; // markLF() requires this
                    markLF();
                } else if (c == '\r') {
                    if (!convertLFs && ptr < inputLen) {
                        if (inputBuf[ptr] == '\n') {
                            ++ptr;
                        }
                        mInputPtr = ptr;
                        markLF();
                    } else {
                        int thisLen = ptr - startPtr - 1;
                        if (thisLen > 0) {
                            tb.append(inputBuf, startPtr, thisLen);
                        }
                        mInputPtr = ptr;
                        c = getNextChar(errorMsg);
                        if (c != '\n') {
                            --mInputPtr; // pusback
                            tb.append(convertLFs ? '\n' : '\r');
                        } else {
                            if (convertLFs) {
                                tb.append('\n');
                            } else {
                                tb.append('\r');
                                tb.append('\n');
                            }
                        }
                        startPtr = ptr = mInputPtr;
                        markLF();
                    }
                }
            }
            int thisLen = ptr - startPtr;
            if (thisLen > 0) {
                tb.append(inputBuf, startPtr, thisLen);
            }
            loadMore(errorMsg);
            startPtr = ptr = mInputPtr;
            inputBuf = mInputBuffer;
            inputLen = mInputLen;
        }
    }

    /*
    //////////////////////////////////////////
    // Internal methods
    //////////////////////////////////////////
     */

    private char resolveCharEnt()
        throws IOException, WstxException
    {
        int value = 0;
        char c = getNextChar(SUFFIX_IN_ENTITY_REF);
        if (c == 'x') { // hex
            while (true) {
                c = (mInputPtr < mInputLen) ? mInputBuffer[mInputPtr++]
                    : getNextChar(SUFFIX_IN_ENTITY_REF);
                if (c == ';') {
                    break;
                }
                value = value << 4;
                if (c <= '9' && c >= '0') {
                    value += (c - '0');
                } else if (c >= 'a' && c <= 'z') {
                    value += 10 + (c - 'a');
                } else if (c >= 'A' && c <= 'Z') {
                    value += 10 + (c - 'A');
                } else {
                    throwUnexpectedChar(c, "; expected a hex number (0-9a-zA-Z).");
                }
            }
        } else { // numeric (decimal)
            while (c != ';') {
                if (c <= '9' && c >= '0') {
                    value = (value * 10) + (c - '0');
                } else {
                    throwUnexpectedChar(c, "; expected a decimal number.");
                }
                c = (mInputPtr < mInputLen) ? mInputBuffer[mInputPtr++]
                    : getNextChar(SUFFIX_IN_ENTITY_REF);
            }
        }
        c = (char) value;
        // Could check for other invalid chars as well...
        if (value == 0) {
            throwParseError("Invalid character reference -- null character not allowed in XML content.");
        }
        return c;
    }

    protected final char[] getNameBuffer(int minSize)
    {
        char[] buf = mNameBuffer;
        
        if (buf == null) {
            mNameBuffer = buf = new char[(minSize > 48) ? (minSize+16) : 64];
        } else if (minSize >= buf.length) { // let's allow one char extra...
            int len = buf.length;
            len += (len >> 1);
            mNameBuffer = buf = new char[(minSize >= len) ? (minSize+16) : len];
        }
        return buf;
    }
    
    protected final char[] expandBy50Pct(char[] buf)
    {
        int len = buf.length;
        char[] newBuf = new char[len + (len >> 1)];
        System.arraycopy(buf, 0, newBuf, 0, len);
        return newBuf;
    }

    /**
     * Method called to throw an exception indicating that a name that
     * should not be namespace-qualified (PI target, entity/notation name)
     * is one, and reader is namespace aware.
     */
    private void throwNsColonException(String name)
        throws WstxException
    {
        throwParseError("Illegal name '"+name+"' (PI target, entity/notation name): can not contain a colon (XML Namespaces 1.0#6)");
    }

    private void throwRecursionError(String entityName)
        throws WstxException
    {
        throwParseError("Illegal entity expansion: entity '"+entityName+"' expands itself recursively.");
    }
}
