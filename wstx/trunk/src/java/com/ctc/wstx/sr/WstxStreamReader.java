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

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.DTDInfo;
import org.codehaus.stax2.XMLStreamReader2;

import com.ctc.wstx.api.ReaderConfig;
import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.dtd.DTDSubset;
import com.ctc.wstx.ent.EntityDecl;
import com.ctc.wstx.exc.WstxException;
import com.ctc.wstx.io.*;
import com.ctc.wstx.util.DefaultXmlSymbolTable;
import com.ctc.wstx.util.SymbolTable;
import com.ctc.wstx.util.TextBuffer;
import com.ctc.wstx.util.TextBuilder;
import com.ctc.wstx.util.URLUtil;

/**
 * Implementation of {@link XMLStreamReader2} that implements non-DTD
 * aware parts of XML handling (plus some minimal support for parsing
 * DOCTYPE declaration and skipping internal DTD subset if necessary).
 * It can be used as is, and it is also the superclass of the DTD-aware
 * implementation(s).
 *<p>
 * This class is also the lowest common denominator for all actual
 * {@link XMLStreamReader2} implementations Woodstox will ever create.
 *<p>
 * Some notes about non-conformancy with XML specs:
 * <ul>
 *  <li>White space recognition is simplified; everything below unicode
 *     0x0020 (including 0x0020, space itself), is considered valid
 *     whitespace, except for null char.
 *     Extra linefeed chars XML 1.1 adds are not recognized.
 *   </li>
 *  <li>Content characters are not restricted to only legal XML 1.0 characters;
 *    all Unicode chars except for markup (and null char) is passed through.
 *   </li>
 * </ul>
 */
public class WstxStreamReader
    extends StreamScanner
    implements XMLStreamReader2, DTDInfo
{
    /**
     * StAX API expects null to indicate "no prefix", not an empty String...
     */
    //protected final static String DEFAULT_NS_PREFIX = SymbolTable.EMPTY_STRING;
    protected final static String DEFAULT_NS_PREFIX = null;

    // // // Standalone values:

    final static int DOC_STANDALONE_UNKNOWN = 0;
    final static int DOC_STANDALONE_YES = 1;
    final static int DOC_STANDALONE_NO = 2;

    // // // Main state consts:

    final static int STATE_PROLOG = 0; // Before root element
    final static int STATE_TREE = 1; // Parsing actual XML tree
    final static int STATE_EPILOG = 2; // After root element has been closed
    final static int STATE_CLOSED = 3; // After root element has been closed

    // // // Bit masks used for quick type comparisons

    final private static int MASK_GET_TEXT = 
        (1 << CHARACTERS) | (1 << CDATA) | (1 << SPACE)
        | (1 << COMMENT) | (1 << DTD) | (1 << ENTITY_REFERENCE);

    final private static int MASK_GET_ELEMENT_TEXT = 
        (1 << CHARACTERS) | (1 << CDATA) | (1 << SPACE)
        | (1 << ENTITY_REFERENCE);

    /*
    ////////////////////////////////////////////////////
    // Symbol handling:
    ////////////////////////////////////////////////////
     */

    final protected static String sPrefixXml = DefaultXmlSymbolTable.getXmlSymbol();

    final protected static String sPrefixXmlns = DefaultXmlSymbolTable.getXmlnsSymbol();

    /**
     * Object to notify about shared stuff, such as symbol tables, as well
     * as to query for additional config settings if necessary.
     */
    final protected ReaderCreator mOwner;

    /*
    ////////////////////////////////////////////////////
    // XML document information (from doc decl if one
    // was found)
    ////////////////////////////////////////////////////
     */

    /**
     * Input stream encoding, if known; null if not.
     */
    String mDocInputEncoding = null;

    /**
     * Character encoding from xml declaration, if any; null if no
     * declaration, or it didn't specify encoding.
     */
    String mDocCharEncoding = null;

    /**
     * XML version used by document, from XML declaration; null if no
     * XML declaration found.
     */
    String mDocXmlVersion = null;

    /**
     * Status about "stand-aloneness" of document; set to 'yes'/'no'/'unknown'
     * based on whether there was xml declaration, and if so, whether
     * it had standalone attribute.
     */
    public int mDocStandalone = DOC_STANDALONE_UNKNOWN;
    // ^^^ Only public to be accessible from test classes...

    /**
     * Prefix of root element, as dictated by DOCTYPE declaration; null
     * if no DOCTYPE declaration, or no root prefix
     */
    String mRootPrefix;

    /**
     * Local name of root element, as dictated by DOCTYPE declaration; null
     * if no DOCTYPE declaration.
     */
    String mRootLName;

    /**
     * Public id of the DTD, if one exists and has been parsed.
     */
    protected String mDtdPublicId;

    /**
     * System id of the DTD, if one exists and has been parsed.
     */
    protected String mDtdSystemId;

    /*
    ////////////////////////////////////////////////////
    // Information about currently open subtree:
    ////////////////////////////////////////////////////
     */

    /**
     * Currently open element tree
     */
    final protected InputElementStack mElementStack;

    /**
     * Object that stores information about currently accessible attributes.
     */
    final protected AttributeCollector mAttrCollector;

    /*
    ////////////////////////////////////////////////////
    // Tokenization state
    ////////////////////////////////////////////////////
     */

    /// Flag set when DOCTYPE declaration has been parsed
    protected boolean mStDoctypeFound = false;
    
    /// Flag set to mark that token is partially parsed
    protected boolean mStTokenUnfinished = false;
    
    /// Flag that indicates current start element is an empty element
    protected boolean mStEmptyElem = false;

    /**
     * Flag that indicates that a partial CDATA segment contents were
     * returned, but that section itself still continues (may not have
     * anything more than end marker, however).
     */
    protected boolean mStPartialCData = false;

    /**
     * Main parsing/tokenization state (STATE_xxx)
     */
    int mParseState;

    /**
     * Current state of the stream, ie token value returned by
     * {@link #getEventType}. Needs to be initialized to START_DOCUMENT,
     * since that's the state it starts in.
     */
    protected int mCurrToken = START_DOCUMENT;

    /**
     * Local full name for the event, if it has one (note: element events
     * do NOT use this variable; those names are stored in element stack):
     * target for processing instructions.
     */
    String mCurrName;
    
    // // // Indicator of type of text in text event (WRT white space)

    final static int ALL_WS_UNKNOWN = 0x0000;
    final static int ALL_WS_YES = 0x0001;
    final static int ALL_WS_NO = 0x0002;

    /**
     * Status of current (text) token's "whitespaceness", ie. whether it is
     * or is not all white space.
     */
    int mWsStatus;

    /*
    ////////////////////////////////////////////////////
    // DTD information (entities, content spec stub)
    ////////////////////////////////////////////////////
     */

    /**
     * Entities parsed from internal/external DTD subsets. Although it
     * will remain null for this class, extended classes make use of it,
     * plus, to be able to share some of entity resolution code, instance
     * is left here even though it semantically belongs to the sub-class.
     */
    protected Map mGeneralEntities = null;


    /**
     * Entity reference stream currently points to; only used when
     * in non-automatically expanding mode.
     */
    protected EntityDecl mCurrEntity;

    /**
     * Mode information needed at this level; mostly to check what kind
     * of textual content (if any) is allowed in current element
     * context. Constants come from
     * {@link com.ctc.wstx.cfg.InputConfigFlags},
     * (like {@link com.ctc.wstx.cfg.InputConfigFlags#CONTENT_ALLOW_MIXED}).
     * Only used inside tree; ignored for prolog/epilog (which
     * have straight-forward static rules).
     */
    protected int mVldContent = CONTENT_ALLOW_MIXED;

    /*
    ////////////////////////////////////////////////////
    // Configuration
    ////////////////////////////////////////////////////
     */

    protected final ReaderConfig mConfig;

    /**
     * Various flags about tokenization state (TF_xxx)
     */
    protected final int mConfigFlags;

    // // // Various extracted settings:

    // Extracted standard on/off settings:

    protected final boolean mCfgReplaceEntities;

    // Extracted wstx-specific settings:

    protected final boolean mCfgNormalizeLFs;
    protected final boolean mCfgNormalizeAttrs;
    protected final boolean mCfgCoalesceText;
    protected final boolean mCfgReportTextAsChars;
    protected final boolean mCfgLazyParsing;

    /**
     * Minimum number of characters parser can return as partial text
     * segment, IF it's not required to coalesce adjacent text
     * segments.
     */
    protected final int mShortestTextSegment;

    /**
     * Map that contains entity id - to - entity declaration entries for
     * any entities caller wants to prepopulate for the document. Note that
     * such entities will override any entities read from DTD (both internal
     * and external subsets).
     */
    final Map mCustomEntities;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle (ctors)
    ////////////////////////////////////////////////////
     */

    /**
     * @param elemStack Input element stack to use; if null, will create
     *   instance locally.
     */
    protected WstxStreamReader(BranchingReaderSource input, ReaderCreator owner,
                               ReaderConfig cfg, InputElementStack elemStack)
        throws IOException, XMLStreamException
    {
        super(input, cfg, cfg.getEntityResolver());

        mOwner = owner;

        mTextBuffer = new TextBuffer(cfg.getTextBufferLength());
        mConfig = cfg;
        mConfigFlags = cfg.getConfigFlags();

        mCfgReplaceEntities = (mConfigFlags & CFG_REPLACE_ENTITY_REFS) != 0;

        mCfgNormalizeLFs = (mConfigFlags & CFG_NORMALIZE_LFS) != 0;
        mCfgNormalizeAttrs = (mConfigFlags & CFG_NORMALIZE_ATTR_VALUES) != 0;
        mCfgCoalesceText = (mConfigFlags & CFG_COALESCE_TEXT) != 0;
        mCfgReportTextAsChars = (mConfigFlags & CFG_REPORT_ALL_TEXT_AS_CHARACTERS) != 0;
        mCfgLazyParsing = (mConfigFlags & CFG_LAZY_PARSING) != 0;

        mShortestTextSegment = cfg.getShortestReportedTextSegment();

        mCustomEntities = cfg.getCustomInternalEntities();
        mElementStack = elemStack;
        mAttrCollector = elemStack.getAttrCollector();

        // And finally, location information may have offsets:
        input.initInputLocation(this);

        elemStack.connectReporter(this);
    }

    /**
     * Factory method for constructing readers.
     *
     * @param owner "Owner" of this reader, factory that created the reader;
     *   needed for returning updated symbol table information after parsing.
     * @param input Input source used to read the XML document.
     * @param cfg Object that contains reader configuration info.
     */
    public static WstxStreamReader createBasicStreamReader
        (BranchingReaderSource input, ReaderCreator owner, ReaderConfig cfg,
         InputBootstrapper bs)
      throws IOException, XMLStreamException
    {
        WstxStreamReader sr = new WstxStreamReader(input, owner, cfg,
                                                   createElementStack(cfg));
        sr.initProlog(bs);
        return sr;
    }

    protected static InputElementStack createElementStack(ReaderConfig cfg)
    {
        InputElementStack es;
        boolean normAttrs = cfg.willNormalizeAttrValues();

        if (cfg.willSupportNamespaces()) {
            return new NsInputElementStack(16, sPrefixXml, sPrefixXmlns, normAttrs);
        }
        return new NonNsInputElementStack(16, normAttrs);
    }

    /*
    ////////////////////////////////////////////////////
    // XMLStreamReader, document info
    ////////////////////////////////////////////////////
     */

    public String getCharacterEncodingScheme() {
        return mDocCharEncoding;
    }

    public String getEncoding() {
        return mDocInputEncoding;
    }

    public String getVersion() {
        return mDocXmlVersion;
    }

    public boolean isStandalone() {
        return mDocStandalone == DOC_STANDALONE_YES;
    }

    public boolean standaloneSet() {
        return mDocStandalone != DOC_STANDALONE_UNKNOWN;
    }

    /*
    ////////////////////////////////////////////////////
    // Public API, configuration
    ////////////////////////////////////////////////////
     */

    /**
     * Base class doesn't have much to implement, since all currently
     * available properties are DTD related.
     */
    public Object getProperty(String name)
    {
        // Need to have full info... (uncomment if adding functionality)
        /*
        if (mStTokenUnfinished) {
            try { finishToken(); } catch (Exception ie) {
                throwLazyError(ie);
            }
        }
        */
        return null;
    }

    /*
    ////////////////////////////////////////////////////
    // XMLStreamReader, current state
    ////////////////////////////////////////////////////
     */

    // // // Attribute access:

    public int getAttributeCount() {
        if (mCurrToken != START_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        return mAttrCollector.getCount();
    }

	public String getAttributeLocalName(int index) {
        if (mCurrToken != START_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        return mAttrCollector.getLocalName(index);
    }

    public QName getAttributeName(int index) {
        if (mCurrToken != START_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        return mAttrCollector.getQName(index);
    }

    public String getAttributeNamespace(int index) {
        if (mCurrToken != START_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        return mAttrCollector.getURI(index);
    }

    public String getAttributePrefix(int index) {
        if (mCurrToken != START_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        return mAttrCollector.getPrefix(index);
    }

    public String getAttributeType(int index) {
        if (mCurrToken != START_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        /* Although the method conceptually should be part of the attribute
         * collector, modularity constraints (attr. collector shouldn't need
         * to know anything about DTDs) mandate it to reside in (or beyond)
         * the input element stack...
         */
        return mElementStack.getAttributeType(index);
    }

    public String getAttributeValue(int index) {
        if (mCurrToken != START_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        return mAttrCollector.getValue(index);
    }

    public String getAttributeValue(String nsURI, String localName) {
        if (mCurrToken != START_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        return mAttrCollector.getValue(nsURI, localName);
    }

    /**
     * From StAX specs:
     *<blockquote>
     * Reads the content of a text-only element, an exception is thrown if
     * this is not a text-only element.
     * Regardless of value of javax.xml.stream.isCoalescing this method always
     * returns coalesced content.
     *<br/>Precondition: the current event is START_ELEMENT.
     *<br/>Postcondition: the current event is the corresponding END_ELEMENT. 
     *</blockquote>
     */
    public String getElementText()
        throws XMLStreamException
    {
        if (mCurrToken != START_ELEMENT) {
            throwParseError(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        String text = null;
        StringBuffer sb = null;

        /**
         * Need to loop to get rid of PIs, comments
         */
        while (true) {
            int type = next();
            if (type == END_ELEMENT) {
                break;
            }
            if (type == COMMENT || type == PROCESSING_INSTRUCTION) {
                continue;
            }
            if (((1 << type) & MASK_GET_ELEMENT_TEXT) == 0) {
                throwParseError("Expected a text token, got "+tokenTypeDesc(type)+".");
            }
            String nextText = getText();
            if (sb != null) {
                sb.append(nextText);
            } else if (text != null) {
                sb = new StringBuffer(text.length() + nextText.length());
                sb.append(text);
                sb.append(nextText);
                text = null;
            } else {
                text = nextText;
            }
        }
        if (sb != null) {
            return sb.toString();
        }
        return (text == null) ? "" : text;
    }

    /**
     * Returns type of the last event returned; or START_DOCUMENT before
     * any events has been explicitly returned.
     */
    public int getEventType()
    {
        /* Only complication -- multi-part coalesced text is to be reported
         * as CHARACTERS always, never as CDATA (StAX specs).
         */
        if (mCurrToken == CDATA) {
            if (mCfgCoalesceText || mCfgReportTextAsChars) {
                return CHARACTERS;
            }
        } else if (mCurrToken == SPACE) {
            if (mCfgReportTextAsChars) {
                return CHARACTERS;
            }
        }
        return mCurrToken;
    }
    
    public String getLocalName() {
        // Note: for this we need not (yet) finish reading element
        if (mCurrToken == START_ELEMENT
            || mCurrToken == END_ELEMENT) {
            return mElementStack.getLocalName();
        }
        if (mCurrToken == ENTITY_REFERENCE) {
            return mCurrEntity.getName();
        }
        throw new IllegalStateException("Current state not START_ELEMENT, END_ELEMENT or ENTITY_REFERENCE");
    }

    // // // getLocation() defined in StreamScanner

    public QName getName() {
        if (mCurrToken != START_ELEMENT && mCurrToken != END_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_ELEM);
        }
        return mElementStack.getQName();
    }

    // // // Namespace access

    public NamespaceContext getNamespaceContext() {
        /* Unlike other getNamespaceXxx methods, this is available
         * for all events.
         * Note that although StAX specs do not require it, the context
         * will actually remain valid throughout parsing, and does not
         * get invalidated when next() is called. StAX compliant apps
         * should not count on this behaviour, however.         
         */
        return mElementStack;
    }

    public int getNamespaceCount() {
        if (mCurrToken != START_ELEMENT && mCurrToken != END_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_ELEM);
        }
        return mElementStack.getCurrentNsCount();
    }

    public String getNamespacePrefix(int index) {
        if (mCurrToken != START_ELEMENT && mCurrToken != END_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_ELEM);
        }
        return mElementStack.getLocalNsPrefix(index);
    }

    public String getNamespaceURI() {
        /* 27-Jul-2004, TSa: As per Javadocs, should just return null
         *    for wrong events, not throw an exception...
         */
        if (mCurrToken != START_ELEMENT && mCurrToken != END_ELEMENT) {
            //throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_ELEM);
            return null;
        }
        return mElementStack.getNsURI();
    }

    public String getNamespaceURI(int index) {
        if (mCurrToken != START_ELEMENT && mCurrToken != END_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_ELEM);
        }
        return mElementStack.getLocalNsURI(index);
    }

    public String getNamespaceURI(String prefix) {
        if (mCurrToken != START_ELEMENT && mCurrToken != END_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_ELEM);
        }
        return mElementStack.getNamespaceURI(prefix);
    }

    public String getPIData() {
        if (mCurrToken != PROCESSING_INSTRUCTION) {
            return null;
        }
        if (mStTokenUnfinished) {
            try { finishToken(); } catch (Exception ie) {
                throwLazyError(ie);
            }
        }
        return mTextBuffer.contentsAsString();
    }

    public String getPITarget() {
        if (mCurrToken != PROCESSING_INSTRUCTION) {
            return null;
        }
        return mCurrName;
    }

    public String getPrefix() {
        if (mCurrToken != START_ELEMENT && mCurrToken != END_ELEMENT) {
            return null;
        }
        return mElementStack.getPrefix();
    }

    public String getText()
    {
        if (((1 << mCurrToken) & MASK_GET_TEXT) == 0) {
            throwNotTextual(mCurrToken);
        }
        if (mStTokenUnfinished) {
            try {
                finishToken();
            } catch (Exception ie) {
                throwLazyError(ie);
            }
        }
        if (mCurrToken == ENTITY_REFERENCE) {
            return mCurrEntity.getReplacementText();
        }
        if (mCurrToken == DTD) {
            /* 16-Aug-2004, TSa: Hmmh. Specs are bit ambiguous on whether this
             *   should return just the internal subset, or the whole
             *   thing...
             */
            return getDTDInternalSubset();
        }
        return mTextBuffer.contentsAsString();
    }

    public char[] getTextCharacters()
    {
        if (((1 << mCurrToken) & MASK_GET_TEXT) == 0) {
            throwNotTextual(mCurrToken);
        }
        if (mStTokenUnfinished) {
            try {
                finishToken();
            } catch (Exception ie) {
                throwLazyError(ie);
            }
        }
        if (mCurrToken == ENTITY_REFERENCE) {
            return mCurrEntity.getReplacementChars();
        }
        if (mCurrToken == DTD) {
            return getDTDInternalSubsetArray();
        }
        /* Note: will be a newly allocated array, since contents would seldom
         * if ever align completely within (and filling) the input buffer...
         */
        return mTextBuffer.getTextBuffer();
    }

    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int len)
    {
        if (((1 << mCurrToken) & MASK_GET_TEXT) == 0) {
            throwNotTextual(mCurrToken);
        }
        if (mStTokenUnfinished) {
            try {
                finishToken();
            } catch (Exception ie) {
                throwLazyError(ie);
            }
        }
        if (mCurrToken == ENTITY_REFERENCE) {
            char[] c = mCurrEntity.getReplacementChars();
            if (c == null) {
                // Is this the right marker? Or should it be 0?
                return -1;
            }
            int max = c.length - sourceStart;
            if (max < len) {
                len = max;
            }
            if (len > 0) {
                System.arraycopy(c, sourceStart, target, targetStart, len);
            }
            return len;
        }
        if (mCurrToken == DTD) {
            /* !!! Note: not really optimal; could get the char array instead
             *   of the String
             */
            String str = getDTDInternalSubset();
            if (str == null) {
                return 0;
            }
            int max = str.length() - sourceStart;
            if (max < len) {
                len = max;
            }
            str.getChars(sourceStart, sourceStart+len, target, targetStart);
            return len;
        }
        return mTextBuffer.contentsToArray(sourceStart, target, targetStart, len);
    }

    public int getTextLength()
    {
        if (((1 << mCurrToken) & MASK_GET_TEXT) == 0) {
            throwNotTextual(mCurrToken);
        }
        if (mStTokenUnfinished) {
            try {
                finishToken();
            } catch (Exception ie) {
                throwLazyError(ie);
            }
        }
        if (mCurrToken == ENTITY_REFERENCE) {
            return mCurrEntity.getReplacementTextLength();
        }
        if (mCurrToken == DTD) {
            char[] ch = getDTDInternalSubsetArray();
            return (ch == null) ? 0 : ch.length;
        }
        return mTextBuffer.size();
    }

    public int getTextStart()
    {
        if (((1 << mCurrToken) & MASK_GET_TEXT) == 0) {
            throwNotTextual(mCurrToken);
        }
        if (mStTokenUnfinished) {
            try {
                finishToken();
            } catch (Exception ie) {
                throwLazyError(ie);
            }
        }
        if (mCurrToken == ENTITY_REFERENCE || mCurrToken == DTD) {
            return 0;
        }
        return mTextBuffer.getTextStart();
    }

    public boolean hasName() {
        return (mCurrToken == START_ELEMENT) || (mCurrToken == END_ELEMENT);
    }

    public boolean hasNext() {
        return (mCurrToken != END_DOCUMENT);
    }

    public boolean hasText() {
        return (((1 << mCurrToken) & MASK_GET_TEXT) != 0);
    }

    public boolean isAttributeSpecified(int index)
    {
        /* No need to check for ATTRIBUTE since we never return that...
         */
        if (mCurrToken != START_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        return mAttrCollector.isSpecified(index);
    }

    public boolean isCharacters() {
        return (mCurrToken == CHARACTERS || mCurrToken == CDATA
                ||mCurrToken == SPACE);
    }

    public boolean isEndElement() {
        return (mCurrToken == END_ELEMENT);
    }

    public boolean isStartElement() {
        return (mCurrToken == START_ELEMENT);
    }

    /**
     *<p>
     * 05-Apr-2004, TSa: Could try to determine status when text is actually
     *   read. That'd prevent double reads... but would it slow down that
     *   one reading so that net effect would be negative?
     */
    public boolean isWhiteSpace()
    {
        if (mCurrToken == CHARACTERS || mCurrToken == CDATA) {
            if (mStTokenUnfinished) {
                try {
                    finishToken();
                } catch (Exception ie) {
                    throwLazyError(ie);
                }
            }
            if (mWsStatus == ALL_WS_UNKNOWN) {
                mWsStatus = mTextBuffer.isAllWhitespace() ?
                    ALL_WS_YES : ALL_WS_NO;
            }
            return mWsStatus == ALL_WS_YES;
        }
        return (mCurrToken == SPACE);
    }
    
    public void require(int type, String nsUri, String localName)
        throws XMLStreamException
    {
        int curr = mCurrToken;

        /* There are some special cases; specifically, SPACE and CDATA
         * are sometimes reported as CHARACTERS. Let's be lenient by
         * allowing both 'real' and reported types, for now.
         */
        if (curr != type) {
            if (curr == CDATA) {
                if (mCfgCoalesceText || mCfgReportTextAsChars) {
                    curr = CHARACTERS;
                }
            } else if (curr == SPACE) {
                if (mCfgReportTextAsChars) {
                    curr = CHARACTERS;
                }
            }
        }

        if (type != curr) {
            throwParseError("Expected type "+tokenTypeDesc(type)
                            +", current type "
                            +tokenTypeDesc(curr));
        }

        if (localName != null) {
            if (curr != START_ELEMENT && curr != END_ELEMENT
                && curr != ENTITY_REFERENCE) {
                throwParseError("Expected non-null local name, but current token not a START_ELEMENT, END_ELEMENT or ENTITY_REFERENCE (was "+tokenTypeDesc(mCurrToken)+")");
            }
            String n = getLocalName();
            if (n != localName && !n.equals(localName)) {
                throwParseError("Expected local name '"+localName+"'; current local name '"+n+"'.");
            }
        }
        if (nsUri != null) {
            if (curr != START_ELEMENT && curr != END_ELEMENT) {
                throwParseError("Expected non-null NS URI, but current token not a START_ELEMENT or END_ELEMENT (was "+tokenTypeDesc(curr)+")");
            }
            String uri = mElementStack.getNsURI();
            // No namespace?
            if (nsUri.length() == 0) {
                if (uri != null && uri.length() > 0) {
                    throwParseError("Expected empty namespace, instead have '"+uri+"'.");
                }
            } else {
                if ((nsUri != uri) && !nsUri.equals(uri)) {
                    throwParseError("Expected namespace '"+nsUri+"'; have '"
                                    +uri+"'.");
                }
            }
        }
        // Ok, fine, all's good
    }

    /*
    ////////////////////////////////////////////////////
    // XMLStreamReader, iterating
    ////////////////////////////////////////////////////
     */

    public int next()
        throws XMLStreamException
    {
        /* First we need to store current input location information, so
         * that exact location at/before current token can be reliably
         * retrieved.
         */
        mTokenInputTotal = mCurrInputProcessed + mInputPtr;
        mTokenInputRow = mCurrInputRow;
        mTokenInputCol = mInputPtr - mCurrInputRowStart;

        try {
            if (mParseState == STATE_TREE) {
                int type = nextFromTree();
                mCurrToken =  type;
                // Lazy-parsing disabled?
                if (!mCfgLazyParsing && mStTokenUnfinished) {
                    finishToken();
                }
                /* Special case -- when coalescing text, CDATA is
                 * reported as CHARACTERS, although we still need to know
                 * it really is (starts with as) CDATA.
                 */
                if (type == CDATA) {
                    if (mCfgCoalesceText || mCfgReportTextAsChars) {
                        return CHARACTERS;
                    }
                } else if (type == SPACE) {
                    if (mCfgReportTextAsChars) {
                        return CHARACTERS;
                    }
                }
                return type;
            }
            if (mParseState == STATE_PROLOG) {
                nextFromProlog(true);
            } else if (mParseState == STATE_EPILOG) {
                nextFromProlog(false);
            } else {
                return END_DOCUMENT;
            }
        } catch (IOException ie) {
            throwFromIOE(ie);
        }

        /* Special case: may want to 'convert' type for ignorable
         * white space:
         */
        if (mCurrToken == SPACE && mCfgReportTextAsChars) {
            return CHARACTERS;
        }

        return mCurrToken;
    }

    public int nextTag()
        throws XMLStreamException
    {
        while (true) {
            int next = next();

            switch (next) {
            case SPACE:
            case COMMENT:
            case PROCESSING_INSTRUCTION:
                continue;
            case CDATA:
            case CHARACTERS:
                if (isWhiteSpace()) {
                    continue;
                }
                throwParseError("Received non-all-whitespace CHARACTERS or CDATA event in nextTag().");
            case START_ELEMENT:
            case END_ELEMENT:
                return next;
            }
            throwParseError("Received event "+ErrorConsts.tokenTypeDesc(next)
                            +", instead of START_ELEMENT or END_ELEMENT.");
        }
    }

    /**
     *<p>
     * Note: as per StAX 1.0 specs, this method does NOT close the underlying
     * input reader.
     */
    public void close()
    {
        if (mParseState != STATE_CLOSED) {
            mParseState = STATE_CLOSED;
            /* Let's see if we should notify factory that symbol table
             * has new entries, and may want to reuse this symbol table
             * instead of current root.
             */
            if (mCurrToken != END_DOCUMENT) {
                mCurrToken = END_DOCUMENT;
                if (mSymbols.isDirty()) {
                    mOwner.updateSymbolTable(mSymbols);
                }
            }
        }
    }

    /*
    ////////////////////////////////////////////////////
    // XMLStreamReader2 implementation
    ////////////////////////////////////////////////////
     */

    // // // StAX 2, per-reader configuration

    public Object getFeature(String name)
    {
        // !!! TBI
        return null;
    }

    public void setFeature(String name, Object value)
    {
        /* !!! TBI:
         * 
         * - Per reader DTD override (by URL, or pre-parsed DTD)
         */
    }

    public int getAttributeIndex(String nsURI, String localName)
    {
        // !!! TBI
        /*
        if (mCurrToken != START_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        return mAttrCollector.getIndex(nsURI, localName);
        */
        return -1;
    }

    public int getIdAttributeIndex(String nsURI, String localName)
    {
        // !!! TBI
        /*
        if (mCurrToken != START_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        return mAttrCollector.getIdIndex(nsURI, localName);
        */
        return -1;
    }

    // // // StAX 2, Pass-through text accessors


    /**
     *<p>
     * TODO: try to optimize to allow completely streaming pass-through:
     * currently will still read all data in memory buffers before
     * outputting
     * 
     * @return Number of characters written to the reader
     */
    public int getText(Writer w)
        throws IOException, XMLStreamException
    {
        if (((1 << mCurrToken) & MASK_GET_TEXT) == 0) {
            throwNotTextual(mCurrToken);
        }
        if (mStTokenUnfinished) {
            try {
                finishToken();
            } catch (Exception ie) {
                throwLazyError(ie);
            }
        }
        if (mCurrToken == ENTITY_REFERENCE) {
            return mCurrEntity.getReplacementText(w);
        }
        if (mCurrToken == DTD) {
            char[] ch = getDTDInternalSubsetArray();
            if (ch != null) {
                w.write(ch);
                return ch.length;
            }
            return 0;
        }
        return mTextBuffer.rawContentsTo(w);
    }

    // // // StAX 2, Other accessors

    /**
     * Since this class implements {@link DTDInfo}, method can just
     * return <code>this</code>.
     */
    public DTDInfo getDTDInfo() throws XMLStreamException
    {
        /* Let's not allow it to be accessed during other events -- that
         * way callers won't count on it being available afterwards.
         */
        if (mCurrToken != DTD) {
            return null;
        }
        if (mStTokenUnfinished) { // need to fully read it in now
            try {
                finishToken();
            } catch (IOException ie) {
                throwFromIOE(ie);
            }
        }
        return this;
    }

    /**
     * @return Number of open elements in the stack; 0 when parser is in
     *  prolog/epilog, 1 inside root element and so on.
     */
    public int getDepth() {
        return mElementStack.getDepth();
    }

    /**
     * @return True, if cursor points to a start or end element that is
     *    constructed from 'empty' element (ends with '/>');
     *    false otherwise.
     */
    public boolean isEmptyElement() throws XMLStreamException
    {
        return mStEmptyElem;
    }

    /*
    ////////////////////////////////////////////////////
    // DTDInfo implementation (StAX 2)
    ////////////////////////////////////////////////////
     */

    /**
     *<p>
     * Note: DTD-handling sub-classes need to override this method.
     */
    public Object getProcessedDTD() {
        return null;
    }

    public String getDTDRootName() {
        if (mRootPrefix == null) {
            return mRootLName;
        }
        return mRootPrefix + ":" + mRootLName;
    }

    public String getDTDPublicId() {
        return mDtdPublicId;
    }

    public String getDTDSystemId() {
        return mDtdSystemId;
    }

    /**
     * @return Internal subset portion of the DOCTYPE declaration, if any;
     *   empty String if none
     */
    public String getDTDInternalSubset() {
        if (mCurrToken != DTD) {
            return null;
        }
        return mTextBuffer.contentsAsString();
    }

    /**
     * Internal method used by implementation
     */
    private char[] getDTDInternalSubsetArray() {
        /* Note: no checks for current state, but only because it's
         * an internal method and callers are known to ensure it's ok
         * to call this
         */
        return mTextBuffer.contentsAsArray();
    }

    /*
    ////////////////////////////////////////////////////
    // Extended Woodstox-specific interface
    ////////////////////////////////////////////////////
     */

    public EntityDecl getCurrentEntityDecl() {
        return mCurrEntity;
    }

    /*
    ////////////////////////////////////////////////////
    // Methods used by core Woodstox classes
    ////////////////////////////////////////////////////
     */

    /**
     * Method called by {@link com.ctc.wstx.evt.DefaultEventAllocator}
     * to get double-indirection necessary for constructing start element
     * events.
     *
     * @return Null, if stream does not point to start element; whatever
     *    callback returns otherwise.
     */
    public Object withStartElement(ElemCallback cb, Location loc)
    {
        if (mCurrToken != START_ELEMENT) {
            return null;
        }
        return cb.withStartElement(loc, getName(), 
                                   mElementStack.createNonTransientNsContext(loc),
                                   mAttrCollector.buildAttrOb(),
                                   mStEmptyElem);
    }

    /*
    ////////////////////////////////////////////////////
    // Internal methods, configuration access:
    ////////////////////////////////////////////////////
     */

    protected final boolean hasConfigFlags(int flags) {
        return (mConfigFlags & flags) == flags;
    }

    /*
    ////////////////////////////////////////////////////
    // Internal methods, parsing help methods
    ////////////////////////////////////////////////////
     */

    /**
     * @return Null, if keyword matches ok; String that contains erroneous
     *   keyword if not.
     */
    protected String checkKeyword(char c, String expected)
        throws IOException, WstxException
    {
      int ptr = 0;
      int len = expected.length();

      while (expected.charAt(ptr) == c && ++ptr < len) {
          if (mInputPtr < mInputLen) {
              c = mInputBuffer[mInputPtr++];
          } else {
              int ci = getNext();
              if (ci < 0) { // EOF
                  break;
              }
              c = (char) ci;
          }
      }
      
      if (ptr == len) {
          // Probable match... but let's make sure keyword is finished:
          int i = peekNext();
          if (i < 0 || (!isNameChar((char) i) && i != ':')) {
              return null;
          }
          // Nope, continues, need to find the rest:
      }
      
      StringBuffer sb = new StringBuffer(expected.length() + 16);
      sb.append(expected.substring(0, ptr));
      sb.append(c);

      while (true) {
          if (mInputPtr < mInputLen) {
              c = mInputBuffer[mInputPtr++];
          } else {
              int ci = getNext();
              if (ci < 0) { // EOF
                  break;
              }
              c = (char) ci;
          }
          if (!isNameChar(c)) {
              // Let's push it back then
              --mInputPtr;
              break;
          }
          sb.append(c);
      }

      return sb.toString();
    }

    protected void checkCData()
        throws IOException, WstxException
    {
        String wrong = checkKeyword(getNextCharFromCurrent(SUFFIX_IN_CDATA), "CDATA");
        if (wrong != null) {
            throwParseError("Unrecognized XML directive '"+wrong+"'; expected 'CDATA'.");
        }
        // Plus, need the bracket too:
        char c = getNextCharFromCurrent(SUFFIX_IN_CDATA);
        if (c != '[') {
            throwUnexpectedChar(c, "excepted '[' after '<![CDATA'");
        }
        // Cool, that's it!
    }

    /**
     * Method that checks that input following is of form
     * '[S]* '=' [S]*' (as per XML specs, production #25).
     * Will push back non-white space characters as necessary, in
     * case no equals char is encountered.
     */
    protected boolean checkEquals(String errorMsg)
        throws IOException, XMLStreamException
    {
        char c = (mInputPtr < mInputLen) ?
            mInputBuffer[mInputPtr++] : getNextCharFromCurrent(errorMsg);

        if (c <= CHAR_SPACE) { // leading WS
            int i = getNextAfterWS();
            if (i < 0) { // No EOF allowed
                return false;
            }
            if (i != '=') {
                // Need to push it back
                --mInputPtr;
                return false;
            }
        } else { // no leading WS
            if (c != '=') {
                --mInputPtr;
                return false;
            }
        }

        // trailing space?
        skipWS();
        return true;
    }

    /**
     * Method that will parse an attribute value enclosed in quotes and
     * then canonicalizes it using symbol table. Thus it's only to be used for
     * that have limited set of values.
     */

    // 13-Aug-2004, TSa: Not used any more... but may be in future?
    /**
     * Temporary working buffer for some parsing
     */
    //char[] mAttrBuffer = null;

    /*
    protected String parseSharedAttrValue(char openingQuote, boolean normalize)
        throws IOException, XMLStreamException
    {
        // Let's first ensure we have some data in there, beyond quote
        if (mInputPtr >= mInputLen) {
            loadMore(" in quoted value.");
        }
        int startPtr = mInputPtr;
        int hash = 0;

        while (mInputPtr < mInputLen) {
            char c = mInputBuffer[mInputPtr++];
            if (c == openingQuote) {
                // voila! got it all in one fell swoop...
                int len = mInputPtr - startPtr - 1;
                if (len == 0) { // empty...
                    return DEFAULT_NS_PREFIX;
                }
                return mSymbols.findSymbol(mInputBuffer, startPtr, len, hash);
            }
            // Entities mean almost certainly we can't just use input
            // buffer as is; same if there's white space which may need
            // be normalized:
            if (c == '&' || (normalize && c <= CHAR_SPACE)) {
                // Need to do longer processing
                --mInputPtr;
                break;
            }
            if (c == '<') {
                throwParseError("Unexpected '<' in quoted value.");
            }
            hash = (hash * 31) + (int) c;
        }

        // Let's initialize temp. buffer, then...
        if (mAttrBuffer == null) { // won't need a big buffer for attr values?
            mAttrBuffer = new char[256];
        }
        int amount = mInputLen - startPtr;
        if (amount > mAttrBuffer.length) {
            mAttrBuffer = new char[amount + 256];
        }
        if (amount > 0) {
            System.arraycopy(mInputBuffer, startPtr, mAttrBuffer, 0, amount);
        }

        // Ok, need to do less efficient handling:
        return parseSharedAttrValue2(openingQuote, normalize,
                                     mAttrBuffer, amount, hash);
    }
*/

/*
    // 13-Aug-2004, TSa: Not used any more... but may be in future?
    protected String parseSharedAttrValue2(char openingQuote, boolean normalize,
                                           char[] buf, int textPtr, int hash)
        throws IOException, XMLStreamException
    {
        int bufSize = buf.length;

        while (true) {
            char c;

            if (mInputPtr >= mInputLen) {
                loadMore(SUFFIX_IN_ATTR_VALUE);
            }
            c = mInputBuffer[mInputPtr++];
            if (c == openingQuote) {
                return mSymbols.findSymbol(buf, 0, textPtr, hash);
            }
            if (c == '&') { // entity of some sort
                // Note: we'll always automatically expand internal entities
                // that are in attribute values...
                if ((mInputLen - mInputPtr) >= 3
                    && (c = resolveSimpleEntity(true)) != CHAR_NULL) {
                    // Cool, can use returned char is, then.
                    ;
                } else {
                    c = fullyResolveEntity(mCustomEntities, mGeneralEntities, false);
                    if (c == CHAR_NULL) {
                        continue; // will just update input buffer
                    }
                }
            } else if (c <= CHAR_SPACE) {
                if (c == CHAR_NULL) {
                    throwNullChar();
                }
                if (normalize) {
                    c = CHAR_SPACE;
                }
            }
            hash = (hash * 31) + (int) c;

            // Need to expand working buffer?
            if (textPtr >= bufSize) {
                char[] old = buf;
                buf = new char[old.length << 1];
                System.arraycopy(old, 0, buf, 0, old.length);
                bufSize = buf.length;
            }
            buf[textPtr++] = c;
        }
    }
*/

    /**
     * Method that will parse an attribute value enclosed in quotes, using
     * an {@link TextBuilder} instance. Will not normalize white space inside
     * attribute value.
     */
    protected void parseNonNormalizedAttrValue(char openingQuote, TextBuilder tb)
        throws IOException, XMLStreamException
    {
        char[] outBuf = tb.getCharBuffer();
        int outPtr = tb.getCharSize();
        int outLen = outBuf.length;
        WstxInputSource currScope = mInput;

        while (true) {
            char c = (mInputPtr < mInputLen) ? mInputBuffer[mInputPtr++]
                : getNextChar(SUFFIX_IN_ATTR_VALUE);
            // Let's do a quick for most attribute content chars:
            if (c < CHAR_FIRST_PURE_TEXT) {
                if (c <= CHAR_SPACE) {
                    if (c == '\n') {
                        markLF();
                    } else if (c == '\r') {
                        c = getNextChar(SUFFIX_IN_ATTR_VALUE);
                        if (c != '\n') { // nope, not 2-char lf (Mac?)
                            --mInputPtr;
                            c = mCfgNormalizeLFs ? '\n' : '\r';
                        } else {
                            if (mCfgNormalizeLFs) {
                                // c is fine, then...
                            } else {
                                // Ok, except need to add leading '\r' first
                                if (outPtr >= outLen) {
                                    outBuf = tb.bufferFull(1);
                                    outLen = outBuf.length;
                                }
                                outBuf[outPtr++] = '\r';
                                // c is fine to continue
                            }
                        }
                        markLF();
                    } else if (c == CHAR_NULL) {
                        throwNullChar();
                    }
                } else if (c == openingQuote) {
                    /* 06-Aug-2004, TSa: Can get these via entities; only "real"
                     *    end quotes in same scope count. Note, too, that since
                     *    this will only be done at root level, there's no need
                     *    to check for "runaway" values; they'll hit EOF
                     */
                    if (mInput == currScope) {
                        break;
                    }
                } else if (c == '&') { // an entity of some sort...
                    if (inputInBuffer() >= 3
                        && (c = resolveSimpleEntity(true)) != CHAR_NULL) {
                        // Ok, fine, c is whatever it is
                    } else { // full entity just changes buffer...
                        c = fullyResolveEntity(mCustomEntities, mGeneralEntities, false);
                        // need to skip output, thusly
                        if (c == CHAR_NULL) {
                            continue;
                        }
                    }
                } else if (c == '<') {
                    throwParseError("Unexpected '<' "+SUFFIX_IN_ATTR_VALUE);
                }
            } // if (c < CHAR_FIRST_PURE_TEXT)

            // Ok, let's just add char in, whatever it was
            if (outPtr >= outLen) {
                outBuf = tb.bufferFull(1);
                outLen = outBuf.length;
            }
            outBuf[outPtr++] = c;
        }

        // Fine; let's tell TextBuild we're done:
        tb.setBufferSize(outPtr);
    }

    /**
     * Method that will parse an attribute value enclosed in quotes, using
     * an {@link TextBuilder} instance. Will normalize white space inside
     * attribute value using default XML rules (change linefeeds to spaces
     * etc.; but won't use DTD information for further coalescing).
     *
     * @param openingQuote Quote character (single or double quote) for
     *   this attribute value
     * @param tb TextBuilder into which attribute value will be added
     */
    protected void parseNormalizedAttrValue(char openingQuote, TextBuilder tb)
        throws IOException, XMLStreamException
    {
        char[] outBuf = tb.getCharBuffer();
        int outPtr = tb.getCharSize();
        int outLen = outBuf.length;
        WstxInputSource currScope = mInput;

        while (true) {
            char c = (mInputPtr < mInputLen) ? mInputBuffer[mInputPtr++]
                : getNextChar(SUFFIX_IN_ATTR_VALUE);
            // Let's do a quick for most attribute content chars:
            if (c < CHAR_FIRST_PURE_TEXT) {
                if (c <= CHAR_SPACE) {
                    if (c == '\n') {
                        markLF();
                    } else if (c == '\r') {
                        c = getNextChar(SUFFIX_IN_ATTR_VALUE);
                        if (c != '\n') { // nope, not 2-char lf (Mac?)
                            --mInputPtr;
                        }
                        markLF();
                    } else if (c == CHAR_NULL) {
                        throwNullChar();
                    }
                    // Whatever it was, it'll be 'normal' space now.
                    c = CHAR_SPACE;
                } else if (c == openingQuote) {
                    /* 06-Aug-2004, TSa: Can get these via entities; only "real"
                     *    end quotes in same scope count. Note, too, that since
                     *    this will only be done at root level, there's no need
                     *    to check for "runaway" values; they'll hit EOF
                     */
                    if (mInput == currScope) {
                        break;
                    }
                } else if (c == '&') { // an entity of some sort...
                    if (inputInBuffer() >= 3
                        && (c = resolveSimpleEntity(true)) != CHAR_NULL) {
                        // Ok, fine, c is whatever it is
                        ;
                    } else { // full entity just changes buffer...
                        c = fullyResolveEntity(mCustomEntities, mGeneralEntities, false);
                        if (c == CHAR_NULL) {
                            // need to skip output, thusly (expanded to new input source)
                            continue;
                        }
                    }
                } else if (c == '<') {
                    throwParseError("Unexpected '<' "+SUFFIX_IN_ATTR_VALUE);
                }
            } // if (c < CHAR_FIRST_PURE_TEXT)

            // Ok, let's just add char in, whatever it was
            if (outPtr >= outLen) {
                outBuf = tb.bufferFull(1);
                outLen = outBuf.length;
            }
            outBuf[outPtr++] = c;
        }

        // Fine; let's tell TextBuild we're done:
        tb.setBufferSize(outPtr);
    }
    
    /*
    /////////////////////////////////////////////////////
    // Internal methods, parsing prolog (before root) and
    // epilog
    /////////////////////////////////////////////////////
     */
    
    protected void initProlog(InputBootstrapper bs)
        throws IOException, XMLStreamException
    {
        /* At this point boot-strap code has read all the data we need...
         * we just have to get information from it.
         */
        // 19-Oct-2004, TSa: These were mixed up:
        //mDocInputEncoding = bs.getDeclaredEncoding();
        //mDocCharEncoding = bs.getAppEncoding();
        mDocInputEncoding = bs.getAppEncoding();
        mDocCharEncoding = bs.getDeclaredEncoding();

        mDocXmlVersion = bs.getVersion();

        String sa = bs.getStandalone();
        if (sa == null) {
            mDocStandalone = DOC_STANDALONE_UNKNOWN;
        } else {
            if ("yes".equals(sa)) {
                mDocStandalone = DOC_STANDALONE_YES;
            } else {
                mDocStandalone = DOC_STANDALONE_NO;
            }
        }

        /* Ok; either we got declaration or not, but in either case we can
         * now initialize prolog parsing settings, without having to really
         * parse anything more.
         */
        mParseState = STATE_PROLOG;
    }

    /**
     * Method called to find type of next token in prolog; either reading
     * just enough information to know the type (lazy parsing), or the
     * full contents (non-lazy)
     */
    private void nextFromProlog(boolean isProlog)
        throws IOException, XMLStreamException
    {
        int i;

        // First, do we need to finish currently open token?
        if (mStTokenUnfinished) {
            mStTokenUnfinished = false;
            i = skipToken();
        } else {
            i = getNext();
        }

        // Any white space to parse or skip?
        if (i <= CHAR_SPACE && i >= 0) {
            // Need to return as an event?
            if (hasConfigFlags(CFG_REPORT_PROLOG_WS)) {
                mCurrToken = SPACE;
                if (readSpacePrimary((char) i, true)) {
                    // no need to worry about coalescing
                    mStTokenUnfinished = false;
                } else {
                    if (mCfgLazyParsing) {
                        mStTokenUnfinished = true;
                    } else {
                        mStTokenUnfinished = false;
                        readSpaceSecondary(true);
                    }
                }
                return;
            }
            // If not, can skip it right away
            i = getNextAfterWS((char)i);
        }

        // Did we hit EOF?
        if (i < 0) {
            if (mSymbols.isDirty()) {
                mOwner.updateSymbolTable(mSymbols);
            }
            mCurrToken = END_DOCUMENT;
            // It's ok to get EOF from epilog but not from prolog
            if (isProlog) {
                throwUnexpectedEOF(SUFFIX_IN_PROLOG);
            }
            return;
        }

        // Now we better have a lt...
        if (i != '<') {
            throwUnexpectedChar(i, (isProlog ? SUFFIX_IN_PROLOG : SUFFIX_IN_EPILOG)
                                +"; expected '<'");
        }

        // And then it should be easy to figure out type:
        char c = getNextChar(isProlog ? SUFFIX_IN_PROLOG : SUFFIX_IN_EPILOG);

        if (c == '?') { // proc. inst
            mCurrToken = PROCESSING_INSTRUCTION;
            readPIPrimary();
        } else  if (c == '!') { // DOCTYPE or comment (or CDATA, but not legal here)
            // Need to figure out bit more first...
            nextFromPrologBang(isProlog);
        } else if (c == '/') { // end tag not allowed...
            if (isProlog) {
                throwParseError("Unexpected character combination '</' in prolog.");
            }
            throwParseError("Unexpected character combination '</' in epilog (extra close tag?).");
        } else if (c == ':' || isNameStartChar(c)) {
            // Root element, only allowed after prolog
            if (!isProlog) {
                throwParseError("Illegal to have multiple roots (start tag in epilog?).");
            }
            // Need to change state, first:
            mParseState = STATE_TREE;
            handleStartElem(c);
            // Does name match with DOCTYPE declaration (if any)?
            /* 21-Jul-2004, TSa: Only check this if we are supporting
             *   DTDs (but not only if validating)
             */
            if (mRootLName != null) {
                if (hasConfigFlags(CFG_SUPPORT_DTD)) {
                    if (!mElementStack.matches(mRootPrefix, mRootLName)) {
                        String str = (mRootPrefix == null) ? mRootLName
                            : (mRootPrefix + ":" + mRootLName);
                        throwParseError("Wrong root element <"
                                        +mElementStack.getTopElementDesc()
                                        +"> (expected <"+str+">)");
                    }
                }
            }
            mCurrToken = START_ELEMENT;
        } else {
            throwUnexpectedChar(c, (isProlog ? SUFFIX_IN_PROLOG : SUFFIX_IN_EPILOG)
                                +", after '<'.");
        }

        // Ok; final twist, maybe we do NOT want lazy parsing?
        if (!mCfgLazyParsing && mStTokenUnfinished) {
            finishToken();
        }
    }

    /**
     * Called after characters '&lt;!' have been found; expectation is that
     * it'll either be DOCTYPE declaration (if we are in prolog and haven't
     * yet seen one), or a comment. CDATA is not legal here; it would start
     * same way otherwise.
     */
    private void nextFromPrologBang(boolean isProlog)
        throws IOException, XMLStreamException
    {
        int i = getNext();
        if (i < 0) {
            throwUnexpectedEOF(SUFFIX_IN_PROLOG);
        }
        if (i == 'D') { // Doctype declaration?
            String keyw = checkKeyword('D', "DOCTYPE");
            if (keyw != null) {
                throwParseError("Unrecognized XML directive '<!"+keyw+"' (misspelled DOCTYPE?).");
            }
            
            if (!isProlog) {
                throwParseError(ErrorConsts.ERR_DTD_IN_EPILOG);
            }
            if (mStDoctypeFound) {
                throwParseError(ErrorConsts.ERR_DTD_DUP);
            }
            mStDoctypeFound = true;
            // Ok; let's read main input (all but internal subset)
            mCurrToken = DTD;
            startDTD();
            return;
        } else if (i == '-') { // comment
            char c = getNextChar(isProlog ? SUFFIX_IN_PROLOG : SUFFIX_IN_EPILOG);
            if (c != '-') {
                throwUnexpectedChar(i, " (malformed comment?)");
            }
            // Likewise, let's delay actual parsing/skipping.
            mStTokenUnfinished = true;
            mCurrToken = COMMENT;
            return;
        } else if (i == '[') { // erroneous CDATA?
            i = peekNext();
            // Let's just add bit of heuristics, to get better error msg
            if (i == 'C') {
                throwUnexpectedChar(i, ErrorConsts.ERR_CDATA_IN_EPILOG);
            }
        }

        throwUnexpectedChar(i, " after '<!' (malformed comment?)");
    }

    /**
     * Method called to parse through most of DOCTYPE declaration; excluding
     * optional internal subset.
     */
    private void startDTD()
        throws IOException, XMLStreamException
    {
        /* 21-Nov-2004, TSa: Let's make sure that the buffer gets cleared
         *   at this point. Need not start branching yet, however, since
         *   DTD event is often skipped.
         */
        mTextBuffer.resetInitialized();

        /* So, what we need is:<code>
         *  <!DOCTYPE' S Name (S ExternalID)? S? ('[' intSubset ']' S?)? '>
         *</code>. And we have already read the DOCTYPE token.
         */

        char c = getNextInCurrAfterWS(SUFFIX_IN_DTD);
        if (mCfgNsEnabled) {
            String str = parseLocalName(c);
            c = getNextChar(SUFFIX_IN_DTD);
            if (c == ':') { // Ok, got namespace and local name
                mRootPrefix = str;
                mRootLName = parseLocalName(getNextChar(SUFFIX_EOF_EXP_NAME));
            } else if (c <= CHAR_SPACE || c == '[' || c == '>') {
                // ok to get white space or '[', or closing '>'
                --mInputPtr; // pushback
                mRootPrefix = null;
                mRootLName = str;
            } else {
                throwUnexpectedChar(c, " in DOCTYPE declaration; expected '[' or white space.");
            }
        } else {
            mRootLName = parseFullName(c);
            mRootPrefix = null;
        }

        // Ok, fine, what next?
        c = getNextCharAfterWS(SUFFIX_IN_DTD);
        if (c != '[' && c != '>') {
            String keyw = null;
            
            if (c == 'P') {
                keyw = checkKeyword(getNextChar(SUFFIX_IN_DTD), "UBLIC");
                if (keyw != null) {
                    keyw = "P" + keyw;
                } else {
                    c = getNextCharAfterWS(SUFFIX_IN_DTD);
                    if (c != '"' && c != '\'') {
                        throwUnexpectedChar(c, SUFFIX_IN_DTD+"; expected a public identifier.");
                    }
                    mDtdPublicId = parsePublicId(c, mCfgNormalizeLFs, SUFFIX_IN_DTD);
                    if (mDtdPublicId.length() == 0) {
                        // According to XML specs, this isn't illegal?
                        mDtdPublicId = null;
                    }
                    c = getNextCharAfterWS(SUFFIX_IN_DTD);
                    if (c != '"' && c != '\'') {
                        throwParseError(SUFFIX_IN_DTD+"; expected a system identifier.");
                    }
                    mDtdSystemId = parseSystemId(c, mCfgNormalizeLFs, SUFFIX_IN_DTD);
                    if (mDtdSystemId.length() == 0) {
                        // According to XML specs, this isn't illegal?
                        mDtdSystemId = null;
                    }
                }
            } else if (c == 'S') {
                mDtdPublicId = null;
                keyw = checkKeyword(getNextChar(SUFFIX_IN_DTD), "YSTEM");
                if (keyw != null) {
                    keyw = "S" + keyw;
                } else {
                    c = getNextCharAfterWS(SUFFIX_IN_DTD);
                    if (c != '"' && c != '\'') {
                        throwUnexpectedChar(c, SUFFIX_IN_DTD+"; expected a system identifier.");
                    }
                    mDtdSystemId = parseSystemId(c, mCfgNormalizeLFs, SUFFIX_IN_DTD);
                    if (mDtdSystemId.length() == 0) {
                        // According to XML specs, this isn't illegal?
                        mDtdSystemId = null;
                    }
                }
            } else {
                if (!isNameStartChar(c)) {
                    throwUnexpectedChar(c, SUFFIX_IN_DTD+"; expected keywords 'PUBLIC' or 'SYSTEM'.");
                } else {
                    --mInputPtr;
              keyw = checkKeyword(c, "SYSTEM"); // keyword passed in doesn't matter
                }
            }
            
            if (keyw != null) { // error:
                throwParseError("Unexpected keyword '"+keyw+"'; expected 'PUBLIC' or 'SYSTEM'");
            }
            
            // Ok, should be done with external DTD identifier:
            c = getNextCharAfterWS(SUFFIX_IN_DTD);
        }
        
        if (c == '[') { // internal subset
            ;
        } else {
            if (c != '>') {
                throwUnexpectedChar(c, SUFFIX_IN_DTD+"; expected closing '>'.");
            }
        }
        
        /* Actually, let's just push whatever char it is, back; this way
         * we can lazily initialize text buffer with DOCTYPE declaration
         * if/as necessary, even if there's no internal subset.
         */
        --mInputPtr; // pushback
        mStTokenUnfinished = true;
    }

    /**
     * This method gets called to handle remainder of DOCTYPE declaration,
     * essentially the optional internal subset. This class implements the
     * basic "ignore it" functionality, but can optionally still store copy
     * of the contents to the read buffer.
     *<p>
     * NOTE: Since this default implementation will be overridden by
     * some sub-classes, make sure you do NOT change the method signature.
     *
     * @param copyContents If true, will copy contents of the internal
     *   subset of DOCTYPE declaration
     *   in the text buffer; if false, will just completely ignore the
     *   subset (if one found).
     */
    protected void finishDTD(boolean copyContents)
        throws IOException, XMLStreamException
    {
        /* We know there are no spaces, as this char was read and pushed
         * back earlier...
         */
        char c = getNextChar(SUFFIX_IN_DTD);
        if (c == '[') {
            // Do we need to get contents as text too?
            if (copyContents) {
                ((BranchingReaderSource) mInput).startBranch(mTextBuffer, mInputPtr, mCfgNormalizeLFs);
            }

            try {
                mConfig.getDtdReader().skipInternalSubset(this, mInput, mConfig);
            } finally {
                /* Let's close branching in any and every case (may allow
                 * graceful recovery in error cases in future
                 */
                if (copyContents) {
                    /* Need to "push back" ']' got in the succesful case
                     * (that's -1 part below);
                     * in error case it'll just be whatever last char was.
                     */
                    ((BranchingReaderSource) mInput).endBranch(mInputPtr-1);
                }
            }

            // And then we need closing '>'
            c = getNextCharAfterWS(SUFFIX_IN_DTD_INTERNAL);
        }

        if (c != '>') {
            throwUnexpectedChar(c, "; expected '>' to finish DOCTYPE declaration.");
        }
    }

    /*
    ////////////////////////////////////////////////////
    // Internal methods, main parsing (inside root)
    ////////////////////////////////////////////////////
     */

    private int nextFromTree()
        throws IOException, XMLStreamException
    {
        int i;

        // First, do we need to finish currently open token?
        if (mStTokenUnfinished) {
            mStTokenUnfinished = false;
            i = skipToken();
        } else {
            /* Start/end elements are never unfinished (ie. are always
             * completely read in)
             */
            if (mCurrToken == START_ELEMENT) {
                // Start tag may be an empty tag:
                if (mStEmptyElem) {
                    // and if so, we'll then get 'virtual' close tag:
                    mStEmptyElem = false;
                    return END_ELEMENT;
                }
            } else if (mCurrToken == END_ELEMENT) {
                // Close tag removes current element from stack
                mVldContent = mElementStack.pop();
                // ... which may be the root element?
                if (mElementStack.isEmpty()) {
                    // if so, we'll get to epilog
                    mParseState = STATE_EPILOG;
                    nextFromProlog(false);
                    return mCurrToken;
                }
            } else if (mStPartialCData) {
                /* The tricky part here is just to ensure there's at least
                 * one character... but let's just read it like a new
                 * CData section first:
                 */
                char c = (mInputPtr < mInputLen) ? mInputBuffer[mInputPtr++]
                    : getNextChar(SUFFIX_IN_CDATA);
                if (readCDataPrimary(c)) { // got it all!
                    /* note: can not be in coalescing mode at this point;
                     * as we can never have partial cdata without unfinished
                     * token
                     */
                    return CDATA;
                }
                /* Hmmh. Have to verify we get at least one char from
                 * CData section...
                 */
                if (mTextBuffer.size() == 0
                    && readCDataSecondary(mCfgLazyParsing
                                          ? 1 : mShortestTextSegment)) {
                    // Ok, all of it read
                    if (mTextBuffer.size() > 0) {
                        // And had some contents
                        return CDATA;
                    }
                    // if nothing read, we'll just fall back (see below)
                } else {
                    // Partial read, with at least one char, good enough
                    // ... and may actually be all we need, too?
                    mStTokenUnfinished = mTextBuffer.size() < mShortestTextSegment;
                    return CDATA;
                }
                
                /* If we get here, it was the end of the section, without
                 * any more text inside CDATA, so let's just continue
                 */
            }
            i = getNext();
        }

        if (i < 0) {
            throwUnexpectedEOF("; was expecting a close tag.");
        }

        /* 26-Aug-2004, TSa: We have to deal with entities, usually, if
         *   they are the next thing; even in non-expanding mode there
         *   are entities and then there are entities... :-)
         *   Let's start with char entities; they can kind of be expanded.
         */
        while (i == '&') {
            /* 30-Aug-2004, TSa: In some contexts entities are not
             *    allowed in any way, shape or form:
             */
            if (mVldContent == CONTENT_ALLOW_NONE) {
                /* May be char entity, general entity; whatever it is it's
                 * invalid!
                 */
                reportInvalidContent(ENTITY_REFERENCE);
            }

            /* Need to call different methods based on whether we can do
             * automatic entity expansion or not:
             */
            char c = mCfgReplaceEntities ?
                fullyResolveEntity(mCustomEntities, mGeneralEntities, true)
                : resolveCharOnlyEntity(true);

            if (c != CHAR_NULL) {
                /* Char-entity... need to initialize text output buffer, then;
                 * independent of whether it'll be needed or not.
                 */
                /* 30-Aug-2004, TSa: In some contexts only white space is
                 *   accepted...
                 */
                if (mVldContent == CONTENT_ALLOW_NON_MIXED) {
                    /* !!! 06-Sep-2004, TSa: Is white space even allowed
                     *   via (char) entity expansion? If it is, we should
                     *   try to read primary white space; if not, throw
                     *   an exception.
                     */
                    if (c > CHAR_SPACE) {
                        reportInvalidContent(CHARACTERS);
                    }
                }

                TextBuffer tb = mTextBuffer;
                tb.resetInitialized();
                tb.append(c);
                mStTokenUnfinished = true;
                return CHARACTERS;
            }

            /* Nope; was a general entity... in auto-mode, it's now been
             * expanded; in non-auto, need to figure out entity itself.
             */
            if (!mCfgReplaceEntities) {
                EntityDecl ed = resolveNonCharEntity
                    (mCustomEntities, mGeneralEntities);
                if (ed == null) {
                    throwParseError("Internal error: Entity neither char nor general entity; yet no exception thrown so far");
                }
                mCurrEntity = ed;
                // Last check; needs to be a parsed entity:
                if (!ed.isParsed()) {
                    throwParseError("Reference to unparsed entity '"
                                    +ed.getName()+"' from content not allowed.");
                }
                return ENTITY_REFERENCE;
            }

            // Otherwise automatic expansion fine; just need the next char:
            i = getNextChar(SUFFIX_IN_DOC);
        }

        if (i == '<') { // Markup
            // And then it should be easy to figure out type:
            char c = getNextChar(SUFFIX_IN_ELEMENT);
            if (c == '?') { // proc. inst
                // 30-Aug-2004, TSa: Not legal for EMPTY elements
                if (mVldContent == CONTENT_ALLOW_NONE) {
                    reportInvalidContent(PROCESSING_INSTRUCTION);
                }
                readPIPrimary();
                return PROCESSING_INSTRUCTION;
            }
            
            if (c == '!') { // CDATA or comment
                // Need to figure out bit more first...
                int type = nextFromTreeCommentOrCData();
                // 30-Aug-2004, TSa: Not legal for EMPTY elements
                if (mVldContent == CONTENT_ALLOW_NONE) {
                    reportInvalidContent(type);
                }
                return type;
            }
            if (c == '/') { // always legal
                readEndElem();
                return END_ELEMENT;
            }

            if (c == ':' || isNameStartChar(c)) {
                // 30-Aug-2004, TSa: Not legal for EMPTY elements
                if (mVldContent == CONTENT_ALLOW_NONE) {
                    reportInvalidContent(START_ELEMENT);
                }
                handleStartElem(c);
                return START_ELEMENT;
            }
            if (c == '[') {
                throwUnexpectedChar(c, " in content after '<' (malformed <![CDATA[]] directive?)");
            }
            throwUnexpectedChar(c, " in content after '<' (malformed start element?).");
        }

        /* Text... ok; better parse the 'easy' (consequtive) portions right
         * away, since that's practically free (still need to scan those
         * characters no matter what, even if skipping).
         */
        /* But first, do we expect to get ignorable white space (only happens
         * in validating mode)? If so, needs bit different handling:
         */
        if (mVldContent <= CONTENT_ALLOW_NON_MIXED) {
            if (mVldContent == CONTENT_ALLOW_NONE
                || (i > CHAR_SPACE)) {
                reportInvalidContent(CHARACTERS);
            }
            /* Note: need not worry about coalescing, since non-whitespace
             * text is illegal (ie. can not have CDATA)
             */
            mStTokenUnfinished = !readSpacePrimary((char) i, false);
            return SPACE;
        }

        // Further, when coalescing, can not be sure if we REALLY got it all
        if (readTextPrimary((char) i)) { // reached following markup
            mStTokenUnfinished = mCfgCoalesceText;
        } else {
            // If not coalescing, this may be enough for current event
            mStTokenUnfinished = mCfgCoalesceText
                || mTextBuffer.size() < mShortestTextSegment;
        }
        return CHARACTERS;
    }

    /**
     * Method that takes care of parsing of start elements; including
     * full parsing of namespace declarations and attributes, as well as
     * namespace resolution.
     */
    private void handleStartElem(char c)
        throws IOException, XMLStreamException
    {
        // First, name of element:
        String prefix, localName;

        if (mCfgNsEnabled) {
            String str = parseLocalName(c);
            c = (mInputPtr < mInputLen) ?
                mInputBuffer[mInputPtr++] : getNextCharFromCurrent(SUFFIX_EOF_EXP_NAME);
            if (c == ':') { // Ok, got namespace and local name
                prefix = str;
                c = (mInputPtr < mInputLen) ?
                    mInputBuffer[mInputPtr++] : getNextCharFromCurrent(SUFFIX_EOF_EXP_NAME);
                localName = parseLocalName(c);
            } else {
                --mInputPtr; // pushback
                prefix = DEFAULT_NS_PREFIX;
                localName = str;
            }
            mElementStack.push(prefix, localName);
            /* Enough about element name itself; let's then parse attributes
             * and namespace declarations. Split into another method for clarity,
             * and so that maybe JIT has easier time to optimize it separately.
             * And who knows, maybe someone wants to override it as well?
             */
            handleNsAttrs();
        } else { // Namespace handling not enabled:
            mElementStack.push(parseFullName(c));
            handleNonNsAttrs();
        }
    }

    private void handleNsAttrs()
        throws IOException, XMLStreamException
    {
        AttributeCollector ac = mAttrCollector;
        boolean isEmpty = false;
        boolean gotDefaultNS = false;

        while (true) {
            char c = getNextCharFromCurrent(SUFFIX_IN_ELEMENT);
            if (c <= CHAR_SPACE) {
                c = getNextInCurrAfterWS(SUFFIX_IN_ELEMENT, c);
            } else if (c != '/' && c != '>') {
                throwUnexpectedChar(c, " excepted space, or '>' or \"/>\"");
            }

            if (c == '/') {
                c = getNextCharFromCurrent(SUFFIX_IN_ELEMENT);
                if (c != '>') {
                    throwUnexpectedChar(c, " expected '>'");
                }
                isEmpty = true;
                break;
            } else if (c == '>') {
                break;
            } else if (c == '<') {
                throwParseError("Unexpected '<' character in element (missing closing '>'?)");
            }

            String prefix, localName;
            String str = parseLocalName(c);
            c = (mInputPtr < mInputLen) ?
                mInputBuffer[mInputPtr++] : getNextCharFromCurrent(SUFFIX_EOF_EXP_NAME);
            if (c == ':') { // Ok, got namespace and local name
                prefix = str;
                c = (mInputPtr < mInputLen) ?
                    mInputBuffer[mInputPtr++] : getNextCharFromCurrent(SUFFIX_EOF_EXP_NAME);
                localName = parseLocalName(c);
            } else {
                --mInputPtr; // pushback
                prefix = DEFAULT_NS_PREFIX;
                localName = str;
            }

            c = (mInputPtr < mInputLen) ?
                mInputBuffer[mInputPtr++] : getNextCharFromCurrent(SUFFIX_IN_ELEMENT);
            if (c <= CHAR_SPACE) {
                c = getNextCharAfterWS(c, SUFFIX_IN_ELEMENT);
            }
            if (c != '=') {
                throwUnexpectedChar(c, " expected '='");
            }
            c = (mInputPtr < mInputLen) ?
                mInputBuffer[mInputPtr++] : getNextCharFromCurrent(SUFFIX_IN_ELEMENT);
            if (c <= CHAR_SPACE) {
                c = getNextCharAfterWS(c, SUFFIX_IN_ELEMENT);
            }

            // And then a quote:
            if (c != '"' && c != '\'') {
                throwUnexpectedChar(c, SUFFIX_IN_ELEMENT+" Expected a quote");
            }

            // And then the actual value
            int startLen = -1;
            TextBuilder tb;

            if (prefix == sPrefixXmlns) { // non-default namespace declaration
                tb = ac.getNsBuilder(localName);
                // returns null if it's a dupe:
                if (tb == null) {
                    throwParseError("Duplicate declaration for namespace prefix '"+localName+"'.");
                }
                startLen = tb.getCharSize();
            } else if (localName == sPrefixXmlns && prefix == DEFAULT_NS_PREFIX) {
                tb = ac.getDefaultNsBuilder();
                // Can only have one default ns declaration...
                if (tb.size() > 0) {
                    throwParseError("Duplicate default namespace declaration.");
                }
            } else {
                tb = ac.getAttrBuilder(prefix, localName);
            }
            tb.startNewEntry();

            if (mCfgNormalizeAttrs) {
                parseNormalizedAttrValue(c, tb);
            } else {
                parseNonNormalizedAttrValue(c, tb);
            }
            /* 19-Jul-2004, TSa: Need to check that non-default namespace
             *     URI is NOT empty, as per XML namespace specs, #2,
             *    ("...In such declarations, the namespace name may not
             *      be empty.")
             */
            /* (note: startLen is only set to first char position for
             * non-default NS declarations, see above...)
             */
            if (startLen >= 0 && tb.getCharSize() == startLen) { // is empty!
                throwParseError("Non-default namespace can not map to empty URI (as per Namespace 1.0 # 2)");
            }
        }

        /* Need to update namespace stack; it would be possible to avoid
         * that for empty tags when skipping, but that would hide some
         * errors.
         * Can't yet pop the stack for empty elements; that's done when
         * accessing the dummy close element later on.
         */
        mVldContent = mElementStack.resolveElem(hasConfigFlags(CFG_INTERN_NS_URIS));
        mStEmptyElem = isEmpty;
    }

    private void handleNonNsAttrs()
        throws IOException, XMLStreamException
    {
        AttributeCollector ac = mAttrCollector;
        boolean isEmpty = false;

        while (true) {
            char c = getNextCharFromCurrent(SUFFIX_IN_ELEMENT);
            if (c <= CHAR_SPACE) {
                c = getNextInCurrAfterWS(SUFFIX_IN_ELEMENT, c);
            } else if (c != '/' && c != '>') {
                throwUnexpectedChar(c, " excepted space, or '>' or \"/>\"");
            }
            if (c == '/') {
                c = getNextCharFromCurrent(SUFFIX_IN_ELEMENT);
                if (c != '>') {
                    throwUnexpectedChar(c, " expected '>'");
                }
                isEmpty = true;
                break;
            } else if (c == '>') {
                break;
            } else if (c == '<') {
                throwParseError("Unexpected '<' character in element (missing closing '>'?)");
            }

            String name = parseFullName(c);
            TextBuilder tb = ac.getAttrBuilder(null, name);
            c = (mInputPtr < mInputLen) ?
                mInputBuffer[mInputPtr++] : getNextCharFromCurrent(SUFFIX_IN_ELEMENT);
            if (c <= CHAR_SPACE) {
                c = getNextCharAfterWS(c, SUFFIX_IN_ELEMENT);
            }
            if (c != '=') {
                throwUnexpectedChar(c, " expected '='");
            }
            c = (mInputPtr < mInputLen) ?
                mInputBuffer[mInputPtr++] : getNextCharFromCurrent(SUFFIX_IN_ELEMENT);
            if (c <= CHAR_SPACE) {
                c = getNextCharAfterWS(c, SUFFIX_IN_ELEMENT);
            }

            // And then a quote:
            if (c != '"' && c != '\'') {
                throwUnexpectedChar(c, SUFFIX_IN_ELEMENT+" Expected a quote");
            }

            // And then the actual value
            tb.startNewEntry();

            if (mCfgNormalizeAttrs) {
                parseNormalizedAttrValue(c, tb);
            } else {
                parseNonNormalizedAttrValue(c, tb);
            }
        }

        mVldContent = mElementStack.resolveElem(false);
        mStEmptyElem = isEmpty;
    }

    /**
     * Method called to completely read a close tag, and update element
     * stack appropriately (including checking that tag matches etc).
     */
    private void readEndElem()
        throws IOException, XMLStreamException
    {
        if (mElementStack.isEmpty()) { // no start element?
            // Let's just offline this for clarity
            reportExtraEndElem();
            return;
        }

        char c = (mInputPtr < mInputLen) ? mInputBuffer[mInputPtr++]
            : getNextCharFromCurrent(SUFFIX_IN_CLOSE_ELEMENT);
        // Quick check first; missing name?
        if  (!isNameStartChar(c) && c != ':') {
            if (c <= CHAR_SPACE) { // space
                throwUnexpectedChar(c, "; missing element name?");
            }
            throwUnexpectedChar(c, "; expected an element name.");
        }

        /* Ok, now; good thing is we know exactly what to compare
         * against...
         */
        String expPrefix = mElementStack.getPrefix();
        String expLocalName = mElementStack.getLocalName();

        // Prefix to match?
        if (expPrefix != null && expPrefix.length() > 0) {
            int len = expPrefix.length();
            int i = 0;

            while (true){
                if (c != expPrefix.charAt(i)) {
                    reportWrongEndPrefix(expPrefix, expLocalName, i);
                    return;
                }
                if (++i >= len) {
                    break;
                }
                c = (mInputPtr < mInputLen) ? mInputBuffer[mInputPtr++]
                    : getNextCharFromCurrent(SUFFIX_IN_CLOSE_ELEMENT);
            }
            // And then we should get a colon
            c = (mInputPtr < mInputLen) ? mInputBuffer[mInputPtr++]
                : getNextCharFromCurrent(SUFFIX_IN_CLOSE_ELEMENT);
            if (c != ':') {
                reportWrongEndPrefix(expPrefix, expLocalName, i);
                return;
            }
            c = (mInputPtr < mInputLen) ? mInputBuffer[mInputPtr++]
                : getNextCharFromCurrent(SUFFIX_IN_CLOSE_ELEMENT);
        } else {
            /* May have an extra colon? (does XML specs allow that?); if so,
             * need to skip it:
             */
            // ... probably only allowed if start tag had it too?
            if (c == ':') {
                c = (mInputPtr < mInputLen) ? mInputBuffer[mInputPtr++]
                    : getNextCharFromCurrent(SUFFIX_IN_CLOSE_ELEMENT);
            }
        }

        // Ok, then, does the local name match?
        int len = expLocalName.length();
        int i = 0;
        
        while (true){
            if (c != expLocalName.charAt(i)) {
                // Not a match...
                reportWrongEndElem(expPrefix, expLocalName, i);
                return;
            }
            if (++i >= len) {
                break;
            }
            c = (mInputPtr < mInputLen) ? mInputBuffer[mInputPtr++]
                : getNextCharFromCurrent(SUFFIX_IN_CLOSE_ELEMENT);
        }

        // Let's see if end element still continues, however?
        c = (mInputPtr < mInputLen) ? mInputBuffer[mInputPtr++]
            : getNextCharFromCurrent(SUFFIX_IN_CLOSE_ELEMENT);
        if (c <= CHAR_SPACE) {
            c = getNextCharAfterWS(c, SUFFIX_IN_CLOSE_ELEMENT);
        } else if (c == '>') {
            ;
        } else if (c == ':' || isNameChar(c)) {
            reportWrongEndElem(expPrefix, expLocalName, len);
        }

        // Ok, fine, match ok; now we just need the closing gt char.
        if (c != '>') {
            throwUnexpectedChar(c, SUFFIX_IN_CLOSE_ELEMENT+" Expected '>'.");
        }
    }

    private void reportExtraEndElem()
        throws IOException, XMLStreamException
    {
        String name = parseFNameForError();
        throwParseError("Unbalanced close tag </"+name+">; no open start tag.");
    }

    private void reportWrongEndPrefix(String prefix, String localName, int done)
        throws IOException, XMLStreamException
    {
        --mInputPtr; // pushback
        String fullName = prefix + ":" + localName;
        String rest = parseFNameForError();
        String actName = fullName.substring(0, done) + rest;
        throwParseError("Unexpected close tag </"+actName+">; expected </"
                        +fullName+">.");
    }

    private void reportWrongEndElem(String prefix, String localName, int done)
        throws IOException, XMLStreamException
    {
        --mInputPtr; // pushback
        String fullName;
        if (prefix != null && prefix.length() > 0) {
            fullName = prefix + ":" + localName;
            done += 1 + prefix.length();
        } else {
            fullName = localName;
        }
        String rest = parseFNameForError();
        String actName = fullName.substring(0, done) + rest;
        throwParseError("Unexpected close tag </"+actName+">; expected </"
                        +fullName+">.");
    }

    /**
     *<p>
     * Note: According to StAX 1.0, coalesced text events are always to be
     * returned as CHARACTERS, never as CDATA. And since at this point we
     * don't really know if there's anything to coalesce (but there may
     * be), let's convert CDATA if necessary.
     */
    private int nextFromTreeCommentOrCData()
        throws IOException, XMLStreamException
    {
        char c = getNextCharFromCurrent(SUFFIX_IN_DOC);
        if (c == '[') {
            checkCData();
            /* Good enough; it is a CDATA section... but let's just also
             * parse the easy ("free") stuff:
             */
            c = (mInputPtr < mInputLen) ? mInputBuffer[mInputPtr++]
                : getNextCharFromCurrent(SUFFIX_IN_CDATA);
            if (readCDataPrimary(c)) { // got it all
                mStTokenUnfinished = mCfgCoalesceText;
            } else { // partial
                // If not coalescing, this may be enough for current event
                mStTokenUnfinished = mCfgCoalesceText
                    || (mTextBuffer.size() < mShortestTextSegment);
            }
            return CDATA;
        }
        if (c == '-' && getNextCharFromCurrent(SUFFIX_IN_DOC) == '-') {
            mStTokenUnfinished = true;
            return COMMENT;
        }
        throwParseError("Unrecognized XML directive; expected CDATA or comment ('<![CDATA[' or '<!--').");
        return 0; // never gets here
    }

    /*
    ////////////////////////////////////////////////////
    // Internal methods, skipping
    ////////////////////////////////////////////////////
     */

    /**
     * Method called to skip last part of current token, when full token
     * has not been parsed. Generally happens when caller is not interested
     * in current token and just calls next() to iterate to next token.
     *
     * @return Next character after node has been skipped, or -1 if EOF
     *    follows
     */
    private int skipToken()
        throws IOException, XMLStreamException
    {
        switch (mCurrToken) {
        case CDATA:
            {
                /* 30-Aug-2004, TSa: Need to be careful here: we may
                 *    actually have finished with CDATA, but are just
                 *    coalescing... if so, need to skip first part of
                 *    skipping
                 */
                if (mStPartialCData) {
                    // Skipping CDATA is easy; just need to spot closing ]]&gt;
                    skipCommentOrCData(SUFFIX_IN_CDATA, ']', false);
                    mStPartialCData = false;
                }
                // Can't get EOF, as CDATA only legal in content tree
                char c = getNextChar(SUFFIX_IN_DOC);
                // ... except if coalescing, may need to skip more:
                if (mCfgCoalesceText) {
                    return skipCoalescedText(c);
                }
                return (int) c;
            }
                
        case COMMENT:
            skipCommentOrCData(SUFFIX_IN_COMMENT, '-', true);
            return getNextChar(SUFFIX_IN_DOC);

        case CHARACTERS:
            {
                char c = skipTokenText(getNextChar(SUFFIX_IN_DOC));
                // ... except if coalescing, need to skip more:
                if (mCfgCoalesceText) {
                    return skipCoalescedText(c);
                }
                return c;
            }

        case DTD:

            finishDTD(false);
            return getNextChar(SUFFIX_IN_PROLOG);

        case PROCESSING_INSTRUCTION:
            {
                while (true) {
                    char c = (mInputPtr < mInputLen)
                        ? mInputBuffer[mInputPtr++] : getNextCharFromCurrent(SUFFIX_IN_PROC_INSTR);
                    if (c == '?') {
                        do {
                            c = (mInputPtr < mInputLen)
                                ? mInputBuffer[mInputPtr++] : getNextCharFromCurrent(SUFFIX_IN_PROC_INSTR);
                        } while (c == '?');
                        if (c == '>') {
                            return getNext();
                        }
                    }
                    if (c <= CHAR_CR_LF_OR_NULL) {
                        if (c == '\n' || c == '\r') {
                            skipCRLF(c);
                        } else if (c == CHAR_NULL) {
                            throwNullChar();
                        }
                    }
                }
            }
            // can never get here

        case SPACE:
            while (true) {
                // Fairly easy to skip through white space...
                while (mInputPtr < mInputLen) {
                    char c = mInputBuffer[mInputPtr++];
                    if (c > CHAR_SPACE) {
                        return c;
                    }
                    if (c == CHAR_NULL) {
                        throwNullChar();
                    }
                }
                int ci = getNext();
                if (ci < 0 || ci > CHAR_SPACE) {
                    return ci;
                } else if (ci == 0) {
                    throwNullChar();
                }
            }
            // never gets in here

        case ENTITY_REFERENCE: // these should never end up in here...
        case ENTITY_DECLARATION:
        case NOTATION_DECLARATION:
        case START_DOCUMENT:
        case END_DOCUMENT:
            // As are start/end document
            throw new IllegalStateException("skipToken() called when current token is "+tokenTypeDesc(mCurrToken));

        case ATTRIBUTE:
        case NAMESPACE:
            // These two are never returned by this class
        case START_ELEMENT:
        case END_ELEMENT:
            /* Never called for elements tokens; start token handled
             * differently, end token always completely read in the first place
             */

        default:
            throw new IllegalStateException("Internal error: unexpected token "+tokenTypeDesc(mCurrToken));

        }
        // never gets this far
    }

    private void skipCommentOrCData(String errorMsg, char endChar, boolean preventDoubles)
        throws IOException, XMLStreamException
    {
        /* Let's skip all chars except for double-ending chars in
         * question (hyphen for comments, right brack for cdata)
         */
        while (true) {
            char c;
            do {
                c = (mInputPtr < mInputLen)
                    ? mInputBuffer[mInputPtr++] : getNextCharFromCurrent(errorMsg);
                if (c <= CHAR_CR_LF_OR_NULL) {
                    if (c == '\n' || c == '\r') {
                        skipCRLF(c);
                    } else if (c == CHAR_NULL) {
                        throwNullChar();
                    }
                }
            } while (c != endChar);

            // Now, we may be getting end mark; first need second marker char:.
            c = getNextChar(errorMsg);
            if (c == endChar) { // Probably?
                // Now; we should be getting a '>', most likely.
                c = getNextChar(errorMsg);
                if (c == '>') {
                    break;
                }
                if (preventDoubles) { // if not, it may be a problem...
                    throwParseError("String '--' not allowed in comment (missing '>'?)");
                }
                // Otherwise, let's loop to see if there is end
                while (c == endChar) {
                    c = (mInputPtr < mInputLen)
                        ? mInputBuffer[mInputPtr++] : getNextCharFromCurrent(errorMsg);
                }
                if (c == '>') {
                    break;
                }
            }

            // No match, did we get a linefeed?
            if (c <= CHAR_CR_LF_OR_NULL) {
                if (c == '\n' || c == '\r') {
                    skipCRLF(c);
                } else if (c == CHAR_NULL) {
                    throwNullChar();
                }
            }

            // Let's continue from beginning, then
        }
    }

    private int skipCoalescedText(char c)
        throws IOException, XMLStreamException
    {
        while (true) {
            // Ok, plain text or markup?
            if (c == '<') { // markup, maybe CDATA?
                // Need to distinguish "<![" from other tags/directives
                if (!ensureInput(3)) {
                    /* Most likely an error condition, but let's leave
                     * it up for other parts of code to complain.
                     */
                    return c;
                }
                if (mInputBuffer[mInputPtr] != '!'
                    || mInputBuffer[mInputPtr+1] != '[') {
                    // Nah, some other tag or directive
                    return c;
                }
                // Let's skip beginning parts, then:
                mInputPtr += 2;
                // And verify we get proper CDATA directive
                checkCData();
                skipCommentOrCData(SUFFIX_IN_CDATA, ']', false);
                c = getNextChar(SUFFIX_IN_DOC);
            } else { // nah, normal text, gotta skip
                c = skipTokenText(c);
                /* Did we hit an unexpandable entity? If so, need to
                 * return ampersand to the caller...
                 */
                if (c == '&') {
                    return c;
                }
            }
        }
    }

    private char skipTokenText(char c)
        throws IOException, XMLStreamException
    {
        /* Fairly easy; except for potential to have entities
         * expand to some crap?
         */
        while (true) {
            if (c == '<') {
                return c;
            }
            if (c == '&') {
                // Can entities be resolved automatically?
                if (mCfgReplaceEntities) {
                    // Let's first try quick resolution:
                    if ((mInputLen - mInputPtr) >= 3
                        && resolveSimpleEntity(true) != CHAR_NULL) {
                        ;
                    } else {
                        c = fullyResolveEntity(mCustomEntities, mGeneralEntities, true);
                        /* Either way, it's just fine; we don't care about
                         * returned single-char value.
                         */
                    }
                } else {
                    /* Can only skip character entities; others need to
                     * be returned separately.
                     */
                    if (resolveCharOnlyEntity(true) == CHAR_NULL) {
                        /* Now points to the char after ampersand, and we need
                         * to return the ampersand itself
                         */
                        return c;
                    }
                }
            } else if (c <= CHAR_CR_LF_OR_NULL) {
                if (c == '\r' || c == '\n') {
                    skipCRLF(c);
                } else if (c == CHAR_NULL) {
                    throwNullChar();
                }
            }
            c = (mInputPtr < mInputLen)
                ? mInputBuffer[mInputPtr++] : getNextChar(SUFFIX_IN_TEXT);
        }
    }

    /*
    ////////////////////////////////////////////////////
    // Internal methods, parsing
    ////////////////////////////////////////////////////
     */

    /**
     * Method called to read in contents of the token completely, if not
     * yet read. Generally called when caller needs to access anything
     * other than basic token type (except for elements), text contents
     * or such.
     */
    protected void finishToken()
        throws IOException, XMLStreamException
    {
        mStTokenUnfinished = false;
        switch (mCurrToken) {
        case CDATA:
            if (mCfgCoalesceText) {
                readCoalescedText(mCurrToken);
            } else {
                readCDataSecondary(mShortestTextSegment);
            }
            return;

        case CHARACTERS:
            if (mCfgCoalesceText) {
                readCoalescedText(mCurrToken);
            } else {
                readTextSecondary(mShortestTextSegment);
            }
            return;

        case SPACE:
            {
                /* Only need to ensure there's no non-whitespace text
                 * when parsing 'real' ignorable white space (in validating
                 * mode, but that's implicit here)
                 */
                boolean prolog = (mParseState != STATE_TREE);
                readSpaceSecondary(prolog);
            }
            return;

        case COMMENT:
            readComment();
            return;

        case DTD:
            finishDTD(true);
            return;

        case PROCESSING_INSTRUCTION:
            readPI();
            return;

        case START_ELEMENT:
        case END_ELEMENT: // these 2 should never end up in here...
        case ENTITY_REFERENCE:
        case ENTITY_DECLARATION:
        case NOTATION_DECLARATION:
        case START_DOCUMENT:
        case END_DOCUMENT:
            throw new IllegalStateException("finishToken() called when current token is "+tokenTypeDesc(mCurrToken));

        case ATTRIBUTE:
        case NAMESPACE:
            // These two are never returned by this class
        default:
        }

        throw new IllegalStateException("Internal error: unexpected token "+tokenTypeDesc(mCurrToken));
    }

    private void readComment()
        throws IOException, XMLStreamException
    {
        char[] inputBuf = mInputBuffer;
        int inputLen = mInputLen;
        int ptr = mInputPtr;
        int start = ptr;

        // Let's first see if we can just share input buffer:
        while (ptr < inputLen) {
            char c = inputBuf[ptr++];
            if (c > '-') {
                continue;
            }

            if (c == '\n') {
                markLF(ptr);
            } else if (c == '\r') {
                if (!mCfgNormalizeLFs && ptr < inputLen) {
                    if (inputBuf[ptr] == '\n') {
                        ++ptr;
                    }
                    markLF(ptr);
                } else {
                    --ptr; // pushback
                    break;
                }
            } else if (c == '-') {
                // Ok; need to get '->', can not get '--'
                
                if ((ptr + 1) >= inputLen) {
                    /* Can't check next 2, let's push '-' back, for rest of
                     * code to take care of
                     */
                    --ptr;
                    break;
                }
                
                if (inputBuf[ptr] != '-') {
                    // Can't skip, might be LF/CR
                    continue;
                }
                // Ok; either get '>' or error:
                c = inputBuf[ptr+1];
                if (c != '>') {
                    throwParseError("String '--' not allowed in comment (missing '>'?)");
                }
                mTextBuffer.resetWithShared(inputBuf, start, ptr-start-1);
                mInputPtr = ptr + 2;
                return;
            } else if (c == CHAR_NULL) {
                throwNullChar();
            }
        }

        mInputPtr = ptr;
        mTextBuffer.resetWithCopy(inputBuf, start, ptr-start);
        readComment2(mTextBuffer);
    }

    private void readComment2(TextBuffer tb)
        throws IOException, XMLStreamException
    {
        /* Output pointers; calls will also ensure that the buffer is
         * not shared, AND has room for at least one more char
         */
        char[] outBuf = mTextBuffer.getCurrentSegment();
        int outPtr = mTextBuffer.getCurrentSegmentSize();
        int outLen = outBuf.length;

        while (true) {
            char c = (mInputPtr < mInputLen) ?
                mInputBuffer[mInputPtr++] : getNextCharFromCurrent(SUFFIX_IN_COMMENT);

            if (c <= CHAR_CR_LF_OR_NULL) {
                if (c == '\n') {
                    markLF();
                } else if (c == '\r') {
                    if (skipCRLF(c)) { // got 2 char LF
                        if (!mCfgNormalizeLFs) {
                            if (outPtr >= outLen) { // need more room?
                                outBuf = mTextBuffer.finishCurrentSegment();
                                outLen = outBuf.length;
                                outPtr = 0;
                            }
                            outBuf[outPtr++] = c;
                        }
                        // And let's let default output the 2nd char
                        c = '\n';
                    } else if (mCfgNormalizeLFs) { // just \r, but need to convert
                        c = '\n'; // For Mac text
                    }
                } else if (c == CHAR_NULL) {
                    throwNullChar();
                }
            } else if (c == '-') { // Ok; need to get '->', can not get '--'
                c = getNextCharFromCurrent(SUFFIX_IN_COMMENT);
                if (c == '-') { // Ok, has to be end marker then:
                    // Either get '>' or error:
                    c = getNextCharFromCurrent(SUFFIX_IN_COMMENT);
                    if (c != '>') {
                        throwParseError(ErrorConsts.ERR_HYPHENS_IN_COMMENT);
                    }
                    break;
                }

                /* Not the end marker; let's just output the first hyphen,
                 * push the second char back , and let main
                 * code handle it.
                 */
                c = '-';
                --mInputPtr;
            }

            // Need more room?
            if (outPtr >= outLen) {
                outBuf = mTextBuffer.finishCurrentSegment();
                outLen = outBuf.length;
                outPtr = 0;
            }
            // Ok, let's add char to output:
            outBuf[outPtr++] = c;
        }

        // Ok, all done, then!
        mTextBuffer.setCurrentLength(outPtr);
    }

    /**
     * Method that reads the primary part of a PI, ie. target, and also
     * skips white space between target and data (if any data)
     */
    private void readPIPrimary()
        throws IOException, XMLStreamException
    {
        // Ok, first we need the name:
        String target = parseFullName();
        mCurrName = target;

        if (target.length() == 0) {
            throwParseError("Missing processing instruction target.");
        }

        // As per XML specs, #17, case-insensitive 'xml' is illegal:
        if (target.length() == 3) {
            char c = target.charAt(0);
            if (c == 'x' || c == 'X') {
                c = target.charAt(1);
                if (c == 'm' || c == 'M') {
                    c = target.charAt(2);
                    if (c == 'l' || c == 'L') {
                        throwParseError("Illegal processing instruction target ('"
                                        +target+"'); 'xml' (case insensitive) is reserved by the specs.");
                    }
                }
            }
        }

        // And then either white space before data, or end marker:
        char c = (mInputPtr < mInputLen) ?
            mInputBuffer[mInputPtr++] : getNextCharFromCurrent(SUFFIX_IN_PROC_INSTR);
        if (isSpaceChar(c)) { // Ok, space to skip
            mStTokenUnfinished = true;
            // Need to skip the WS...
            skipWS();
        } else { // Nope; apparently finishes right away...
            mStTokenUnfinished = false;
            mTextBuffer.resetWithEmpty();
            if (c != '?') {
                throwUnexpectedChar(c, "excepted either space or \"?>\" after PI target");
            }
            c = getNextCharFromCurrent(SUFFIX_IN_PROC_INSTR);
            if (c != '>') {
                throwUnexpectedChar(c, "excepted '>' (as part of \"?>\") after PI target");
            }
        }
    }

    /**
     * Method that parses a processing instruction's data portion; at this
     * point target has been parsed.
     */
    private void readPI()
        throws IOException, XMLStreamException
    {
        int ptr = mInputPtr;
        int start = ptr;
        char[] inputBuf = mInputBuffer;
        int inputLen = mInputLen;

        /* Output pointers; calls will also ensure that the buffer is
         * not shared, AND has room for one more char
         */
        char[] outBuf = mTextBuffer.getCurrentSegment();
        int outPtr = mTextBuffer.getCurrentSegmentSize();

        outer_loop:
        while (ptr < inputLen) {
            char c = inputBuf[ptr++];
            if (c <= CHAR_CR_LF_OR_NULL) {
                if (c == '\n') {
                    markLF(ptr);
                } else if (c == '\r') {
                    if (ptr < inputLen && !mCfgNormalizeLFs) {
                        if (inputBuf[ptr] == '\n') {
                            ++ptr;
                        }
                        markLF(ptr);
                    } else {
                        --ptr; // pushback
                        break;
                    }
                } else if (c == CHAR_NULL) {
                    throwNullChar();
                }
            } else if (c == '?') {
                // K; now just need '>' after zero or more '?'s
                while (true) {
                    if (ptr >= inputLen) {
                        /* end of buffer; need to push back at least one of
                         * question marks (not all, since just one is needed
                         * to close the PI)
                         */
                        --ptr;
                        break outer_loop;
                    }
                    c = inputBuf[ptr++];
                    if (c == '>') {
                        mInputPtr = ptr;
                        // Need to discard trailing '?>'
                        mTextBuffer.resetWithShared(inputBuf, start, ptr-start-2);
                        return;
                    }
                    if (c != '?') {
                        // Not end, can continue, but need to push back last char, in case it's LF/CR
                        --ptr;
                        break;
                    }
                }
            }
        }
        
        mInputPtr = ptr;
        // No point in trying to share... let's just append
        mTextBuffer.resetWithCopy(inputBuf, start, ptr-start);
        readPI2(mTextBuffer);
    }

    private void readPI2(TextBuffer tb)
        throws IOException, XMLStreamException
    {
        char[] inputBuf = mInputBuffer;
        int inputLen = mInputLen;
        int inputPtr = mInputPtr;

        /* Output pointers; calls will also ensure that the buffer is
         * not shared, AND has room for one more char
         */
        char[] outBuf = tb.getCurrentSegment();
        int outPtr = tb.getCurrentSegmentSize();

        main_loop:
        while (true) {
            // Let's first ensure we have some data in there...
            if (inputPtr >= inputLen) {
                loadMoreFromCurrent(SUFFIX_IN_PROC_INSTR);
                inputBuf = mInputBuffer;
                inputPtr = mInputPtr;
                inputLen = mInputLen;
            }

            // And then do chunks
            char c = inputBuf[inputPtr++];
            if (c <= CHAR_CR_LF_OR_NULL) {
                if (c == '\n') {
                    markLF(inputPtr);
                } else if (c == '\r') {
                    mInputPtr = inputPtr;
                    if (skipCRLF(c)) { // got 2 char LF
                        if (!mCfgNormalizeLFs) {
                            // Special handling, to output 2 chars at a time:
                            if (outPtr >= outBuf.length) { // need more room?
                                outBuf = mTextBuffer.finishCurrentSegment();
                                outPtr = 0;
                            }
                            outBuf[outPtr++] = c;
                        }
                        // And let's let default output the 2nd char, either way
                        c = '\n';
                    } else if (mCfgNormalizeLFs) { // just \r, but need to convert
                        c = '\n'; // For Mac text
                    }
                    /* Since skipCRLF() needs to peek(), buffer may have
                     * changed, even if there was no CR+LF.
                     */
                    inputPtr = mInputPtr;
                    inputBuf = mInputBuffer;
                    inputLen = mInputLen;
                } else if (c == CHAR_NULL) {
                    throwNullChar();
                }
            } else if (c == '?') { // Ok, just need '>' after zero or more '?'s
                mInputPtr = inputPtr; // to allow us to call getNextChar

                qmLoop:
                while (true) {
                    c = (mInputPtr < mInputLen) ? mInputBuffer[mInputPtr++]
                        : getNextCharFromCurrent(SUFFIX_IN_PROC_INSTR);
                    if (c == '>') { // got it!
                        break main_loop;
                    } else if (c == '?') {
                        if (outPtr >= outBuf.length) { // need more room?
                            outBuf = tb.finishCurrentSegment();
                            outPtr = 0;
                        }
                        outBuf[outPtr++] = c;
                    } else {
                        /* Hmmh. Wasn't end mark after all. Thus, need to
                         * fall back to normal processing, with one more
                         * question mark (first one matched that wasn't
                         * yet output),
                         * reset variables, and go back to main loop.
                         */
                        inputPtr = --mInputPtr; // push back last char
                        inputBuf = mInputBuffer;
                        inputLen = mInputLen;
                        c = '?';
                        break qmLoop;
                    }
                }
            } // if (c == '?)

            // Need more room?
            if (outPtr >= outBuf.length) {
                outBuf = tb.finishCurrentSegment();
                outPtr = 0;
            }
            // Ok, let's add char to output:
            outBuf[outPtr++] = c;

        } // while (true)

        tb.setCurrentLength(outPtr);
    }

    /**
     * Method called to read the content of both current CDATA/CHARACTERS
     * events, and all following consequtive events into the text buffer.
     * At this point the current type is known, prefix (for CDATA) skipped,
     * and initial consequtive contents (if any) read in.
     */
    private void readCoalescedText(int currType)
        throws IOException, XMLStreamException
    {
        boolean wasCData;

        // Ok; so we may need to combine adjacent text/CDATA chunks.
        if (currType == CHARACTERS || currType == SPACE) {
            readTextSecondary(Integer.MAX_VALUE);
            wasCData = false;
        } else if (currType == CDATA) {
            /* We may have actually really finished it, but just left
             * the 'unfinished' flag due to need to coalesce...
             */
            if (mStPartialCData) {
                readCDataSecondary(Integer.MAX_VALUE);
            }
            wasCData = true;
        } else {
            throw new IllegalStateException("Internal error: unexpected token "+tokenTypeDesc(mCurrToken)+"; expected CHARACTERS, CDATA or SPACE.");
        }

        // But how about additional text?
        while (true) {
            if (mInputPtr >= mInputLen) {
                mTextBuffer.ensureNotShared();
                if (!loadMore()) {
                    // ??? Likely an error but let's just break
                    break;
                }
            }
            // Let's peek, ie. not advance it yet
            char c = mInputBuffer[mInputPtr];
            if (c == '<') { // CDATA, maybe?
                // Need to distinguish "<![" from other tags/directives
                if ((mInputLen - mInputPtr) < 3) {
                    mTextBuffer.ensureNotShared();
                    if (!ensureInput(3)) {
                        break;
                    }
                }
                if (mInputBuffer[mInputPtr+1] != '!'
                    || mInputBuffer[mInputPtr+2] != '[') {
                    // Nah, some other tag or directive
                    break;
                }
                // Let's skip beginning parts, then:
                mInputPtr += 3;
                // And verify we get proper CDATA directive
                checkCData();
                /* No need to call the primary data; it's only useful if
                 * there's a chance for sharing buffers... so let's call
                 * the secondary loop straight on.
                 */
                readCDataSecondary(Integer.MAX_VALUE);
                wasCData = true;
            } else { // text
                /* Did we hit an 'unexpandable' entity? If so, need to
                 * just bail out.
                 */
                if (c == '&' && !wasCData) {
                    break;
                }
                // Likewise, can't share buffers, let's call secondary loop:
                readTextSecondary(Integer.MAX_VALUE);
                wasCData = false;
            }
        }
    }

    /**
     * Method called to read in consequtive beginning parts of a CDATA
     * segment, up to either end of the segment (]] and >) or until
     * first 'hole' in text (buffer end, 2-char lf to convert, entity).
     *<p>
     * When the method is called, it's expected that the first character
     * has been read as is in the current input buffer just before current
     * pointer
     *
     * @param c First character in the CDATA segment (possibly part of end
     *   marker for empty segments
     *
     * @return True if the whole CDATA segment was completely read; this
     *   happens only if lt-char is hit; false if it's possible that
     *   it wasn't read (ie. end-of-buffer or entity encountered).
     */
    private boolean readCDataPrimary(char c)
        throws IOException, XMLStreamException
    {
        mWsStatus = (c <= CHAR_SPACE) ? ALL_WS_UNKNOWN : ALL_WS_NO;

        int ptr = mInputPtr;
        int inputLen = mInputLen;
        char[] inputBuf = mInputBuffer;
        int start = ptr-1;

        outer_loop:
        while (true) {
            if (c <= CHAR_CR_LF_OR_NULL) {
                if (c == '\n') {
                    markLF(ptr);
                } else if (c == '\r') {
                    if (ptr >= inputLen) { // can't peek?
                        --ptr;
                        break;
                    }
                    if (mCfgNormalizeLFs) { // can we do in-place Mac replacement?
                        if (inputBuf[ptr] == '\n') { // nope, 2 char lf
                            --ptr;
                            break;
                        }
                        inputBuf[ptr-1] = '\n'; // yup
                    } else {
                        // No LF normalization... can we just skip it?
                        if (inputBuf[ptr] == '\n') {
                            ++ptr;
                        }
                    }
                    markLF(ptr);
                } else if (c == CHAR_NULL) {
                    throwNullChar();
                }
            } else if (c == ']') {
                // Ok; need to get one or more ']'s, then '>'
                if ((ptr + 1) >= inputLen) { // not enough room? need to push it back
                    --ptr;
                    break;
                }

                // Needs to be followed by another ']'...
                if (inputBuf[ptr] == ']') {
                    ++ptr;
                    inner_loop:
                    while (true) {
                        if (ptr >= inputLen) {
                            /* Need to push back last 2 right brackets; it may
                             * be end marker divided by input buffer boundary
                             */
                            ptr -= 2;
                            break inner_loop;
                        }
                        c = inputBuf[ptr++];
                        if (c == '>') { // Ok, got it!
                            mInputPtr = ptr;
                            ptr -= (start+3);
                            mTextBuffer.resetWithShared(inputBuf, start, ptr);
                            mStPartialCData = false;
                            return true;
                        }
                        if (c != ']') {
                            // Need to re-check this char (may be linefeed)
                            --ptr;
                            break inner_loop;
                        }
                        // Fall through to next round
                    }
                }
            }

            if (ptr >= inputLen) { // end-of-buffer?
                break;
            }
            c = inputBuf[ptr++];
        }

        mInputPtr = ptr;

        /* If we end up here, we either ran out of input, or hit something
         * which would leave 'holes' in buffer... fine, let's return then;
         * we can still update shared buffer copy: would be too early to
         * make a copy since caller may not even be interested in the
         * stuff.
         */
        mTextBuffer.resetWithShared(inputBuf, start, ptr - start);
        mStPartialCData = true;
        return false;
    }

    /**
     * @return True if the whole CData section was completely read (we
     *   hit the end marker); false if a shorter segment was returned.
     */
    private boolean readCDataSecondary(int shortestSegment)
        throws IOException, XMLStreamException
    {
        // Input pointers
        char[] inputBuf = mInputBuffer;
        int inputLen = mInputLen;
        int inputPtr = mInputPtr;

        /* Output pointers; calls will also ensure that the buffer is
         * not shared, AND has room for one more char
         */
        char[] outBuf = mTextBuffer.getCurrentSegment();
        int outPtr = mTextBuffer.getCurrentSegmentSize();

        while (true) {
            if (inputPtr >= inputLen) {
                loadMore(SUFFIX_IN_CDATA);
                inputBuf = mInputBuffer;
                inputPtr = mInputPtr;
                inputLen = mInputLen;
            }
            char c = inputBuf[inputPtr++];

            if (c <= CHAR_CR_LF_OR_NULL) {
                if (c == '\n') {
                    markLF(inputPtr);
                } else if (c == '\r') {
                    mInputPtr = inputPtr;
                    if (skipCRLF(c)) { // got 2 char LF
                        if (!mCfgNormalizeLFs) {
                            // Special handling, to output 2 chars at a time:
                            outBuf[outPtr++] = c;
                            if (outPtr >= outBuf.length) { // need more room?
                                outBuf = mTextBuffer.finishCurrentSegment();
                                outPtr = 0;
                            }
                        }
                        // And let's let default output the 2nd char, either way
                        c = '\n';
                    } else if (mCfgNormalizeLFs) { // just \r, but need to convert
                        c = '\n'; // For Mac text
                    }
                    /* Since skipCRLF() needs to peek(), buffer may have
                     * changed, even if there was no CR+LF.
                     */
                    inputPtr = mInputPtr;
                    inputBuf = mInputBuffer;
                    inputLen = mInputLen;
                } else if (c == CHAR_NULL) {
                    throwNullChar();
                }
            } else if (c == ']') {
                // Ok; need to get ']>'
                mInputPtr = inputPtr;
                if (checkCDataEnd(outBuf, outPtr)) {
                    mStPartialCData = false;
                    return true;
                }
                inputPtr = mInputPtr;
                inputBuf = mInputBuffer;
                inputLen = mInputLen;

                outBuf = mTextBuffer.getCurrentSegment();
                outPtr = mTextBuffer.getCurrentSegmentSize();
                continue; // need to re-process last (non-bracket) char
            }

            // Ok, let's add char to output:
            outBuf[outPtr++] = c;

            // Need more room?
            if (outPtr >= outBuf.length) {
                TextBuffer tb = mTextBuffer;
                // Perhaps we have now enough to return?
                if (!mCfgCoalesceText) {
                    tb.setCurrentLength(outBuf.length);
                    if (tb.size() >= mShortestTextSegment) {
                        mInputPtr = inputPtr;
                        mStPartialCData = true;
                        return false;
                    }
                }
                // If not, need more buffer space:
                outBuf = tb.finishCurrentSegment();
                outPtr = 0;
            }
        }
        // never gets here
    }

    /**
     * Method that will check, given the starting ']', whether there is
     * ending ']]>' (including optional extra ']'s); if so, will updated
     * output buffer with extra ]s, if not, will make sure input and output
     * are positioned for further checking.
     * 
     * @return True, if we hit the end marker; false if not.
     */
    private boolean checkCDataEnd(char[] outBuf, int outPtr)
        throws IOException, XMLStreamException
    {
        int bracketCount = 0;
        char c;
        do {
            ++bracketCount;
            c = (mInputPtr < mInputLen) ? mInputBuffer[mInputPtr++]
                : getNextCharFromCurrent(SUFFIX_IN_CDATA);
        } while (c == ']');

        boolean match = (bracketCount >= 2 && c == '>');
        if (match) {
            bracketCount -= 2;
        }
        while (bracketCount > 0) {
            --bracketCount;
            outBuf[outPtr++] = ']';
            if (outPtr >= outBuf.length) {
                /* Can't really easily return, even if we have enough
                 * stuff here, since we've more than one char...
                 */
                outBuf = mTextBuffer.finishCurrentSegment();
                outPtr = 0;
            }
        }
        mTextBuffer.setCurrentLength(outPtr);
        // Match? Can break, then:
        if (match) {
            return true;
        }
        // No match, need to push the last char back and admit defeat...
        --mInputPtr;
        return false;
    }

    /**
     * Method called to read in consequtive beginning parts of a text
     * segment, up to either end of the segment (lt char) or until
     * first 'hole' in text (buffer end, 2-char lf to convert, entity).
     *<p>
     * When the method is called, it's expected that the first character
     * has been read as is in the current input buffer just before current
     * pointer
     *
     * @param c First character of the text segment
     *
     * @return True if the whole text segment was completely read; this
     *   happens only if lt-char is hit; false if it's possible that
     *   it wasn't read (ie. end-of-buffer or entity encountered).
     */
    private boolean readTextPrimary(char c)
        throws IOException, XMLStreamException
    {
        mWsStatus = (c <= CHAR_SPACE) ? ALL_WS_UNKNOWN : ALL_WS_NO;
        
        int ptr = mInputPtr;
        char[] inputBuf = mInputBuffer;
        int inputLen = mInputLen;
        int start = ptr-1;

        // Let's first see if we can just share input buffer:
        while (true) {
            if (c < CHAR_FIRST_PURE_TEXT) {
                if (c == '<') {
                    mInputPtr = --ptr;
                    mTextBuffer.resetWithShared(inputBuf, start, ptr-start);
                    return true;
                }
                if (c == '\n') {
                    markLF(ptr);
                } else if (c == '\r') {
                    if (ptr >= inputLen) { // can't peek?
                        --ptr;
                        break;
                    }
                    if (mCfgNormalizeLFs) { // can we do in-place Mac replacement?
                        if (inputBuf[ptr] == '\n') { // nope, 2 char lf
                            --ptr;
                            break;
                        }
                        /* This would otherwise be risky (may modify value
                         * of a shared entity value), but since DTDs are
                         * cached/accessed based on properties including
                         * lf-normalization there's no harm in 'fixing' it
                         * in place.
                         */
                        inputBuf[ptr-1] = '\n'; // yup
                    } else {
                        // No LF normalization... can we just skip it?
                        if (inputBuf[ptr] == '\n') {
                            ++ptr;
                        }
                    }
                    markLF(ptr);
                } else if (c == '&') {
                    // Let's push it back and break
                    --ptr;
                    break;
                } else if (c == '>') {
                    // Let's see if we got ']]>'?
                    if ((ptr - start) >= 3) {
                        if (inputBuf[ptr-3] == ']' && inputBuf[ptr-2] == ']') {
                            mInputPtr = ptr; // to get error info right
                            throwParseError(ErrorConsts.ERR_BRACKET_IN_TEXT);
                        }
                    }
                } else if (c == CHAR_NULL) {
                    throwNullChar();
                }
            } // if (char in lower code range)

            if (ptr >= inputLen) { // end-of-buffer?
                break;
            }
            c = inputBuf[ptr++];
        }

        mInputPtr = ptr;

        /* If we end up here, we either ran out of input, or hit something
         * which would leave 'holes' in buffer... fine, let's return then;
         * we can still update shared buffer copy: would be too early to
         * make a copy since caller may not even be interested in the
         * stuff.
         */
        mTextBuffer.resetWithShared(inputBuf, start, ptr - start);
        return false;
    }

    /**
     *
     * @return True if the text segment was completely read ('<' was hit,
     *   or in non-entity-expanding mode, a non-char entity); false if
     *   it may still continue
     */
    private boolean readTextSecondary(int shortestSegment)
        throws IOException, XMLStreamException
    {
        /* Output pointers; calls will also ensure that the buffer is
         * not shared, AND has room for at least one more char
         */
        char[] outBuf = mTextBuffer.getCurrentSegment();
        int outPtr = mTextBuffer.getCurrentSegmentSize();

        while (true) {
            char c = (mInputPtr < mInputLen) ?
                mInputBuffer[mInputPtr++] : getNextChar(SUFFIX_IN_TEXT);

            // Most common case is we don't have special char, thus:
            if (c < CHAR_FIRST_PURE_TEXT) {
                if (c == '\n') {
                    markLF();
                } else if (c == '<') { // end is nigh!
                    --mInputPtr;
                    mTextBuffer.setCurrentLength(outPtr);
                    return true;
                } else if (c == '\r') {
                    if (skipCRLF(c)) { // got 2 char LF
                        if (!mCfgNormalizeLFs) {
                            // Special handling, to output 2 chars at a time:
                            outBuf[outPtr++] = c;
                            if (outPtr >= outBuf.length) { // need more room?
                                outBuf = mTextBuffer.finishCurrentSegment();
                                outPtr = 0;
                            }
                        }
                        // And let's let default output the 2nd char
                        c = '\n';
                    } else if (mCfgNormalizeLFs) { // just \r, but need to convert
                        c = '\n'; // For Mac text
                    }
                } else if (c == '&') {
                    if (mCfgReplaceEntities) { // can we expand all entities?
                        if ((mInputLen - mInputPtr) >= 3
                            && (c = resolveSimpleEntity(true)) != CHAR_NULL) {
                            // Ok, it's fine, c will get output
                        } else {
                            c = fullyResolveEntity(mCustomEntities, mGeneralEntities, true);
                            if (c == CHAR_NULL) {
                                // Output buffer changed, nothing to output quite yet:
                                continue;
                            }
                            // otherwise char is now fine...
                        }
                    } else {
                        /* Nope, can only return char entities; others need
                         * to be separately handled.
                         */
                        c = resolveCharOnlyEntity(true);
                        if (c == CHAR_NULL) { // some other entity...
                            // can't expand, so:
                            --mInputPtr; // push back ampersand
                            mTextBuffer.setCurrentLength(outPtr);
                            return true;
                        }
                        // .. otherwise we got char we needed
                    }
                } else if (c == '>') {
                    // Let's see if we got ']]>'?
                    if (outPtr >= 2) { // can we do it here?
                        if (outBuf[outPtr-2] == ']' && outBuf[outPtr-1] == ']') {
                            throwParseError(ErrorConsts.ERR_BRACKET_IN_TEXT);
                        }
                    } else { // nope, have to ask buffer, due to boundary
                        TextBuffer tb = mTextBuffer;
                        tb.setCurrentLength(outPtr);
                        if (tb.endsWith("]]")) {
                            throwParseError(ErrorConsts.ERR_BRACKET_IN_TEXT);
                        }
                    }
                } else if (c == CHAR_NULL) {
                    throwNullChar();
                }
            }
                
            // Ok, let's add char to output:
            outBuf[outPtr++] = c;

            // Need more room?
            if (outPtr >= outBuf.length) {
                TextBuffer tb = mTextBuffer;
                // Perhaps we have now enough to return?
                tb.setCurrentLength(outBuf.length);
                if (tb.size() >= shortestSegment) {
                    return false;
                }
                // If not, need more buffer space:
                outBuf = tb.finishCurrentSegment();
                outPtr = 0;
            }
        }
    }

    /**
     * Reading whitespace should be very similar to reading normal text;
     * although couple of simplifications can be made. Further, since this
     * method is very unlikely to be of much performance concern, some
     * optimizations are left out, where it simplifies code.
     *
     * @param c First white space characters; known to contain white space
     *   at this point
     * @param prologWS If true, is reading white space outside XML tree,
     *   and as such can get EOF. If false, should not get EOF, nor be
     *   followed by any other char than &lt;
     *
     * @return True if the whole white space segment was read; false if
     *   something prevented that (end of buffer, replaceable 2-char lf)
     */
    private boolean readSpacePrimary(char c, boolean prologWS)
        throws IOException, XMLStreamException
    {
        int ptr = mInputPtr;
        char[] inputBuf = mInputBuffer;
        int inputLen = mInputLen;
        int start = ptr-1;

        // Let's first see if we can just share input buffer:
        while (true) {
            // End of whitespace? May or may not need to check it...
            if (c > CHAR_SPACE) { // End of whitespace
                if (!prologWS && c != '<') {
                    throwNotWS(c);
                }
                mInputPtr = --ptr;
                mTextBuffer.resetWithShared(mInputBuffer, start, ptr-start);
                return true;
            }

            if (c == '\n') {
                markLF(ptr);
            } else if (c == '\r') {
                if (ptr >= mInputLen) { // can't peek?
                    --ptr;
                    break;
                }
                if (mCfgNormalizeLFs) { // can we do in-place Mac replacement?
                    if (inputBuf[ptr] == '\n') { // nope, 2 char lf
                        --ptr;
                        break;
                    }
                    inputBuf[ptr-1] = '\n'; // yup
                } else {
                    // No LF normalization... can we just skip it?
                    if (inputBuf[ptr] == '\n') {
                        ++ptr;
                    }
                }
                markLF(ptr);
            } else if (c == CHAR_NULL) {
                throwNullChar();
            }
            if (ptr >= inputLen) { // end-of-buffer?
                break;
            }
            c = inputBuf[ptr++];
        }

        mInputPtr = ptr;
        
        /* Ok, couldn't read it completely, let's just return whatever
         * we did get as shared data
         */
        mTextBuffer.resetWithShared(inputBuf, start, ptr - start);
        return false;
    }

    /**
     * This is very similar to readSecondaryText(); called when we need
     * to read in rest of (ignorable) white space segment.
     */
    private void readSpaceSecondary(boolean prologWS)
        throws IOException, XMLStreamException
    {
        /* Let's not bother optimizing input. However, we can easily optimize
         * output, since it's easy to do, yet has more effect on performance
         * than localizing input variables.
         */
        char[] outBuf = mTextBuffer.getCurrentSegment();
        int outPtr = mTextBuffer.getCurrentSegmentSize();

        while (true) {
            if (mInputPtr >= mInputLen) {
                if (!loadMore()) {
                    if (!prologWS) {
                        throwUnexpectedEOF(SUFFIX_IN_TEXT);
                    }
                    break;
                }
            }
            char c = mInputBuffer[mInputPtr];
            if (c > CHAR_SPACE) { // end of WS?
                if (!prologWS && c != '<') {
                    throwNotWS(c);
                }
                break;
            }
            ++mInputPtr;
            if (c == '\n') {
                markLF();
            } else if (c == '\r') {
                if (skipCRLF(c)) {
                    if (!mCfgNormalizeLFs) {
                        // Special handling, to output 2 chars at a time:
                        outBuf[outPtr++] = c;
                        if (outPtr >= outBuf.length) { // need more room?
                            outBuf = mTextBuffer.finishCurrentSegment();
                            outPtr = 0;
                        }
                    }
                    c = '\n';
                } else if (mCfgNormalizeLFs) {
                    c = '\n'; // For Mac text
                }
            } else if (c == CHAR_NULL) {
                throwNullChar();
            }
                
            // Ok, let's add char to output:
            outBuf[outPtr++] = c;

            // Need more room?
            if (outPtr >= outBuf.length) {
                outBuf = mTextBuffer.finishCurrentSegment();
                outPtr = 0;
            }
        }
        mTextBuffer.setCurrentLength(outPtr);
    }

    /*
    ////////////////////////////////////////////////////
    // Internal methods, validation, error reporting
    ////////////////////////////////////////////////////
     */

    private void throwNotTextual(int type)
    {
        throw new IllegalStateException("Not a textual event ("
                                        +tokenTypeDesc(mCurrToken)+")");
    }

    private void throwNotWS(char c)
        throws WstxException
    {
        throwUnexpectedChar(c, "; element <"
                            +mElementStack.getTopElementDesc()
                            +"> does not allow mixed content");
        
    }

    /**
     * Stub method implemented by validating parsers, to report content
     * that's not valid for current element context. Defined at this
     * level since some such problems need to be caught at low-level;
     * however, details of error reports are not needed here.
     */
    protected void reportInvalidContent(int evtType)
        throws WstxException
    {
        // should never happen; sub-class has to override:
        throwParseError("Internal error: sub-class should override method");
    }

    /*
    ////////////////////////////////////////////////////
    // Support for debugging, profiling:
    ////////////////////////////////////////////////////
     */

    public SymbolTable getSymbolTable() {
        return mSymbols;
    }
}
