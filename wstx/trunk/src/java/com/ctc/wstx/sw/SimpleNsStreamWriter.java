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
 * Namespace-aware implementation of {@link XMLStreamWriter}, that does
 * not do namespace repairing, ie doesn't try to resolve possible
 * conflicts between prefixes and namespace URIs, or automatically
 * create namespace bindings.
 */
public class SimpleNsStreamWriter
    extends BaseNsStreamWriter
{
    /*
    ////////////////////////////////////////////////////
    // Life-cycle (ctors)
    ////////////////////////////////////////////////////
     */

    public SimpleNsStreamWriter(Writer w, WriterConfig cfg)
    {
        super(w, cfg);
    }

    /*
    ////////////////////////////////////////////////////
    // XMLStreamWriter API
    ////////////////////////////////////////////////////
     */

    //public NamespaceContext getNamespaceContext()
    //public void setNamespaceContext(NamespaceContext context)
    //public String getPrefix(String uri)
    //public void setPrefix(String prefix, String uri)
    //public void setDefaultNamespace(String uri)
    //public void writeDefaultNamespace(String nsURI)

    //public void writeAttribute(String localName, String value)
    //public void writeAttribute(String nsURI, String localName, String value)
    //public void writeAttribute(String prefix, String nsURI, String localName, String value)

    //public void writeEmptyElement(String localName) throws XMLStreamException
    //public void writeEmptyElement(String nsURI, String localName) throws XMLStreamException
    //public void writeEmptyElement(String prefix, String localName, String nsURI) throws XMLStreamException

    //public void writeEndElement() throws XMLStreamException

    public void writeDefaultNamespace(String nsURI)
        throws XMLStreamException
    {
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

    public void writeNamespace(String prefix, String nsURI)
        throws XMLStreamException
    {
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

    //public void writeEndElement(QName name) throws XMLStreamException

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

    //public String getTopElemName()

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

    //private void checkStartElement(String localName)

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

    //private void doWriteNamespace(String prefix, String nsURI)

    //private void doWriteAttr(String localName, String nsURI, String prefix, String value)

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
