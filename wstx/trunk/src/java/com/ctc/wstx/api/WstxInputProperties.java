package com.ctc.wstx.api;

import javax.xml.stream.XMLResolver;

/**
 * Class that contains constant for property names used to configure
 * cursor and event readers produced by Wstx implementation of
 * {@link javax.xml.stream.XMLInputFactory}.
 *<p>
 * TODO:
 *
 * - CHECK_CHAR_VALIDITY (separate for white spaces?)
 * - CATALOG_RESOLVER? (or at least, ENABLE_CATALOGS)
 */
public final class WstxInputProperties
{
    /*
    ///////////////////////////////////////////////////////
    // Simple on/off settings:
    ///////////////////////////////////////////////////////
     */

    // // // Normalization:


    /**
     * Whether non-standard linefeeds (\r, \r\n) need to be converted
     * to standard ones (\n) or not, as per XML specs.
     *<p>
     * Turning this option
     * off may help performance when processing content that has non-standard
     * linefeeds (Mac, Windows); otherwise effect is negligible.
     */
    public final static String P_NORMALIZE_LFS = "com.ctc.wstx.normalizeLFs";

    /**
     * Whether white space in attribute values should be normalized as
     * specified by XML specs or not.
     *<p>
     * Turning this option may help performance if attributes generally
     * have non-normalized white space; otherwise effect is negligible.
     */
    public final static String P_NORMALIZE_ATTR_VALUES = "com.ctc.wstx.normalizeAttrValues";


    // // // Event type conversions/suppression

    /**
     * Whether reader will generate 'ignorable white space' events during
     * prolog and epilog; if true, will generate those events; if false,
     * will just ignore white space in these parts of the parsed document.
     *<p>
     * Turning this feature off may give slight performance improvement,
     * although usually effect should be negligible. This option is usually
     * only turned on when round-trip output should be as similar to input
     * as possible.
     */
    public final static String P_REPORT_PROLOG_WHITESPACE = "com.ctc.wstx.reportPrologWhitespace";

    /**
     * Whether cursor-based reader will ever generate CDATA events; if true,
     * CDATA events may be generated for non-coalesced CDATA sections. If
     * false, all CDATA sections are reported as CHARACTERS types. It may
     * still be possible for event methods to distinguish between underlying
     * type, but event type code will be reported as CHARACTERS.
     *<p>
     * State of property does not have any effect on performance.
     */
    public final static String P_REPORT_ALL_TEXT_AS_CHARACTERS = "com.ctc.wstx.reportAllTextAsCharacters";


    // // // Interning settings:

    /**
     * Whether namespace URIs parsed should be interned or not.
     * Interning can
     * make access by fully-qualified name faster, but it adds some overhead
     * when encountering symbol for the first time.
     */
    public final static String P_INTERN_NS_URIS = "com.ctc.wstx.internURIs";


    // // // XML character validation:

    // // !!! TBI (feature)

    /**
     * Whether readers will verify that characters in text content are fully
     * valid XML characters (not just Unicode). If true, will check
     * that they are valid (including white space); if false, will not
     * check.
     *<p>
     * Turning this option off may improve parsing performance; leaving
     * it on guarantees compatibility with XML 1.0 specs regarding character
     * validity rules.
     */
    public final static String P_VALIDATE_TEXT_CHARS = "com.ctc.wstx.validateTextChars";


    // // // Caching:

    /**
     * Whether readers will try to cache parsed external DTD subsets or not.
     */

    public final static String P_CACHE_DTDS = "com.ctc.wstx.cacheDTDs";


    // // // Enabling/disabling lazy/incomplete parsing

    public final static String P_LAZY_PARSING = "com.ctc.wstx.lazyParsing";

    /**
     * Property that determines whether Event objects created will
     * contain (accurate) {@link javax.xml.stream.Location} information or not.
     * If not, Location may be null or a fixed location (beginning of main
     * XML file).
     *<p>
     * Note, however, that the underlying parser will still keep track
     * of location information for error reporting purposes; it's only
     * Event objects that are affected.
     */
    public final static String P_PRESERVE_LOCATION = "com.ctc.wstx.preserveLocation";

    // // // Enabling/disabling support for dtd++

    /**
     * Whether the Reader will recognized DTD++ extensions when parsing
     * DTD subsets.
     */
    public final static String P_SUPPORT_DTDPP = "com.ctc.wstx.supportDTDPP";

    // // // Enabling alternate mode for parsing XML fragments instead
    // // // of full documents

    // !!! Note: following is not yet implemented in parsers !!!
    /**
     * If true, will parse XML content in looser "fragment" mode; if false
     * will expect regular fully well-formed document.
     *<p>
     * In fragment more it is not
     * necessary to have just one root element; input can have multiple
     * ones (or none). Elements will still need to be balanced properly.
     * A single xml declaration is still allowed, but only
     * in the beginning of the stream (just as in regular mode), and
     * DTD declarations are allowed at the main level (outside of elements).
     * If multiple DTDs are found, they will be used for validation (if
     * enabled) as expected, ie. affecting following main-level "root"
     * elements and their descendants.
     */
    public final static String P_FRAGMENT_MODE = "com.ctc.wstx.fragmentMode";

    /*
    ///////////////////////////////////////////////////////
    // More complex settings:
    ///////////////////////////////////////////////////////
     */

    // // // Buffer sizes;

    /**
     * Size of input buffer (in chars), to use for reading XML content
     * from input stream/reader.
     */
    public final static String P_INPUT_BUFFER_LENGTH = "com.ctc.wstx.inputBufferLength";

    /**
     * Initial/default size of output buffer used for temporarily storing
     * parsed textual content. Note that this effects size of text segments
     * returned if no coalescing is enforced. When coalescing, only
     * determines initial buffer allocation; more (or bigger) buffers are
     * allocated as needed when coalescing text.
     */
    public final static String P_TEXT_BUFFER_LENGTH = "com.ctc.wstx.textBufferLength";


    // // // Constraints on sizes of text segments parsed:


    /**
     * Property to specify shortest non-complete text segment (part of
     * CDATA section or text content) that parser is allowed to return,
     * if not required to coalesce text.
     */
    public final static String P_MIN_TEXT_SEGMENT = "com.ctc.wstx.minTextSegment";

    // // // Entity handling

    /**
     * Property of type {@link java.util.Map}, that defines explicit set of
     * internal (generic) entities that will define of override any entities
     * defined in internal or external subsets; except for the 5 pre-defined
     * entities (lt, gt, amp, apos, quot). Can be used to explicitly define
     * entites that would normally come from a DTD.
     */
    public final static String P_CUSTOM_INTERNAL_ENTITIES = "com.ctc.wstx.customInternalEntities";

    /**
     * Property of type {@link XMLResolver}, that
     * will allow overriding of default DTD and external parameter entity
     * resolution.
     */
    public final static String P_DTD_RESOLVER = "com.ctc.wstx.dtdResolver";

    /**
     * Property of type {@link XMLResolver}, that
     * will allow overriding of default external general entity
     * resolution. Note that using this property overrides settings done
     * using {@link javax.xml.stream.XMLInputFactory#RESOLVER} (and vice versa).
     */
    public final static String P_ENTITY_RESOLVER = "com.ctc.wstx.entityResolver";
    
    /**
     * Property of type {@link java.net.URL}, that will allow specifying
     * context URL to use when resolving relative references, for the
     * main-level entities (external DTD subset, references from the internal
     * DTD subset).
     */
    public final static String P_BASE_URL = "com.ctc.wstx.baseURL";
}
