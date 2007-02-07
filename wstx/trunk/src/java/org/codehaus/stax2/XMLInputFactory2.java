package org.codehaus.stax2;

import java.io.File;
import java.net.URL;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Extension of {@link XMLInputFactory} that adds some convenience factory
 * methods as new standard properties that conforming stream
 * reader factory and instance implementations need to
 * recognize, and preferably support. There are also some profile-based
 * configuration methods which allow implementations to set proper goal-based
 * values for custom properties.
 *<br />
 * NOTE: although actual values for the property names are
 * visible, implementations should try to use the symbolic constants
 * defined here instead, to avoid typos.
 */
public abstract class XMLInputFactory2
    extends XMLInputFactory
    implements XMLStreamProperties
{
    /*
    ////////////////////////////////////////////////////
    // We share some options with other factories
    ////////////////////////////////////////////////////
     */

    //public final static String XSP_IMPLEMENTATION_NAME
    //public final static String XSP_IMPLEMENTATION_VERSION
    //public final static String XSP_SUPPORTS_XML11
    //public final static String XSP_NAMESPACE_AWARE
    //public final static String XSP_PROBLEM_REPORTER

    //public final static String XSP_SUPPORT_XMLID

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
     *<p>
     * Default value for this setting is implementation dependant.
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
     *<p>
     * Default value for this setting is implementation dependant.
     */
    public final static String P_REPORT_CDATA = "http://java.sun.com/xml/stream/properties/report-cdata-event";


    // // // Optimization settings
 
    /**
     * Whether name symbols (element, attribute, entity and notation names,
     * namespace prefixes) should be interned or not (or when
     * querying an instance, whether the instance will guarantee that
     * the names will be intern()ed).
     * Interning generally makes access faster (both internal and externally),
     * and saves memory, but may add some overhead for processing.
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
     *<p>
     * Default value for this setting is implementation dependant.
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
     * all. Implementations are still encouraged to keep some location
     * information for error reporting purposes, even if they do not
     * maintain accurate
     * <code>XMLEvent</code> locations, or exact byte/character offsets.
     *<p>
     * Default value for this setting is true.
     */
    public final static String P_PRESERVE_LOCATION = "org.codehaus.stax2.preserveLocation";
 
    // // // Input source settings

    /**
     * Whether stream reader is to close the underlying input source (input
     * stream, reader) when stream reader is closed. Basic StAX2
     * specification mandates this feature to be set to false by default
     * (for sources that are passed by the application).
     *<p>
     * Note: if set to true, readers are also allowed (but not required) to
     * close the underlying input source when they do not need it any more,
     * for example when encountering EOF, or when throwing an unrecoverable
     * parsing exception
     */
    public final static String P_AUTO_CLOSE_INPUT = "org.codehaus.stax2.closeInputSource";

    // // // Validation settings

    /**
     * Feature used to specify the source for DTD external subset to use
     * instead of DTD specified by the XML document itself (if any).
     *<p>
     * Note: uses name used earlier for a feature (that could be used with
     * XMLStreamReader).
     */
    public final static String P_DTD_OVERRIDE = "org.codehaus.stax2.propDtdOverride";

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
     * by the XML specification (linefeed conversions, attr value
     * normalizations).
     *<p>
     * Regarding the default StAX property settings,
     * implementations are suggested to do following:
     *<ul>
     * <li>Enable <code>SUPPORT_DTD</code> property.
     *  </li>
     * <li>Enable <code>IS_NAMESPACE_AWARE</code>
     *  </li>
     * <li>Enable <code>IS_REPLACING_ENTITY_REFERENCES</code>
     *  </li>
     * <li>Enable <code>IS_SUPPORTING_EXTERNAL_ENTITIES</code>
     *  </li>
     *</ul>
     * All the other standard settings should be left as is.
     *<p>
     * In addition, implementations should set implementation-dependant
     * settings appropriately, to be as strict as possible with regards
     * to XML specification mandated checks and transformations.
     */
    public abstract void configureForXmlConformance();

    /**
     * Method to call to make Reader created be as "convenient" to use
     * as possible; ie try to avoid having to deal with some of things
     * like segmented text chunks. This may incur some slight performance
     * penalties, but should not affect XML conformance.
     *<p>
     * Regarding the default StAX property settings,
     * implementations are suggested to do following:
     *<ul>
     * <li>Enable <code>IS_COALESCING</code> (text coalescing)
     *  </li>
     * <li>Enable <code>IS_REPLACING_ENTITY_REFERENCES</code>
     *  </li>
     * <li>Disable <code>P_REPORT_PROLOG_WHITESPACE</code> (StAX2); so
     *   that the application need not skip possible <code>SPACE</code>
     *   (and <code>COMMENT</code>, <code>PROCESSING_INSTRUCTION</code>)
     *   events.
     *  </li>
     * <li>Enable <code>P_REPORT_ALL_TEXT_AS_CHARACTERS</code> (StAX2)
     *  </li>
     * <li>Enable <code>P_PRESERVE_LOCATION</code> (StAX2)
     *  </li>
     *</ul>
     * All the other standard settings should be left as is.
     *<p>
     * In addition, implementations should set implementation-dependant
     * settings appropriately as well.
     */
    public abstract void configureForConvenience();

    /**
     * Method to call to make the Reader created be as fast as possible reading
     * documents, especially for long-running processes where caching is
     * likely to help. This means reducing amount of information collected
     * (ignorable white space in prolog/epilog, accurate Location information
     * for Event API), and possibly even including simplifying handling
     * of XML-specified transformations (skip attribute value and text
     * linefeed normalization).
     * Potential downsides are somewhat increased memory usage
     * (for full-sized input buffers), and reduced XML conformance (will not
     * do some of transformations).
     *<p>
     * Regarding the default StAX property settings,
     * implementations are suggested to do following:
     *<ul>
     * <li>Disable <code>IS_COALESCING</code> (text coalescing)
     *  </li>
     * <li>Disable <code>P_PRESERVE_LOCATION</code> (StAX2)
     *  </li>
     * <li>Disable <code>P_REPORT_PROLOG_WHITESPACE</code> (StAX2)
     *  </li>
     * <li>Enable <code>P_INTERN_NAMES</code> (StAX2)
     *  </li>
     * <li>Enable <code>P_INTERN_NS_URIS</code> (StAX2)
     *  </li>
     *</ul>
     * All the other standard settings should be left as is.
     *<p>
     * In addition, implementations should set implementation-dependant
     * settings appropriately as well.
     */
    public abstract void configureForSpeed();

    /**
     * Method to call to minimize the memory usage of the stream/event reader;
     * both regarding Objects created, and the temporary memory usage during
     * parsing.
     * This generally incurs some performance penalties, due to using
     * smaller input buffers.
     *<p>
     * Regarding the default StAX property settings,
     * implementations are suggested to do following:
     *<ul>
     * <li>Disable <code>IS_COALECING</code> (text coalescing, can force
     *   longer internal result buffers to be used)
     *  </li>
     * <li>Disable <code>P_PRESERVE_LOCATION</code> (StAX) to reduce
     *   temporary memory usage.
     *  </li>
     *</ul>
     * All the other standard settings should be left as is.
     *<p>
     * In addition, implementations should set implementation-dependant
     * settings appropriately so that the memory usage is minimized.
     */
    public abstract void configureForLowMemUsage();
    
    /**
     * Method to call to make Reader try to preserve as much of input
     * formatting as possible, so that round-tripping would be as lossless
     * as possible. This means that the matching writer should be able to
     * reproduce output as closely matching input format as possible
     * (most implementations won't be able to provide 100% vis-a-vis; 
     * white space between attributes is generally lost, as well as use
     * of character entities).
     *<p>
     * Regarding the default StAX property settings,
     * implementations are suggested to do following:
     *<ul>
     * <li>Disable <code>IS_COALESCING</code> (to prevent CDATA and Text
     *   segments from getting combined)
     *  <li>
     * <li>Disable <code>IS_REPLACING_ENTITY_REFERENCES</code> to allow for
     *    preserving explicitly declared general entity references (that is,
     *    there is no way to preserve character entities, or pre-defined
     *    entities like 'gt', 'lt', 'amp', 'apos' and 'quot').
     *  <li>
     * <li>Disable <code>P_REPORT_ALL_TEXT_AS_CHARACTERS</code> (StAX2)
     *   (so that CDATA sections are not reported as 'normal' text)
     *  <li>
     * <li>Enable <code>P_REPORT_PROLOG_WHITESPACE</code> (StAX2)
     *  </li>
     *</ul>
     * All the other standard settings should be left as is.
     *<p>
     * In addition, implementations should set implementation-dependant
     * settings appropriately as well.
     */
    public abstract void configureForRoundTripping();
}
