package org.codehaus.stax2.validation;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;

/**
 * Interface that defines functionality exposed by the "owner" of the
 * content to validate (stream reader or stream writer usually) to
 * validators. Some of functionality is optional (for example, writer
 * may not have any useful location information).
 */
public interface ValidationContext
{
    /*
    //////////////////////////////////////////////////////
    // Input element stack access
    //////////////////////////////////////////////////////
     */

    /**
     * Method that can be used to access name information of the 
     * innermost (top) element in the element stack.
     *
     * @return Name of the element at the top of the current element
     *   stack, if any. During validation calls it refers to the 
     *   element being processed (start or end tag), or its parent
     *   (when processing text nodes), or null (in document prolog
     *   and epilog).
     */
    public QName getCurrentElementName();

    /**
     * Method that can be called by the validator to resolve a namespace
     * prefix of the currently active top-level element. This may be
     * necessary for things like DTD validators (which may need to
     * heuristically guess proper namespace URI of attributes, esp.
     * ones with default values).
     */
    public String getNamespaceURI(String prefix);

    /*
    //////////////////////////////////////////////////////
    // Location information, error reporting
    //////////////////////////////////////////////////////
     */

    /**
     * Method that will return the location that best represents current
     * location within document to be validated, if such information
     * is available.
     *<p>
     * Note: it is likely that even when a location is known, it may not
     * be very accurate; for example, when attributes are validated, it
     * is possible that they all would point to a single location that
     * may point to the start of the element that contains attributes.
     */
    public Location getValidationLocation();

    /*
    //////////////////////////////////////////////////////
    // Infoset modifiers
    //////////////////////////////////////////////////////
     */

    /**
     * An optional method that can be used to add a new attribute value for
     * an attribute
     * that was not yet contained by the container, as part of using attribute
     * default value mechanism. Optional here means that it is possible that
     * no operation is actually done by the context object. This would be
     * the case, for example, when validation is done on the writer side:
     * since default attributes are implied by a DTD, they should not be
     * added to the output.
     *<p>
     * Note: caller has to ensure that the addition would not introduce a
     * duplicate; attribute container implementation is not required to do
     * any validation on attribute name (local name, prefix, uri) or value.
     *
     * @return Index of the newly added attribute, if operation was
     *    succesful; -1 if not.
     */
    public int addDefaultAttribute(String localName, String uri, String prefix,
                                   String value);
}
