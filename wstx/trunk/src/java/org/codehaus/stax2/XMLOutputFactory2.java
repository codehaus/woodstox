package org.codehaus.stax2;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter; // only for javadoc

/**
 * Extension of {@link javax.xml.stream.XMLInputFactory} to add missing functionality
 * (which currently means 'nothing'...)
 *<p>
 * Also contains extended standard properties that conforming stream
 * writer factory and instance implementations should at least
 * recognize, and preferably support.
 *<br />
 * NOTE: although actual values for the property names are
 * visible, implementations should try to use the symbolic constants
 * defined here instead, to avoid typos.
 */
public abstract class XMLOutputFactory2
    extends XMLOutputFactory
{ 
    /*
    ////////////////////////////////////////////////////
    // Additional standard configuration properties
    ////////////////////////////////////////////////////
     */

    // // General output options:

    /**
     * Whether stream writers are allowed to automatically output empty
     * elements, when a start element is immediately followed by matching
     * end element.
     * If true, will output empty elements; if false, will always create
     * separate end element (unless a specific method that produces empty
     * elements is called).
     *<p>
     * Default value for implementations should be 'true'; both values should
     * be recognized, and 'false' must be honored. However, 'true' value
     * is only a suggestion, and need not be implemented (since there is
     * the explicit 'writeEmptyElement()' method).
     */
    public final static String P_AUTOMATIC_EMPTY_ELEMENTS = "org.codehaus.stax2.automaticEmptyElements";


    // // Namespace options:

    /**
     * Whether output classes should keep track of and output namespace
     * information provided via write methods.
     *<p>
     * When enabled (set to Boolean.TRUE), will use all namespace information
     * provided, and does not allow colons in names (local name, prefix).
     * What exactly is kept track
     * of depends on other settings, specifically whether
     * writer is in "repairing" mode or not.
     *<p>
     * When disabled, will only make use of local name part, which
     * may contain colons, and ignore prefix and namespace URI if any
     * are passed.
     *<p>
     * Turning this option off may improve performance if no namespace
     * handling is needed.
     *<p>
     * Default value for implementations should be 'true'; implementations
     * are not required to implement 'false'.
     */
    public final static String P_NAMESPACE_AWARE = "org.codehaus.stax2.namespaceAware";

    /**
     * Prefix to use for automatically created namespace prefixes, when
     * namespace support is enabled, the writer is in "repairing" 
     * mode, and a new prefix name is needed. The value is a String,
     * and needs to be a valid namespace prefix in itself, as defined
     * by the namespace specification. Will be prepended by a trailing
     * part (often a sequence number), in order to make it unique to
     * be usable as a temporary non-colliding prefix.
     */
    public final static String P_AUTOMATIC_NS_PREFIX = "org.codehaus.stax2.automaticNsPrefix";

    // // Text/attribute value escaping options:

    /**
     * Property that can be set if a custom output escaping for textual
     * content is needed.
     * The value set needs to be of type {@link EscapingWriterFactory}.
     * When set, the factory will be used to create a per-writer
     * instance used to escape all textual content written, both
     * via explicit {@link XMLStreamWriter#writeCharacters} methods,
     * and via copy methods ({@link XMLStreamWriter2#copyEventFromReader}).
     */
    public final static String P_TEXT_ESCAPER = "org.codehaus.stax2.textEscaper";

    /**
     * Property that can be set if a custom output escaping for attribute
     * value content is needed.
     * The value set needs to be of type {@link EscapingWriterFactory}.
     * When set, the factory will be used to create a per-writer
     * instance used to escape all attribute values written, both
     * via explicit {@link XMLStreamWriter#writeAttribute} methods,
     * and via copy methods ({@link XMLStreamWriter2#copyEventFromReader}).
     */
    public final static String P_ATTR_VALUE_ESCAPER = "org.codehaus.stax2.attrValueEscaper";


    /*
    ////////////////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////////////////
     */

    protected XMLOutputFactory2() {
        super();
    }

    /*
    ////////////////////////////////////////////////////
    // New StAX2 API
    ////////////////////////////////////////////////////
     */
}
