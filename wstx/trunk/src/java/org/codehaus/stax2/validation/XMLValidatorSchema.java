package org.codehaus.stax2.validation;

/**
 * Defines the API that validator schema instances have to implement. Schema
 * objects are results of parsing of input that defines validation rules;
 * things like DTD files, W3c Schema input documents and so on. Schema
 * instances can not be directly used for validation; they are blueprints
 * for constructing such validator Objects. Because of this, they are
 * also guaranteed to be thread-safe and reusable. One way to think of this
 * is that schemas are actual validator factories instead of
 * {@link XMLValidatorFactory} instances.
 */
public abstract class XMLValidatorSchema
{
    protected XMLValidatorSchema() { }
}
