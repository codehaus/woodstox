package org.codehaus.stax2.validation;

import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.XMLStreamWriter2;

/**
 * Defines the API that validator schema instances have to implement. Schema
 * objects are results of parsing of input that defines validation rules;
 * things like DTD files, W3c Schema input documents and so on. Schema
 * instances can not be directly used for validation; they are blueprints
 * for constructing such validator Objects. Because of this, they are
 * also guaranteed to be thread-safe and reusable. One way to think of this
 * is that schemas are actual validator factories instead of
 * {@link XMLValidatorFactory} instances.
 *<p>
 * One note about creation of validator instances: due to potential differences
 * in validation in input vs. output modes, there are separate factory
 * methods for both sides. Actual instances created may or may not be
 * different.
 */
public interface XMLValidatorSchema
{
    public XMLValidator createValidator(XMLStreamReader2 reader)
        throws XMLStreamException;

    public XMLValidator createValidator(XMLStreamWriter2 reader)
        throws XMLStreamException;

    /*
    ///////////////////////////////////////////////////
    // Configuration, properties
    ///////////////////////////////////////////////////
     */

    /**
     * Returns type of this schema.
     *
     * @return One of external schema identifier values from
     *   {@link XMLValidatorFactory} (such as
     *   {@link XMLValidatorFactory#SCHEMA_ID_DTD}).
     */
    public String getSchemaType();
}
