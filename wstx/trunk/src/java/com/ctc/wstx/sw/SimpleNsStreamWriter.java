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

import com.ctc.wstx.api.WriterConfig;
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
        super(w, cfg, false);
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

    //public void writeAttribute(String localName, String value)

    public void writeAttribute(String nsURI, String localName, String value)
        throws XMLStreamException
    {
        // No need to set mAnyOutput, nor close the element
        if (!mStartElementOpen) {
            throw new IllegalStateException("Trying to write an attribute when there is no open start element.");
        }
        String prefix = findPrefix(nsURI, false);
        doWriteAttr(localName, nsURI, prefix, value);
    }

    public void writeAttribute(String prefix, String nsURI,
                               String localName, String value)
        throws XMLStreamException
    {
        if (!mStartElementOpen) {
            throw new IllegalStateException("Trying to write an attribute when there is no open start element.");
        }

        // Want to verify namespace consistency?
        if (mCheckNS) {
            checkNsDecl(prefix, nsURI);
        }
        doWriteAttr(localName, nsURI, prefix, value);
    }

    //public void writeEmptyElement(String localName) throws XMLStreamException
    //public void writeEmptyElement(String nsURI, String localName) throws XMLStreamException
    //public void writeEmptyElement(String prefix, String localName, String nsURI) throws XMLStreamException

    //public void writeEndElement() throws XMLStreamException

    public void writeDefaultNamespace(String nsURI)
        throws XMLStreamException
    {
        if (!mStartElementOpen) {
            throw new IllegalStateException(ERR_NSDECL_WRONG_STATE);
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
            throw new IllegalStateException(ERR_NSDECL_WRONG_STATE);
        }
        
        if (mCheckNS) { // Was it declared the same way?
            mCurrElem.checkNsWrite(mRootNsContext, prefix, nsURI);
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
        } else {
            String prefix = name.getPrefix();
            writeStartElement(prefix, name.getLocalPart(), nsURI);
        }

        // And now we need to output namespaces (including default), if any:
        it = elem.getNamespaces();
        while (it.hasNext()) {
            Namespace ns = (Namespace) it.next();
            String prefix = ns.getPrefix();
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

    //public String getTopElemName()

    protected void writeStartOrEmpty(String localName, String nsURI)
        throws XMLStreamException
    {
        checkStartElement(localName);

        mCurrElem = new OutputElement(mCurrElem, localName, mNsDecl, mCheckNS);

        // Need a prefix...
        String prefix = findPrefix(nsURI, true);
        mCurrElem.setPrefix(prefix);
        doWriteStartElement(prefix, localName);

        // Need to clear namespace declaration info now for next start elem:
        mNsDecl = null;
    }

    protected void writeStartOrEmpty(String prefix, String localName, String nsURI)
        throws XMLStreamException
    {
        checkStartElement(localName);
        mCurrElem = new OutputElement(mCurrElem, localName, mNsDecl, mCheckNS);

        // Need to clear ns declarations for next start/empty elems:
        mNsDecl = null;

        // Ok, need to check validity of the prefix?
        if (mCheckNS) {
            checkNsDecl(prefix, nsURI);
        }

        mCurrElem.setPrefix(prefix);
        doWriteStartElement(prefix, localName);
    }

    /*
    ////////////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////////////
     */

    private final String findPrefix(String nsURI, boolean canUseDefault)
        throws XMLStreamException
    {
        String prefix = mCurrElem.findPrefix(nsURI, canUseDefault);
        if (prefix == null) {
            /* 11-Nov-2004, TSa: May also be specified by the root namespace
             *   context
             */
            if (mRootNsContext != null) {
                prefix = mRootNsContext.getPrefix(nsURI);
                /* If we got it, better call setPrefix() now...
                 * (note: we do not use default namespace root may define;
                 * it'd be hard to make that work ok)
                 */
                if (prefix != null && prefix.length() > 0) {
                    mCurrElem.addPrefix(prefix, nsURI);
                }
            }
            throw new XMLStreamException("Unbound namespace prefix '"+prefix+"'");
        }
        return prefix;
    }

    private final void checkNsDecl(String prefix, String nsURI)
        throws XMLStreamException
    {
        int status = mCurrElem.isPrefixValid(prefix, nsURI, true, false);
        if (status != OutputElement.PREFIX_OK) {
            if (status == OutputElement.PREFIX_UNBOUND) {
                throw new XMLStreamException("Unbound namespace prefix '"+prefix+"'");
            }
            String actURI = mCurrElem.getNamespaceURI(prefix);
            throw new XMLStreamException("Misbound namespace prefix '"+prefix+"': was declared as '"+actURI+"', trying to use it as '"+nsURI+"'");
        }
    }
}
