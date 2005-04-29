package org.codehaus.stax2;

import java.io.File;
import java.net.URL;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Extension of {@link XMLInputFactory} to add missing functionality.
 *<p>
 * Also contains extended standard properties that conforming stream
 * reader factory and instance implementations should at least
 * recognize, and preferably support.
 *<br />
 * NOTE: although actual values for the property names are
 * visible, implementations should try to use the symbolic constants
 * defined here instead, to avoid typos.
 */
public abstract class XMLInputFactory2
    extends XMLInputFactory
{
    /*
    ////////////////////////////////////////////////////
    // Additional standard configuration properties
    ////////////////////////////////////////////////////
     */

    // // // Parsing settings

    /**
     * Whether reader will generate 'ignorable white space' events during
     * prolog and epilog (before and after the main XML root element);
     * if true, will generate those events; if false,
     * will just ignore white space in these parts of the parsed document.
     *<p>
     * Turning this feature off may give slight performance improvement,
     * although usually effect should be negligible. This option is usually
     * only turned on when round-trip output should be as similar to input
     * as possible.
     */
    public final static String P_REPORT_PROLOG_WHITESPACE = "org.codehaus.stax2.reportPrologWhitespace";

    /**
     * Whether cursor-based reader will ever generate CDATA events; if true,
     * CDATA events may be generated for non-coalesced CDATA sections. If
     * false, all CDATA sections are reported as CHARACTERS types. It may
     * still be possible for event methods to distinguish between underlying
     * type, but event type code will be reported as CHARACTERS.
     *<p>
     * State of property does not have any effect on performance.
     */
    public final static String P_REPORT_ALL_TEXT_AS_CHARACTERS = "org.codehaus.stax2.reportAllTextAsCharacters";

    // // // Optimization settings
 
    /**
     * Whether name symbols (element, attribute, entity and notation names,
     * namespace prefixes) should be interned or not (or when
     * querying an instance, whether the instance will guarantee that
     * the names will be intern()ed).
     * Interning generally makes access faster (both internal and externally),
     * and saves memory, but may add some overhead.
     */
    public final static String P_INTERN_NAMES = "org.codehaus.stax2.internNames";

    /**
     * Whether namespace URIs parsed should be interned or not (or when
     * querying an instance, whether the instance will guarantee that
     * the URIs will be intern()ed).
     * Interning can make access by fully-qualified name faster as well
     * as save memory, but it can also add
     * some overhead when encountering a namespace URI for the first
     * time.
     */
    public final static String P_INTERN_NS_URIS = "org.codehaus.stax2.internNsUris";

    /**
     * Property that determines whether stream reader instances are required
     * to try to keep track of the parser Location in the input documents.
     *<p>
     * When turned on, the stream reader should try to do its best to keep
     * track of the locations, to be able to properly create
     * <code>XMLEvent</code> objects with accurate Location information.
     * Similarly, implementation should keep track of the location for
     * error reporting purposes, and include this information within
     * <code>XMLStreamException</code> instances.
     *<p>
     * When turned off, implementations are allowed to optimize things,
     * and only keep/pass partial Location information, or even none at
     * all. Implementations are encouraged to keep some location information
     * for error reporting purposes, even if they do not accurately maintain
     * <code>XMLEvent</code> locations, or exact byte/character offsets.
     */
    public final static String P_PRESERVE_LOCATION = "org.codehaus.stax2.preserveLocation";
 
    /*
    ////////////////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////////////////
     */

    protected XMLInputFactory2() {
        super();
    }


    // // // New event reader creation methods:

    /**
     * Factory method that allows for parsing a document accessible via
     * specified URL. Note that URL may refer to all normal URL accessible
     * resources, from files to web- and ftp-accessible documents.
     */
    public abstract XMLEventReader2 createXMLEventReader(URL src)
        throws XMLStreamException;

    /**
     * Convenience factory method that allows for parsing a document
     * stored in the specified file.
     */
    public abstract XMLEventReader2 createXMLEventReader(File f)
        throws XMLStreamException;

    // // // New stream reader creation methods:

    /**
     * Factory method that allows for parsing a document accessible via
     * specified URL. Note that URL may refer to all normal URL accessible
     * resources, from files to web- and ftp-accessible documents.
     */
    public abstract XMLStreamReader2 createXMLStreamReader(URL src)
        throws XMLStreamException;

    /**
     * Convenience factory method that allows for parsing a document
     * stored in the specified file.
     */
    public abstract XMLStreamReader2 createXMLStreamReader(File f)
        throws XMLStreamException;
 
    /*
    ////////////////////////////////////////////////////
    // Configuring using profiles
    ////////////////////////////////////////////////////
     */

    /**
     * Method to call to make Reader created conform as closely to XML
     * standard as possible, doing all checks and transformations mandated
     * (linefeed conversions, attr value normalizations). Note that this
     * does NOT include enabling DTD validation.
     *<p>
     * Currently does following changes to settings:
     *<ul>
     * <li>Enables all XML mandated transformations: will convert linefeeds
     *   to canonical LF, will convert white space in attributes as per
     *   specs
     *  <li>
     *</ul>
     *<p>
     * Notes: Does NOT change
     *<ul>
     *  <li>DTD-settings (validation, enabling)
     * </li>
     *  <li>namespace settings
     * </li>
     *  <li>entity handling
     * </li>
     *  <li>'performance' settings (buffer sizes, DTD caching, coalescing,
     *    interning, accurate location info).
     * </li>
     *</ul>
     */
    //public abstract void configureForXmlConformance();

    /**
     * Method to call to make Reader created be as "convenient" to use
     * as possible; ie try to avoid having to deal with some of things
     * like segmented text chunks. This may incure some slight performance
     * penalties, but shouldn't affect conformance.
     *<p>
     * Currently does following changes to settings:
     *<ul>
     * <li>Enables text coalescing.
     *  <li>
     * <li>Forces all non-ignorable text events (Text, CDATA) to be reported
     *    as CHARACTERS event.
     *  <li>
     * <li>Enables automatic entity reference replacement.
     *  <li>
     * <li>Disables reporting of ignorable whitespace in prolog and epilog
     *   (outside element tree)
     *  <li>
     *</ul>
     *<p>
     * Notes: Does NOT change
     *<ul>
     *  <li>Text normalization (whether it's more convenient that linefeeds
     *    are converted or not is an open question).
     *   </li>
     *  <li>DTD-settings (validation, enabling)
     * </li>
     *  <li>Namespace settings
     * </li>
     *  <li>other 'performance' settings (buffer sizes, interning, DTD caching)
     *    than coalescing
     * </li>
     *</ul>
     */
    //public abstract void configureForConvenience();

    /**
     * Method to call to make Reader created be as fast as possible reading
     * documents, especially for long-running processes where caching is
     * likely to help. 
     * Potential trade-offs are somewhat increased memory usage
     * (full-sized input buffers), and reduced XML conformance (will not
     * do some of transformations).
     *<p>
     * Currently does following changes to settings:
     *<ul>
     * <li>Disables text coalescing, sets lowish value for min. reported
     *    text segment length.
     *  </li>
     * <li>Increases input buffer length a bit from default.
     *  </li>
     * <li>Disables text normalization (linefeeds, attribute values)
     *  </li>
     * <li>Enables all interning (ns URIs)
     *  </li>
     * <li>Enables DTD caching
     *  </li>
     *</ul>
     *
     *<p>
     * Notes: Does NOT change
     *<ul>
     *  <li>DTD-settings (validation, enabling)
     * </li>
     *  <li>Namespace settings
     * </li>
     *  <li>Entity replacement settings (automatic, support external)
     * </li>
     *</ul>
     */
    //public abstract void configureForSpeed();

    /**
     * Method to call to make Reader created minimize its memory usage.
     * This generally incurs some performance penalties, due to using
     * smaller input buffers.
     *<p>
     * Currently does following changes to settings:
     *<ul>
     * <li>Turns off coalescing, to prevent having to store long text
     *   segments in consequtive buffer; resets min. reported text segment
     *   to the default value.
     *  <li>
     * <li>Reduces buffer sizes from default
     *  <li>
     * <li>Turns off DTD-caching
     *  <li>
     *</ul>
     * Notes: Does NOT change
     *<ul>
     *  <li>Normalization (linefeed, attribute value)
     * </li>
     *  <li>DTD-settings (validation, enabling)
     * </li>
     *  <li>namespace settings
     * </li>
     *  <li>entity handling
     * </li>
     *  <li>Interning settings (may or may not affect mem usage)
     * </li>
     *</ul>
     */
    //public abstract void configureForLowMemUsage();
    
    /**
     * Method to call to make Reader try to preserve as much of input
     * formatting as possible, so that round-tripping would be as lossless
     * as possible, ie that matching writer could produce output as closely
     * matching input format as possible.
     *<p>
     * Currently does following changes to settings:
     *<ul>
     * <li>Enables reporting of ignorable whitespace in prolog and epilog
     *   (outside element tree)
     *  <li>
     * <li>Disables XML mandated transformations (linefeed, attribute values),
     *   to preserve most of original formatting.
     *  <li>
     * <li>Disables coalescing, to prevent CDATA and Text segments from getting
     *   combined.
     *  <li>
     * <li>Increases minimum report text segment length so that all original
     *    text segment chunks are reported fully
     *  <li>
     * <li>Disables automatic entity replacement, to allow for preserving
     *    such references.
     *  <li>
     * <li>Disables automatic conversion of CDATA to Text events.
     *  <li>
     *</ul>
     * Notes: Does NOT change
     *<ul>
     *  <li>DTD-settings (validation, enabling)
     * </li>
     *  <li>namespace settings (enable/disable)
     * </li>
     *  <li>Some perfomance settings: interning settings, DTD caching
     * </li>
     *  <li>Whether to preserve additional information not relevant
     *    to outputting (like CFG_PRESERVE_LOCATION).
     * </li>
     *</ul>
     */
    //public abstract void configureForRoundTripping();
}
