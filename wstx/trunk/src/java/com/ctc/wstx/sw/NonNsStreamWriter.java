/* Woodstox XML processor
 *
 * Copyright (c) 2004 Tatu Saloranta, tatu.saloranta@iki.fi
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

package com.ctc.wstx.sw;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.TreeMap;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

import org.codehaus.stax2.XMLStreamReader2;

import com.ctc.wstx.api.WriterConfig;
import com.ctc.wstx.sr.AttributeCollector;
import com.ctc.wstx.sr.InputElementStack;
import com.ctc.wstx.sr.StreamReaderImpl;
import com.ctc.wstx.util.EmptyNamespaceContext;
import com.ctc.wstx.util.StringVector;

/**
 * Implementation of {@link XMLStreamWriter} used when namespace support
 * is not enabled. This means that only local names are used for elements
 * and attributes; and if rudimentary namespace declarations need to be
 * output, they are output using attribute writing methods.
 */
public class NonNsStreamWriter
    extends BaseStreamWriter
{
    /*
    ////////////////////////////////////////////////////
    // Configuration (options, features)
    ////////////////////////////////////////////////////
     */

    // // // Additional specific config flags base class doesn't have

    /*
    ////////////////////////////////////////////////////
    // State information
    ////////////////////////////////////////////////////
     */

    /**
     * Stack of currently open start elements; only local names
     * are included.
     */
    final StringVector mElements;

    /**
     * Container for attribute names for current element; used only
     * if uniqueness of attribute names is to be enforced.
     *<p>
     * TreeMap is used mostly because clearing it up is faster than
     * clearing up HashMap or HashSet, and the only access is done by
     * adding entries and see if an value was already set.
     */
    TreeMap mAttrNames;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle (ctors)
    ////////////////////////////////////////////////////
     */

    public NonNsStreamWriter(Writer w, WriterConfig cfg)
    {
        super(w, cfg);
        mElements = new StringVector(32);
    }

    /*
    ////////////////////////////////////////////////////
    // XMLStreamWriter API
    ////////////////////////////////////////////////////
     */

    public NamespaceContext getNamespaceContext() {
        return EmptyNamespaceContext.getInstance();
    }

    public String getPrefix(String uri) {
        return null;
    }

    public void setDefaultNamespace(String uri)
        throws XMLStreamException
    {
        throw new IllegalArgumentException("Can not set default namespace for non-namespace writer.");
    }

    public void setNamespaceContext(NamespaceContext context)
    {
        throw new IllegalArgumentException("Can not set NamespaceContext for non-namespace writer.");
    }

    public void setPrefix(String prefix, String uri)
        throws XMLStreamException
    {
        throw new IllegalArgumentException("Can not set namespace prefix for non-namespace writer.");
    }

    public void writeAttribute(String localName, String value)
        throws XMLStreamException
    {
        // No need to set mAnyOutput, nor close the element
        if (!mStartElementOpen) {
            throw new XMLStreamException("Trying to write an attribute when there is no open start element.");

        }

        // May need to check uniqueness?
        if (mCheckNames) {
            checkNameValidity(localName, true);
        }
        if (mCheckAttr) {
            if (mAttrNames == null) {
                mAttrNames = new TreeMap();
                mAttrNames.put(localName, value);
            } else {
                Object old = mAttrNames.put(localName, value);
                if (old != null) {
                    throw new IllegalArgumentException("Trying to write attribute '"+localName+"' twice (first value '"+old+"'; second '"+value+"').");
                }
            }
        }
        
        try {
            mWriter.write(' ');
            mWriter.write(localName);
            mWriter.write("=\"");
            mAttrValueWriter.write(value);
            mWriter.write('"');
        } catch (IOException ioe) {
            throw new XMLStreamException(ioe);
        }
    }

    public void writeAttribute(String nsURI, String localName, String value)
        throws XMLStreamException
    {
        writeAttribute(localName, value);
    }

    public void writeAttribute(String prefix, String nsURI,
                               String localName, String value)
        throws XMLStreamException
    {
        writeAttribute(localName, value);
    }

    public void writeDefaultNamespace(String nsURI)
        throws XMLStreamException
    {
        throw new IllegalArgumentException("Can not set write namespaces with non-namespace writer.");
    }

    public void writeEmptyElement(String localName)
        throws XMLStreamException
    {
        doWriteStartElement(localName);
        mEmptyElement = true;
    }

    public void writeEmptyElement(String nsURI, String localName)
        throws XMLStreamException
    {
        writeEmptyElement(localName);
    }

    public void writeEmptyElement(String prefix, String localName, String nsURI)
        throws XMLStreamException
    {
        writeEmptyElement(localName);
    }

    public void writeEndElement()
        throws XMLStreamException
    {
        if (mState != STATE_TREE) {
            throw new XMLStreamException("No open start element, when calling writeEndElement.");
        }
        doWriteEndElement(mElements.removeLast(), mCfgOutputEmptyElems);
    }

    public void writeNamespace(String prefix, String nsURI)
        throws XMLStreamException
    {
        throw new IllegalArgumentException("Can not set write namespaces with non-namespace writer.");
    }

    public void writeStartElement(String localName)
        throws XMLStreamException
    {
        doWriteStartElement(localName);
        mEmptyElement = false;
    }

    public void writeStartElement(String nsURI, String localName)
        throws XMLStreamException
    {
        writeStartElement(localName);
    }

    public void writeStartElement(String prefix, String localName, String nsURI)
        throws XMLStreamException
    {
        writeStartElement(localName);
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
        // Better have something to close... (to figure out what to close)
        if (mState != STATE_TREE) {
            throw new XMLStreamException("No open start element, when calling writeFullEndElement.");
        }
        doWriteEndElement(mElements.removeLast(), false);
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
        writeStartElement(name.getLocalPart());
        Iterator it = elem.getAttributes();
        while (it.hasNext()) {
            Attribute attr = (Attribute) it.next();
            name = attr.getName();
            writeAttribute(name.getLocalPart(), attr.getValue());
        }
    }

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
        String local;
        if (mCheckStructure) {
            if (mElements.isEmpty()) {
                throw new XMLStreamException("No open start element, when calling writeEndElement.");
            }
            local = name.getLocalPart();
            String local2 = mElements.removeLast();
            if (!local.equals(local2)) {
                throw new IllegalArgumentException("Mismatching close element name, '"+local+"'; expected '"+local2+"'.");
            }
        } else {
            local = name.getLocalPart();
            if (!mElements.isEmpty()) {
                mElements.removeLast();
            }
        }
        doWriteEndElement(local, mCfgOutputEmptyElems);
    }

    /**
     * Method called to close an open start element, when another
     * main-level element (not namespace declaration or
     * attribute) is being output; except for end element which is
     * handled differently.
     */
    public void closeStartElement(boolean emptyElem)
        throws XMLStreamException
    {
        mStartElementOpen = false;
        if (mAttrNames != null) {
            mAttrNames.clear();
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
            mElements.removeLast();
            if (mElements.isEmpty()) {
                mState = STATE_EPILOG;
            }
        }
    }

    public String getTopElemName() {
        return mElements.getLastString();
    }

    /**
     * Element copier method implementation suitable to be used with
     * non-namespace-aware writers. The only special thing here is that
     * the copier can convert namespace declarations to equivalent
     * attribute writes.
     */
    public void copyStartElement(InputElementStack elemStack,
                                 AttributeCollector attrCollector)
        throws IOException, XMLStreamException
    {
        String ln = elemStack.getLocalName();
        boolean nsAware = elemStack.isNamespaceAware();
        
        /* First, since we are not to output namespace stuff as is,
         * we just need to copy the element:
         */
        if (nsAware) { // but reader is ns-aware? Need to add prefix?
            String prefix = elemStack.getPrefix();
            if (prefix != null && prefix.length() > 0) { // yup
                ln = prefix + ":" + ln;
            }
        }
        writeStartElement(ln);
        
        /* However, if there are any namespace declarations, we probably
         * better output them just as 'normal' attributes:
         */
        if (nsAware) {
            int nsCount = elemStack.getCurrentNsCount();
            if (nsCount > 0) {
                for (int i = 0; i < nsCount; ++i) {
                    String prefix = elemStack.getLocalNsPrefix(i);
                    if (prefix == null || prefix.length() == 0) { // default NS decl
                        prefix = XMLConstants.XML_NS_PREFIX;
                    } else {
                        prefix = "xmlns:"+prefix;
                    }
                    writeAttribute(prefix, elemStack.getLocalNsURI(i));
                }
            }
        }
        
        /* And then let's just output attributes, if any:
         */
        // Let's only output explicit attributes?
        // !!! Should it be configurable?
        int attrCount = attrCollector.getSpecifiedCount();
        
        for (int i = 0; i < attrCount; ++i) {
            /* There's nothing special about writeAttribute() (except for
             * checks we should NOT need -- reader is assumed to have verified
             * well-formedness of the input document)... it just calls
             * doWriteAttr (of the base class)... so what we have here is
             * just a raw output method:
             */
            mWriter.write(' ');
            attrCollector.writeAttribute(i, DEFAULT_QUOTE_CHAR, mWriter,
                                         mAttrValueWriter);
        }
    }

    /*
    ////////////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////////////
     */

    private void doWriteStartElement(String localName)
        throws XMLStreamException
    {
        mAnyOutput = true;
        // Need to finish an open start element?
        if (mStartElementOpen) {
            closeStartElement(mEmptyElement);
        }
        if (mCheckContent) {
            checkNameValidity(localName, true);
        }

        if (mState == STATE_PROLOG) {
            mState = STATE_TREE;
        } else if (mCheckStructure && mState == STATE_EPILOG) {
            throw new IllegalStateException("Trying to output second root ('"
                                            +localName+"').");
        }

        mStartElementOpen = true;
        mElements.addString(localName);
        try {
            mWriter.write('<');
            mWriter.write(localName);
        } catch (IOException ioe) {
            throw new XMLStreamException(ioe);
        }
    }

    /**
     *<p>
     * Note: Caller has to do actual removal of the element from element
     * stack, before calling this method.
     *
     * @param localName Name of the closing element; since this is a
     *   non-namespace-aware writer, may also contain prefix
     * @param allowEmpty If true, is allowed to create the empty element
     *   if the closing element was truly empty; if false, has to write
     *   the full empty element no matter what
     */
    private void doWriteEndElement(String localName, boolean allowEmpty)
        throws XMLStreamException
    {
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
                    if (mElements.isEmpty()) {
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
            mWriter.write(localName);
            mWriter.write('>');
        } catch (IOException ioe) {
            throw new XMLStreamException(ioe);
        }

        if (mElements.isEmpty()) {
            mState = STATE_EPILOG;
        }
    }
}
