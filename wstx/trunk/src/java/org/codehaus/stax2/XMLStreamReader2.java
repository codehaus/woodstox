package org.codehaus.stax2;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Extended interface that implements functionality that is necessary
 * to properly build event API on top of {@link XMLStreamReader}.
 * It also adds limited number of methods that are important for
 * efficient pass-through processing (such as one needed when routing
 * SOAP-messages).
 *<p>
 * The features supported via {@link #setFeature} are:
 *<dt>
 * <dt>FEATURE_DTD_OVERRIDE: (write-only)</dt>
 * <dd>Feature used to specify the source for DTD external subset to use
 *    instead of DTD specified by the XML document itself (if any).
 *    Setting the feature for a reader that supports DTD validation
 *    essentially allows for injecting an alternate DOCTYPE declaration.
 *    Note that setting this value to null is both legal, and sometimes
 *    useful: it is equivalent of removing the DOCTYPE declaration.
 *   <br />Feature is write-only, since storing it after loading the DTD
 *    in question does not have much use.
 *  </dt>
 *</dt>
 */
public interface XMLStreamReader2
    extends XMLStreamReader
{
    /**
     * Feature used to specify the source for DTD external subset to use
     * instead of DTD specified by the XML document itself (if any).
     */
    public final static String FEATURE_DTD_OVERRIDE = "org.codehaus.stax2.propDtdOverride";

    /*
    ///////////////////////////
    // Configuration
    ///////////////////////////
    */

    /**
     * Method that can be used to get per-reader values; both generic
     * ones (names for which are defined as constants in this class),
     * and implementation dependant ones.
     *<p>
     * Note: although some feature names are shared with
     * {@link #setFeature}, not all are: some features are read-only,
     * some write-only
     *
     * @deprecated Should use {@link #getProperty} instead
     *
     * @param name Name of the feature of which value to get
     *
     * @return Value of the feature (possibly null), if supported; null
     *     otherwise
     */
    public Object getFeature(String name);

    /**
     * Method that can be used to set per-reader features such as configuration
     * settings; both generic
     * ones (names for which are defined as constants in this class),
     * and implementation dependant ones.
     *<p>
     * Note: although some feature names are shared with
     * {@link #getFeature}, not all are: some features are read-only,
     * some write-only
     *
     * @deprecated Should use {@link #setProperty} instead
     *
     * @param name Name of the feature to set
     * @param value Value to set feature to.
     */
    public void setFeature(String name, Object value);

    /**
     * Method similar to {@link XMLInputFactory#isPropertySupported}, used
     * to determine whether a property is supported by the Reader
     * <b>instance</b>. This means that this method may return false
     * for some properties that the input factory does support: specifically,
     * it should only return true if the value is mutable on per-instance
     * basis. False means that either the property is not recognized, or
     * is not mutable via reader instance.
     */
    public boolean isPropertySupported(String name);

    /**
     * Method that can be used to set per-reader properties; a subset of
     * properties one can set via matching {@link XMLInputFactory2}
     * instance. Exactly which methods are mutable is implementation
     * specific.
     *
     * @param name Name of the property to set
     * @param value Value to set property to.
     */
    public void setProperty(String name, Object value);

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
    /////////////////////////////////////////
    // Additional location information access
    /////////////////////////////////////////
    */

    public LocationInfo getLocationInfo();

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
     * data. Finally, note that this method is also guaranteed NOT
     * to return fragments, even when coalescing is not enabled and
     * a parser is otherwised allowed to return partial segments: this
     * requirement is due to there being little benefit in returning
     * such short chunks when streaming. Coalescing property is still
     * honored normally.
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

    /**
     * Method similar to {@link #getText()}, except
     * that onlt a {@link Reader} is returned, which may then be used
     * by the caller to read all the text.
     * For further optimization, it may also be allowed to do true
     * pass-through, thus possibly avoiding one temporary copy of the
     * data (without allowing that to be done, stream reader still needs
     * to create a copy of data read, thus negating benefits of using
     * a simple Reader).
     *<p>
     * Method can only be called on states CDATA, CHARACTERS, COMMENT,
     * DTD, ENTITY_REFERENCE; if called when reader is in another state,
     * {@link IllegalStateException} will be thrown. Content written
     * for elements is same as with {@link #getText()}.
     *
     * @param preserveContents If true, reader has to preserve contents
     *   so that further calls to <code>getText</code> will return
     *   proper conntets. If false, reader is allowed to skip creation
     *   of such copies: this can improve performance, but it also means
     *   that further calls to <code>getText</code> is not guaranteed to
     *   return meaningful data.
     *
     * @return Reader through which textual contents of the current event
     *    can be read.
     */
    /* 
      // 19-Apr-2005, TSa: This method was left out of StAX2 v1.0...
    public Reader getTextReader(boolean preserveContents)
        throws IOException, XMLStreamException;
    */

    /*
    ///////////////////////////
    // Other accessors
    ///////////////////////////
    */

    /**
     * Method that can be used to check whether current START_ELEMENT
     * event was created for an empty element (xml short-hand notation
     * where one tag implies start and end, ending with "/>"), or not.
     *<p>
     * Note: method may need to read more data to know if the element
     * is an empty one, and as such may throw an i/o or parsing exception
     * (as {@link XMLStreamException}); however, it won't throw exceptions
     * for non-START_ELEMENT event types.
     *
     * @return True, if current event is START_ELEMENT
     *   and is based on a parsed empty element; otherwise false
     */
    public boolean isEmptyElement() throws XMLStreamException;

    /**
     * Method that returns the number of open elements in the stack; 0 when
     * the reader is in prolog/epilog, 1 inside root element and so on.
     * Depth is same for matching start/end elements, as well as for the
     * immediate non-element children of the element.
     *
     * @return Number of open elements currently in the reader's stack
     */
    public int getDepth();
}

