package com.ctc.wstx.cfg;

/**
 * Constant interface that contains configuration flag used by parser
 * and parser factory, as well as some other input constants.
 */
public interface InputConfigFlags
{
    /*
    //////////////////////////////////////////////////////
    // Flags for standard StAX features:
    //////////////////////////////////////////////////////
     */

    // // // Namespace handling:

    /**
     * If true, parser will handle namespaces according to XML specs; if
     * false, will only pass them as part of element/attribute name value
     * information.
     */
    final static int CFG_NAMESPACE_AWARE =  0x0001;


    // // // Text normalization


    /// Flag that indicates iterator should coalesce all text segments.
    final static int CFG_COALESCE_TEXT  =   0x0002;


    // // // Entity handling

    /**
     * Flag that enables automatic replacement of internal entities
     */
    final static int CFG_REPLACE_ENTITY_REFS = 0x0004;

    /**
     * ???.
     *<p>
     *What does this option really mean?
     */
    final static int CFG_SUPPORT_EXTERNAL_ENTITIES = 0x0008;

    // // // DTD handling

    /**
     * Whether DTD handling is enabled or disabled; disabling means both
     * internal and external subsets will just be skipped unprocessed.
     */
    final static int CFG_SUPPORT_DTD = 0x0010;

    /**
     * Not yet (fully) supported; added as the placeholder
     */
    final static int CFG_VALIDATE_AGAINST_DTD = 0x0020;

    // // Note: can add 2 more 'standard' flags here... 

    /*
    //////////////////////////////////////////////////////
    // Wstx Flags for extended features
    //////////////////////////////////////////////////////
     */

    /**
     * If true, will convert all 'alien' linefeeds (\r\n, \r) to
     * standard linefeed char (\n), in content like text, CDATA,
     * processing instructions and comments. If false, will leave
     * linefeeds as they were.
     *<p>
     * Note: not normalizing linefeeds is against XML 1.0 specs
     */
    final static int CFG_NORMALIZE_LFS  =   0x0100;

    /**
     * If true, will do attribute value normalization as explained in
     * XML specs; if false, will leave values as they are in input (including
     * not converting linefeeds).
     *<p>
     * Note: not normalizing attribute values is against XML 1.0 specs
     */
    final static int CFG_NORMALIZE_ATTR_VALUES = 0x0200;



    // // // String interning:

    /**
     * It true, will call intern() on all namespace URIs parsed; otherwise
     * will just use 'regular' Strings created from parsed contents. Interning
     * makes namespace-based access faster, but has initial overhead of
     * intern() call.
     */
    final static int CFG_INTERN_NS_URIS = 0x0400;


    // // // Type conversions:


    /**
     * If true, parser will report CDATA and SPACE events as CHARACTERS,
     * independent of coalescing settings.
     */
    final static int CFG_REPORT_ALL_TEXT_AS_CHARACTERS = 0x0800;

    /**
     * If true, parser will report (ignorable) white space events in prolog
     * and epilog; if false, it will silently ignore them.
     */
    final static int CFG_REPORT_PROLOG_WS = 0x1000;


    // // // XML character class validation


    /**
     * If true, will check that all characters in textual content of
     * the document (content that is not part of markup; including content
     * in CDATA, comments and processing instructions) are valid XML (1.1)
     * characters.
     * If false, will accept all Unicode characters outside of ones signalling
     * markup in the context.
     *<p>
     * !!! TBI.
     */
    final static int CFG_VALIDATE_TEXT_CHARS =   0x2000;


    // // // Caching

    /**
     * If true, input factory is allowed cache parsed external DTD subsets,
     * potentially speeding up things for which DTDs are needed for: entity
     * substitution, attribute defaulting, and of course DTD-based validation.
     */
    final static int CFG_CACHE_DTDS = 0x4000;

    // // // Lazy parsing

    /**
     * If true, input factory can defer parsing of nodes until data is
     * actually needed; if false, it has to read all the data in right
     * away when next type is requested. Setting it to true is good for
     * performance, in the cases where some of the nodes (like comments,
     * processing instructions, or whole subtrees) are ignored. Otherwise
     * setting will not make much of a difference. Downside is that error
     * reporting is also done 'lazily'; not right away when getting the next
     * even type but when either accessing data, or skipping it.
     */
    final static int CFG_LAZY_PARSING = 0x8000;

    // // // DTD++ support

    /**
     * If true, DTD-parser will recognize DTD++ features, and the validator
     * will also use any such information found from DTD when DTD validation
     * is enabled.
     */
    final static int CFG_SUPPORT_DTDPP = 0x00010000;

    /*
    //////////////////////////////////////////////////////
    // Other constants
    //////////////////////////////////////////////////////
     */

    // // // Constants related to (DTD-based) validation:

    final static int CONTENT_ALLOW_NONE = 0;
    final static int CONTENT_ALLOW_NON_MIXED = 1;

    /**
     * Content allowed to 'any' content specification type from DTD.
     * This is NOT the same as more generic 'whatever' implied by
     * non-validating parser, now even 'mixed' content spec. According
     * to XML specs it's actually bit more limiting... 
     */
    final static int CONTENT_ALLOW_DTD_ANY = 2;
    final static int CONTENT_ALLOW_MIXED = 3;

    /**
     * Dummy marker type; used to mark 'undefined' element entries;
     * entries created as placeholders.
     */
    final static int CONTENT_ALLOW_UNDEFINED = 4;

}

