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
    ///////////////////////////
    // DOCTYPE info accessors
    ///////////////////////////
    */

    /**
     * @return If current event is DTD, DTD support is enabled,
     *   and reader supports DTD processing, returns an internal
     *   Object implementation uses for storing/processing DTD;
     *   otherwise returns null.
     */
    public Object getProcessedDTD();

    /**
     * @return If current event is DTD, returns the full root name
     *   (including prefix, if any); otherwise returns null
     */
    public String getDTDRootName();

    /**
     * @return If current event is DTD, and has a system id, returns the
     *   system id; otherwise returns null.
     */
    public String getDTDSystemId();

    /**
     * @return If current event is DTD, and has a public id, returns the
     *   public id; otherwise returns null.
     */
    public String getDTDPublicId();

    /**
     * @return If current event is DTD, and has an internal subset,
     *   returns the internal subset; otherwise returns null.
     */
    public String getDTDInternalSubset();

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

