package com.ctc.wstx.cfg;

/**
 * Constant interface that contains configuration flag used by output
 * classes internally, for presenting on/off configuration options.
 */
public interface OutputConfigFlags
{
    /**
     * Flag that indicates whether writer is namespace-aware or not; if not,
     * only local part is ever used.
     */
    final static int CFG_ENABLE_NS    =        0x0001;

    /// Flag that indicates that output class should auto-generate namespace prefixes as necessary.
    final static int CFG_AUTOMATIC_NS =        0x0002;

    /// Flag that indicates we can output 'automatic' empty elements.
    final static int CFG_AUTOMATIC_EMPTY_ELEMS =  0x0004;

    /**
     * Whether writer should just automatically convert all calls that
     * would normally produce CDATA to produce (quoted) text.
     */
    final static int CFG_OUTPUT_CDATA_AS_TEXT = 0x0008;

    /**
     * Flag that indicates whether attributes expanded from default attribute
     * values should be copied to output, when using copy methods.
     */
    final static int CFG_COPY_DEFAULT_ATTRS =  0x0010;

    /**
     * Flag that indicates whether CR (\r, ascii 13) characters occuring
     * in text (CHARACTERS) and attribute values should be escaped using
     * character entities or not. Escaping is needed to enable seamless
     * round-tripping (preserving CR characters).
     */
    final static int CFG_ESCAPE_CR =  0x0020;

    /// Flag that indicates we should check validity of namespace/prefix mappings.
    //final static int CFG_VALIDATE_NS = ;

    /// Flag that indicates we should check validity of output XML structure.
    final static int CFG_VALIDATE_STRUCTURE =  0x0100;

    /**
     * Flag that indicates we should check validity of textual content of
     * nodes that have constraints.
     *<p>
     * Specifically: comments can not have '--', CDATA sections can not
     * have ']]>' and processing instruction can not have '?&lt;' character
     * combinations in content passed in.
     */
    final static int CFG_VALIDATE_CONTENT =    0x0200;

    /**
     * Flag that indicates we should check validity of names (element and
     * attribute names and prefixes; processing instruction names), that they
     * contain only legal identifier characters.
     */
    final static int CFG_VALIDATE_NAMES = 0x0400;

    /**
     * Flag that indicates we should check uniqueness of attribute names,
     * to prevent accidental output of duplicate attributes.
     */
    final static int CFG_VALIDATE_ATTR = 0x0800;

    /**
     * Flag that will enable writer that checks for validity of content
     * to try to fix the problem, by splitting output segments as
     * necessary. If disabled, validation will throw an exception; and
     * without validation no problem is noticed by writer (but instead
     * invalid output is created).
     */
    final static int CFG_FIX_CONTENT =   0x1000;

}
