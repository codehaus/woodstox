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
    // Additional event traversing
    /////////////////////////////////
    */

    /**
     * Method that will skip all the contents of the element that the
     * stream currently points to. Current event when calling the method
     * has to be START_ELEMENT (or otherwise {@link IllegalStateException}
     * is thrown); after the call the stream will point to the matching
     * END_ELEMENT event, having skipped zero or more intervening events
     * for the contents.
     */
    public void skipElement() throws XMLStreamException;

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
     *
     * @return Information object for accessing further DOCTYPE information,
     *   iff the reader currently points to DTD event, AND is operating
     *   in mode that parses such information (DTD-aware at least, and
     *   usually also validating)
     */
    public DTDInfo getDTDInfo() throws XMLStreamException;

    /*
    /////////////////////////////////
    // Additional attribute accessors
    /////////////////////////////////
    */

    /**
     * Method that can be called to get additional information about
     * attributes related to the current start element, as well as
     * related DTD-based information if available. Note that the
     * reader has to currently point to START_ELEMENT; if not,
     * a {@link IllegalStateException} will be thrown.
     */
    public AttributeInfo getAttributeInfo() throws XMLStreamException;

    /*
    ///////////////////////////////
    // Pass-through text accessors
    ///////////////////////////////
    */

    /**
     * Method similar to {@link #getText()}, except
     * that it just uses provided Writer to write all textual content.
     * For further optimization, it may also be allowed to do true
     * pass-through, thus possibly avoiding one temporary copy of the
     * data.
     *<p>
     * Method can only be called on states CDATA, CHARACTERS, COMMENT,
     * DTD, ENTITY_REFERENCE; if called when reader is in another state,
     * {@link IllegalStateException} will be thrown. Content written
     * for elements is same as with {@link #getText()}.
     *
     * @param w Writer to use for writing textual contents
     * @param preserveContents If true, reader has to preserve contents
     *   so that further calls to <code>getText</code> will return
     *   proper conntets. If false, reader is allowed to skip creation
     *   of such copies: this can improve performance, but it also means
     *   that further calls to <code>getText</code> is not guaranteed to
     *   return meaningful data.
     *
     * @return Number of characters written to the reader
     */
    public int getText(Writer w, boolean preserveContents)
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

