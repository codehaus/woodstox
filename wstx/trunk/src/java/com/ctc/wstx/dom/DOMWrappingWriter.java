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

    protected Node mCurrNode;

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
            break;

        case Node.DOCUMENT_FRAGMENT_NODE: // as with element...
            mDocument = treeRoot.getOwnerDocument();

            // Above types are fine
            break;

        default: // other Nodes not usable
            throw new XMLStreamException("Can not create an XMLStreamWriter for a DOM node of type "+treeRoot.getClass());
        }
        mCurrNode = treeRoot;

        if (mDocument == null) {
            throw new XMLStreamException("Can not create an XMLStreamWriter for given node (of type "+treeRoot.getClass()+"): did not have owner document");
        }
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

    public NamespaceContext getNamespaceContext() {
        // !!! TBI
        return null;
    }

    public String getPrefix(String uri) {
        // !!! TBI
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
        writeAttribute(null, localName, value);
    }

    public void writeAttribute(String namespaceURI, String localName, String value) {
        checkIsElement();
        // !!! TBI
    }

    public void writeAttribute(String prefix, String namespaceURI, String localName, String value)
    {
        checkIsElement();
        // !!! TBI
    }

    public void writeCData(String data) {
        mCurrNode.appendChild(mDocument.createCDATASection(data));
    }

    public void writeCharacters(char[] text, int start, int len)
    {
        writeCharacters(new String(text, start, len));
    }
    
    public void writeCharacters(String text) {
        mCurrNode.appendChild(mDocument.createTextNode(text));
    }

    public void writeComment(String data) {
        mCurrNode.appendChild(mDocument.createCDATASection(data));
    }

    public void writeDefaultNamespace(String namespaceURI) {
        checkIsElement();

        // !!! TBI
    }

    public void writeDTD(String dtd)
    {
        // Would have to parse, not worth trying...
        reportUnsupported("writeDTD()");
    }

    public void writeEmptyElement(String localName) {
        writeEmptyElement(null, localName);
    }

    public void writeEmptyElement(String namespaceURI, String localName)
    {
        // Note: can not just call writeStartElement(), since this should not change context for good!

        // !!! TBI
    }

    public void writeEmptyElement(String prefix, String localName, String namespaceURI)
    {
        // !!! TBI
    }

    public void writeEndDocument()
    {
        mCurrNode = mDocument;
    }

    public void writeEndElement() {
    }

    public void writeEntityRef(String name) {
        mCurrNode.appendChild(mDocument.createEntityReference(name));
    }

    public void writeNamespace(String prefix, String namespaceURI) {
        checkIsElement();

        // !!! TBI
    }

    public void writeProcessingInstruction(String target) {
        writeProcessingInstruction(target, null);
    }

    public void writeProcessingInstruction(String target, String data) {
        mCurrNode.appendChild(mDocument.createProcessingInstruction(target, data));
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

    public void writeStartElement(String namespaceURI, String localName) {
        // !!! TBI
    }

    public void writeStartElement(String prefix, String localName, String namespaceURI) {
        // !!! TBI
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
        if (mCurrNode != mDocument) {
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
    // Stax2, pass-through methods
    ///////////////////////////////
    */

    private void reportUnsupported(String operName)
    {
        throw new UnsupportedOperationException(operName+" can not be used with DOM-backed writer");
    }

    private void checkIsElement()
    {
        int type = mCurrNode.getNodeType();
        if (type != Node.ELEMENT_NODE) {
            throw new IllegalStateException("Operation only allowed when last output event was START_ELEMENT (context node is of type "+type+")");
        }
    }
}
