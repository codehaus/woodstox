package org.codehaus.stax2.typed;

import java.math.BigDecimal;
import java.math.BigInteger;
import javax.xml.namespace.QName;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * This interface provides a typed extension to 
 * {@link javax.xml.stream.XMLStreamReader}. It defines methods for
 * reading XML data and converting it into Java types.
 * 
 * @author Santiago.PericasGeertsen@sun.com
 * @author Tatu Saloranta
 */
public interface TypedXMLStreamReader {
    
    // -- Elements --------------------------------------------------
    
    /**
     * <p>Read an element content as a boolean. The lexical
     * representation of a boolean is defined by the 
     * <a href="http://www.w3.org/TR/xmlschema-2/#boolean">XML Schema
     * boolean</a> data type. Whitespace MUST be 
     * <a href="http://www.w3.org/TR/xmlschema-2/
     *   datatypes.html#rf-whiteSpace">collapsed</a>
     * according to the whiteSpace facet for the XML Schema boolean
     * data type.
     * An exception is thrown if, after whitespace is
     * collapsed, the resulting sequence of characters is not in 
     * the lexical space defined by the XML Schema boolean data type.</p>
     * 
     * <p>These are the pre and post conditions of calling this
     * method, regardless of whether an exception is thrown or not.
     * <ul>
     * <li>Precondition: the current event is START_ELEMENT.</li>
     * <li>Postcondition: the current event is the corresponding 
     *     END_ELEMENT.</li>
     * </ul>
     * </p>
     * 
     * @throws TypedXMLStreamException  If unable to convert the resulting
     *         character sequence into an XML Schema boolean value.
     */
    boolean getElementAsBoolean() throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #getElementAsBoolean()} replacing boolean by int.</p>
     */
    int getElementAsInt() throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #getElementAsBoolean()} replacing boolean by long.</p>
     */
    long getElementAsLong() throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #getElementAsBoolean()} replacing boolean by float.</p>
     */
    float getElementAsFloat() throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #getElementAsBoolean()} replacing boolean by double.</p>
     */
    double getElementAsDouble() throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #getElementAsBoolean()} replacing boolean by long.</p>
     */
    BigInteger getElementAsInteger() throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #getElementAsBoolean()} replacing boolean by decimal.</p>
     */
    BigDecimal getElementAsDecimal() throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #getElementAsBoolean()} replacing boolean by QName.</p>
     */
    QName getElementAsQName() throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     */
    XMLGregorianCalendar getElementAsCalendar() throws TypedXMLStreamException;
    
    /**
     * <p>Read an element content as a byte array. The lexical
     * representation of a byte array is defined by the 
     * <a href="http://www.w3.org/TR/xmlschema-2/#base64Binary">XML Schema
     * base64Binary</a> data type. Whitespace MUST be 
     * <a href="http://www.w3.org/TR/2004/REC-xmlschema-2-20041028/
     * datatypes.html#rf-whiteSpace">collapsed</a>
     * according to the whiteSpace facet for the XML Schema base64Binary
     * data type. An exception is thrown if, after whitespace is
     * collapsed, the resulting sequence of characters is not in 
     * the lexical space defined by the XML Schema base64Binary data type.</p>
     *
     * <p>These are the pre and post conditions of calling this
     * method:
     * <ul>
     * <li>Precondition: the current event is START_ELEMENT.</li>
     * <li>Postcondition: the current event is the corresponding 
     *     END_ELEMENT or CHARACTERS if only a portion of the 
     *     array has been copied thus far.</li>
     * </ul>
     * This method can be called multiple times until the cursor
     * is positioned at the corresponding END_ELEMENT event. Stated
     * differently, after the method is called for the first time,
     * the cursor will move and remain in the CHARACTERS position while there
     * are more bytes available for reading. If an exception is thrown,
     * the cursor will be moved to the END_ELEMENT position.
     * </p>
     *
     * @param value   The array in which to copy the bytes.
     * @param from    The index in the array from which copying starts.
     * @param length  The maximun number of bytes to copy.
     * @return        The number of bytes actually copied which must
     *                be less or equal than <code>length</code>.
     */
    int getElementAsBinary(byte[] value, int from, int length)
        throws TypedXMLStreamException;
    
    /**
     * <p>Read an element content as an int array. The lexical
     * representation of a int array is defined by the following
     * XML schema type:
     * <pre>
     *    &lt;xs:simpleType name="intArray">
     *       &lt;xs:list itemType="xs:int"/>
     *    &lt;/xs:simpleType></pre> 
     * whose lexical space is a list of space-separated ints.
     * Whitespace MUST be 
     * <a href="http://www.w3.org/TR/2004/REC-xmlschema-2-20041028/
     * datatypes.html#rf-whiteSpace">collapsed</a>
     * according to the whiteSpace facet for the <code>intArray</code>
     * type shown above. An exception is thrown if, after whitespace is
     * collapsed, the resulting sequence of characters is not in 
     * the lexical space defined by the <code>intArray</code> data 
     * type.</p>
     *
     * <p>These are the pre and post conditions of calling this
     * method:
     * <ul>
     * <li>Precondition: the current event is START_ELEMENT.</li>
     * <li>Postcondition: the current event is the corresponding 
     *     END_ELEMENT or CHARACTERS if only a portion of the 
     *     array has been copied thus far.</li>
     * </ul>
     * This method can be called multiple times until the cursor
     * is positioned at the corresponding END_ELEMENT event. Stated
     * differently, after the method is called for the first time,
     * the cursor will move and remain in the CHARACTERS position while there
     * are more bytes available for reading. If an exception is thrown,
     * the cursor will be moved to the END_ELEMENT position.
     * </p>
     *
     * @param value   The array in which to copy the ints.
     * @param from    The index in the array from which copying starts.
     * @param length  The maximun number of ints to copy.
     * @return        The number of ints actually copied which must
     *                be less or equal than <code>length</code>.
     */
    int getElementAsIntArray(int[] value, int from, int length)
        throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #getElementAsIntArray(int[], int, int)} replacing int 
     * by long.</p>
     */
    int getElementAsLongArray(long[] value, int from, int length)
        throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #getElementAsIntArray(int[], int, int)} replacing int 
     * by float.</p>
     */
    int getElementAsFloatArray(float[] value, int from, int length)
        throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #getElementAsIntArray(int[], int, int)} replacing int 
     * by double.</p>
     */
    int getElementAsDoubleArray(double[] value, int from, int length)
        throws TypedXMLStreamException;
    
    // -- Attributes ------------------------------------------------
    
    /**
     * Returns the index of the attribute whose local name is 
     * <code>localName</code> and URI is <code>namespaceURI</code>
     * or <code>-1</code> if no such attribute exists. If 
     * <code>namespaceURI</code> is <code>null</code> the namespace 
     * is not checked for equality.
     * 
     * @param namespaceURI  The attribute's namespace URI.
     * @param localName  The attribute's local name.
     * @return The attribute's index or <code>-1</code> if no
     *          such attribute exists.
     * @throws java.lang.IllegalStateException  If this is not
     *          a START_ELEMENT or ATTRIBUTE event.
     * @throws TypedXMLStreamException  If unable to convert the resulting
     *         character sequence into an XML Schema boolean value.
     */
    int getAttributeIndex(String namespaceURI, String localName);
    
   /**
     * <p>Read an attribute value as a boolean. The lexical
     * representation of a boolean is defined by the 
     * <a href="http://www.w3.org/TR/xmlschema-2/#boolean">XML Schema
     * boolean</a> data type. Whitespace MUST be 
     * <a href="http://www.w3.org/TR/xmlschema-2/
     *   datatypes.html#rf-whiteSpace">collapsed</a>
     * according to the whiteSpace facet for the XML Schema boolean
     * data type.
     * An exception is thrown if, after whitespace is
     * collapsed, the resulting sequence of characters is not in 
     * the lexical space defined by the XML Schema boolean data type.</p>
     * 
     * @param index  The attribute's index as returned by {@link
     *        #getAttributeIndex(String, String)}
     * @throws java.lang.IllegalStateException  If this is not
     *         a START_ELEMENT or ATTRIBUTE event.
     * @throws TypedXMLStreamException  If unable to convert the resulting
     *         character sequence into an XML Schema boolean value.
     */
    boolean getAttributeAsBoolean(int index) throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #getAttributeAsBoolean(int)} replacing boolean by int.</p>
     */
    int getAttributeAsInt(int index) throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #getAttributeAsBoolean(int)} replacing boolean by long.</p>
     */
    long getAttributeAsLong(int index) throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #getAttributeAsBoolean(int)} replacing boolean by float.</p>
     */
    float getAttributeAsFloat(int index) throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #getAttributeAsBoolean(int)} replacing boolean by double.</p>
     */
    double getAttributeAsDouble(int index) throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #getAttributeAsBoolean(int)} replacing boolean by integer.</p>
     */
    BigInteger getAttributeAsInteger(int index) throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #getAttributeAsBoolean(int)} replacing boolean by decimal.</p>
     */
    BigDecimal getAttributeAsDecimal(int index) throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #getAttributeAsBoolean(int)} replacing boolean by QName.</p>
     */
    QName getAttributeAsQName(int index) throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     */
    XMLGregorianCalendar getAttributeAsCalendar(int index) throws TypedXMLStreamException;
    
    /**
     * <p>Read an attribute value as a byte array. The lexical
     * representation of a byte array is defined by the 
     * <a href="http://www.w3.org/TR/xmlschema-2/#base64Binary">XML Schema
     * base64Binary</a> data type. Whitespace MUST be 
     * <a href="http://www.w3.org/TR/2004/REC-xmlschema-2-20041028/
     * datatypes.html#rf-whiteSpace">collapsed</a>
     * according to the whiteSpace facet for the XML Schema base64Binary
     * data type. An exception is thrown if, after whitespace is
     * collapsed, the resulting sequence of characters is not in 
     * the lexical space defined by the XML Schema base64Binary data type.</p>
     * 
     * @param index  The attribute's index as returned by {@link
     *        #getAttributeIndex(String, String)}.
     * @return An array of bytes with the content.
     * @throws java.lang.IllegalStateException  If this is not
     *         a START_ELEMENT or ATTRIBUTE event.
     * @throws TypedXMLStreamException  If unable to convert the resulting
     *         character sequence into an XML Schema boolean value.
     */
    byte[] getAttributeAsBinary(int index) throws TypedXMLStreamException;
    
    /**
     * <p>Read an attribute content as an int array. The lexical
     * representation of a int array is defined by the following
     * XML schema type:
     * <pre>
     *    &lt;xs:simpleType name="intArray">
     *       &lt;xs:list itemType="xs:int"/>
     *    &lt;/xs:simpleType></pre> 
     * whose lexical space is a list of space-separated ints.
     * Whitespace MUST be 
     * <a href="http://www.w3.org/TR/2004/REC-xmlschema-2-20041028/
     * datatypes.html#rf-whiteSpace">collapsed</a>
     * according to the whiteSpace facet for the <code>intArray</code>
     * type shown above. An exception is thrown if, after whitespace is
     * collapsed, the resulting sequence of characters is not in 
     * the lexical space defined by the <code>intArray</code> data 
     * type.</p>
     * 
     * @param index  The attribute's index as returned by {@link
     *        #getAttributeIndex(String, String)}.
     * @return An array of ints with the content.
     * @throws java.lang.IllegalStateException  If this is not
     *         a START_ELEMENT or ATTRIBUTE event.
     * @throws TypedXMLStreamException  If unable to convert the resulting
     *         character sequence into an XML Schema boolean value.
     */
    int[] getAttributeAsIntArray(int index) throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #getAttributeAsIntArray(int)} replacing int by long.</p>
     */
    long[] getAttributeAsLongArray(int index) throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #getAttributeAsIntArray(int)} replacing int by float.</p>
     */
    float[] getAttributeAsFloatArray(int index) throws TypedXMLStreamException;    
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #getAttributeAsIntArray(int)} replacing int by double.</p>
     */
    double[] getAttributeAsDoubleArray(int index) throws TypedXMLStreamException;    
}