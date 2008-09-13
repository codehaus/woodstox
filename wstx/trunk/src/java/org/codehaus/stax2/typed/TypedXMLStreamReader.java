package org.codehaus.stax2.typed;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.math.BigDecimal;
import java.math.BigInteger;
import javax.xml.namespace.QName;
// !!! 30-Jan-2008, TSa: JDK 1.5 only -- is that ok?
//import javax.xml.datatype.XMLGregorianCalendar;

/**
 * This interface provides a typed extension to 
 * {@link javax.xml.stream.XMLStreamReader}. It defines methods for
 * reading XML data and converting it into Java types.
 *<p>
 * Implementation notes:
 *
 * - As of July 2008:
 *   * Writer-side is completed, including:
 *     - Primitives (numbers, boolean)
 *     - Other scalars (QName)
 *     - Primitive arrays
 *     - Binary (base64)
 *   * Reader-side partially completed:
 *     - Primitives
 *     - Other scalars
 *    (meaning that the rest; primitive arrays and binary; have
 *    not been implemented yet)
 * 
 * @author Santiago.PericasGeertsen@sun.com
 * @author Tatu Saloranta
 *
 * @since 3.0
 */
public interface TypedXMLStreamReader
    extends XMLStreamReader
{
    /*
    //////////////////////////////////////////////////////////
    // First, typed element accessors for scaler values
    //////////////////////////////////////////////////////////
     */
    
    /**
     * <p>Read an element content as a boolean. The lexical
     * representation of a boolean is defined by the 
     * <a href="http://www.w3.org/TR/xmlschema-2/#boolean">XML Schema
     * boolean</a> data type. Whitespace MUST be 
     * <a href="http://www.w3.org/TR/xmlschema-2/datatypes.html#rf-whiteSpace">collapsed</a>
     * according to the whiteSpace facet for the XML Schema boolean
     * data type.
     * An exception is thrown if, after whitespace is
     * collapsed, the resulting sequence of characters is not in 
     * the lexical space defined by the XML Schema boolean data type.
     * (note: allowed lexical values are canonicals "true" and
     * "false", as well as non-canonical "0" and "1")
     * </p>
     *<p>
     * These are the pre- and post-conditions of calling this method:
     * <ul>
     * <li>Precondition: the current event is START_ELEMENT.</li>
     * <li>Postcondition: the current event is the corresponding 
     *     END_ELEMENT.</li>
     * </ul>
     * </p>
     * 
     * @throws XMLStreamException  If unable to access content
     * @throws TypedXMLStreamException  If unable to convert the resulting
     *         character sequence into an XML Schema boolean value.
     */
    public boolean getElementAsBoolean() throws XMLStreamException;

    /**
     * <p>Read an element content as a 32-bit integer. The lexical
     * representation of a integer is defined by the 
     * <a href="http://www.w3.org/TR/xmlschema-2/#integer">XML Schema
     * integer</a> data type. Whitespace MUST be 
     * <a href="http://www.w3.org/TR/xmlschema-2/datatypes.html#rf-whiteSpace">collapsed</a>
     * according to the whiteSpace facet for the XML Schema integer data type.
     * An exception is thrown if, after whitespace is
     * collapsed, the resulting sequence of characters is not in 
     * the lexical space defined by the XML Schema integer data type.
     * </p><p>
     * These are the pre and post conditions of calling this method:
     * <ul>
     * <li>Precondition: the current event is START_ELEMENT.</li>
     * <li>Postcondition: the current event is the corresponding 
     *     END_ELEMENT.</li>
     * </ul>
     * </p>
     * 
     * @throws XMLStreamException  If unable to access content
     * @throws TypedXMLStreamException  If unable to convert the resulting
     *         character sequence into a Java (32-bit) integer.
     */
    public int getElementAsInt() throws XMLStreamException;
    
    /**
     * <p>Read an element content as a 64-bit integer. The lexical
     * representation of a integer is defined by the 
     * <a href="http://www.w3.org/TR/xmlschema-2/#integer">XML Schema
     * integer</a> data type. Whitespace MUST be 
     * <a href="http://www.w3.org/TR/xmlschema-2/datatypes.html#rf-whiteSpace">collapsed</a>
     * according to the whiteSpace facet for the XML Schema integer data type.
     * An exception is thrown if, after whitespace is
     * collapsed, the resulting sequence of characters is not in 
     * the lexical space defined by the XML Schema integer data type.
     * </p><p>
     * These are the pre and post conditions of calling this method:
     * <ul>
     * <li>Precondition: the current event is START_ELEMENT.</li>
     * <li>Postcondition: the current event is the corresponding 
     *     END_ELEMENT.</li>
     * </ul>
     * </p>
     * 
     * @throws XMLStreamException  If unable to access content
     * @throws TypedXMLStreamException  If unable to convert the resulting
     *         character sequence into a Java (64-bit) integer.
     */
    public long getElementAsLong() throws XMLStreamException;
    
    /**
     * <p>Read an element content as a 32-bit floating point value.
     * The lexical representation is defined by the 
     * <a href="http://www.w3.org/TR/xmlschema-2/#float">XML Schema
     * float</a> data type. Whitespace MUST be 
     * <a href="http://www.w3.org/TR/xmlschema-2/datatypes.html#rf-whiteSpace">collapsed</a>
     * according to the whiteSpace facet for the XML Schema float data type.
     * An exception is thrown if, after whitespace is
     * collapsed, the resulting sequence of characters is not in 
     * the lexical space defined by the XML Schema integer data type.
     *<br />
     * Note that valid representations include basic Java textual
     * representations, as well as 3 special tokens: "INF", "-INF"
     * and "NaN"
     * </p>
     * <p>
     * These are the pre and post conditions of calling this method:
     * <ul>
     * <li>Precondition: the current event is START_ELEMENT.</li>
     * <li>Postcondition: the current event is the corresponding 
     *     END_ELEMENT.</li>
     * </ul>
     * </p>
     * 
     * @throws XMLStreamException  If unable to access content
     * @throws TypedXMLStreamException  If unable to convert the resulting
     *         character sequence into a Java float
     */
    public float getElementAsFloat() throws XMLStreamException;
    
    /**
     * <p>Read an element content as a 64-bit floating point value.
     * The lexical representation is defined by the 
     * <a href="http://www.w3.org/TR/xmlschema-2/#double">XML Schema
     * double</a> data type. Whitespace MUST be 
     * <a href="http://www.w3.org/TR/xmlschema-2/datatypes.html#rf-whiteSpace">collapsed</a>
     * according to the whiteSpace facet for the XML Schema double data type.
     * An exception is thrown if, after whitespace is
     * collapsed, the resulting sequence of characters is not in 
     * the lexical space defined by the XML Schema integer data type.
     *<br />
     * Note that valid representations include basic Java textual
     * representations, as well as 3 special tokens: "INF", "-INF"
     * and "NaN"
     * </p><p>
     * These are the pre and post conditions of calling this method:
     * <ul>
     * <li>Precondition: the current event is START_ELEMENT.</li>
     * <li>Postcondition: the current event is the corresponding 
     *     END_ELEMENT.</li>
     * </ul>
     * </p>
     * 
     * @throws XMLStreamException  If unable to access content
     * @throws TypedXMLStreamException  If unable to convert the resulting
     *         character sequence into a Java double
     */
    public double getElementAsDouble() throws XMLStreamException;
    
    public BigInteger getElementAsInteger() throws XMLStreamException;
    
    public BigDecimal getElementAsDecimal() throws XMLStreamException;
    
    public QName getElementAsQName() throws XMLStreamException;
    
    // !!! 30-Jan-2008, TSa: JDK 1.5 only -- is that ok?
    /**
     * <p><i>[TODO] </i>
     */
    //public XMLGregorianCalendar getElementAsCalendar() throws XMLStreamException;
    
    /**
     * Generic decoding method that can be used for efficient
     * decoding of additional types not support natively
     * by the typed stream reader. When method is called,
     * stream reader will collect all textual content of
     * the current element (effectively doing something
     * similar to a call to {@link #getElementText},
     * and then call one of decode methods defined in
     * {@link TypedValueDecoder}. The only difference is that
     * passed value will be trimmed: that is, any leading or
     * trailing white space will be removed prior to calling
     * decode method.
     * After the call, passed
     * decoder object will have decoded and stored value
     * (if succesful) or thrown an exception (if not).
     *<p>
     * The main benefit of using this method (over just getting
     * all content by calling {@link #getElementText}
     * is efficiency: the stream reader can efficiently gather all textual
     * content necessary and pass it to the decoder, often avoiding
     * construction of intemediate Strings.
     *<p>
     * These are the pre- and post-conditions of calling this method:
     * <ul>
     * <li>Precondition: the current event is START_ELEMENT.</li>
     * <li>Postcondition: the current event is the corresponding 
     *     END_ELEMENT.</li>
     * </ul>
     * </p>
     *<p>
     * Note that caller has to know more specific type of decoder,
     * since the base interface does not specify methods
     * for accessing actual decoded value.
     */
    public void getElementAs(TypedValueDecoder tvd) throws XMLStreamException;

    /*
    //////////////////////////////////////////////////////////
    // Then streaming/chunked typed element accessors
    // for non-scalar (array, binary data) values
    //////////////////////////////////////////////////////////
     */

    /**
     * Read element content as decoded byte sequence; possibly only
     * reading a fragment of all element content.
     * The lexical representation of a byte array is defined by the 
     * <a href="http://www.w3.org/TR/xmlschema-2/#base64Binary">XML Schema
     * base64Binary</a> data type. Whitespace MUST be 
     * <a href="http://www.w3.org/TR/2004/REC-xmlschema-2-20041028/datatypes.html#rf-whiteSpace">collapsed</a>
     * according to the whiteSpace facet for the XML Schema base64Binary
     * data type. An exception is thrown if content is not in 
     * the lexical space defined by the XML Schema base64Binary data type.</p>
     * </p><p>
     * Each call will read at least one decoded byte (and no more than
     * the specified maximum length), if there is any content remaining.
     * If none is available and END_ELEMENT is encountered, -1 is
     * returned.
     * </p><p>
     * These are the pre and post conditions of calling this method:
     * <ul>
     * <li>Precondition: the current event is either START_ELEMENT,
     *   or a textual event (CHARACTERS, CDATA), or END_ELEMENT
     *   (END_ELEMENT is allowed for convenience; if so, no read
     *   operation is tried, and -1 is returned immediately
     *   </li>
     * <li>Postcondition: the current event is the corresponding 
     *     END_ELEMENT, if all remaining binary content was read,
     *     or CHARACTERS if only a portion of the array was read
     *   </li>
     * </ul>
     * This method can be called multiple times until the cursor
     * is positioned at the corresponding END_ELEMENT event. Stated
     * differently, after the method is called for the first time,
     * the cursor will move and remain in the CHARACTERS position while there
     * are potentially more bytes available for reading.
     *</p>
     *
     * @param value   The array in which to copy the bytes.
     * @param from    The index in the array from which copying starts.
     * @param length  The maximun number of bytes to copy.
     *
     * @return        The number of bytes actually copied, if any were
     *   available; -1 if there is no more content. If content was
     *  copied, value must be less or equal than <code>length</code>
     */
    //public int readElementAsBinary(byte[] value, int from, int length)
    //    throws XMLStreamException;
    
    /**
     * Read an element content as an int array. The lexical
     * representation of a int array is defined by the following
     * XML schema type:
     * <pre>
     *    &lt;xs:simpleType name="intArray">
     *       &lt;xs:list itemType="xs:int"/>
     *    &lt;/xs:simpleType></pre> 
     * whose lexical space is a list of space-separated ints.
     * Whitespace MUST be 
     * <a href="http://www.w3.org/TR/2004/REC-xmlschema-2-20041028/datatypes.html#rf-whiteSpace">collapsed</a>
     * according to the whiteSpace facet for the <code>intArray</code>
     * type shown above. An exception is thrown if, after whitespace is
     * collapsed, the resulting sequence of characters is not in 
     * the lexical space defined by the <code>intArray</code> data 
     * type.</p>
     *
     *<p>
     *These are the pre and post conditions of calling this
     * method:
     * <ul>
     * <li>Precondition: the current event is either START_ELEMENT,
     *   or a textual event (CHARACTERS, CDATA), or END_ELEMENT
     *   (END_ELEMENT is allowed for convenience; if so, no read
     *   operation is tried, and -1 is returned immediately
     *   </li>
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
     * @param length  The maximun number of ints to copy. Minimum value
     *   is 1; others an {@link IllegalArgumentException} is thrown
     *
     * @return        The number of ints actually copied which must
     *                be less or equal than <code>length</code>, but
     *               at least one if any ints found. If not, -1 is returned
     *   to signal end of ints to parse.
     */
    public int readElementAsIntArray(int[] value, int from, int length) throws XMLStreamException;

    public int readElementAsLongArray(long[] value, int from, int length) throws XMLStreamException;
    
    public int readElementAsFloatArray(float[] value, int from, int length) throws XMLStreamException;
    
    public int readElementAsDoubleArray(double[] value, int from, int length) throws XMLStreamException;

    /**
     * Read an element content as an array of tokens. This is done by
     * reader tokenizing textual content by white space, and sending
     * each token to specified decoder for decoding. This is repeated
     * as long as element content has more tokens and decoder can
     * accept more values.
     *<p>
     *These are the pre- and post-conditions of calling this
     * method:
     * <ul>
     * <li>Precondition: the current event is either START_ELEMENT,
     *   or a textual event (CHARACTERS, CDATA), or END_ELEMENT
     *   (END_ELEMENT is allowed for convenience; if so, no read
     *   operation is tried, and -1 is returned immediately
     *   </li>
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
     *<p>
     * Note: passed decoder must accept at least one value, reader will
     * not verify capacity before calling it with the first token.
     * 
     * @return Number of elements decoded, or -1 to indicate that there
     *    was no more element content tokens to decode.
     */
    public int readElementAsArray(TypedArrayDecoder tad) throws XMLStreamException;

    /*
    //////////////////////////////////////////////////////////
    // Then, typed attribute accessors
    //////////////////////////////////////////////////////////
     */
    
    /**
     * Returns the index of the attribute whose local name is 
     * <code>localName</code> and URI is <code>namespaceURI</code>
     * or <code>-1</code> if no such attribute exists.
     * 
     * @param namespaceURI  The attribute's namespace URI. Values of
     *   null and "" are considered the same, i.e. "no namespace"
     *   (or "empty" namespace)
     * @param localName  The attribute's local name.
     * @return The attribute's index or <code>-1</code> if no
     *          such attribute exists.
     * @throws java.lang.IllegalStateException  If this is not
     *          a START_ELEMENT event
     * @throws XMLStreamException  If unable to convert the resulting
     *         character sequence into an XML Schema boolean value.
     */
    public int getAttributeIndex(String namespaceURI, String localName);
    
   /**
     * <p>Read an attribute value as a boolean. The lexical
     * representation of a boolean is defined by the 
     * <a href="http://www.w3.org/TR/xmlschema-2/#boolean">XML Schema boolean</a>
     * data type. Whitespace MUST be 
     * <a href="http://www.w3.org/TR/xmlschema-2/datatypes.html#rf-whiteSpace">collapsed</a>
     * according to the whiteSpace facet for the XML Schema boolean
     * data type.
     * An exception is thrown if, after whitespace is
     * collapsed, the resulting sequence of characters is not in 
     * the lexical space defined by the XML Schema boolean data type.</p>
     * 
     * @param index  The attribute's index as returned by {@link #getAttributeIndex(String, String)}
     * @throws java.lang.IllegalStateException  If this is not
     *         a START_ELEMENT event.
     * @throws XMLStreamException  If unable to convert the resulting
     *         character sequence into an XML Schema boolean value.
     */
    public boolean getAttributeAsBoolean(int index) throws XMLStreamException;
    
    /**
     * <p>Read an attribute value as a boolean. The lexical
     * representation of a boolean is defined by the 
     * <a href="http://www.w3.org/TR/xmlschema-2/#integer">XML Schema integer</a>
     * data type. Whitespace MUST be 
     * <a href="http://www.w3.org/TR/xmlschema-2/datatypes.html#rf-whiteSpace">collapsed</a>
     * according to the whiteSpace facet for the type.
     * An exception is thrown if, after whitespace is
     * collapsed, the resulting sequence of characters is not in 
     * the lexical space defined by the XML Schema integer data type.</p>
     * 
     * @param index  The attribute's index as returned by {@link #getAttributeIndex(String, String)}
     * @throws java.lang.IllegalStateException If this is not a START_ELEMENT event.
     * @throws XMLStreamException  If unable to convert the resulting
     *         character sequence into an XML Schema boolean value.
     */
    public int getAttributeAsInt(int index) throws XMLStreamException;
    
    /**
     * <p>Read an attribute value as a boolean. The lexical
     * representation of a boolean is defined by the 
     * <a href="http://www.w3.org/TR/xmlschema-2/#long">XML Schema long</a>
     * data type. Whitespace MUST be 
     * <a href="http://www.w3.org/TR/xmlschema-2/datatypes.html#rf-whiteSpace">collapsed</a>
     * according to the whiteSpace facet for the type.
     * An exception is thrown if, after whitespace is
     * collapsed, the resulting sequence of characters is not in 
     * the lexical space defined by the XML Schema long data type.
     *</p>
     * @param index  The attribute's index as returned by {@link #getAttributeIndex(String, String)}
     * @throws java.lang.IllegalStateException If this is not a START_ELEMENT event.
     * @throws XMLStreamException  If unable to convert the resulting
     *         character sequence into an XML Schema boolean value.
     */
    public long getAttributeAsLong(int index) throws XMLStreamException;

    public float getAttributeAsFloat(int index) throws XMLStreamException;
    
    public double getAttributeAsDouble(int index) throws XMLStreamException;
    
    public BigInteger getAttributeAsInteger(int index) throws XMLStreamException;

    public BigDecimal getAttributeAsDecimal(int index) throws XMLStreamException;

    public QName getAttributeAsQName(int index) throws XMLStreamException;
    
    // !!! 30-Jan-2008, TSa: JDK 1.5 only -- is that ok?
    //XMLGregorianCalendar getAttributeAsCalendar(int index) throws XMLStreamException;
    
    /**
     * Generic access method that can be used for efficient
     * decoding of additional types not support natively
     * by the typed stream reader. The main benefit of using
     * this method is that the stream reader can efficient
     * gather all textual content necessary and pass it
     * to the decoder, often avoiding construction of intemediate
     * Strings.
     *<p>
     * As with {@link #getElementAs}, value passed to a decode
     * method will be trimmed of any leading or trailing white space.
     */
    public void getAttributeAs(int index, TypedValueDecoder tvd) throws XMLStreamException;

    /**
     *Read an attribute value as a byte array. The lexical
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
     * @throws XMLStreamException  If unable to convert the resulting
     *         character sequence into an XML Schema boolean value.
     */
    //public byte[] getAttributeAsBinary(int index) throws XMLStreamException;
    
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
     * @throws XMLStreamException  If unable to convert the resulting
     *         character sequence into an XML Schema boolean value.
     */
    public int[] getAttributeAsIntArray(int index) throws XMLStreamException;
    
    //public long[] getAttributeAsLongArray(int index) throws XMLStreamException;

    //public float[] getAttributeAsFloatArray(int index) throws XMLStreamException;    
    
    //public double[] getAttributeAsDoubleArray(int index) throws XMLStreamException;    

    /**
     * Method that allows reading contents of an attribute as an array
     * of whitespace-separate tokens, decoded using specified decoder.
     */
    public void getAttributeAsArray(TypedArrayDecoder tad) throws XMLStreamException;
}
