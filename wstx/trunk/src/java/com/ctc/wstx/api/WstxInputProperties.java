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
    /**
     * Constants used when no DTD handling is done, and we do not know the
     * 'real' type of an attribute. Seems like CDATA is the safe choice.
     */
    public final static String UNKNOWN_ATTR_TYPE = "CDATA";

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


    // // // Enabling/disabling support for dtd++

    /**
     * Whether the Reader will recognized DTD++ extensions when parsing
     * DTD subsets.
     *<p>
     * Note: not implemented as of 2.0.x
     */
    public final static String P_SUPPORT_DTDPP = "com.ctc.wstx.supportDTDPP";

    // // // Enabling alternate mode for parsing XML fragments instead
    // // // of full documents

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
     * Property of type {@link XMLResolver}, that
     * will allow graceful handling of references to undeclared (general)
     * entities.
     */
    public final static String P_UNDECLARED_ENTITY_RESOLVER = "com.ctc.wstx.undeclaredEntityResolver";

    /**
     * Property of type {@link java.net.URL}, that will allow specifying
     * context URL to use when resolving relative references, for the
     * main-level entities (external DTD subset, references from the internal
     * DTD subset).
     */
    public final static String P_BASE_URL = "com.ctc.wstx.baseURL";

    // // // Alternate parsing modes

    /**
     * Three-valued property (one of
     * {@link #PARSING_MODE_DOCUMENT},
     * {@link #PARSING_MODE_FRAGMENT} or
     * {@link #PARSING_MODE_DOCUMENTS}; default being the document mode)
     * that can be used to handle "non-standard" XML content. The default
     * mode (<code>PARSING_MODE_DOCUMENT</code>) allows parsing of only
     * well-formed XML documents, but the other two modes allow more lenient
     * parsing. Fragment mode allows parsing of XML content that does not
     * have a single root element (can have zero or more), nor can have
     * XML or DOCTYPE declarations: this may be useful if parsing a subset
     * of a full XML document. Multi-document
     * (<code>PARSING_MODE_DOCUMENTS</code>) mode on the other hand allows
     * parsing of a stream that contains multiple consequtive well-formed
     * documents, with possibly multiple XML and DOCTYPE declarations.
     *<p>
     * The main difference from the API perspective is that in first two
     * modes, START_DOCUMENT and END_DOCUMENT are used as usual (as the first
     * and last events returned), whereas the multi-document mode can return
     * multiple pairs of these events: although it is still true that the
     * first event (one cursor points to when reader is instantiated or
     * returned by the event reader), there may be intervening pairs that
     * signal boundary between two adjacent enclosed documents.
     */
    public final static String P_INPUT_PARSING_MODE = "com.ctc.wstx.fragmentMode";

    // // // DTD defaulting, overriding

    // TO BE IMPLEMENTED:

    /**
     * If defined, the (DTD) validator instance that should be used for
     * validation, if no DOCTYPE declaration (or DTD override setting)
     * exists for the document.
     */
    public final static String P_DEFAULT_DTD =  "com.ctc.wstx.defaultDTD";

    /**
     * If defined, the (DTD) validator instance that should always be used for
     * validation, independent of any default DTD setting, or DOCTYPE
     * declaration.
     */
    public final static String P_OVERRIDE_DTD =  "com.ctc.wstx.overrideDTD";

    /*
    ////////////////////////////////////////////////////////////////////
    // Helper classes, values enumerations
    ////////////////////////////////////////////////////////////////////
     */

    public final static ParsingMode PARSING_MODE_DOCUMENT = new ParsingMode();
    public final static ParsingMode PARSING_MODE_FRAGMENT = new ParsingMode();
    public final static ParsingMode PARSING_MODE_DOCUMENTS = new ParsingMode();

    /**
     * Inner class used for creating type-safe enumerations (prior to JDK 1.5).
     */
    public final static class ParsingMode
    {
        ParsingMode() { }
    }
}
