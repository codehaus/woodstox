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
import com.ctc.wstx.sr.StreamReaderImpl;
import com.ctc.wstx.util.DefaultXmlSymbolTable;
import com.ctc.wstx.util.XMLQuoter;

/**
 * Namespace-aware implementation of {@link XMLStreamWriter}, that does
 * namespace repairing, ie resolves possible conflicts between prefixes
 * (add new bindings as necessary), as well as automatically creates
 * namespace declarations as necessary.
 */
public class RepairingNsStreamWriter
    extends BaseNsStreamWriter
{
    /*
    ////////////////////////////////////////////////////
    // Configuration (options, features)
    ////////////////////////////////////////////////////
     */

    // // // Additional specific config flags base class doesn't have

    final String mAutomaticNsPrefix;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle (ctors)
    ////////////////////////////////////////////////////
     */

    public RepairingNsStreamWriter(Writer w, WriterConfig cfg)
    {
        super(w, cfg, true);
        mAutomaticNsPrefix = cfg.getAutomaticNsPrefix();
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
        // Need a prefix...
        String prefix = findOrCreatePrefix(nsURI, false);
        doWriteAttr(localName, nsURI, prefix, value);
    }

    public void writeAttribute(String prefix, String nsURI,
                               String localName, String value)
        throws XMLStreamException
    {
        if (!mStartElementOpen) {
            throw new IllegalStateException("Trying to write an attribute when there is no open start element.");
        }

        // In repairing mode, better ensure validity:
        prefix = validatePrefix(prefix, nsURI, false);
        doWriteAttr(localName, nsURI, prefix, value);
    }

    public void writeDefaultNamespace(String nsURI)
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
         *  For now, let's do (b) or (c), depending on whether we are to do
         * more thorough namespace output verification
         */
        // ... still, shouldn't get called in the wrong time, ever:
        if (!mStartElementOpen) {
            throw new IllegalStateException(ERR_NSDECL_WRONG_STATE);
        }
        if (mCheckNS) {
            mCurrElem.checkDefaultNsWrite(nsURI);
        }
        //throwOutputError("Should not call writeNamespace() for namespace-repairing writers");
    }

    //public void writeEmptyElement(String localName) throws XMLStreamException

    public void writeNamespace(String prefix, String nsURI)
        throws XMLStreamException
    {
        /* 21-Sep-2004, TSa: Shouldn't get called in namespace-repairing
         *    mode; see discussion in 'writeDefaultNamespace()' for details.
         */
        if (!mStartElementOpen) {
            throw new IllegalStateException(ERR_NSDECL_WRONG_STATE);
        }
        if (mCheckNS) {
            mCurrElem.checkNsWrite(mRootNsContext, prefix, nsURI);
        }
        //throwOutputError("Should not call writeNamespace() for namespace-repairing writers");
    }

    /*
    ////////////////////////////////////////////////////
    // Package methods:
    ////////////////////////////////////////////////////
     */

    ElementCopier createElementCopier(XMLStreamReader2 sr)
    {
        /* !!! 21-Feb-2005, TSa: Should probably also work with non-Woodstox
         *  stream readers? If so, should first test if the interface is
         *  implemented, and if not, use a fallback method of using
         *  accessors...
         *
         * For now, we'll only be able to use Woodstox stream readers.
         */
        return new CopierImpl((StreamReaderImpl) sr, this);
    }

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
	    if (prefix == null || prefix.length() == 0) {
		writeStartElement(nsURI, name.getLocalPart());
	    }
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

        // Need a prefix....
        String prefix = findOrCreatePrefix(nsURI, false);
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

        // In repairing mode, better ensure validity:
        prefix = validatePrefix(prefix, nsURI, true);
        mCurrElem.setPrefix(prefix);
        doWriteStartElement(prefix, localName);
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
     * @param nsURI URI of namespace for which we need a prefix
     * @param defaultNsOk Whether default namespace prefix (empty String)
     *   is acceptable (can not be used for attributes)
     */
    protected final String findOrCreatePrefix(String nsURI, boolean defaultNsOk)
        throws XMLStreamException
    {
        String prefix = mCurrElem.findPrefix(nsURI, defaultNsOk);
        if (prefix != null) {
            return prefix;
        }

        /* Ok; need to generate a new mapping. Let's first see if the
         * root context has suggested mapping; if so, let's use it:
         */
        if (mRootNsContext != null) {
            prefix = mRootNsContext.getPrefix(nsURI);
            /* Note: root namespace context can NOT define default
             * namespace; it would be tricky to get to work right.
             */
            if (prefix == null || prefix.length() == 0) {
                prefix = mCurrElem.generatePrefix(mRootNsContext);
            }
        } else {
            prefix = mCurrElem.generatePrefix(mRootNsContext);
        }

        mCurrElem.addPrefix(prefix, nsURI);
        doWriteNamespace(prefix, nsURI);
        return prefix;
    }

    /**
     * Method called to make sure that passed prefix/URI combination
     * is valid for current element; and if not, to create alternate
     * binding and return its prefix
     */
    private final String validatePrefix(String prefix, String nsURI,
                                        boolean canUseDefault)
        throws XMLStreamException
    {
        int status = mCurrElem.isPrefixValid(prefix, nsURI, mCheckNS,
                                             canUseDefault);
        if (status != OutputElement.PREFIX_OK) {
            // Not bound? Easy enough, can just add such mapping:
            if (status == OutputElement.PREFIX_UNBOUND) {
                mCurrElem.addPrefix(prefix, nsURI);
                doWriteNamespace(prefix, nsURI);
            } else {
                // mis-bound? Need to find better one
                // First, do we have a mapping for URI?
                prefix = getPrefix(nsURI);
                if (prefix == null) { // nope, need to generate
                    prefix = mCurrElem.generatePrefix(mRootNsContext);
                    mCurrElem.addPrefix(prefix, nsURI);
                    doWriteNamespace(prefix, nsURI);
                }
            }
        }
        return prefix;
    }

    /*
    ////////////////////////////////////////////////////
    // Helper classes:
    ////////////////////////////////////////////////////
     */

    /**
     * Element copier implementation suitable for use with
     * namespace-aware writers in repairing mode.
     * The trickiest thing is having to properly
     * order calls to <code>setPrefix</code>, <code>writeNamespace</code>
     * and <code>writeStartElement</code>; the order writers expect is
     * bit different from the order in which element information is
     * passed in.
     */
    final static class CopierImpl
        extends ElementCopier
    {
        protected final StreamReaderImpl mReader;
        protected final RepairingNsStreamWriter mWriter;

        CopierImpl(StreamReaderImpl sr, RepairingNsStreamWriter sw)
        {
            super();
            mReader = sr;
            mWriter = sw;
        }

        public final void copyElement()
            throws XMLStreamException
        {
            mReader.iterateStartElement(this);
        }

        public void iterateElement(String prefix, String localName,
                                   String nsURI, boolean isEmpty)
            throws XMLStreamException
        {
            /* In case of repairing stream writer, we can actually just
             * go ahead and output the element: stream writer should
             * be able to resolve namespace mapping for the element
             * automatically, as necessary.
             */
            mWriter.writeStartElement(prefix, localName, nsURI);
        }
        
        public void iterateNamespace(String prefix, String nsURI)
            throws XMLStreamException
        {
            /* Since repairing writer will deal with namespace bindings
             * and declarations automatically, we wouldn't necessarily
             * have to do anything here... but it's probably a good idea
             * to suggest proper mapping now:
             */
            mWriter.validatePrefix(prefix, nsURI, true);
        }
        
        public void iterateAttribute(String prefix, String localName,
                                     String nsURI, boolean isSpecified,
                                     String value)
            throws XMLStreamException
        {
            // Let's only output explicit attributes?
            // !!! Should it be configurable?
            if (isSpecified) {
                mWriter.writeAttribute(prefix, nsURI, localName, value);
            }
        }
    }
}
