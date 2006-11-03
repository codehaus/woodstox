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
 * {@link XMLStreamWriter} interface. It implements a sufficiently
 * complement subset of functionality, so 
 *<p>
 * Note that the implementation is only to be used for use with
 * <code>javax.xml.transform.dom.DOMResult</code>.
 *<p>
 * Some notes regarding missing/incomplete functionality:
 * <ul>
 *  </ul>
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

    /*
    ////////////////////////////////////////////////////
    // State
    ////////////////////////////////////////////////////
     */

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
            /* Should try to find encoding, version and stand-alone
             * settings... but is there a standard way of doing that?
             */
        case Node.ELEMENT_NODE: // can make sub-tree... ok
            // But should we skip START/END_DOCUMENT? For now, let's not

        case Node.DOCUMENT_FRAGMENT_NODE: // as with element...

            // Above types are fine
            break;

        default: // other Nodes not usable
            throw new XMLStreamException("Can not create an XMLStreamWriter for a DOM node of type "+treeRoot.getClass());
        }
        mCurrNode = treeRoot;
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

    public void close()
    {
    }
    
    public void flush() {}

    public NamespaceContext getNamespaceContext() {
        // !!! TBI
        return null;
    }

    public String getPrefix(String uri) {
        return null;
    }

    public Object getProperty(String name) {
        return null;
    }

    public void setDefaultNamespace(String uri) {}
    public void setNamespaceContext(NamespaceContext context) {}
    public void setPrefix(String prefix, String uri) {}
    public void writeAttribute(String localName, String value) {}
    public void writeAttribute(String namespaceURI, String localName, String value) {}
    public void writeAttribute(String prefix, String namespaceURI, String localName, String value) {}
    public void writeCData(String data) {}
    public void writeCharacters(char[] text, int start, int len) {}
    public void writeCharacters(String text) {}
    public void writeComment(String data) {}
    public void writeDefaultNamespace(String namespaceURI) {}
    public void writeDTD(String dtd) {}
    public void writeEmptyElement(String localName) {}
    public void writeEmptyElement(String namespaceURI, String localName) {}
    public void writeEmptyElement(String prefix, String localName, String namespaceURI) {}
    public void writeEndDocument() {}
    public void writeEndElement() {}
    public void writeEntityRef(String name) {}
    public void writeNamespace(String prefix, String namespaceURI) {}
    public void writeProcessingInstruction(String target) {}
    public void writeProcessingInstruction(String target, String data) {}

    public void writeStartDocument()
    {
        writeStartDocument(WstxOutputProperties.DEFAULT_OUTPUT_ENCODING,
                           WstxOutputProperties.DEFAULT_XML_VERSION);
    }

    public void writeStartDocument(String version) {}

    public void writeStartDocument(String encoding, String version)
    {
        mEncoding = encoding;
    }

    public void writeStartElement(String localName) {}
    public void writeStartElement(String namespaceURI, String localName) {}
    public void writeStartElement(String prefix, String localName, String namespaceURI) {
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
        // !!! TBI
    }

    public void writeDTD(String rootName, String systemId, String publicId,
                         String internalSubset)
        throws XMLStreamException
    {
        // !!! TBI
    }

    public void writeFullEndElement() throws XMLStreamException
    {
        // !!! TBI
    }

    public void writeStartDocument(String version, String encoding,
                                   boolean standAlone)
        throws XMLStreamException
    {
        // !!! TBI
    }
    
    /*
    ///////////////////////////////
    // Stax2, pass-through methods
    ///////////////////////////////
    */

    public void writeRaw(String text)
        throws XMLStreamException
    {
        // !!! TBI: Can it be implemented at all?
    }

    public void writeRaw(String text, int start, int offset)
        throws XMLStreamException
    {
        // !!! TBI: Can it be implemented at all?
    }

    public void writeRaw(char[] text, int offset, int length)
        throws XMLStreamException
    {
        // !!! TBI
    }

    public void copyEventFromReader(XMLStreamReader2 r, boolean preserveEventData)
        throws XMLStreamException
    {
        // !!! TBI
    }
}
