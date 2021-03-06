package org.codehaus.stax2;

/**
 * This interface defines configuration properties shared by multiple
 * kinds of factories (input, output, validation) or instances produces
 * (readers, writers, validators).
 *<p>
 * Some of the properties here are same as ones earlier defined in
 * {@link javax.xml.stream.XMLInputFactory} and
 * {@link javax.xml.stream.XMLOutputFactory}, and are redeclared here
 * to emphasize the fact they are usable with broader context (esp.
 * properties that use to be only used with input factories but can
 * now be used with output or validation factories).
 */
public interface XMLStreamProperties
{
    // // // Information about implementation

    /**
     * This read-only property returns name of the implementation. It
     * can be used to determine implementation-specific feature sets,
     * in case other methods (calling <code>isPropertySupported</code>)
     * does not work adequately.
     */
    public final static String XSP_IMPLEMENTATION_NAME = "org.codehaus.stax2.implName";

    /**
     * This read-only property returns the version of the implementation,
     * and is to be used with implementation name
     * ({@link #XSP_IMPLEMENTATION_NAME}) property.
     */
    public final static String XSP_IMPLEMENTATION_VERSION = "org.codehaus.stax2.implVersion";

    /**
     * This read-only property indicates whether the implementation
     * supports xml 1.1 content; Boolean.TRUE indicates it does,
     * Boolean.FALSE that it does not.
     */
    public final static String XSP_SUPPORTS_XML11 = "org.codehaus.stax2.supportsXml11";

    // // // Re-declared properties from XMLInputFactory

    /**
     * Property that can be set to indicate that namespace information is
     * to be handled in conformance to the xml namespaces specifiation; or
     * false to indicate no namespace handling should be done.
     */
    public final static String XSP_NAMESPACE_AWARE = "javax.xml.stream.isNamespaceAware";

    /**
     * Property that can be set to specify a problem handler which will get
     * notified of non-fatal problem (validation errors in non-validating mode,
     * warnings). Its value has to be of type
     * {@link javax.xml.stream.XMLReporter}
     */
    public final static String XSP_PROBLEM_REPORTER = "javax.xml.stream.reporter";

    // // // Generic XML feature support:

    /**
     * Read/write property that can be set to change the level of xml:id
     * specification support, if the implementation implements xml:id
     * specification. Value to set should be one of
     * <code>XSP_V_XMLID_xxx</code> constants (like
     * {@link #XSP_V_XMLID_TYPING}).
     *<p>
     * Default value is implementation-specific, but recommended default
     * value is <code>XSP_V_XMLID_TYPING</code> for implementations
     * that do support Xml:id specification: those that do not, have to
     * default to <code>XSP_V_XMLID_NONE.
     * For Xml:id-enabled implementations, typing support is the most
     * logical default, since it
     * provides the intuitive behavior of xml:id functionality, as well
     * as reasonable performance (very little overhead in non-validating
     * mode; usual id checking overhead for validating mode).
     */
    public final static String XSP_SUPPORT_XMLID = "org.codehaus.stax2.supportXmlId";

    /**
     * Property value to use with property
     * {@link #XSP_SUPPORT_XMLID}.
     * Property value that indicates that no xml:id support should be
     * enabled.
     */
    public final static String XSP_V_XMLID_NONE = "disable";

    /**
     * Property value to use with property
     * {@link #XSP_SUPPORT_XMLID}.
     * Value indicates that the attribute type assignment
     * portion of xml:id should be supported (all 'xml:id' attributes
     * that are not explicitly declared should have attribute type of
     * ID), but that no xml:id specific validation is to be done.
     * Due to typing, value normalization should occur.
     * It is still possible that schema (DTD, RNG, W3C Schema) based
     * validation is done, including validation of values of xml:id
     * attributes, but only based on Schema information.
     *<p>
     * The main reason for specifying this value (as opposed to the full
     * enabling) is that there is some overhead associated with
     * validation (especially uniqueness constraints checking, which means
     * that all values of xml:id attributes in the input document have to
     * be kept in memory), whereas typing is a low overhead operation.
     */
    public final static String XSP_V_XMLID_TYPING = "xmlidTyping";

    /**
     * Property value to use with property
     * {@link #XSP_SUPPORT_XMLID}.
     * Value indicates that both the attribute type assignment
     * portion and the validation portion of xml:id should be supported.
     * This validation is independent of the actual schema (DTD, RNG,
     * W3C Schema) validation, in the sense that even if no such validation
     * is enabled, xml:id value validation will still be done if this
     * property value is used. Conversely, if schema-based validation
     * is used, both xml:id and schema validation may be done (although
     * schema validator instances may choose to ignore validity checks
     * if they know that full xml:id support is enabled).
     */
    public final static String XSP_V_XMLID_FULL = "xmlidFull";
}
