package org.codehaus.stax2.util;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;

import org.codehaus.stax2.AttributeInfo;
import org.codehaus.stax2.DTDInfo;
import org.codehaus.stax2.LocationInfo;
import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.typed.TypedArrayDecoder;
import org.codehaus.stax2.typed.TypedValueDecoder;
import org.codehaus.stax2.validation.ValidationProblemHandler;
import org.codehaus.stax2.validation.XMLValidationSchema;
import org.codehaus.stax2.validation.XMLValidator;

public class StreamReader2Delegate
    extends StreamReaderDelegate
    implements XMLStreamReader2
{
    protected XMLStreamReader2 mDelegate2;

    /*
    //////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////
     */

    public StreamReader2Delegate(XMLStreamReader2 sr)
    {
        super(sr);
        mDelegate2 = sr;
    }

    // @Override
    public void setParent(XMLStreamReader pr)
    {
        super.setParent(pr);
        mDelegate2 = (XMLStreamReader2) pr;
    }

    /*
    //////////////////////////////////////////////
    // XMLStreamReader2 implementation
    //////////////////////////////////////////////
     */

    public void closeCompletely() throws XMLStreamException {
        mDelegate2.closeCompletely();
    }

    public AttributeInfo getAttributeInfo() throws XMLStreamException {
        return mDelegate2.getAttributeInfo();
    }

    public DTDInfo getDTDInfo() throws XMLStreamException {
        return mDelegate2.getDTDInfo();
    }

    public int getDepth() {
        return mDelegate2.getDepth();
    }

    public Object getFeature(String name) {
        return mDelegate2.getFeature(name);
    }

    public LocationInfo getLocationInfo() {
        return mDelegate2.getLocationInfo();
    }

    public NamespaceContext getNonTransientNamespaceContext() {
        return mDelegate2.getNonTransientNamespaceContext();
    }

    public String getPrefixedName() {
        return mDelegate2.getPrefixedName();
    }

    public int getText(Writer w, boolean preserveContents)
        throws IOException, XMLStreamException
    {
        return mDelegate2.getText(w, preserveContents);
    }

    public boolean isEmptyElement()
        throws XMLStreamException
    {
        return mDelegate2.isEmptyElement();
    }

    public boolean isPropertySupported(String name) {
        return mDelegate2.isPropertySupported(name);
    }

    public void setFeature(String name, Object value) {
        mDelegate2.setFeature(name, value);
    }

    public boolean setProperty(String name, Object value) {
        return mDelegate2.setProperty(name, value);
    }

    public void skipElement() throws XMLStreamException {
        mDelegate2.skipElement();
    }

    public void getAttributeAs(int index, TypedValueDecoder tvd)
        throws XMLStreamException
    {
        mDelegate2.getAttributeAs(index, tvd);
    }

    public void getAttributeAsArray(TypedArrayDecoder tad)
        throws XMLStreamException
    {
        mDelegate2.getAttributeAsArray(tad);
    }

    public boolean getAttributeAsBoolean(int index) throws XMLStreamException
    {
        return mDelegate2.getAttributeAsBoolean(index);
    }

    public BigDecimal getAttributeAsDecimal(int index)
        throws XMLStreamException
    {
        return mDelegate2.getAttributeAsDecimal(index);
    }

    public double getAttributeAsDouble(int index) throws XMLStreamException {
        return mDelegate2.getAttributeAsDouble(index);
    }

    public float getAttributeAsFloat(int index) throws XMLStreamException {
        return mDelegate2.getAttributeAsFloat(index);
    }

    public int getAttributeAsInt(int index) throws XMLStreamException {
        return mDelegate2.getAttributeAsInt(index);
    }

    public int[] getAttributeAsIntArray(int index) throws XMLStreamException {
        return mDelegate2.getAttributeAsIntArray(index);
    }

    public BigInteger getAttributeAsInteger(int index)
        throws XMLStreamException
    {
        return mDelegate2.getAttributeAsInteger(index);
    }

    public long getAttributeAsLong(int index) throws XMLStreamException {
        return mDelegate2.getAttributeAsLong(index);
    }

    public QName getAttributeAsQName(int index) throws XMLStreamException {
        return mDelegate2.getAttributeAsQName(index);
    }

    public int getAttributeIndex(String namespaceURI, String localName) {
        return mDelegate2.getAttributeIndex(namespaceURI, localName);
    }

    public void getElementAs(TypedValueDecoder tvd) throws XMLStreamException {
        mDelegate2.getElementAs(tvd);
    }

    public boolean getElementAsBoolean() throws XMLStreamException {
        return mDelegate2.getElementAsBoolean();
    }

    public BigDecimal getElementAsDecimal() throws XMLStreamException {
        return mDelegate2.getElementAsDecimal();
    }

    public double getElementAsDouble() throws XMLStreamException {
        return mDelegate2.getElementAsDouble();
    }

    public float getElementAsFloat() throws XMLStreamException {
        return mDelegate2.getElementAsFloat();
    }

    public int getElementAsInt() throws XMLStreamException {
        return mDelegate2.getElementAsInt();
    }

    public BigInteger getElementAsInteger() throws XMLStreamException {
        return mDelegate2.getElementAsInteger();
    }

    public long getElementAsLong() throws XMLStreamException {
        return mDelegate2.getElementAsLong();
    }

    public QName getElementAsQName() throws XMLStreamException {
        return mDelegate2.getElementAsQName();
    }

    public int readElementAsArray(TypedArrayDecoder tad)
        throws XMLStreamException
    {
        return mDelegate2.readElementAsArray(tad);
    }

    public int readElementAsDoubleArray(double[] value, int from, int length)
        throws XMLStreamException
    {
        return mDelegate2.readElementAsDoubleArray(value, from, length);
    }

    public int readElementAsFloatArray(float[] value, int from, int length)
        throws XMLStreamException
    {
        return mDelegate2.readElementAsFloatArray(value, from, length);
    }

    public int readElementAsIntArray(int[] value, int from, int length)
        throws XMLStreamException {
        return mDelegate2.readElementAsIntArray(value, from, length);
    }

    public int readElementAsLongArray(long[] value, int from, int length)
        throws XMLStreamException
    {
        return mDelegate2.readElementAsLongArray(value, from, length);
    }

    public ValidationProblemHandler setValidationProblemHandler(ValidationProblemHandler h) {
        return mDelegate2.setValidationProblemHandler(h);
    }

    public XMLValidator stopValidatingAgainst(XMLValidationSchema schema)
        throws XMLStreamException
    {
        return mDelegate2.stopValidatingAgainst(schema);
    }

    public XMLValidator stopValidatingAgainst(XMLValidator validator)
        throws XMLStreamException
    {
        return mDelegate2.stopValidatingAgainst(validator);
    }

    public XMLValidator validateAgainst(XMLValidationSchema schema)
        throws XMLStreamException
    {
        return mDelegate2.validateAgainst(schema);
    }

}
