package com.ctc.wstx.dom;

import java.util.*;

import javax.xml.namespace.*;
import javax.xml.stream.*;
import javax.xml.transform.dom.DOMResult;

import org.w3c.dom.*;

import org.codehaus.stax2.XMLStreamLocation2;
import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.XMLStreamWriter2;
import org.codehaus.stax2.validation.ValidationProblemHandler;
import org.codehaus.stax2.validation.XMLValidationSchema;
import org.codehaus.stax2.validation.XMLValidator;

import com.ctc.wstx.api.WriterConfig;
import com.ctc.wstx.api.WstxOutputProperties;
import com.ctc.wstx.util.EmptyNamespaceContext;

/**
 * This is an adapter class that allows building a DOM tree using
 * {@link XMLStreamWriter} interface.
 *<p>
 * Note that the implementation is only to be used for use with
 * <code>javax.xml.transform.dom.DOMResult</code>.
 *<p>
 * Some notes regarding missing/incomplete functionality:
 * <ul>
 *  </ul>
 *
 * @author Tatu Saloranta
 * @author Dan Diephouse
 */
public class DOMWrappingWriter
    implements XMLStreamWriter2
{
    /*
    ////////////////////////////////////////////////////
    // Configuration
    ////////////////////////////////////////////////////
     */

    protected final WriterConfig mConfig;

    protected final boolean mNsAware;

    protected final boolean mNsRepairing;

    /**
     * This member variable is to keep information about encoding
     * that seems to be used for the document (or fragment) to output,
     * if known.
     */
    protected String mEncoding = null;

    /**
     * If we are being given info about existing bindings, it'll come
     * as a NamespaceContet.
     */
    protected NamespaceContext mNsContext;

    /*
    ////////////////////////////////////////////////////
    // State
    ////////////////////////////////////////////////////
     */

    /**
     * We need a reference to the document hosting nodes to
     * be able to create new nodes
     */
    protected final Document mDocument;

    /**
     * This element is the current context element, under which
     * all other nodes are added, until matching end element
     * is output. Null outside of the main element tree.
     */
    protected Element mParentElem;

    /**
     * This element is non-null right after a call to
     * either <code>writeStartElement</code> and
     * <code>writeEmptyElement</code>, and can be used to
     * add attributes and namespace declarations.
     */
    protected Element mOpenElement;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////////////////
     */

    private DOMWrappingWriter(WriterConfig cfg, Node treeRoot)
        throws XMLStreamException
    {
        if (treeRoot == null) {
            throw new IllegalArgumentException("Can not pass null Node for constructing a DOM-based XMLStreamWriter");
        }
        mConfig = cfg;
        mNsAware = cfg.willSupportNamespaces();
        mNsRepairing = mNsAware && cfg.automaticNamespacesEnabled();

        /* 15-Sep-2007, TSa: Repairing mode not yet supported, so better
         *   signal that right away
         */
        if (mNsRepairing) {
            throw new XMLStreamException("Repairing mode not (yet) supported with DOM-backed writer");
        }

        Element elem = null;
        
        /* Ok; we need a document node; or an element node; or a document
         * fragment node.
         */
        switch (treeRoot.getNodeType()) {
        case Node.DOCUMENT_NODE: // fine
            mDocument = (Document) treeRoot;

            /* Should try to find encoding, version and stand-alone
             * settings... but is there a standard way of doing that?
             */
            break;

        case Node.ELEMENT_NODE: // can make sub-tree... ok
            mDocument = treeRoot.getOwnerDocument();
            elem = (Element) treeRoot;
            break;

        case Node.DOCUMENT_FRAGMENT_NODE: // as with element...
            mDocument = treeRoot.getOwnerDocument();
            // Above types are fine
            break;

        default: // other Nodes not usable
            throw new XMLStreamException("Can not create an XMLStreamWriter for a DOM node of type "+treeRoot.getClass());
        }
        if (mDocument == null) {
            throw new XMLStreamException("Can not create an XMLStreamWriter for given node (of type "+treeRoot.getClass()+"): did not have owner document");
        }
        mParentElem = mOpenElement = elem;
    }

    public static DOMWrappingWriter createFrom(WriterConfig cfg, DOMResult dst)
        throws XMLStreamException
    {
        Node rootNode = dst.getNode();
        return new DOMWrappingWriter(cfg, rootNode);
    }

    /*
    ////////////////////////////////////////////////////
    // XMLStreamWriter API (Stax 1.0)
    ////////////////////////////////////////////////////
     */

    public void close() {
        // NOP
    }
    
    public void flush() {
        // NOP
    }

    public NamespaceContext getNamespaceContext()
    {
        if (!mNsAware) {
            return EmptyNamespaceContext.getInstance();
        }
        // !!! TBI: 
        return mNsContext;
    }

    public String getPrefix(String uri)
    {
        if (!mNsAware) {
            return null;
        }
        // !!! TBI: 
        return null;
    }

    public Object getProperty(String name) {
        // !!! TBI
        return null;
    }

    public void setDefaultNamespace(String uri) {
        // !!! TBI
    }

    public void setNamespaceContext(NamespaceContext context) {
        mNsContext = context;
    }

    public void setPrefix(String prefix, String uri) {
        // !!! TBI
    }

    public void writeAttribute(String localName, String value)
    {
        outputAttribute(null, null, localName, value);
    }

    public void writeAttribute(String nsURI, String localName, String value) {
        outputAttribute(nsURI, null, localName, value);
    }

    public void writeAttribute(String prefix, String nsURI, String localName, String value)
    {
        outputAttribute(nsURI, prefix, localName, value);
    }

    public void writeCData(String data) {
        appendLeaf(mDocument.createCDATASection(data));
    }

    public void writeCharacters(char[] text, int start, int len)
    {
        writeCharacters(new String(text, start, len));
    }
    
    public void writeCharacters(String text) {
        appendLeaf(mDocument.createTextNode(text));
    }

    public void writeComment(String data) {
        appendLeaf(mDocument.createCDATASection(data));
    }

    public void writeDefaultNamespace(String nsURI)
    {
        writeNamespace(null, nsURI);
    }

    public void writeDTD(String dtd)
    {
        // Would have to parse, not worth trying...
        reportUnsupported("writeDTD()");
    }

    public void writeEmptyElement(String localName) {
        writeEmptyElement(null, localName);
    }

    public void writeEmptyElement(String nsURI, String localName)
    {
        /* Note: can not just call writeStartElement(), since this
         * element will only become the open elem, but not a parent elem
         */
        createStartElem(nsURI, null, localName, true);
    }

    public void writeEmptyElement(String prefix, String localName, String nsURI)
    {
        createStartElem(nsURI, prefix, localName, true);
    }

    public void writeEndDocument()
    {
        mParentElem = mOpenElement = null;
    }

    public void writeEndElement()
    {
        // Simple, just need to traverse up... if we can
        if (mParentElem == null) {
            throw new IllegalStateException("No open start element to close");
        }
        mOpenElement = null; // just in case it was open
        Node parent = mParentElem.getParentNode();
        mParentElem = (parent == mDocument) ? null : (Element) parent;
    }

    public void writeEntityRef(String name) {
        appendLeaf(mDocument.createEntityReference(name));
    }

    public void writeNamespace(String prefix, String nsURI)
    {
        boolean defNS = (prefix == null || prefix.length() == 0);

        if (!mNsAware) {
            if (defNS) {
                outputAttribute(null, null, "xmlns", nsURI);
            } else {
                outputAttribute(null, "xmlns", prefix, nsURI);
            }
        } else {
            // !!! TBI
            /* For now, let's output it (in non-repairing ns-aware
             * mode), but not keep track of bindings.
             */
            if (defNS) {
                outputAttribute(null, null, "xmlns", nsURI);
            } else {
                outputAttribute(null, "xmlns", prefix, nsURI);
            }
        }
    }

    public void writeProcessingInstruction(String target) {
        writeProcessingInstruction(target, null);
    }

    public void writeProcessingInstruction(String target, String data) {
        appendLeaf(mDocument.createProcessingInstruction(target, data));
    }

    public void writeSpace(char[] text, int start, int len) {
        writeSpace(new String(text, start, len));
    }

    public void writeSpace(String text) {
        /* This won't work all that well, given there's no way to
         * prevent quoting/escaping. But let's do what we can, since
         * the alternative (throwing an exception) doesn't seem
         * especially tempting choice.
         */
        writeCharacters(text);
    }

    public void writeStartDocument()
    {
        writeStartDocument(WstxOutputProperties.DEFAULT_OUTPUT_ENCODING,
                           WstxOutputProperties.DEFAULT_XML_VERSION);
    }

    public void writeStartDocument(String version)
    {
        writeStartDocument(null, version);
    }

    public void writeStartDocument(String encoding, String version)
    {
        // Is there anything here we can or should do? No?
        mEncoding = encoding;
    }

    public void writeStartElement(String localName) {
        writeStartElement(null, localName);
    }

    public void writeStartElement(String nsURI, String localName)
    {
        createStartElem(nsURI, null, localName, false);
    }

    public void writeStartElement(String prefix, String localName, String nsURI)
    {
        createStartElem(nsURI, prefix, localName, false);
    }

    /*
    ////////////////////////////////////////////////////
    // XMLStreamWriter2 API (Stax2 v2.0)
    ////////////////////////////////////////////////////
     */

    public boolean isPropertySupported(String name)
    {
        // !!! TBI: not all these properties are really supported
        return mConfig.isPropertySupported(name);
    }

    public boolean setProperty(String name, Object value)
    {
        /* Note: can not call local method, since it'll return false for
         * recognized but non-mutable properties
         */
        return mConfig.setProperty(name, value);
    }

    public XMLValidator validateAgainst(XMLValidationSchema schema)
        throws XMLStreamException
    {
        // !!! TBI
        return null;
    }

    public XMLValidator stopValidatingAgainst(XMLValidationSchema schema)
        throws XMLStreamException
    {
        // !!! TBI
        return null;
    }

    public XMLValidator stopValidatingAgainst(XMLValidator validator)
        throws XMLStreamException
    {
        // !!! TBI
        return null;
    }

    public ValidationProblemHandler setValidationProblemHandler(ValidationProblemHandler h)
    {
        // Not implemented by the basic reader
        return null;
    }

    public XMLStreamLocation2 getLocation() {
        return null;
    }

    public String getEncoding() {
        return mEncoding;
    }

    public void writeCData(char[] text, int start, int len)
        throws XMLStreamException
    {
        writeCData(new String(text, start, len));
    }

    public void writeDTD(String rootName, String systemId, String publicId,
                         String internalSubset)
        throws XMLStreamException
    {
        /* Alas: although we can create a DocumentType object, there
         * doesn't seem to be a way to attach it in DOM-2!
         */
        if (mParentElem != null) {
            throw new IllegalStateException("Operation only allowed to the document before adding root element");
        }
        reportUnsupported("writeDTD()");
    }

    public void writeFullEndElement() throws XMLStreamException
    {
        // No difference with DOM
        writeEndElement();
    }

    public void writeStartDocument(String version, String encoding,
                                   boolean standAlone)
        throws XMLStreamException
    {
        writeStartDocument(encoding, version);
    }
    
    /*
    ///////////////////////////////
    // Stax2, pass-through methods
    ///////////////////////////////
    */

    public void writeRaw(String text)
        throws XMLStreamException
    {
        reportUnsupported("writeRaw()");
    }

    public void writeRaw(String text, int start, int offset)
        throws XMLStreamException
    {
        reportUnsupported("writeRaw()");
    }

    public void writeRaw(char[] text, int offset, int length)
        throws XMLStreamException
    {
        reportUnsupported("writeRaw()");
    }

    public void copyEventFromReader(XMLStreamReader2 r, boolean preserveEventData)
        throws XMLStreamException
    {
        // !!! TBI
    }

    /*
    ///////////////////////////////
    // Internal methods
    ///////////////////////////////
    */

    protected void appendLeaf(Node n)
        throws IllegalStateException
    {
        if (mParentElem == null) { // to add to document
            mDocument.appendChild(n);
        } else {
            // Will also close the open element, if any
            mOpenElement = null;
            mParentElem.appendChild(n);
        }
    }

    protected void createStartElem(String nsURI, String prefix, String localName, boolean isEmpty)
    {
        Element elem;

        if (mNsAware) {
            if (mNsRepairing) {
                /* Need to ensure proper bindings... ugh.
                 * May change prefix
                 */
                // !!! TBI
            }
            if (prefix != null && prefix.length() > 0) {
                localName = prefix + ":" + localName;
            }
            elem = mDocument.createElementNS(nsURI, localName);
        } else { // non-ns, simple
            if (prefix != null && prefix.length() > 0) {
                localName = prefix + ":" + localName;
            }
            elem = mDocument.createElement(localName);
        }

        appendLeaf(elem);
        mOpenElement = elem;
        if (!isEmpty) {
            mParentElem = elem;
        }
    }

    protected void outputAttribute(String nsURI, String prefix, String localName, String value)
    {
        if (mOpenElement == null) {
            throw new IllegalStateException("No currently open START_ELEMENT, cannot write attribute");
        }

        if (mNsAware) {
            if (mNsRepairing) {
                /* Need to ensure proper bindings... ugh.
                 * May change prefix
                 */
                // !!! TBI
            }
            if (prefix != null && prefix.length() > 0) {
                localName = prefix + ":" + localName;
            }
            mOpenElement.setAttributeNS(nsURI, localName, value);
        } else { // non-ns, simple
            if (prefix != null && prefix.length() > 0) {
                localName = prefix + ":" + localName;
            }
            mOpenElement.setAttribute(localName, value);
        }
    }

    private void reportUnsupported(String operName)
    {
        throw new UnsupportedOperationException(operName+" can not be used with DOM-backed writer");
    }
}
