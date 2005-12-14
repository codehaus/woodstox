/* Woodstox XML processor
 *
 * Copyright (c) 2004 Tatu Saloranta, tatu.saloranta@iki.fi
 *
 * Licensed under the License specified in the file LICENSE,
 * included with the source code.
 * You may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ctc.wstx.sw;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;

import org.codehaus.stax2.XMLStreamReader2;

import com.ctc.wstx.api.WriterConfig;
import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.util.DefaultXmlSymbolTable;

/**
 * Mid-level base class of namespace-aware stream writers. Contains
 * shared functionality between repairing and non-repairing implementations.
 */
public abstract class BaseNsStreamWriter
    extends BaseStreamWriter
{
    /*
    ////////////////////////////////////////////////////
    // Constants
    ////////////////////////////////////////////////////
     */

    final protected static String sPrefixXml = DefaultXmlSymbolTable.getXmlSymbol();

    final protected static String sPrefixXmlns = DefaultXmlSymbolTable.getXmlnsSymbol();

    final protected static String ERR_NSDECL_WRONG_STATE =
        "Trying to write a namespace declaration when there is no open start element.";


    /*
    ////////////////////////////////////////////////////
    // Configuration (options, features)
    ////////////////////////////////////////////////////
     */

    // // // Additional specific config flags base class doesn't have

    /**
     * True, if writer needs to automatically output namespace declarations
     * (we are in repairing mode)
     */
    final protected boolean mAutomaticNS;

    /*
    ////////////////////////////////////////////////////
    // State information
    ////////////////////////////////////////////////////
     */

    protected SimpleOutputElement mCurrElem = SimpleOutputElement.createRoot();

    /**
     * Optional "root" namespace context that application can set. If so,
     * it can be used to lookup namespace/prefix mappings
     */
    protected NamespaceContext mRootNsContext = null;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle (ctors)
    ////////////////////////////////////////////////////
     */

    public BaseNsStreamWriter(Writer w, String enc, WriterConfig cfg,
                              boolean repairing)
    {
        super(w, enc, cfg);
        mAutomaticNS = repairing;
    }

    /*
    ////////////////////////////////////////////////////
    // XMLStreamWriter API
    ////////////////////////////////////////////////////
     */

    public NamespaceContext getNamespaceContext() {
        return mCurrElem;
    }

    public String getPrefix(String uri) {
        return mCurrElem.getPrefix(uri);
    }

    public abstract void setDefaultNamespace(String uri)
        throws XMLStreamException;

    /**
     *<p>
     * Note: Root namespace context works best if automatic prefix
     * creationg ("namespace/prefix repairing" in StAX lingo) is enabled.
     */
    public void setNamespaceContext(NamespaceContext ctxt)
    {
        // This is only allowed before root element output:
        if (mState != STATE_PROLOG) {
            throw new IllegalStateException("Called setNamespaceContext() after having already output root element.");
        }

        mRootNsContext = ctxt;
        mCurrElem.setRootNsContext(ctxt);
    }

    public void setPrefix(String prefix, String uri)
        throws XMLStreamException
    {
        if (prefix == null) {
            throw new NullPointerException("Can not pass null 'prefix' value");
        }
        // Are we actually trying to set the default namespace?
        if (prefix.length() == 0) {
            setDefaultNamespace(uri);
            return;
        }
        if (uri == null) {
            throw new NullPointerException("Can not pass null 'uri' value");
        }

        /* 25-Sep-2004, TSa: Let's check that "xml" and "xmlns" are not
         *     (re-)defined to any other value, nor that value they 
         *     are bound to are bound to other prefixes.
         */
        /* 01-Apr-2005, TSa: And let's not leave it optional: such
         *   bindings should never succeed.
         */
        {
            if (prefix.equals(sPrefixXml)) { // prefix "xml"
                if (!uri.equals(XMLConstants.XML_NS_URI)) {
                    throwOutputError(ErrorConsts.ERR_NS_REDECL_XML, uri);
                }
            } else if (prefix.equals(sPrefixXmlns)) { // prefix "xmlns"
                if (!uri.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
                    throwOutputError(ErrorConsts.ERR_NS_REDECL_XMLNS, uri);
                }
            } else {
                // Neither of prefixes.. but how about URIs?
                if (uri.equals(XMLConstants.XML_NS_URI)) {
                    throwOutputError(ErrorConsts.ERR_NS_REDECL_XML_URI, prefix);
                } else if (uri.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
                    throwOutputError(ErrorConsts.ERR_NS_REDECL_XMLNS_URI, prefix);
                }
            }
            
            /* 05-Feb-2005, TSa: Also, as per namespace specs; the 'empty'
             *   namespace URI can not be bound as a non-default namespace
             *   (ie. for any actual prefix)
             */
            if (uri.length() == 0) {
                throwOutputError(ErrorConsts.ERR_NS_EMPTY);
            }
        }

        doSetPrefix(prefix, uri);
    }

    /**
     * It's assumed calling this method implies caller just wants to add
     * an attribute that does not belong to any namespace; as such no
     * namespace checking or prefix generation is needed.
     */
    public void writeAttribute(String localName, String value)
        throws XMLStreamException
    {
        // No need to set mAnyOutput, nor close the element
        if (!mStartElementOpen) {
            throw new IllegalStateException(ErrorConsts.WERR_ATTR_NO_ELEM);
        }
        doWriteAttr(localName, null, null, value);
    }

    public abstract void writeAttribute(String nsURI, String localName, String value)
        throws XMLStreamException;

    public abstract void writeAttribute(String prefix, String nsURI,
                                        String localName, String value)
        throws XMLStreamException;

    /**
     *<p>
     * Note: It is assumed caller just wants the element to belong to whatever
     * is the current default namespace.
     */
    public void writeEmptyElement(String localName)
        throws XMLStreamException
    {
        checkStartElement(localName);
        if (mValidator != null) {
            mValidator.validateElementStart(localName, NO_NS_URI, NO_PREFIX);
        }
        mEmptyElement = true;
        mCurrElem = mCurrElem.createChild(localName);
        doWriteStartElement(NO_PREFIX, localName);

    }

    public void writeEmptyElement(String nsURI, String localName)
        throws XMLStreamException
    {
        writeStartOrEmpty(localName, nsURI);
        mEmptyElement = true;
    }

    public void writeEmptyElement(String prefix, String localName, String nsURI)
        throws XMLStreamException
    {
        writeStartOrEmpty(prefix, localName, nsURI);
        mEmptyElement = true;
    }

    public void writeEndElement()
        throws XMLStreamException
    {
        doWriteEndElement(null, mCfgAutomaticEmptyElems);
    }

    /**
     * This method is assumed to just use default namespace (if any),
     * and no further checks should be done.
     */
    public void writeStartElement(String localName)
        throws XMLStreamException
    {
        checkStartElement(localName);
        if (mValidator != null) {
            mValidator.validateElementStart(localName, NO_NS_URI, NO_PREFIX);
        }
        mEmptyElement = false;
        mCurrElem = mCurrElem.createChild(localName);

        doWriteStartElement(NO_PREFIX, localName);
    }

    public void writeStartElement(String nsURI, String localName)
        throws XMLStreamException
    {
        writeStartOrEmpty(localName, nsURI);
        mEmptyElement = false;
    }

    public void writeStartElement(String prefix, String localName, String nsURI)
        throws XMLStreamException
    {
        writeStartOrEmpty(prefix, localName, nsURI);
        mEmptyElement = false;
    }

    /*
    ////////////////////////////////////////////////////
    // Remaining XMLStreamWriter2 methods (StAX2)
    ////////////////////////////////////////////////////
     */

    /**
     * Similar to {@link #writeEndElement}, but never allows implicit
     * creation of empty elements.
     */
    public void writeFullEndElement()
        throws XMLStreamException
    {
        doWriteEndElement(null, false);
    }

    /*
    ////////////////////////////////////////////////////
    // Remaining ValidationContext methods (StAX2)
    ////////////////////////////////////////////////////
     */

    public QName getCurrentElementName()
    {
        return mCurrElem.getName();
    }

    public String getNamespaceURI(String prefix) {
        return mCurrElem.getNamespaceURI(prefix);
    }

    /*
    //////////////////////////////////////////////////////////
    // Implementations for base-class defined abstract methods
    //////////////////////////////////////////////////////////
     */

    /**
     * Method called by {@link com.ctc.wstx.evt.WstxEventWriter} (instead of the version
     * that takes no argument), so that we can verify it does match the
     * start element, if necessary
     */
    public void writeEndElement(QName name)
        throws XMLStreamException
    {
        doWriteEndElement(mCheckStructure ? name : null,
                          mCfgAutomaticEmptyElems);
    }

    /**
     * Method called to close an open start element, when another
     * main-level element (not namespace declaration or attribute)
     * is being output; except for end element which is handled differently.
     *
     * @param emptyElem If true, the element being closed is an empty
     *   element; if false, a separate stand-alone start element.
     */
    protected void closeStartElement(boolean emptyElem)
        throws XMLStreamException
    {
        mStartElementOpen = false;

        // 01-Apr-2005, TSa: Can we check anything regarding NS output?

        try {
            if (emptyElem) {
                // Extra space for readability (plus, browsers like it if using XHTML)
                mWriter.write(" />");
            } else {
                mWriter.write('>');
            }
        } catch (IOException ioe) {
            throw new XMLStreamException(ioe);
        }

        // Need bit more special handling for empty elements...
        if (emptyElem) {
            mCurrElem = mCurrElem.getParent();
            if (mCurrElem.isRoot()) {
                mState = STATE_EPILOG;
            }
        }
    }

    protected String getTopElementDesc() {
        return mCurrElem.getNameDesc();
    }

    /*
    ////////////////////////////////////////////////////
    // Package methods sub-classes may also need
    ////////////////////////////////////////////////////
     */

    /**
     * Method that is called to ensure that we can start writing an
     * element, both from structural point of view, and from syntactic
     * (close previously open start element, if any).
     */
    protected void checkStartElement(String localName)
        throws XMLStreamException
    {
        // Need to finish an open start element?
        if (mStartElementOpen) {
            closeStartElement(mEmptyElement);
        } else if (mCheckStructure && mState == STATE_EPILOG) {
            throw new IllegalStateException("Trying to output second root ('"
                                            +localName+"').");
        }

        if (mCheckContent) {
            verifyNameValidity(localName, mNsAware);
        }

        if (mState == STATE_PROLOG) {
            mState = STATE_TREE;
        }
    }

    protected void doWriteAttr(String localName, String nsURI, String prefix,
                               String value)
        throws XMLStreamException
    {
        if (mCheckNames) {
            verifyNameValidity(localName, true);
        }
        if (mCheckAttr || mValidator != null) { // still need to ensure no duplicate attrs?
            mCurrElem.checkAttrWrite(nsURI, localName, value);
        }

        if (mValidator != null) {
            /* No need to get it normalized... even if validator does normalize
             * it, we don't use that for anything
             */
            mValidator.validateAttribute(localName, nsURI, prefix, value);
        }
        try {
            if (mAttrValueWriter == null) {
                mAttrValueWriter = constructAttributeValueWriter();
            }
            mWriter.write(' ');
            if (prefix != null && prefix.length() > 0) {
                mWriter.write(prefix);
                mWriter.write(':');
            }
            mWriter.write(localName);
            mWriter.write("=\"");
            mAttrValueWriter.write(value);
            mWriter.write('"');
        } catch (IOException ioe) {
            throw new XMLStreamException(ioe);
        }
    }

    protected void doWriteNamespace(String prefix, String nsURI)
        throws XMLStreamException
    {
        try {
            mWriter.write(' ');
            mWriter.write(XMLConstants.XMLNS_ATTRIBUTE);
            if (prefix != null && prefix.length() > 0) {
                mWriter.write(':');
                mWriter.write(prefix);
            }
            mWriter.write("=\"");
            if (nsURI != null && nsURI.length() > 0) {
                mWriter.write(nsURI);
            }
            mWriter.write('"');
        } catch (IOException ioe) {
            throw new XMLStreamException(ioe);
        }
    }

    protected void doWriteStartElement(String prefix, String localName)
        throws XMLStreamException
    {
        if (mCheckNames) {
            if (prefix != null && prefix.length() > 0) {
                verifyNameValidity(prefix, true);
            }
            verifyNameValidity(localName, true);
        }
        mAnyOutput = true;
        mStartElementOpen = true;
        try {
            mWriter.write('<');
            if (prefix != null && prefix.length() > 0) {
                mWriter.write(prefix);
                mWriter.write(':');
            }
            mWriter.write(localName);
        } catch (IOException ioe) {
            throw new XMLStreamException(ioe);
        }
    }

    /**
     *
     * @param expName Name that the closing element should have; null
     *   if whatever is in stack should be used
     * @param allowEmpty If true, is allowed to create the empty element
     *   if the closing element was truly empty; if false, has to write
     *   the full empty element no matter what
     */
    protected void doWriteEndElement(QName expName, boolean allowEmpty)
        throws XMLStreamException
    {
        /* First of all, do we need to close up an earlier empty element?
         * (open start element that was not created via call to
         * writeEmptyElement gets handled later on)
         */
        if (mStartElementOpen && mEmptyElement) {
            mEmptyElement = false;
            closeStartElement(true);
        }

        // Better have something to close... (to figure out what to close)
        if (mState != STATE_TREE) {
            throwOutputError("No open start element, when trying to write end element");
        }

        String prefix = mCurrElem.getPrefix();
        String localName = mCurrElem.getLocalName();
        // Ok, and then let's pop that element from the stack
        mCurrElem = mCurrElem.getParent();

        if (expName != null) {
            /* Let's only check the local name, for now...
             */
            if (!localName.equals(expName.getLocalPart())) {
                /* Only gets called when trying to output an XMLEvent... in
                 * which case names can actually be compared
                 */
                throw new IllegalArgumentException("Mismatching close element local name, '"+localName+"'; expected '"+expName.getLocalPart()+"'.");
            }
        }

        /* And this seems like the place to handle validation, right before
         * outputting it:
         */
        if (mValidator != null) {
            mVldContent = mValidator.validateElementEnd(localName, mCurrElem.getNamespaceURI(), prefix);
        }

        /* Now, do we have an unfinished start element (created via
         * writeStartElement() earlier)?
         */
        if (mStartElementOpen) {
            /* Can't/shouldn't call closeStartElement, but need to do same
             * processing. Thus, this is almost identical to closeStartElement:
             */
            mStartElementOpen = false;

            try {
                // We could write an empty element, implicitly?
                if (allowEmpty) {
                    // Extra space for readability
                    mWriter.write(" />");
                    if (mCurrElem.isRoot()) {
                        mState = STATE_EPILOG;
                    }
                    return;
                }
                // Nah, need to close open elem, and then output close elem
                mWriter.write('>');
            } catch (IOException ioe) {
                throw new XMLStreamException(ioe);
            }
        }

        try {
            mWriter.write("</");
            if (prefix != null && prefix.length() > 0) {
                mWriter.write(prefix);
                mWriter.write(':');
            }
            mWriter.write(localName);
            mWriter.write('>');
        } catch (IOException ioe) {
            throw new XMLStreamException(ioe);
        }

        if (mCurrElem.isRoot()) {
            mState = STATE_EPILOG;
        }
    }

    /*
    ////////////////////////////////////////////////////
    // More abstract methods for sub-classes to implement
    ////////////////////////////////////////////////////
     */

    public abstract void doSetPrefix(String prefix, String uri)
        throws XMLStreamException;

    public abstract void writeDefaultNamespace(String nsURI)
        throws XMLStreamException;
    public abstract void writeNamespace(String prefix, String nsURI)
        throws XMLStreamException;

    public abstract void writeStartElement(StartElement elem)
        throws XMLStreamException;

    protected abstract void writeStartOrEmpty(String localName, String nsURI)
        throws XMLStreamException;

    protected abstract void writeStartOrEmpty(String prefix, String localName, String nsURI)
        throws XMLStreamException;
}
