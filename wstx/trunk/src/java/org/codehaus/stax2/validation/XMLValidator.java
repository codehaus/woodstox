package org.codehaus.stax2.validation;

/**
 * Class that defines interface that individual (possibly) stateful validator
 * instances have to implement, and that both
 * {@link javax.xml.stream.XMLStreamReader} and
 * {@link javax.xml.stream.XMLStreamWriter} instances can call to validate
 * xml documents.
 *<p>
 * Validator instances are created from and by non-stateful
 * {@link XMLValidatorSchema} instances. A new validator instance has to
 * be created for each document read or written, ie. can not be shared
 * or reused, unlike schema instances which can be.
 */
public abstract class XMLValidator
{
    // // // Shared constants

    /* First, constants used by validators to indicate kind of pre-validation
     * (with respect to text, and in some cases, other non-element events)
     * caller needs to take, before calling the validator. The idea is to
     * allow stream readers and writers to do parts of validity checks they
     * are in best position to do, while leaving the real structural and
     * content-based validation to validators.
     */

    /**
     * This value indicates that no content whatsoever
     * is legal within current context, ie. where the only legal content
     * to follow is the closing end tag -- not even comments or processing
     * instructions are allowed.  This is the case for example for
     * elements that DTD defines to have EMPTY content model.
     *<p>
     */
    public final static int CONTENT_ALLOW_NONE = 0;

    /**
     * This value indicates that only white space text content is allowed,
     * not other kinds of text. Other events may be allowed; validator will
     * deal with element validation.
     */
    public final static int CONTENT_ALLOW_WS = 1;

    /**
     * This value indicates that textual content is allowed, but that
     * the validator needs to be called to let it do actual content-based
     * validation. Other event types are ok, and elements will need to be
     * validated by the validator as well.
     */
    public final static int CONTENT_ALLOW_VALIDATABLE_TEXT = 2;

    /**
     * This value indicates that any textual content (plain PCTEXT) is
     * allowed, and that validator is not going to do any validation
     * for it. It will, however, need to be called with respect
     * to element events.
     */
    public final static int CONTENT_ALLOW_ANY_TEXT = 3;

    /**
     * This value is a placeholder that should never be returned by
     * validators, but that can be used internally as an uninitialized
     * value.
     */
    public final static int CONTENT_ALLOW_UNDEFINED = 5;

    /*
    ///////////////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////////////
     */

    protected XMLValidator() { }

    /*
    ///////////////////////////////////////////////////
    // Actual validation interface
    ///////////////////////////////////////////////////
     */

}
