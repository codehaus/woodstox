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
        super(w, cfg);
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
        /* 21-Sep-2004, TSa: Shouldn't get called in namespace-repairing
         *    mode; see discussion in 'writeNamespace()' for details.
         */
	return;
	//throwOutputError("Should not call writeNamespace() for namespace-repairing writers");
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
	return;
	//throwOutputError("Should not call writeNamespace() for namespace-repairing writers");
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

	// No need to check that all was output, in repairing mode

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

    protected String generatePrefix(String oldPrefix, String nsURI)
        throws XMLStreamException
    {
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

        return mCurrElem.generatePrefix(mRootNsContext);
    }

    //void checkStartElement(String localName)

    protected void writeStartOrEmpty(String prefix, String localName, String nsURI,
                                   boolean isEmpty)
        throws XMLStreamException
    {
        checkStartElement(localName);
        mCurrElem = new OutputElement(mCurrElem, localName, mNsDecl, mCheckNS);

        // Need to clear ns declarations for next start/empty elems:
        mNsDecl = null;

	// Always have to check, in repairing mode:
	int status = mCurrElem.isPrefixValid(prefix, nsURI, mCheckNS, true);
	if (status != OutputElement.PREFIX_OK) {
	    /* Either wrong prefix (need to find the right one), or
	     * non-existing one...
	     */
	    if (status == OutputElement.PREFIX_MISBOUND) { // mismatch?
		prefix = mCurrElem.findPrefix(nsURI, true);
	    } else { // just not declared
		mCurrElem.addPrefix(prefix, nsURI);
	    }
	    
	    // Either way, we may have to create a new prefix?
	    if (prefix == null) {
		prefix = generatePrefix(null, nsURI);
		mCurrElem.addPrefix(prefix, nsURI);
	    }
	}

        mCurrElem.setPrefix(prefix);
        doWriteStartElement(prefix, localName);
    }

    protected void doWriteStartElement(String prefix, String localName)
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
	    mCurrElem.outputDeclaredNamespaces(mWriter);
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
    protected void doWriteEndElement(String prefix, String localName)
        throws XMLStreamException
    {
        if (mStartElementOpen) {
            /* Can't/shouldn't call closeStartElement, but need to do same
             * processing. Thus, this is almost identical to closeStartElement:
             */
            mStartElementOpen = false;
            
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
