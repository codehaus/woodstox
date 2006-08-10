package com.ctc.wstx.api;

/**
 * Class that contains constant for property names used to configure
 * cursor and event writers produced by Wstx implementation of
 * {@link javax.xml.stream.XMLOutputFactory}.
 *<p>
 */
public final class WstxOutputProperties
{
    /**
     * Default xml version number output, if none was specified by
     * application. Version 1.0 is used 
     * to try to maximize compatibility (some older parsers
     * may barf on 1.1 and later...)
     */
    public final static String DEFAULT_XML_VERSION = "1.0";

    /**
     * If no encoding is passed, we should just default to what xml
     * in general expects (and can determine), UTF-8.
     *<p>
     * Note: you can check out bug entry [WSTX-18] for more details
     */
    public final static String DEFAULT_OUTPUT_ENCODING = "UTF-8";

    // // // Output options, simple on/off settings:

    /**
     * Whether writer should just automatically convert all calls that
     * would normally produce CDATA to produce (quoted) text.
     */
    public final static String P_OUTPUT_CDATA_AS_TEXT = "com.ctc.wstx.outputCDataAsText";

    /**
     * Whether writer should copy attributes that were initially expanded
     * using default settings ("implicit" attributes) or not.
     */
    public final static String P_COPY_DEFAULT_ATTRS = "com.ctc.wstx.copyDefaultAttrs";


    // // // Validation options:

    /**
     * Whether output classes should validate namespace/prefix mapping, ie.
     * to check that element and attribute prefixes (when passed) do have
     * existing mapping. If false, will just happily output prefixes without
     * any checking.
     */
    //public final static String P_OUTPUT_VALIDATE_NS = "com.ctc.wstx.outputValidateNS";

    /**
     * Whether output classes should do basic verification that the output
     * structure is well-formed (start and end elements match); that
     * there is one and only one root, and that there is no textual content
     * in prolog/epilog. If false, won't do any checking regarding structure.
     */
    public final static String P_OUTPUT_VALIDATE_STRUCTURE = "com.ctc.wstx.outputValidateStructure";

    /**
     * Whether output classes should do basic verification that the textual
     * content output as part of nodes should be checked for validity,
     * if there's a possibility of invalid content. Nodes that include
     * such constraints are: comment/'--', cdata/']]>',
     * proc. instr/'?>'.
     */
    public final static String P_OUTPUT_VALIDATE_CONTENT = "com.ctc.wstx.outputValidateContent";

    /**
     * Whether output classes should check uniqueness of attribute names,
     * to prevent accidental output of duplicate attributes.
     */
    public final static String P_OUTPUT_VALIDATE_ATTR = "com.ctc.wstx.outputValidateAttr";

    /**
     * Whether output classes should check validity of names, ie that they
     * only contain legal XML identifier characters.
     */
    public final static String P_OUTPUT_VALIDATE_NAMES = "com.ctc.wstx.outputValidateNames";

    /**
     * Property that further modifies handling of invalid content so
     * that if {@link #P_OUTPUT_VALIDATE_CONTENT} is enabled, instead of
     * reporting an error, writer will try to fix the problem.
     * Invalid content in this context refers  to comment
     * content with "--", CDATA with "]]>" and proc. instr data with "?>".
     * This can
     * be done for some content (CDATA, possibly comment), by splitting
     * content into separate
     * segments; but not for others (proc. instr, since that might
     * change the semantics in unintended ways).
     */
    public final static String P_OUTPUT_FIX_CONTENT = "com.ctc.wstx.outputFixContent";

}
