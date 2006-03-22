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
import org.codehaus.stax2.validation.XMLValidationSchema;
import org.codehaus.stax2.validation.XMLValidator;

import com.ctc.wstx.api.ReaderConfig;
import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.exc.WstxParsingException;
import com.ctc.wstx.io.WstxInputLocation;
import com.ctc.wstx.util.TextAccumulator;

/**
 * This is an adapter class that presents a DOM document as if it was
 * a regular {@link XMLStreamReader}
 *<p>
 * Note that the implementation is only to be used for use with
 * <code>javax.xml.transform.dom.DOMSource</code>. It can however be
 * used for both full documents, and single element root fragments,
 * depending on what node is passed as the argument.
 */
public class DOMWrappingReader
    implements XMLStreamReader2,
               DTDInfo, LocationInfo,
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

    // // // State:

    protected int mCurrEvent = START_DOCUMENT;

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
        mConfig = cfg;
        mSystemId = sysId;

        /* Ok; we need a document node; or an element node; or a document
         * fragment node.
         */

        if (treeRoot instanceof Document) {
        } else if (treeRoot instanceof Document) {
        } else if (treeRoot instanceof DocumentFragment) {
        } else {
            if (treeRoot == null) {
                throw new IllegalArgumentException("Can not pass null Node for constructing a DOM-based XMLStreamReader");
            }
            throw new XMLStreamException("Can not create an XMLStreamReader for a DOM node of type "+treeRoot.getClass());
        }
        mRootNode = treeRoot;
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
        // !!! TBI:
        return null;
    }

    /**
     * As per Stax (1.0) specs, needs to return whatever parser determined
     * the encoding was, if it was able to figure it out. If not (there are
     * cases where this can not be found; specifically when being passed a
     * {@link java.io.Reader}), it should return null.
     */
    public String getEncoding() {
        // !!! TBI:
        return null;
    }

    public String getVersion()
    {
        // !!! TBI:
        return null;
    }

    public boolean isStandalone() {
        // !!! TBI:
        return false;
    }

    public boolean standaloneSet() {
        // !!! TBI:
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

    public int getAttributeCount() {
        if (mCurrEvent != START_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        // !!! TBI
        return 0;
    }

	public String getAttributeLocalName(int index) {
        if (mCurrEvent != START_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        // !!! TBI
        return null;
    }

    public QName getAttributeName(int index) {
        if (mCurrEvent != START_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        // !!! TBI
        return null;
    }

    public String getAttributeNamespace(int index) {
        if (mCurrEvent != START_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        // !!! TBI
        return null;
    }

    public String getAttributePrefix(int index) {
        if (mCurrEvent != START_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        // !!! TBI
        return null;
    }

    public String getAttributeType(int index) {
        if (mCurrEvent != START_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        // !!! TBI
        return null;
    }

    public String getAttributeValue(int index) {
        if (mCurrEvent != START_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        // !!! TBI
        return null;
    }

    public String getAttributeValue(String nsURI, String localName) {
        if (mCurrEvent != START_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        // !!! TBI
        return null;
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
        // Note: for this we need not (yet) finish reading element
        if (mCurrEvent == START_ELEMENT || mCurrEvent == END_ELEMENT) {
            // !!! TBI
            return null;
        } else if (mCurrEvent == ENTITY_REFERENCE) {
            // !!! TBI
            return null;
        } else {
            throw new IllegalStateException("Current state not START_ELEMENT, END_ELEMENT or ENTITY_REFERENCE");
        }
    }

    // // // getLocation() defined in StreamScanner

    public QName getName()
    {
        if (mCurrEvent != START_ELEMENT && mCurrEvent != END_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_ELEM);
        }
        // !!! TBI
        return null;
    }

    // // // Namespace access

    public NamespaceContext getNamespaceContext() {
        // !!! TBI
        return null;
    }

    public int getNamespaceCount() {
        if (mCurrEvent != START_ELEMENT && mCurrEvent != END_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_ELEM);
        }
        // !!! TBI
        return 0;
    }

    public String getNamespacePrefix(int index) {
        if (mCurrEvent != START_ELEMENT && mCurrEvent != END_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_ELEM);
        }
        // !!! TBI
        return null;
    }

    public String getNamespaceURI() {
        if (mCurrEvent != START_ELEMENT && mCurrEvent != END_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_ELEM);
        }
        // !!! TBI
        return null;
    }

    public String getNamespaceURI(int index) {
        if (mCurrEvent != START_ELEMENT && mCurrEvent != END_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_ELEM);
        }
        // !!! TBI
        return null;
    }

    public String getNamespaceURI(String prefix) {
        if (mCurrEvent != START_ELEMENT && mCurrEvent != END_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_ELEM);
        }
        // !!! TBI
        return null;
    }

    public String getPIData() {
        if (mCurrEvent != PROCESSING_INSTRUCTION) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_PI);
        }
        // !!! TBI
        return null;
    }

    public String getPITarget() {
        if (mCurrEvent != PROCESSING_INSTRUCTION) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_PI);
        }
        // !!! TBI
        return null;
    }

    public String getPrefix() {
        if (mCurrEvent != START_ELEMENT && mCurrEvent != END_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_ELEM);
        }
        // !!! TBI
        return null;
    }

    public String getText()
    {
        if (((1 << mCurrEvent) & MASK_GET_TEXT) == 0) {
            throwNotTextual(mCurrEvent);
        }
        // !!! TBI
        return null;
    }

    public char[] getTextCharacters()
    {
        if (((1 << mCurrEvent) & MASK_GET_TEXT) == 0) {
            throwNotTextual(mCurrEvent);
        }
        // !!! TBI
        return null;
    }

    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int len)
    {
        if (((1 << mCurrEvent) & MASK_GET_TEXT) == 0) {
            throwNotTextual(mCurrEvent);
        }
        // !!! TBI
        return -1;
    }

    public int getTextLength()
    {
        if (((1 << mCurrEvent) & MASK_GET_TEXT) == 0) {
            throwNotTextual(mCurrEvent);
        }
        // !!! TBI
        return 0;
    }

    public int getTextStart()
    {
        if (((1 << mCurrEvent) & MASK_GET_TEXT) == 0) {
            throwNotTextual(mCurrEvent);
        }
        // !!! TBI
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
        // !!! TBI
        return true;
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
            // !!! TBI
            return false;
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

            // !!! TBI
            /*
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
            */
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
        // !!! TBI
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
        // !!! TBI
        return -1;
    }

    // // // StAX 2, Other accessors

    /**
     * @return Number of open elements in the stack; 0 when parser is in
     *  prolog/epilog, 1 inside root element and so on.
     */
    public int getDepth() {
        // !!! TBI
        return -1;
    }

    /**
     * @return True, if cursor points to a start or end element that is
     *    constructed from 'empty' element (ends with '/>');
     *    false otherwise.
     */
    public boolean isEmptyElement() throws XMLStreamException
    {
        // !!! TBI
        return false;
    }

    public NamespaceContext getNonTransientNamespaceContext()
    {
        // !!! TBI
        return null;
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
        throw new IllegalStateException("Current state not START_ELEMENT, END_ELEMENT, ENTITY_REFERENCE, PROCESSING_INSTRUCTION or DTD");
    }

    public void closeCompletely() throws XMLStreamException
    {
        // !!! TBI
    }

    /*
    ////////////////////////////////////////////////////
    // DTDInfo implementation (StAX 2)
    ////////////////////////////////////////////////////
     */

    public Object getProcessedDTD() {
        // !!! TBI
        return null;
    }

    public String getDTDRootName() {
        // !!! TBI
        return null;
    }

    public String getDTDPublicId() {
        // !!! TBI
        return null;
    }

    public String getDTDSystemId() {
        // !!! TBI
        return null;
    }

    /**
     * @return Internal subset portion of the DOCTYPE declaration, if any;
     *   empty String if none
     */
    public String getDTDInternalSubset() {
        // !!! TBI
        return null;
    }

    // // StAX2, v2.0

    /**
     * Sub-class will override this method
     */
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

    /*
    ////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////
     */

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

    public void throwParseError(String msg)
        throws WstxParsingException
    {
        throw new WstxParsingException(msg, getLastCharLocation());
    }

    public void throwParseError(String format, Object arg)
        throws WstxParsingException
    {
        String msg = MessageFormat.format(format, new Object[] { arg });
        throw new WstxParsingException(msg, getLastCharLocation());
    }

}

