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

package com.ctc.wstx.stax;

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

import com.ctc.wstx.stax.cfg.ErrorConsts;
import com.ctc.wstx.stax.cfg.WriterConfig;
import com.ctc.wstx.stax.ns.OutputElement;
import com.ctc.wstx.stax.stream.BaseStreamWriter;
import com.ctc.wstx.util.XMLQuoter;

/**
 * Implementation of {@link XMLStreamWriter}, that is otherwise fairly
 * basic, with optional namespace/prefix allocation, but can also optionally
 * do reasonable validation of well-formedness of output.
 */
public class WstxNsStreamWriter
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

    final boolean mAutomaticNS;
    final boolean mCheckNS;

    final String mAutomaticNsPrefix;

    /*
    ////////////////////////////////////////////////////
    // State information
    ////////////////////////////////////////////////////
     */

    final static OutputElement sSharedRootElem = OutputElement.getRootInstance();

    /**
     * Currently active output element; contains information necessary
     * for handling attributes and namespaces
     */
    OutputElement mCurrElem = sSharedRootElem;

    /**
     * Container in which namespace declarations are stored, before the
     * start element has been output. Will be used for passing namespace
     * declaration information to the start element.
     */
    OutputElement.Declarations mNsDecl = null;

    /**
     * Optional "root" namespace context that application can set. If so,
     * it can be used to lookup namespace/prefix mappings
     */
    NamespaceContext mRootNsContext = null;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle (ctors)
    ////////////////////////////////////////////////////
     */

    public WstxNsStreamWriter(Writer w, WriterConfig cfg)
    {
        super(w, cfg);
        int flags = cfg.getConfigFlags();
        mAutomaticNS = (flags & CFG_AUTOMATIC_NS) != 0;
        mCheckNS = (flags & CFG_VALIDATE_NS) != 0;
        mAutomaticNsPrefix = cfg.getAutomaticNsPrefix();
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

    public void writeDefaultNamespace(String nsURI)
        throws XMLStreamException
    {
        /* 21-Sep-2004, TSa: Shouldn't get called in namespace-repairing
         *    mode; see discussion in 'writeNamespace()' for details.
         */
        if (mAutomaticNS) {
            return;
            //throwOutputError("Should not call writeNamespace() for namespace-repairing writers");
        }

        // No need to set mAnyOutput, nor close the element
        if (!mStartElementOpen) {
            throw new IllegalStateException("Trying to write a namespace declaration when there is no open start element.");
        }

        if (mCheckNS) { // Was it declared the same way?
            mCurrElem.checkDefaultNsWrite(nsURI);
        }

        try {
            mWriter.write(' ');
            mWriter.write(XMLConstants.XMLNS_ATTRIBUTE);
            mWriter.write("=\"");
            mWriter.write(nsURI);
            mWriter.write('"');
        } catch (IOException ioe) {
            throw new XMLStreamException(ioe);
        }
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
        // 21-Sep-2004, TSa: Shouldn't be needed any more...
        /*
        if (!nsOk) {
            doWriteNamespace(prefix, nsURI);
        }
        */

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

    public void writeNamespace(String prefix, String nsURI)
        throws XMLStreamException
    {
        /* 21-Sep-2004, TSa: Should not call this in "namespace repairing"
         *    mode. However, if it is called, what should be done? There
         *    are multiple possibilities; like:
         *   (a) Throw an exception
         *   (b) Ignore the call
         *   (c) Check potential validity; ignore if it matched a declaration,
         *     throw an exception if it didn't.
         *
         *  For now, let's do (b), since event writer will call this method...
         */
        if (mAutomaticNS) {
            return;
            //throwOutputError("Should not call writeNamespace() for namespace-repairing writers");
        }

        if (prefix == null || prefix.length() == 0
            || prefix.equals(XMLConstants.XMLNS_ATTRIBUTE)) {
            writeDefaultNamespace(nsURI);
            return;
        }

        // No need to set mAnyOutput, and shouldn't close the element.
        // But element needs to be open, obviously.
        if (!mStartElementOpen) {
            throw new IllegalStateException("Trying to write a namespace declaration when there is no open start element.");
        }
        
        if (mCheckNS) { // Was it declared the same way?
            mCurrElem.checkNsWrite(prefix, nsURI);
        }
        
        doWriteNamespace(prefix, nsURI);
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
        // 21-Sep-2004, TSa: Shouldn't be needed any more...
        /*
        if (!nsOk) {
            doWriteNamespace(prefix, nsURI);
        }
        */

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
    // Package methods:
    ////////////////////////////////////////////////////
     */

    public void writeStartElement(StartElement elem)
        throws XMLStreamException
    {
        QName name = elem.getName();
        Iterator it = elem.getNamespaces();
        
        while (it.hasNext()) {
            Namespace ns = (Namespace) it.next();
            // First need to 'declare' namespace:
            String prefix = ns.getPrefix();
            if (prefix == null || prefix.length() == 0) {
                setDefaultNamespace(ns.getNamespaceURI());
            } else {
                setPrefix(prefix, ns.getNamespaceURI());
            }
        }

        /* Outputting element itself is fairly easy. The main question
         * is whether namespaces match. Let's use simple heuristics:
         * if writer is to do automatic prefix matching, let's only
         * pass explicit prefix (not default one); otherwise we'll
         * pass all parameters as is.
         */
        /* Quick check first though: if URI part of QName is null, it's
         * assumed element will just use whatever is current default
         * namespace....
         */
        String nsURI = name.getNamespaceURI();
        if (nsURI == null) {
            writeStartElement(name.getLocalPart());
        }
        
        String prefix = name.getPrefix();
        if (mAutomaticNS && 
            (prefix == null || prefix.length() == 0)) {
            writeStartElement(nsURI, name.getLocalPart());
                
        } else {
            writeStartElement(prefix, name.getLocalPart(), nsURI);
        }
    
        // And now we need to output namespaces (including default), if any:
        it = elem.getNamespaces();
        while (it.hasNext()) {
            Namespace ns = (Namespace) it.next();
            prefix = ns.getPrefix();
            if (prefix == null || prefix.length() == 0) {
                writeDefaultNamespace(ns.getNamespaceURI());
            } else {
                writeNamespace(prefix, ns.getNamespaceURI());
            }
        }
    

        // And finally, need to output attributes as well:
        
        it = elem.getAttributes();
        while (it.hasNext()) {
            Attribute attr = (Attribute) it.next();
            name = attr.getName();
            writeAttribute(name.getLocalPart(), attr.getValue());
        }
    }

    /**
     * Method called by {@link WstxEventWriter} (instead of the version
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

    /**
     * Method called to close an open start element, when another
     * main-level element (not namespace declaration or attribute)
     * is being output; except for end element which is handled differently.
     *
     * @param emptyElem If true, the element being closed is an empty
     *   element; if false, a separate stand-alone start element.
     */
    public void closeStartElement(boolean emptyElem)
        throws XMLStreamException
    {
        mStartElementOpen = false;
        // Ok... time to verify namespaces were written ok?
        if (mCheckNS && !mAutomaticNS) {
            mCurrElem.checkAllNsWrittenOk();
        }

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

    public String getTopElemName() {
        return mCurrElem.getElementName();
    }

    /*
    ////////////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////////////
     */

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
    private String findOrCreatePrefix(String oldPrefix, String nsURI,
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

    private String generatePrefix(String oldPrefix, String nsURI)
        throws XMLStreamException
    {
        // If not, how about in root namespace context?
        /* ??? 10-May-2004, TSa: Is it ok to make use of root context, even
         *   if 'automatic namespace' feature is not set? But if not, what
         *   good would root namespace be?
         */
        if (mRootNsContext != null) {
            // If so, it has to be declared and output:
            String prefix = mRootNsContext.getPrefix(nsURI);
            /* Note: root namespace context can NOT define default
             * namespace; it would be tricky to get to work right.
             */
            if (prefix != null && prefix.length() > 0) {
                return prefix;
            }
        }

        // Can we generate it?
        if (!mAutomaticNS) {
            if (oldPrefix != null) {
                throw new XMLStreamException("Prefix '"+oldPrefix+"' did not match (or wasn't declared for) namespace '"
                                             +nsURI+"', can not generate a new prefix since feature '"
                                             +XMLOutputFactory.IS_REPAIRING_NAMESPACES
                                             +"' not enabled.");
            }
            throw new XMLStreamException("Can not create automatic namespace for namespace URI '"
                                         +nsURI+"', feature '"
                                         +XMLOutputFactory.IS_REPAIRING_NAMESPACES
                                         +"' not enabled.");
        }
        return mCurrElem.generatePrefix(mRootNsContext);
    }

    private void checkStartElement(String localName)
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

    private void writeStartOrEmpty(String prefix, String localName, String nsURI,
                                   boolean isEmpty)
        throws XMLStreamException
    {
        checkStartElement(localName);
        mCurrElem = new OutputElement(mCurrElem, localName, mNsDecl, mCheckNS);

        // Need to clear ns declarations for next start/empty elems:
        mNsDecl = null;

        /* 21-Sep-2004, TSa: Shouldn't need this -- automatic namespace
         *   repairing should take care of this, in doWriteStartElement.
         */
        // Ok, need to check validity of the prefix?
        //boolean outputNS = false;

        if (mCheckNS) {
            int status = mCurrElem.isPrefixValid(prefix, nsURI, mCheckNS, true);
            if (status != OutputElement.PREFIX_OK) {
System.err.println("Wrong prefix '"+prefix+"' -> "+status);
                /* Either wrong prefix (need to find the right one), or
                 * non-existing one...
                 */
                if (status == OutputElement.PREFIX_MISBOUND) { // mismatch?
                    prefix = mCurrElem.findPrefix(nsURI, true);
                } else { // just not declared
                    if (mAutomaticNS) { // Let's just add it automatically
                        mCurrElem.addPrefix(prefix, nsURI);
                    } else { // error
                        throwOutputError("Undeclared prefix '"+prefix+"' for element <"+prefix+":"+localName+">");
                    }
                }

                // Either way, we may have to create a new prefix?
                if (prefix == null) {
                    //outputNS = true;
                    prefix = generatePrefix(null, nsURI);
                    mCurrElem.addPrefix(prefix, nsURI);
                }
            }
        }

        mCurrElem.setPrefix(prefix);
        doWriteStartElement(prefix, localName);
        /*
        if (outputNS) {
            doWriteNamespace(prefix, nsURI);
        }
        */
    }

    private void doWriteStartElement(String prefix, String localName)
        throws XMLStreamException
    {
        mAnyOutput = true;
        mStartElementOpen = true;
        try {
            mWriter.write('<');
            if (prefix != null && prefix.length() > 0) {
                mWriter.write(prefix);
                mWriter.write(':');
            }
            mWriter.write(localName);

            /* 21-Sep-2004, TSa: In ns-repairing mode we can/need to
             *    also automatically output all declared namespaces
             *    (whether they are automatic or not)
             */
            if (mAutomaticNS) {
                mCurrElem.outputDeclaredNamespaces(mWriter);
            }
        } catch (IOException ioe) {
            throw new XMLStreamException(ioe);
        }
    }

    private void doWriteNamespace(String prefix, String nsURI)
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

    /**
     *<p>
     * Note: Caller has to do actual removal of the element from element
     * stack, before calling this method.
     */
    private void doWriteEndElement(String prefix, String localName)
        throws XMLStreamException
    {
        if (mStartElementOpen) {
            /* Can't/shouldn't call closeStartElement, but need to do same
             * processing. Thus, this is almost identical to closeStartElement:
             */
            mStartElementOpen = false;
            if (mCheckNS && !mAutomaticNS) {
                mCurrElem.checkAllNsWrittenOk();
            }
            
            try {
                // We could write an empty element, implicitly?
                if (!mEmptyElement && mCfgOutputEmptyElems) {
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
}
