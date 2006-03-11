package org.codehaus.stax2.validation;

import javax.xml.stream.XMLStreamException;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;

/**
 * Interface that defines validation interface used by both stream readers
 * and writers.
 */
public interface Validatable
{
    /**
     * Method that will construct a {@link XMLValidator} instance from the
     * given schema (unless a validator for that schema has already been
     * added),
     * initialize it if necessary, and make validatable object (reader,
     * writer)
     * call appropriate validation methods from this point on until the
     * end of the document (that is, it's not scoped with sub-trees), or until
     * validator is removed by an explicit call to
     * {@link #stopValidatingAgainst}.
     *<p>
     * Note that while this method can be called at any point in output
     * processing, validator instances are not required to be able to handle
     * addition at other points than right before outputting the root element.
     *
     * @return Validator instance constructed, if validator was added, or null
     *   if a validator for the schema has already been constructed.
     */
    public XMLValidator validateAgainst(XMLValidationSchema schema)
        throws XMLStreamException;

    /**
     * Method that can be called by application to stop validating
     * output against a schema, for which {@link #validateAgainst}
     * was called earlier.
     *
     * @return Validator instance created from the schema that was removed,
     *   if one was in use; null if no such schema in use.
     */
    public XMLValidator stopValidatingAgainst(XMLValidationSchema schema)
        throws XMLStreamException;

    /**
     * Method that can be called by application to stop validating
     * output using specified validator. The validator passed should be
     * an earlier return value for a call to {@link #validateAgainst}.
     *<p>
     * Note: the specified validator is compared for identity with validators
     * in use, not for equality.
     *
     * @return Validator instance found (ie. argument <code>validator</code>)
     *   if it was being used for validating current document; null if not.
     */
    public XMLValidator stopValidatingAgainst(XMLValidator validator)
        throws XMLStreamException;

}
