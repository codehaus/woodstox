package com.ctc.wstx.stax;

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
     * application.
     */
    public final static String DEFAULT_XML_VERSION = "1.0";

    // // // Output options, simple on/off settings:

    /**
     * Whether output classes should output (and optionally verify) namespace
     * information or not. If false, will only make use of local part (which
     * may contain colons); otherwise will consider prefix and namespace URI
     * too.
     */
    public final static String P_OUTPUT_ENABLE_NS = "com.ctc.wstx.outputEnableNS";

    /**
     * Whether output classes should output empty elements, when a start
     * element is immediately followed by matching end element, or not.
     * If true, will output empty elements; if false, will always create
     * separate end element (unless a specific method that produces empty
     * elements is called).
     */
    public final static String P_OUTPUT_EMPTY_ELEMENTS = "com.ctc.wstx.outputEmptyElements";


    // // // Output options, complex values:

    /**
     * Prefix to use for automatically created namespace prefixes.
     */
    public final static String P_OUTPUT_AUTOMATIC_NS_PREFIX = "com.ctc.wstx.outputAutomaticNsPrefix";

    // // // Validation options:

    /**
     * Whether output classes should validate namespace/prefix mapping, ie.
     * to check that element and attribute prefixes (when passed) do have
     * existing mapping. If false, will just happily output prefixes without
     * any checking.
     */
    public final static String P_OUTPUT_VALIDATE_NS = "com.ctc.wstx.outputValidateNS";

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
}

