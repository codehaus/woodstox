package org.codehaus.stax2;

import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Extended interface that implements functionality that is necessary
 * to properly build event API on top of {@link XMLStreamReader}.
 * It also adds limited number of methods that are important for
 * efficient pass-through processing (such as one needed when routing
 * SOAP-messages).
 */
public interface XMLStreamReader2
    extends XMLStreamReader
{
    /*
    ///////////////////////////
    // Configuration
    ///////////////////////////
    */

    /**
     * @param name Name of the feature of which value to get
     *
     * @return Value of the feature (possibly null), if supported; null
     *     otherwise
     */
    public Object getFeature(String name);

    /**
     * @param name Name of the feature to set
     * @param value Value to set feature to.
     */
    public void setFeature(String name, Object value);

    /*
    /////////////////////////////////
    // Additional DTD access
    /////////////////////////////////
    */

    /**
     * Method that can be called to get information about DOCTYPE declaration
     * that the reader is currently pointing to, if the reader has parsed
     * it. Implementations can also choose to return null to indicate they
     * do not provide extra information; but they should not throw any
     * exceptions beyond normal parsing exceptions.
     */
    public DTDInfo getDTDInfo() throws XMLStreamException;

    /*
    /////////////////////////////////
    // Additional attribute accessors
    /////////////////////////////////
    */

    /**
     * @return Index of the specified attribute, if the current element
     *   has such an attribute (explicit, or one created via default
     *   value expansion); -1 if not.
     *
     * @throws IllegalStateException
     *   if current node is not a START_ELEMENT
     */
    public int getAttributeIndex(String nsURI, String localName);

    /**
     * @return Index of the specified ID attribute (attribute that has
     *   DTD-defined type of ID), if the current element has such an
     *   attribute defined; -1 if not.
     *
     * @throws IllegalStateException
     *   if current node is not a START_ELEMENT
     */
    public int getIdAttributeIndex(String nsURI, String localName);

    /*
    ///////////////////////////////
    // Pass-through text accessors
    ///////////////////////////////
    */

    /**
     * @return Number of characters written to the reader
     */
    public int getText(Writer w)
        throws IOException, XMLStreamException;

    /*
    ///////////////////////////
    // Other accessors
    ///////////////////////////
    */

    /**
     *<p>
     * Note: method may need to read more data to know if the element
     * is an empty one, and as such may throw an i/o or parsing exception.
     *
     * @return True, if current event is START_ELEMENT
     *   and is based on a parsed empty element.
     */
    public boolean isEmptyElement() throws XMLStreamException;

    /**
     * @return Number of open elements in the stack; 0 when parser is in
     *  prolog/epilog, 1 inside root element and so on. Depth is same
     *  for matching start/end elements.
     */
    public int getDepth();

}

