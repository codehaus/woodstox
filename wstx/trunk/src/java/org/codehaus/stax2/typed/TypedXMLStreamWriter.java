package org.codehaus.stax2.typed;

import java.math.BigDecimal;
import java.math.BigInteger;
// !!! 30-Jan-2008, TSa: JDK 1.5 only -- is that ok?
//import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

/**
 * This interface provides a typed extension to 
 * {@link javax.xml.stream.XMLStreamWriter}. It defines methods for
 * writing XML data from Java types.
 *
 * @author Santiago.PericasGeertsen@sun.com
 * @author Tatu Saloranta
 */
public interface TypedXMLStreamWriter {
    
    // -- Elements --------------------------------------------------
    
    /**
     * <p>Write a boolean value to the output. The lexical
     * representation of a boolean is defined by the
     * <a href="http://www.w3.org/TR/xmlschema-2/#boolean">XML Schema
     * boolean</a> data type.
     *
     * @param value  The boolean value to write.
     */
    void writeBoolean(boolean value) throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #writeBoolean(boolean)} replacing boolean by int.</p>
     */
    void writeInt(int value) throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #writeBoolean(boolean)} replacing boolean by long.</p>
     */
    void writeLong(long value) throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #writeBoolean(boolean)} replacing boolean by float.</p>
     */
    void writeFloat(float value) throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #writeBoolean(boolean)} replacing boolean by double.</p>
     */
    void writeDouble(double value) throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #writeBoolean(boolean)} replacing boolean by integer.</p>
     */
    void writeInteger(BigInteger value) throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #writeBoolean(boolean)} replacing boolean by decimal.</p>
     */
    void writeDecimal(BigDecimal value) throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #writeBoolean(boolean)} replacing boolean by QName.</p>
     */
    void writeQName(QName value) throws TypedXMLStreamException;
    
    // !!! 30-Jan-2008, TSa: JDK 1.5 only -- is that ok?
    /**
     * <p><i>[TODO] </i>
     */
    //void writeCalendar(XMLGregorianCalendar value) throws TypedXMLStreamException;
    
    /**
     * <p>Write a byte array to the output. The lexical
     * representation of a byte array is defined by the
     * <a href="http://www.w3.org/TR/xmlschema-2/#base64Binary">XML Schema
     * base64Binary</a> data type. This method can be called 
     * multiple times to write the array in chunks.</p>
     *
     * @param value   The array from which to write the bytes.
     * @param from    The index in the array from which writing starts.
     * @param length  The number of bytes to write.
     */
    void writeBinary(byte[] value, int from, int length)
        throws TypedXMLStreamException;
    
    /**
     * <p>Write int array to the output. The lexical
     * representation of a int array is defined by the following
     * XML schema type:
     * <pre>
     *    &lt;xs:simpleType name="intArray">
     *       &lt;xs:list itemType="xs:int"/>
     *    &lt;/xs:simpleType></pre>
     * whose lexical space is a list of space-separated ints.
     * This method can be called multiple times to write the 
     * array in chunks.</p>
     *
     * @param value   The array from which to write the ints.
     * @param from    The index in the array from which writing starts.
     * @param length  The number of ints to write.
     */
    void writeIntArray(int[] value, int from, int length)
        throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #writeIntArray(int[], int, int)} replacing int 
     * by long.</p>
     */
    void writeLongArray(long[] value, int from, int length)
        throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #writeIntArray(int[], int, int)} replacing int 
     * by float.</p>
     */
    void writeFloatArray(float[] value, int from, int length)
        throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #writeIntArray(int[], int, int)} replacing int 
     * by double.</p>
     */
    void writeDoubleArray(double[] value, int from, int length)
        throws TypedXMLStreamException;
    
 
    // -- Attributes ------------------------------------------------
    
    /**
     * <p>Write a boolean attribute. The lexical representation of a 
     * boolean is defined by the
     * <a href="http://www.w3.org/TR/xmlschema-2/#boolean">XML Schema
     * boolean</a> data type.
     *
     * @param prefix  The attribute's prefix.
     * @param namespaceURI  The attribute's URI.
     * @param localName  The attribute's local name.
     * @param value  The boolean value to write.
     */
    void writeBooleanAttribute(String prefix, String namespaceURI, 
        String localName, boolean value) throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #writeBooleanAttribute(String, String, String, boolean)} 
     * replacing boolean by int.</p>
     */
    void writeIntAttribute(String prefix, String namespaceURI, 
        String localName, int value) throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #writeBooleanAttribute(String, String, String, boolean)} 
     * replacing boolean by long.</p>
     */
    void writeLongAttribute(String prefix, String namespaceURI, 
        String localName, long value) throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #writeBooleanAttribute(String, String, String, boolean)} 
     * replacing boolean by float.</p>
     */
    void writeFloatAttribute(String prefix, String namespaceURI, 
        String localName, float value) throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #writeBooleanAttribute(String, String, String, boolean)} 
     * replacing boolean by double.</p>
     */
    void writeDoubleAttribute(String prefix, String namespaceURI, 
        String localName, double value) throws TypedXMLStreamException;
 
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #writeBooleanAttribute(String, String, String, boolean)} 
     * replacing boolean by integer.</p>
     */
    void writeIntegerAttribute(String prefix, String namespaceURI, 
        String localName, BigInteger value) throws TypedXMLStreamException;

    /**
     * <p><i>[TODO] </i>
     * Same as {@link #writeBooleanAttribute(String, String, String, boolean)} 
     * replacing boolean by decimal.</p>
     */
    void writeDecimalAttribute(String prefix, String namespaceURI, 
        String localName, BigDecimal value) throws TypedXMLStreamException;

    /**
     * <p><i>[TODO] </i>
     * Same as {@link #writeBooleanAttribute(String, String, String, boolean)} 
     * replacing boolean by QName.</p>
     */
    void writeQNameAttribute(String prefix, String namespaceURI, 
        String localName, QName value) throws TypedXMLStreamException;

    // !!! 30-Jan-2008, TSa: JDK 1.5 only -- is that ok?
    /**
     * <p><i>[TODO] </i>
     */
    //void writeCalendarAttribute(String prefix, String namespaceURI, String localName, XMLGregorianCalendar value)  throws TypedXMLStreamException;
        
    /**
     * <p>Write a byte array attribute. The lexical
     * representation of a byte array is defined by the
     * <a href="http://www.w3.org/TR/xmlschema-2/#base64Binary">XML Schema
     * base64Binary</a> data type.</p>
     *
     * @param prefix  The attribute's prefix.
     * @param namespaceURI  The attribute's URI.
     * @param localName  The attribute's local name.
     * @param value   The array from which to write the bytes.
     */
    void writeBinaryAttribute(String prefix, String namespaceURI, 
        String localName, byte[] value) throws TypedXMLStreamException;
    
    /**
     * <p>Write int array attribute. The lexical
     * representation of a int array is defined by the following
     * XML schema type:
     * <pre>
     *    &lt;xs:simpleType name="intArray">
     *       &lt;xs:list itemType="xs:int"/>
     *    &lt;/xs:simpleType></pre>
     * whose lexical space is a list of space-separated ints.</p>
     *
     * @param prefix  The attribute's prefix.
     * @param namespaceURI  The attribute's URI.
     * @param localName  The attribute's local name.
     * @param value   The array from which to write the ints.
     */
    void writeIntArrayAttribute(String prefix, String namespaceURI, 
        String localName, int[] value) throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #writeIntArrayAttribute(String, String, String, int[])} 
     * replacing int by long.</p>
     */
    void writeLongArrayAttribute(String prefix, String namespaceURI, 
        String localName, long[] value) throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #writeIntArrayAttribute(String, String, String, int[])} 
     * replacing int by float.</p>
     */
    void writeFloatArrayAttribute(String prefix, String namespaceURI, 
        String localName, float[] value) throws TypedXMLStreamException;
    
    /**
     * <p><i>[TODO] </i>
     * Same as {@link #writeIntArrayAttribute(String, String, String, int[])} 
     * replacing int by double.</p>
     */
    void writeDoubleArrayAttribute(String prefix, String namespaceURI, 
        String localName, double[] value) throws TypedXMLStreamException;
}
