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

import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.util.DefaultXmlSymbolTable;
import com.ctc.wstx.util.XMLQuoter;

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

    /*
    ////////////////////////////////////////////////////
    // Configuration (options, features)
    ////////////////////////////////////////////////////
     */

    // // // Additional specific config flags base class doesn't have

    final protected boolean mCheckNS;

    /*
    ////////////////////////////////////////////////////
    // State information
    ////////////////////////////////////////////////////
     */

    final protected static OutputElement sSharedRootElem = OutputElement.getRootInstance();

    /**
     * Currently active output element; contains information necessary
     * for handling attributes and namespaces
     */
    protected OutputElement mCurrElem = sSharedRootElem;

    /**
     * Container in which namespace declarations are stored, before the
     * start element has been output. Will be used for passing namespace
     * declaration information to the start element.
     */
    protected OutputElement.Declarations mNsDecl = null;

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

    public BaseNsStreamWriter(Writer w, WriterConfig cfg)
    {
        super(w, cfg);
        mCheckNS = cfg.willValidateNamespaces();
    }

    /*
    ////////////////////////////////////////////////////
    // XMLStreamWriter API
    ////////////////////////////////////////////////////
     */

    public NamespaceContext getNamespaceContext()
    {
        /* We could always create the declarations Object; that would make
         * NamespaceContext live (ie. reflect further bindings)... but
         * there's probably no need for that?
         */
        if (mNsDecl == null) {
            return mCurrElem;
        }
        return mNsDecl;
    }

    public String getPrefix(String uri) {
        if (mNsDecl != null) {
            return mNsDecl.getPrefix(uri);
        }
        return mCurrElem.getPrefix(uri);
    }

    public void setDefaultNamespace(String uri)
        throws XMLStreamException
    {
        if (mNsDecl == null) {
            mNsDecl = new OutputElement.Declarations(mCurrElem);
        }
        mNsDecl.setDefaultNsUri(uri);
    }

    /**
     *<p>
     * Note: Root namespace context works best if automatic prefix
     * creationg ("namespace/prefix repairing" in StAX lingo) is enabled.
     */
    public void setNamespaceContext(NamespaceContext context)
    {
        // This is only allowed before root element output:
        if (mState != STATE_PROLOG) {
            throw new IllegalStateException("Called setNamespaceContext() after having already output root element.");
        }

        mRootNsContext = context;
    }

    public void setPrefix(String prefix, String uri)
        throws XMLStreamException
    {
        /* 25-Sep-2004, TSa: Let's check that "xml" and "xmlns" are not
         *     (re-)defined to any other value, nor that value they 
         *     are bound to are bound to other prefixes.
         */
        if (mCheckNS) {
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
        }

        if (mNsDecl == null) {
            mNsDecl = new OutputElement.Declarations(mCurrElem);
        }
        mNsDecl.addNamespace(prefix, uri);
    }

    /**
     *<p>
     * It's assumed calling this method implies caller just wants to add
     * an attribute that does not belong to any namespace.
     */
    public void writeAttribute(String localName, String value)
        throws XMLStreamException
    {
        if (mCheckContent) {
            checkNameValidity(localName, false);
        }

        // No need to set mAnyOutput, nor close the element
        if (!mStartElementOpen) {
            throw new IllegalStateException("Trying to write an attribute when there is no open start element.");
        }

        doWriteAttr(localName, null, null, value);
    }

    public void writeAttribute(String nsURI, String localName, String value)
        throws XMLStreamException
    {
        // No need to set mAnyOutput, nor close the element
        if (!mStartElementOpen) {
            throw new IllegalStateException("Trying to write an attribute when there is no open start element.");
        }
        // Need a prefix...
        String prefix = findOrCreatePrefix(null, nsURI, false);
        doWriteAttr(localName, nsURI, prefix, value);
    }

    public void writeAttribute(String prefix, String nsURI,
                               String localName, String value)
        throws XMLStreamException
    {
        // No need to set mAnyOutput, nor close the element
        if (!mStartElementOpen) {
            throw new IllegalStateException("Trying to write an attribute when there is no open start element.");
        }

        // May want to verify prefix validity:
        if (mCheckNS) {
            int status = mCurrElem.isPrefixValid(prefix, nsURI, mCheckNS, false);
            if (status != OutputElement.PREFIX_OK) {
                if (status == OutputElement.PREFIX_MISBOUND) {
                    prefix = findOrCreatePrefix(prefix, nsURI, false);
                }
                mCurrElem.addPrefix(prefix, nsURI);
                doWriteNamespace(prefix, nsURI);
            }
        }
        doWriteAttr(localName, nsURI, prefix, value);
    }

    /**
     *<p>
     * Note: It is assumed caller just wants the element to belong to whatever
     * is the current default namespace.
     */
    public void writeEmptyElement(String localName)
        throws XMLStreamException
    {
        checkStartElement(localName);

        mEmptyElement = true;
        mCurrElem = new OutputElement(mCurrElem, localName, mNsDecl, mCheckNS);
        doWriteStartElement(null, localName);

        // Need to clear namespace declaration info now for next start elem:
        mNsDecl = null;

    }

    public void writeEmptyElement(String nsURI, String localName)
        throws XMLStreamException
    {
        checkStartElement(localName);

        mEmptyElement = true;
        mCurrElem = new OutputElement(mCurrElem, localName, mNsDecl, mCheckNS);

        // Need a prefix....
        String prefix = mCurrElem.findPrefix(nsURI, true);
        boolean nsOk = (prefix != null);

        if (!nsOk) {
            prefix = generatePrefix(null, nsURI);
            mCurrElem.addPrefix(prefix, nsURI);
        }
        mCurrElem.setPrefix(prefix);
        doWriteStartElement(prefix, localName);

        // Need to clear namespace declaration info now for next start elem:
        mNsDecl = null;

    }

    public void writeEmptyElement(String prefix, String localName, String nsURI)
        throws XMLStreamException
    {
        writeStartOrEmpty(prefix, localName, nsURI, true);
    }

    public void writeEndElement()
        throws XMLStreamException
    {
        // Better have something to close... (to figure out what to close)
        if (mState != STATE_TREE) {
            throw new XMLStreamException("No open start element, when calling writeEndElement.");
        }

        String prefix = mCurrElem.getPrefix();
        String localName = mCurrElem.getLocalName();
        mCurrElem = mCurrElem.getParent();
        doWriteEndElement(prefix, localName);
    }

    public void writeStartElement(String localName)
        throws XMLStreamException
    {
        checkStartElement(localName);

        mEmptyElement = false;
        mCurrElem = new OutputElement(mCurrElem, localName, mNsDecl, mCheckNS);

        doWriteStartElement(null, localName);

        // Need to clear namespace declaration info now for next start elem:
        mNsDecl = null;
    }

    public void writeStartElement(String nsURI, String localName)
        throws XMLStreamException
    {
        checkStartElement(localName);

        mEmptyElement = false;
        mCurrElem = new OutputElement(mCurrElem, localName, mNsDecl, mCheckNS);

        // Need a prefix?
        String prefix = mCurrElem.findPrefix(nsURI, true);
        boolean nsOk = (prefix != null);

        if (!nsOk) {
            prefix = generatePrefix(null, nsURI);
            mCurrElem.addPrefix(prefix, nsURI);
        }
        mCurrElem.setPrefix(prefix);
        doWriteStartElement(prefix, localName);

        // Need to clear namespace declaration info now for next start elem:
        mNsDecl = null;
    }

    public void writeStartElement(String prefix, String localName, String nsURI)
        throws XMLStreamException
    {
        writeStartOrEmpty(prefix, localName, nsURI, true);
    }
    
    /*
    ////////////////////////////////////////////////////
    // Package methods implementations
    ////////////////////////////////////////////////////
     */

    /**
     * Method called by {@link com.ctc.wstx.evt.WstxEventWriter} (instead of the version
     * that takes no argument), so that we can verify it does match the
     * start element, if necessary
     */
    public void writeEndElement(QName name)
        throws XMLStreamException
    {
        /* Well, for one, we better have an open element in stack; otherwise
         * there's no way to figure out which element name to use.
         */
        String prefix = mCurrElem.getPrefix();
        String local = mCurrElem.getLocalName();
        if (mCheckStructure) {
            if (mState != STATE_TREE) {
                throw new XMLStreamException("No open start element, when calling writeEndElement.");
            }
            String local2 = mCurrElem.getLocalName();
            if (!local.equals(local2)) {
                throw new IllegalArgumentException("Mismatching close element local name, '"+local+"'; expected '"+local2+"'.");
            }
            String prefix2 = mCurrElem.getPrefix();
            if (!prefix2.equals(prefix)) {
                throw new IllegalArgumentException("Mismatching close element prefix, '"+prefix+"'; expected '"+prefix2+"'.");
            }
        }
        // Need to remove that thing from the stack...
        if (mCurrElem != sSharedRootElem) {
            // ... except if no checking is to be done, and we are at root already
            mCurrElem = mCurrElem.getParent();
        }
        doWriteEndElement(prefix, local);
    }

    public String getTopElemName() {
        return mCurrElem.getElementName();
    }

    /**
     * Method called to somehow find a prefix for given namespace; either
     * use an existing one, or generate a new one.
     *
     * @param oldPrefix Prefix caller originally suggested that did not match
     *    the namespace URI, if any; null if caller didn't suggest a prefix.
     * @param nsURI URI of namespace for which we need a prefix
     * @param defaultNsOk Whether default namespace prefix (empty String)
     *   is acceptable (can not be used for attributes)
     */
    protected String findOrCreatePrefix(String oldPrefix, String nsURI,
					boolean defaultNsOk)
        throws XMLStreamException
    {
        String prefix = mCurrElem.findPrefix(nsURI, defaultNsOk);
        if (prefix != null) {
            return prefix;
        }

        prefix = generatePrefix(oldPrefix, nsURI);
        mCurrElem.addPrefix(prefix, nsURI);
        doWriteNamespace(prefix, nsURI);
        return prefix;
    }

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
            checkNameValidity(localName, false);
        }

        if (mState == STATE_PROLOG) {
            mState = STATE_TREE;
        }
    }

    protected void doWriteNamespace(String prefix, String nsURI)
        throws XMLStreamException
    {
        try {
            mWriter.write(' ');
            mWriter.write(XMLConstants.XMLNS_ATTRIBUTE);
            if (prefix != null) {
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

    /*
    ////////////////////////////////////////////////////
    // Package methods for sub-classes to implement
    ////////////////////////////////////////////////////
     */

    public abstract void writeDefaultNamespace(String nsURI)
        throws XMLStreamException;
    public abstract void writeNamespace(String prefix, String nsURI)
        throws XMLStreamException;
    protected abstract String generatePrefix(String oldPrefix, String nsURI)
        throws XMLStreamException;

    public abstract void writeStartElement(StartElement elem)
        throws XMLStreamException;

    /**
     * Method called to close an open start element, when another
     * main-level element (not namespace declaration or attribute)
     * is being output; except for end element which is handled differently.
     *
     * @param emptyElem If true, the element being closed is an empty
     *   element; if false, a separate stand-alone start element.
     */
    public abstract void closeStartElement(boolean emptyElem)
        throws XMLStreamException;

    protected abstract void writeStartOrEmpty(String prefix, String localName, String nsURI,
					      boolean isEmpty)
        throws XMLStreamException;

    protected abstract void doWriteStartElement(String prefix, String localName)
        throws XMLStreamException;

    /**
     *<p>
     * Note: Caller has to do actual removal of the element from element
     * stack, before calling this method.
     */
    protected abstract void doWriteEndElement(String prefix, String localName)
        throws XMLStreamException;

    /*
    ////////////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////////////
     */

    private void doWriteAttr(String localName, String nsURI, String prefix,
                             String value)
        throws XMLStreamException
    {
        if (mCheckAttr) { // still need to ensure no duplicate attrs?
            mCurrElem.checkAttrWrite(nsURI, localName, value);
        }

        try {
            mWriter.write(' ');
            if (prefix != null) {
                mWriter.write(prefix);
                mWriter.write(':');
            }
            mWriter.write(localName);
            mWriter.write("=\"");
            XMLQuoter.outputDoubleQuotedAttr(mWriter, value);
            mWriter.write('"');
        } catch (IOException ioe) {
            throw new XMLStreamException(ioe);
        }
    }
}
