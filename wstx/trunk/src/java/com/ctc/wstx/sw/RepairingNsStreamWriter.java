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
import com.ctc.wstx.sr.AttributeCollector;
import com.ctc.wstx.sr.InputElementStack;
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
                /* 06-Jan-2005, TSa: Actually, better not try to force
                 *   writing of the defualt namespace, since (a) it may
                 *   already have been output, and (b) if it wasn't might
                 *   make element output itself invalid.
                 */
                //doWriteDefaultNamespace(ns.getNamespaceURI());
            } else {
                doWriteNamespace(prefix, ns.getNamespaceURI());
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
        String prefix = findOrCreatePrefix(nsURI, true);
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

    /**
     * Element copier method implementation suitable for use with
     * namespace-aware writers in repairing mode.
     * The trickiest thing is having to properly
     * order calls to <code>setPrefix</code>, <code>writeNamespace</code>
     * and <code>writeStartElement</code>; the order writers expect is
     * bit different from the order in which element information is
     * passed in.
     */
    public final void copyStartElement(InputElementStack elemStack,
                                       AttributeCollector attrCollector)
        throws IOException, XMLStreamException
    {
        /* In case of repairing stream writer, we can actually just
         * go ahead and first output the element: stream writer should
         * be able to resolve namespace mapping for the element
         * automatically, as necessary.
         */
        writeStartElement(elemStack.getPrefix(),
                          elemStack.getLocalName(),
                          elemStack.getNsURI());
        
        // Any namespace declarations/bindings?
        int nsCount = elemStack.getCurrentNsCount();
        
        /* Since repairing writer will deal with namespace bindings
         * and declarations automatically, we wouldn't necessarily
         * have to do anything here... but it's probably a good idea
         * to suggest proper mapping now
         * (call has to be done _after_ outputting the element!)
         */
        /* 06-Feb-2005, TSa: No, better just let actual attribute/element
         *    writes bind and output namespace declarations.
         */
        /*
        if (nsCount > 0) {
            for (int i = 0; i < nsCount; ++i) {
                validatePrefix(elemStack.getLocalNsPrefix(i),
                               elemStack.getLocalNsURI(i),
                               true);
            }
        }
        */

        /* note: in repairing mode, it is NOT allowed to explicitly
         * (try to) write namespace declarations...
         */
        
        /* And then let's just output attributes, if any:
             */
        // Let's only output explicit attributes?
        // !!! Should it be configurable?
        AttributeCollector ac = mAttrCollector;
        int attrCount = ac.getSpecifiedCount();

        /* Unlike in non-ns and simple-ns modes, we can not simply literally
         * copy the attributes here. It is possible that some namespace
         * prefixes have been remapped... so need to be bit more careful.
         */
        for (int i = 0; i < attrCount; ++i) {
            /* First; need to make sure that the prefix-to-ns mapping
             * attribute has is valid... and can not output anything
             * before that's done (since remapping will output a namespace
             * declaration!)
             */
            String uri = attrCollector.getNsURI(i);
            String prefix = attrCollector.getPrefix(i);

            /* With attributes, missing/empty prefix always means 'no
             * namespace', can take a shortcut:
             */
            if (prefix == null) {
                ;
            } else if (prefix.length() == 0) { // should never happen?
                prefix = null;
            } else {
                // Does have a namespace, is it valid?
                // (May need to re-map it; false -> can not use def ns)
                prefix = validatePrefix(prefix, uri, false);
            }
            mWriter.write(' ');
            if (prefix != null) {
                mWriter.write(prefix);
                mWriter.write(':');
            }
            mWriter.write(attrCollector.getLocalName(i));
            mWriter.write('=');
            mWriter.write(DEFAULT_QUOTE_CHAR);
            attrCollector.writeValue(i, mAttrValueWriter);
            mWriter.write(DEFAULT_QUOTE_CHAR);
        }
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
     * @param isElement True, when the prefix is for an element, false if
     *   for attribute. Elements can use the default namespace, and the
     *   new namespace bindings need not be output right away; attributes
     *   can not use default namespaces, and the explicit declarations
     *   need to be output immediately.
     */
    protected final String findOrCreatePrefix(String nsURI, boolean isElement)
        throws XMLStreamException
    {
        /* 06-Feb-2005, TSa: Special care needs to be taken for the
         *   "empty" (or missing) namespace:
         */
        if (nsURI.length() == 0) {
            if (isElement) {
                /* Since only the default namespace can be mapped to
                 * the empty URI, the default namespace has to either
                 * still point to the empty URI, or re-mapped to
                 * point to it:
                 */
                String currURL = mCurrElem.getDefaultNsUri();
                if (currURL != null && currURL.length() > 0) {
                    // Need to clear it out...
                    mCurrElem.setDefaultNs(currURL);
                } // otherwise it was already "empty" namespace
            } else {
                /* Attributes never use the default namespace; missing
                 * prefix always leads to the empty ns... so nothing
                 * special is needed here.
                 */
                ;
            }
            // Either way, no prefix can be used:
            return null;
        }
        String prefix = mCurrElem.findPrefix(nsURI, isElement);
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
                prefix = mCurrElem.generatePrefix(mRootNsContext, mAutomaticNsPrefix);
            }
        } else {
            prefix = mCurrElem.generatePrefix(mRootNsContext, mAutomaticNsPrefix);
        }

        mCurrElem.addPrefix(prefix, nsURI);
        if (!isElement) {
            doWriteNamespace(prefix, nsURI);
        }
        return prefix;
    }

    /**
     * Method called to make sure that passed prefix/URI combination
     * is valid for current element; and if not, to create alternate
     * binding and return its prefix
     */
    private final String validatePrefix(String prefix, String nsURI,
                                        boolean isElement)
        throws XMLStreamException
    {
        /* 06-Feb-2005, TSa: Special care needs to be taken for the
         *   "empty" (or missing) namespace:
         *   (see comments from findOrCreatePrefix())
         */
        if (nsURI.length() == 0) {
            if (isElement) {
                String currURL = mCurrElem.getDefaultNsUri();
                if (currURL != null && currURL.length() > 0) {
                    mCurrElem.setDefaultNs(currURL);
                }
            } // attributes are fine as is
            // Either way, no prefix can be used:
            return null;
        }
        
        int status = mCurrElem.isPrefixValid(prefix, nsURI, mCheckNS,
                                             isElement);
        if (status == OutputElement.PREFIX_OK) {
            return prefix;
        }

        
        // Not bound? Easy enough, can just add such mapping:
        if (status == OutputElement.PREFIX_UNBOUND) {
            mCurrElem.addPrefix(prefix, nsURI);
            if (!isElement) {
                doWriteNamespace(prefix, nsURI);
            }
        } else {
            // mis-bound? Need to find better one
            // First, do we have a mapping for URI?
            prefix = getPrefix(nsURI);
            if (prefix == null) { // nope, need to generate
                prefix = mCurrElem.generatePrefix(mRootNsContext, mAutomaticNsPrefix);
                mCurrElem.addPrefix(prefix, nsURI);
                if (!isElement) {
                    doWriteNamespace(prefix, nsURI);
                }
            }
        }
        return prefix;
    }
}
