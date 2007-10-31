/* Woodstox XML processor
 *
 * Copyright (c) 2004- Tatu Saloranta, tatu.saloranta@iki.fi
 *
 * Licensed under the License specified in the file LICENSE which is
 * included with the source code.
 * You may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ctc.wstx.dom;

import java.io.IOException;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.*;

import javax.xml.transform.dom.DOMSource;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.*;

import org.w3c.dom.*;

import org.codehaus.stax2.AttributeInfo;
import org.codehaus.stax2.DTDInfo;
import org.codehaus.stax2.LocationInfo;
import org.codehaus.stax2.XMLStreamLocation2;
import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.validation.DTDValidationSchema;
import org.codehaus.stax2.validation.ValidationProblemHandler;
import org.codehaus.stax2.validation.XMLValidationSchema;
import org.codehaus.stax2.validation.XMLValidator;

import com.ctc.wstx.api.ReaderConfig;
import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.exc.WstxParsingException;
import com.ctc.wstx.io.WstxInputLocation;
import com.ctc.wstx.util.EmptyIterator;
import com.ctc.wstx.util.EmptyNamespaceContext;
import com.ctc.wstx.util.SingletonIterator;
import com.ctc.wstx.util.TextAccumulator;

/**
 * This is an adapter class that presents a DOM document as if it was
 * a regular {@link XMLStreamReader}.
 *<p>
 * Note that the implementation is only to be used for use with
 * <code>javax.xml.transform.dom.DOMSource</code>. It can however be
 * used for both full documents, and single element root fragments,
 * depending on what node is passed as the argument.
 *<p>
 * Some notes regarding missing/incomplete functionality:
 * <ul>
 *  <li>DOM does not seem to have access to information from the XML
 *    declaration (although Document node can be viewed as representing
 *    it). Consequently, all accessors return no information (version,
 *    encoding, standalone).
 *   </li>
 *  <li>No location info is provided, since (you guessed it!) DOM
 *    does not provide that info.
 *   </li>
 *  </ul>
 */
public class DOMWrappingReader
    implements XMLStreamReader2,
               DTDInfo, LocationInfo, NamespaceContext,
               XMLStreamConstants
{
    // // // Bit masks used for quick type comparisons

    final private static int MASK_GET_TEXT = 
        (1 << CHARACTERS) | (1 << CDATA) | (1 << SPACE)
        | (1 << COMMENT) | (1 << DTD) | (1 << ENTITY_REFERENCE);

    final private static int MASK_GET_ELEMENT_TEXT = 
        (1 << CHARACTERS) | (1 << CDATA) | (1 << SPACE)
        | (1 << ENTITY_REFERENCE);

    // // // Configuration:

    protected final ReaderConfig mConfig;

    protected final String mSystemId;

    protected final Node mRootNode;

    protected final boolean mNsAware;

    // // // State:

    protected int mCurrEvent = START_DOCUMENT;

    /**
     * Current node is the DOM node that contains information
     * regarding the current event.
     */
    protected Node mCurrNode;

    protected int mDepth = 0;

    // // // Attribute/namespace declaration state

    /* DOM, alas, does not distinguish between namespace declarations
     * and attributes (due to its roots prior to XML namespaces?).
     * Because of this, two lists need to be separated. Since this
     * information is often not needed, it will be lazily generated.
     */

    /**
     * Lazily instantiated List of all actual attributes for the
     * current (start) element, NOT including namespace declarations.
     * As such, elements are {@link org.w3c.dom.Attr} instances.
     *<p>
     */
    protected List mAttrList = null;

    /**
     * Lazily instantiated String pairs of all namespace declarations for the
     * current (start/end) element. String pair means that for each
     * declarations there are two Strings in the list: first one is prefix
     * (empty String for the default namespace declaration), and second
     * URI it is bound to.
     */
    protected List mNsDeclList = null;

    /*
    ////////////////////////////////////////////////////
    // Construction
    ////////////////////////////////////////////////////
     */

    /**
     * @param cfg Configuration of this reader
     * @param treeRoot Node that is the tree of the DOM document, or
     *   fragment.
     */
    private DOMWrappingReader(ReaderConfig cfg, Node treeRoot, String sysId)
        throws XMLStreamException
    {
        if (treeRoot == null) {
            throw new IllegalArgumentException("Can not pass null Node for constructing a DOM-based XMLStreamReader");
        }

        mConfig = cfg;
        mNsAware = cfg.willSupportNamespaces();
        mSystemId = sysId;
        
        /* Ok; we need a document node; or an element node; or a document
         * fragment node.
         */
        switch (treeRoot.getNodeType()) {
        case Node.DOCUMENT_NODE: // fine
            /* Should try to find encoding, version and stand-alone
             * settings... but is there a standard way of doing that?
             */
        case Node.ELEMENT_NODE: // can make sub-tree... ok
            // But should we skip START/END_DOCUMENT? For now, let's not

        case Node.DOCUMENT_FRAGMENT_NODE: // as with element...

            // Above types are fine
            break;

        default: // other Nodes not usable
            throw new XMLStreamException("Can not create an XMLStreamReader for a DOM node of type "+treeRoot.getClass());
        }
        mRootNode = mCurrNode = treeRoot;
    }

    public static DOMWrappingReader createFrom(ReaderConfig cfg, DOMSource src)
        throws XMLStreamException
    {
        Node rootNode = src.getNode();
        String systemId = src.getSystemId();
        return new DOMWrappingReader(cfg, rootNode, systemId);
    }

    /*
    ////////////////////////////////////////////////////
    // XMLStreamReader, document info
    ////////////////////////////////////////////////////
     */

    /**
     * As per Stax (1.0) specs, needs to return whatever xml declaration
     * claimed encoding is, if any; or null if no xml declaration found.
     */
    public String getCharacterEncodingScheme() {
        /* No standard way to figure it out from a DOM Document node;
         * have to return null
         */
        return null;
    }

    /**
     * As per Stax (1.0) specs, needs to return whatever parser determined
     * the encoding was, if it was able to figure it out. If not (there are
     * cases where this can not be found; specifically when being passed a
     * {@link java.io.Reader}), it should return null.
     */
    public String getEncoding() {
        /* We have no information regarding underlying stream/Reader, so
         * best we can do is to see if we know xml declaration encoding.
         */
        return getCharacterEncodingScheme();
    }

    public String getVersion()
    {
        /* No standard way to figure it out from a DOM Document node;
         * have to return null
         */
        return null;
    }

    public boolean isStandalone() {
        /* No standard way to figure it out from a DOM Document node;
         * have to return false
         */
        return false;
    }

    public boolean standaloneSet() {
        /* No standard way to figure it out from a DOM Document node;
         * have to return false
         */
        return false;
    }

    /*
    ////////////////////////////////////////////////////
    // Public API, configuration
    ////////////////////////////////////////////////////
     */

    public Object getProperty(String name)
    {
        return mConfig.getProperty(name);
    }

    /*
    ////////////////////////////////////////////////////
    // XMLStreamReader, current state
    ////////////////////////////////////////////////////
     */

    // // // Attribute access:

    public int getAttributeCount()
    {
        if (mCurrEvent != START_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        if (mAttrList == null) {
            calcNsAndAttrLists(true);
        }
        return mAttrList.size();
    }

	public String getAttributeLocalName(int index)
    {
        if (mCurrEvent != START_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        if (mAttrList == null) {
            calcNsAndAttrLists(true);
        }
        if (index >= mAttrList.size() || index < 0) {
            handleIllegalAttrIndex(index);
            return null;
        }
        Attr attr = (Attr) mAttrList.get(index);
        return safeGetLocalName(attr);
    }

    public QName getAttributeName(int index)
    {
        if (mCurrEvent != START_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        if (mAttrList == null) {
            calcNsAndAttrLists(true);
        }
        if (index >= mAttrList.size() || index < 0) {
            handleIllegalAttrIndex(index);
            return null;
        }
        Attr attr = (Attr) mAttrList.get(index);
        return constructQName(attr.getNamespaceURI(), safeGetLocalName(attr),
                              attr.getPrefix());
    }

    public String getAttributeNamespace(int index)
    {
        if (mCurrEvent != START_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        if (mAttrList == null) {
            calcNsAndAttrLists(true);
        }
        if (index >= mAttrList.size() || index < 0) {
            handleIllegalAttrIndex(index);
            return null;
        }
        Attr attr = (Attr) mAttrList.get(index);
        return attr.getNamespaceURI();
    }

    public String getAttributePrefix(int index)
    {
        if (mCurrEvent != START_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        if (mAttrList == null) {
            calcNsAndAttrLists(true);
        }
        if (index >= mAttrList.size() || index < 0) {
            handleIllegalAttrIndex(index);
            return null;
        }
        Attr attr = (Attr) mAttrList.get(index);
        return attr.getPrefix();
    }

    public String getAttributeType(int index)
    {
        if (mCurrEvent != START_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        if (mAttrList == null) {
            calcNsAndAttrLists(true);
        }
        if (index >= mAttrList.size() || index < 0) {
            handleIllegalAttrIndex(index);
            return null;
        }
        //Attr attr = (Attr) mAttrList.get(index);
        // First, a special case, ID... since it's potentially most useful
        /* 26-Apr-2006, TSa: Turns out that following methods are
         *    DOM Level3, and as such not available in JDK 1.4 and prior.
         *    Thus, let's not yet use them (could use dynamic discovery
         *    for graceful downgrade)
         */
        /*
        if (attr.isId()) {
            return "ID";
        }
        TypeInfo schemaType = attr.getSchemaTypeInfo();
        return (schemaType == null) ? "CDATA" : schemaType.getTypeName();
        */
        return "CDATA";
    }

    public String getAttributeValue(int index)
    {
        if (mCurrEvent != START_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        if (mAttrList == null) {
            calcNsAndAttrLists(true);
        }
        if (index >= mAttrList.size() || index < 0) {
            handleIllegalAttrIndex(index);
            return null;
        }
        Attr attr = (Attr) mAttrList.get(index);
        return attr.getValue();
    }

    public String getAttributeValue(String nsURI, String localName)
    {
        if (mCurrEvent != START_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        Element elem = (Element) mCurrNode;
        NamedNodeMap attrs = elem.getAttributes();
        /* Hmmh. DOM javadocs claim "Per [XML Namespaces], applications
         * must use the value null as the namespaceURI parameter for methods
         * if they wish to have no namespace.".
         * Not sure how true that is, but:
         */
        if (nsURI != null && nsURI.length() == 0) {
            nsURI = null;
        }
        Attr attr = (Attr) attrs.getNamedItemNS(nsURI, localName);
        return (attr == null) ? null : attr.getValue();
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
        if (mCurrEvent != START_ELEMENT) {
            throwParseError(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        TextAccumulator acc = new TextAccumulator();

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
                throwParseError("Expected a text token, got "+ErrorConsts.tokenTypeDesc(type)+".");
            }
            acc.addText(getText());
        }
        return acc.getAndClear();
    }

    /**
     * Returns type of the last event returned; or START_DOCUMENT before
     * any events has been explicitly returned.
     */
    public int getEventType()
    {
        return mCurrEvent;
    }
    
    public String getLocalName()
    {
        if (mCurrEvent == START_ELEMENT || mCurrEvent == END_ELEMENT) {
            return safeGetLocalName(mCurrNode);
        }
        if (mCurrEvent == ENTITY_REFERENCE) {
            return mCurrNode.getNodeName();
        }
        throw new IllegalStateException("Current state ("+ErrorConsts.tokenTypeDesc(mCurrEvent)+") not START_ELEMENT, END_ELEMENT or ENTITY_REFERENCE");
    }

    // // // getLocation() defined in StreamScanner

    public QName getName()
    {
        if (mCurrEvent != START_ELEMENT && mCurrEvent != END_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_ELEM);
        }
        return constructQName(mCurrNode.getNamespaceURI(), safeGetLocalName(mCurrNode), mCurrNode.getPrefix());
    }

    // // // Namespace access

    public NamespaceContext getNamespaceContext() {
        return this;
    }

    public int getNamespaceCount() {
        if (mCurrEvent != START_ELEMENT && mCurrEvent != END_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_ELEM);
        }
        if (mNsDeclList == null) {
            if (!mNsAware) {
                return 0;
            }
            calcNsAndAttrLists(mCurrEvent == START_ELEMENT);
        }
        return mNsDeclList.size() / 2;
    }

    /**
     * Alas, DOM does not expose any of information necessary for
     * determining actual declarations. Thus, have to indicate that
     * there are no declarations.
     */
    public String getNamespacePrefix(int index) {
        if (mCurrEvent != START_ELEMENT && mCurrEvent != END_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_ELEM);
        }
        if (mNsDeclList == null) {
            if (!mNsAware) {
                handleIllegalNsIndex(index);
            }
            calcNsAndAttrLists(mCurrEvent == START_ELEMENT);
        }
        if (index < 0 || (index + index) >= mNsDeclList.size()) {
            handleIllegalNsIndex(index);
        }
        return (String) mNsDeclList.get(index + index);
    }

    public String getNamespaceURI() {
        if (mCurrEvent != START_ELEMENT && mCurrEvent != END_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_ELEM);
        }
        return mCurrNode.getNamespaceURI();
    }

    public String getNamespaceURI(int index) {
        if (mCurrEvent != START_ELEMENT && mCurrEvent != END_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_ELEM);
        }
        if (mNsDeclList == null) {
            if (!mNsAware) {
                handleIllegalNsIndex(index);
            }
            calcNsAndAttrLists(mCurrEvent == START_ELEMENT);
        }
        if (index < 0 || (index + index) >= mNsDeclList.size()) {
            handleIllegalNsIndex(index);
        }
        return (String) mNsDeclList.get(index + index + 1);
    }

    // Note: implemented as part of NamespaceContext
    //public String getNamespaceURI(String prefix)

    public String getPIData() {
        if (mCurrEvent != PROCESSING_INSTRUCTION) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_PI);
        }
        return mCurrNode.getNodeValue();
    }

    public String getPITarget() {
        if (mCurrEvent != PROCESSING_INSTRUCTION) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_PI);
        }
        return mCurrNode.getNodeName();
    }

    public String getPrefix() {
        if (mCurrEvent != START_ELEMENT && mCurrEvent != END_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_ELEM);
        }
        return mCurrNode.getPrefix();
    }

    public String getText()
    {
        if (((1 << mCurrEvent) & MASK_GET_TEXT) == 0) {
            throwNotTextual(mCurrEvent);
        }
        return mCurrNode.getNodeValue();
    }

    public char[] getTextCharacters()
    {
        String text = getText();
        return text.toCharArray();
    }

    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int len)
    {
        if (((1 << mCurrEvent) & MASK_GET_TEXT) == 0) {
            throwNotTextual(mCurrEvent);
        }
        String text = getText();
        if (len > text.length()) {
            len = text.length();
        }
        text.getChars(sourceStart, sourceStart+len, target, targetStart);
        return len;
    }

    public int getTextLength()
    {
        if (((1 << mCurrEvent) & MASK_GET_TEXT) == 0) {
            throwNotTextual(mCurrEvent);
        }
        return getText().length();
    }

    public int getTextStart()
    {
        if (((1 << mCurrEvent) & MASK_GET_TEXT) == 0) {
            throwNotTextual(mCurrEvent);
        }
        return 0;
    }

    public boolean hasName() {
        return (mCurrEvent == START_ELEMENT) || (mCurrEvent == END_ELEMENT);
    }

    public boolean hasNext() {
        return (mCurrEvent != END_DOCUMENT);
    }

    public boolean hasText() {
        return (((1 << mCurrEvent) & MASK_GET_TEXT) != 0);
    }

    public boolean isAttributeSpecified(int index)
    {
        if (mCurrEvent != START_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        Element elem = (Element) mCurrNode;
        Attr attr = (Attr) elem.getAttributes().item(index);
        if (attr == null) {
            handleIllegalAttrIndex(index);
            return false;
        }
        return attr.getSpecified();
    }

    public boolean isCharacters()
    {
        return (mCurrEvent == CHARACTERS);
    }

    public boolean isEndElement() {
        return (mCurrEvent == END_ELEMENT);
    }

    public boolean isStartElement() {
        return (mCurrEvent == START_ELEMENT);
    }

    /**
     *<p>
     * 05-Apr-2004, TSa: Could try to determine status when text is actually
     *   read. That'd prevent double reads... but would it slow down that
     *   one reading so that net effect would be negative?
     */
    public boolean isWhiteSpace()
    {
        if (mCurrEvent == CHARACTERS || mCurrEvent == CDATA) {
            String text = getText();
            for (int i = 0, len = text.length(); i < len; ++i) {
                /* !!! If xml 1.1 was to be handled, should check for
                 *   LSEP and NEL too?
                 */
                if (text.charAt(i) > 0x0020) {
                    return false;
                }
            }
            return true;
        }
        return (mCurrEvent == SPACE);
    }
    
    public void require(int type, String nsUri, String localName)
        throws XMLStreamException
    {
        int curr = mCurrEvent;

        /* There are some special cases; specifically, SPACE and CDATA
         * are sometimes reported as CHARACTERS. Let's be lenient by
         * allowing both 'real' and reported types, for now.
         */
        if (curr != type) {
            if (curr == CDATA) {
                curr = CHARACTERS;
            } else if (curr == SPACE) {
                curr = CHARACTERS;
            }
        }

        if (type != curr) {
            throwParseError("Expected type "+ErrorConsts.tokenTypeDesc(type)
                            +", current type "
                            +ErrorConsts.tokenTypeDesc(curr));
        }

        if (localName != null) {
            if (curr != START_ELEMENT && curr != END_ELEMENT
                && curr != ENTITY_REFERENCE) {
                throwParseError("Expected non-null local name, but current token not a START_ELEMENT, END_ELEMENT or ENTITY_REFERENCE (was "+ErrorConsts.tokenTypeDesc(mCurrEvent)+")");
            }
            String n = getLocalName();
            if (n != localName && !n.equals(localName)) {
                throwParseError("Expected local name '"+localName+"'; current local name '"+n+"'.");
            }
        }
        if (nsUri != null) {
            if (curr != START_ELEMENT && curr != END_ELEMENT) {
                throwParseError("Expected non-null NS URI, but current token not a START_ELEMENT or END_ELEMENT (was "+ErrorConsts.tokenTypeDesc(curr)+")");
            }

            String uri = getNamespaceURI();
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
        /* For most events, we just need to find the next sibling; and
         * that failing, close the parent element. But there are couple
         * of special cases, which are handled first:
         */
        switch (mCurrEvent) {

        case START_DOCUMENT: // initial state
            /* What to do here depends on what kind of node we started
             * with...
             */
            switch (mCurrNode.getNodeType()) {
            case Node.DOCUMENT_NODE:
            case Node.DOCUMENT_FRAGMENT_NODE:
                // For doc, fragment, need to find first child
                mCurrNode = mCurrNode.getFirstChild();
                break;

            case Node.ELEMENT_NODE:
                // For element, curr node is fine:
                return (mCurrEvent = START_ELEMENT);

            default:
                throw new XMLStreamException("Internal error: unexpected DOM root node type "+mCurrNode.getNodeType()+" for node '"+mCurrNode+"'");
            }
            break;

        case END_DOCUMENT: // end reached: should not call!
            throw new java.util.NoSuchElementException("Can not call next() after receiving END_DOCUMENT");

        case START_ELEMENT: // element returned, need to traverse children, if any
            ++mDepth;
            mAttrList = null; // so it will not get reused accidentally
            {
                Node firstChild = mCurrNode.getFirstChild();
                if (firstChild == null) { // empty? need to return virtual END_ELEMENT
                    /* Note: need not clear namespace declarations, because
                     * it'll be the same as for the start elem!
                     */
                    return (mCurrEvent = END_ELEMENT);
                }
                mNsDeclList = null;

                /* non-empty is easy: let's just swap curr node, and
                 * fall through to regular handling
                 */
                mCurrNode = firstChild;
                break;
            }

        case END_ELEMENT:

            --mDepth;
            // Need to clear these lists
            mAttrList = null;
            mNsDeclList = null;

            /* One special case: if we hit the end of children of
             * the root element (when tree constructed with Element,
             * instead of Document or DocumentFragment). If so, it'll
             * be END_DOCUMENT:
             */
            if (mCurrNode == mRootNode) {
                return (mCurrEvent = END_DOCUMENT);
            }
            // Otherwise need to fall through to default handling:

        default:
            /* For anything else, we can and should just get the
             * following sibling.
             */
            {
                Node next = mCurrNode.getNextSibling();
                // If sibling, let's just assign and fall through
                if (next != null) {
                    mCurrNode = next;
                    break;
                }
                /* Otherwise, need to climb up the stack and either
                 * return END_ELEMENT (if parent is element) or
                 * END_DOCUMENT (if not; needs to be root, then)
                 */
                mCurrNode = mCurrNode.getParentNode();
                int type = mCurrNode.getNodeType();
                if (type == Node.ELEMENT_NODE) {
                    return (mCurrEvent = END_ELEMENT);
                }
                // Let's do sanity check; should really be Doc/DocFragment
                if (mCurrNode != mRootNode ||
                    (type != Node.DOCUMENT_NODE && type != Node.DOCUMENT_FRAGMENT_NODE)) {
                    throw new XMLStreamException("Internal error: non-element parent node ("+type+") that is not the initial root node");
                }
                return (mCurrEvent = END_DOCUMENT);
            }
        }

        // Ok, need to determine current node type:
        switch (mCurrNode.getNodeType()) {
        case Node.CDATA_SECTION_NODE:
            mCurrEvent = CDATA;
            break;
        case Node.COMMENT_NODE:
            mCurrEvent = COMMENT;
            break;
        case Node.DOCUMENT_TYPE_NODE:
            mCurrEvent = DTD;
            break;
        case Node.ELEMENT_NODE:
            mCurrEvent = START_ELEMENT;
            break;
        case Node.ENTITY_REFERENCE_NODE:
            mCurrEvent = ENTITY_REFERENCE;
            break;
        case Node.PROCESSING_INSTRUCTION_NODE:
            mCurrEvent = PROCESSING_INSTRUCTION;
            break;
        case Node.TEXT_NODE:
            mCurrEvent = CHARACTERS;
            break;

            // Should not get other nodes (notation/entity decl., attr)
        case Node.ATTRIBUTE_NODE:
        case Node.ENTITY_NODE:
        case Node.NOTATION_NODE:
            throw new XMLStreamException("Internal error: unexpected DOM node type "+mCurrNode.getNodeType()+" (attr/entity/notation?), for node '"+mCurrNode+"'");

        default:
            throw new XMLStreamException("Internal error: unrecognized DOM node type "+mCurrNode.getNodeType()+", for node '"+mCurrNode+"'");
        }

        return mCurrEvent;
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
		break; // never gets here, but jikes complains without
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
     * input reader. That is, unless the new StAX2 property
     * {@link org.codehaus.stax2.XMLInputFactory2#P_AUTO_CLOSE_INPUT} is
     * set to true.
     */
    public void close()
        throws XMLStreamException
    {
        // Since DOM tree has no real input source, nothing to do
    }


    /*
    ////////////////////////////////////////////////////
    // NamespaceContext
    ////////////////////////////////////////////////////
     */

    public String getNamespaceURI(String prefix)
    {
        /* 26-Apr-2006, TSa: Alas, these methods are DOM Level 3,
         *   i.e. require JDK 1.5 or higher
         */
        /*
        if (prefix.length() == 0) { // def NS
            return mCurrNode.lookupNamespaceURI(null);
        }
        return mCurrNode.lookupNamespaceURI(prefix);
        */
        return null;
    }

    public String getPrefix(String namespaceURI)
    {
        /* 26-Apr-2006, TSa: Alas, these methods are DOM Level 3,
         *   i.e. require JDK 1.5 or higher
         */
        /*
        String prefix = mCurrNode.lookupPrefix(namespaceURI);
        if (prefix == null) { // maybe default NS?
            String defURI = mCurrNode.lookupNamespaceURI(null);
            if (defURI != null && defURI.equals(namespaceURI)) {
                return "";
            }
        }
        return prefix;
        */
        return null;
    }

    public Iterator getPrefixes(String namespaceURI) 
    {
        String prefix = getPrefix(namespaceURI);
        if (prefix == null) {
            return EmptyIterator.getInstance();
        }
        return new SingletonIterator(prefix);
    }

    /*
    ////////////////////////////////////////////////////
    // XMLStreamReader2 (StAX2) implementation
    ////////////////////////////////////////////////////
     */

    // // // StAX2, per-reader configuration

    public Object getFeature(String name)
    {
        // No readable features defined yet...
        throw new IllegalArgumentException(MessageFormat.format(ErrorConsts.ERR_UNKNOWN_FEATURE, new Object[] { name })); 
    }

    public void setFeature(String name, Object value)
    {
        // Base-class has no settable features at this point.
        throw new IllegalArgumentException(MessageFormat.format(ErrorConsts.ERR_UNKNOWN_FEATURE, new Object[] { name })); 
    }

    // NOTE: getProperty() defined in Stax 1.0 interface

    public boolean isPropertySupported(String name) {
        // !!! TBI: not all these properties are really supported
        return mConfig.isPropertySupported(name);
    }

    /**
     * @param name Name of the property to set
     * @param value Value to set property to.
     *
     * @return True, if the specified property was <b>succesfully</b>
     *    set to specified value; false if its value was not changed
     */
    public boolean setProperty(String name, Object value)
    {
        /* Note: can not call local method, since it'll return false for
         * recognized but non-mutable properties
         */
        return mConfig.setProperty(name, value);
    }

    // // // StAX2, additional traversal methods

    public void skipElement() throws XMLStreamException
    {
        if (mCurrEvent != START_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        int nesting = 1; // need one more end elements than start elements

        while (true) {
            int type = next();
            if (type == START_ELEMENT) {
                ++nesting;
            } else if (type == END_ELEMENT) {
                if (--nesting == 0) {
                    break;
                }
            }
        }
    }

    // // // StAX2, additional attribute access

    public AttributeInfo getAttributeInfo() throws XMLStreamException
    {
        if (mCurrEvent != START_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        // !!! TBI
        return null;
    }

    // // // StAX2, Additional DTD access

    /**
     * Since this class implements {@link DTDInfo}, method can just
     * return <code>this</code>.
     */
    public DTDInfo getDTDInfo() throws XMLStreamException
    {
        /* Let's not allow it to be accessed during other events -- that
         * way callers won't count on it being available afterwards.
         */
        if (mCurrEvent != DTD) {
            return null;
        }
        return this;
    }

    // // // StAX2, Additional location information

    /**
     * Location information is always accessible, for this reader.
     */
    public final LocationInfo getLocationInfo() {
        return this;
    }

    // // // StAX2, Pass-through text accessors


    /**
     * Method similar to {@link #getText()}, except
     * that it just uses provided Writer to write all textual content.
     * For further optimization, it may also be allowed to do true
     * pass-through, thus possibly avoiding one temporary copy of the
     * data.
     *<p>
     * TODO: try to optimize to allow completely streaming pass-through:
     * currently will still read all data in memory buffers before
     * outputting
     * 
     * @param w Writer to use for writing textual contents
     * @param preserveContents If true, reader has to preserve contents
     *   so that further calls to <code>getText</code> will return
     *   proper conntets. If false, reader is allowed to skip creation
     *   of such copies: this can improve performance, but it also means
     *   that further calls to <code>getText</code> is not guaranteed to
     *   return meaningful data.
     *
     * @return Number of characters written to the reader
     */
    public int getText(Writer w, boolean preserveContents)
        throws IOException, XMLStreamException
    {
        if (((1 << mCurrEvent) & MASK_GET_TEXT) == 0) {
            throwNotTextual(mCurrEvent);
        }
        String text = getText();
        w.write(text);
        return text.length();
    }

    // // // StAX 2, Other accessors

    /**
     * @return Number of open elements in the stack; 0 when parser is in
     *  prolog/epilog, 1 inside root element and so on.
     */
    public int getDepth() {
        return mDepth;
    }

    /**
     * @return True, if cursor points to a start or end element that is
     *    constructed from 'empty' element (ends with '/>');
     *    false otherwise.
     */
    public boolean isEmptyElement() throws XMLStreamException
    {
        // No way to really figure it out via DOM is there?
        return false;
    }

    public NamespaceContext getNonTransientNamespaceContext()
    {
        /* Since DOM does not expose enough functionality to figure
         * out complete declaration stack, can not implement.
         * Can either return null, or a dummy instance. For now, let's
         * do latter:
         */
        return EmptyNamespaceContext.getInstance();
    }

    public String getPrefixedName()
    {
        switch (mCurrEvent) {
        case START_ELEMENT:
        case END_ELEMENT:
            {
                String prefix = getPrefix();
                String ln = getLocalName();

                if (prefix == null) {
                    return ln;
                }
                StringBuffer sb = new StringBuffer(ln.length() + 1 + prefix.length());
                sb.append(prefix);
                sb.append(':');
                sb.append(ln);
                return sb.toString();
            }
        case ENTITY_REFERENCE:
            return getLocalName();
        case PROCESSING_INSTRUCTION:
            return getPITarget();
        case DTD:
            return getDTDRootName();

        }
        throw new IllegalStateException("Current state ("+ErrorConsts.tokenTypeDesc(mCurrEvent)+") not START_ELEMENT, END_ELEMENT, ENTITY_REFERENCE, PROCESSING_INSTRUCTION or DTD");
    }

    public void closeCompletely() throws XMLStreamException
    {
        // Nothing special to do...
    }

    /*
    ////////////////////////////////////////////////////
    // DTDInfo implementation (StAX 2)
    ////////////////////////////////////////////////////
     */

    public Object getProcessedDTD() {
        return null;
    }

    public String getDTDRootName() {
        if (mCurrEvent == DTD) {
            return ((DocumentType) mCurrNode).getName();
        }
        return null;
    }

    public String getDTDPublicId() {
        if (mCurrEvent == DTD) {
            return ((DocumentType) mCurrNode).getPublicId();
        }
        return null;
    }

    public String getDTDSystemId() {
        if (mCurrEvent == DTD) {
            return ((DocumentType) mCurrNode).getSystemId();
        }
        return null;
    }

    /**
     * @return Internal subset portion of the DOCTYPE declaration, if any;
     *   empty String if none
     */
    public String getDTDInternalSubset() {
        /* DOM (level 3) doesn't expose anything extra; would need to
         * synthetize subset... which would only contain some of the
         * entity and notation declarations.
         */
        return null;
    }

    // // StAX2, v2.0

    public DTDValidationSchema getProcessedDTDSchema() {
        return null;
    }

    /*
    ////////////////////////////////////////////////////
    // LocationInfo implementation (StAX 2)
    ////////////////////////////////////////////////////
     */

    // // // First, the "raw" offset accessors:

    public long getStartingByteOffset() {
        // !!! TBI
        return -1L;
    }

    public long getStartingCharOffset() {
        // !!! TBI
        return 0;
    }

    public long getEndingByteOffset() throws XMLStreamException
    {
        // !!! TBI
        return -1;
    }

    public long getEndingCharOffset() throws XMLStreamException
    {
        // !!! TBI
        return -1;
    }

    // // // and then the object-based access methods:

    public final Location getLocation() {
        return getStartLocation();
    }

    public XMLStreamLocation2 getStartLocation()
    {
        // !!! TBI
        return null;
    }

    public XMLStreamLocation2 getCurrentLocation()
    {
        // !!! TBI
        return null;
    }

    public final XMLStreamLocation2 getEndLocation()
        throws XMLStreamException
    {
        // !!! TBI
        return null;
    }

    /*
    ////////////////////////////////////////////////////
    // Stax2 validation
    ////////////////////////////////////////////////////
     */

    public XMLValidator validateAgainst(XMLValidationSchema schema)
        throws XMLStreamException
    {
        // Not implemented by the basic reader:
        return null;
    }

    public XMLValidator stopValidatingAgainst(XMLValidationSchema schema)
        throws XMLStreamException
    {
        // Not implemented by the basic reader:
        return null;
    }

    public XMLValidator stopValidatingAgainst(XMLValidator validator)
        throws XMLStreamException
    {
        // Not implemented by the basic reader:
        return null;
    }

    public ValidationProblemHandler setValidationProblemHandler(ValidationProblemHandler h)
    {
        // Not implemented by the basic reader
        return null;
    }

    /*
    ////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////
     */

    private QName constructQName(String uri, String ln, String prefix)
    {
        // Stupid QName impls barf on nulls...
        return new QName((uri == null) ? "" : uri, ln,
                         (prefix == null) ? "" : prefix);
    }

    /**
     * @param attrsToo Whether to include actual attributes too, or
     *   just namespace declarations
     */
    private void calcNsAndAttrLists(boolean attrsToo)
    {
        NamedNodeMap attrsIn = mCurrNode.getAttributes();

        // A common case: neither attrs nor ns decls, can use short-cut
        int len = attrsIn.getLength();
        if (len == 0) {
            mAttrList = mNsDeclList = Collections.EMPTY_LIST;
            return;
        }

        if (!mNsAware) {
            mAttrList = new ArrayList(len);
            for (int i = 0; i < len; ++i) {
                mAttrList.add(attrsIn.item(i));
            }
            mNsDeclList = Collections.EMPTY_LIST;
            return;
        }

        // most should be attributes... and possibly no ns decls:
        ArrayList attrsOut = null;
        ArrayList nsOut = null;

        for (int i = 0; i < len; ++i) {
            Node attr = attrsIn.item(i);
            String prefix = attr.getPrefix();

            // Prefix?
            if (prefix == null || prefix.length() == 0) { // nope
                // default ns decl?
                if (!"xmlns".equals(attr.getLocalName())) { // nope
                    if (attrsToo) {
                        if (attrsOut == null) {
                            attrsOut = new ArrayList(len - i);
                        }
                        attrsOut.add(attr);
                    }
                    continue;
                }
                prefix = "";
            } else { // explicit ns decl?
                if (!"xmlns".equals(prefix)) { // nope
                    if (attrsToo) {
                        if (attrsOut == null) {
                            attrsOut = new ArrayList(len - i);
                        }
                        attrsOut.add(attr);
                    }
                    continue;
                }
                prefix = attr.getLocalName();
            }
            if (nsOut == null) {
                nsOut = new ArrayList((len - i) * 2);
            }
            nsOut.add(prefix);
            nsOut.add(attr.getNodeValue());
        }

        mAttrList = (attrsOut == null) ? Collections.EMPTY_LIST : attrsOut;
        mNsDeclList = (nsOut == null) ? Collections.EMPTY_LIST : nsOut;
    }

    /**
     * Method that returns location of the last character returned by this
     * reader; that is, location "one less" than the currently pointed to
     * location.
     */
    protected WstxInputLocation getLastCharLocation()
    {
        // !!! TBI
        return null;
    }

    private void throwNotTextual(int type)
    {
        throw new IllegalStateException("Not a textual event ("
                                        +ErrorConsts.tokenTypeDesc(mCurrEvent)+")");
    }

    private void throwParseError(String msg)
        throws WstxParsingException
    {
        throw new WstxParsingException(msg, getLastCharLocation());
    }

    /*
    private void throwParseError(String format, Object arg)
        throws WstxParsingException
    {
        String msg = MessageFormat.format(format, new Object[] { arg });
        throw new WstxParsingException(msg, getLastCharLocation());
    }
    */

    private void handleIllegalAttrIndex(int index)
    {
        Element elem = (Element) mCurrNode;
        NamedNodeMap attrs = elem.getAttributes();
        int len = attrs.getLength();
        String msg = "Illegal attribute index "+index+"; element <"+elem.getNodeName()+"> has "+((len == 0) ? "no" : String.valueOf(len))+" attributes";
        throw new IllegalArgumentException(msg);
    }

    private void handleIllegalNsIndex(int index)
    {
        String msg = "Illegal namespace declaration index "+index+" (has "+getNamespaceCount()+" ns declarations)";
        throw new IllegalArgumentException(msg);
    }

    /**
     * Due to differences in how namespace-aware and non-namespace modes
     * work in DOM, different methods are needed. We may or may not be
     * able to detect namespace-awareness mode of the source Nodes
     * directly; but at any rate, should contain some logic for handling
     * problem cases.
     */
    private String safeGetLocalName(Node n)
    {
        String ln = n.getLocalName();
        if (ln == null) {
            ln = n.getNodeName();
        }
        return ln;
    }
}

