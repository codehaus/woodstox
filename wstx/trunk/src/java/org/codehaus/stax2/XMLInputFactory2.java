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
}
