package org.codehaus.stax2.ri;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.*;

import org.codehaus.stax2.*;
import org.codehaus.stax2.validation.*;
import org.codehaus.stax2.typed.TypedArrayDecoder;
import org.codehaus.stax2.typed.TypedValueDecoder;

/**
 * Simple straight-forward implementation of a filtering stream reader,
 * which can fully adapt Stax2 stream reader 
 * ({@link XMLStreamReader2}).
 */
public class Stax2FilteredStreamReader
    implements XMLStreamReader2,
               XMLStreamConstants
{
    final XMLStreamReader2 mReader;
    final StreamFilter mFilter;

    public Stax2FilteredStreamReader(XMLStreamReader r, StreamFilter f)
    {
        mReader = Stax2ReaderAdapter.wrapIfNecessary(r);
        mFilter = f;
    }

    /*
    ////////////////////////////////////////////////////
    // Basic XMLStreamReader implementation
    ////////////////////////////////////////////////////
     */

    /*
    /////////////////////////////////////////////////
    // First methods that need non-trivial handling
    /////////////////////////////////////////////////
     */

    public int next()
        throws XMLStreamException
    {
        int type;
        do {
            type = mReader.next();
            if (mFilter.accept(this)) {
                break;
            }
        } while (type != END_DOCUMENT);

        return type;
    }

    public int nextTag()
        throws XMLStreamException
    {
        int type;
        // Can be implemented very much like next()
        while (true) {
            type = mReader.nextTag();
            if (mFilter.accept(this)) {
                break;
            }
        }
        return type;
    }

    /*
    /////////////////////////////////////////////////
    // Then simple pass-through methods:
    /////////////////////////////////////////////////
     */

    public String getCharacterEncodingScheme() {
        return mReader.getCharacterEncodingScheme();
    }

    public String getEncoding() {
        return mReader.getEncoding();
    }

    public String getVersion() {
        return mReader.getVersion();
    }

    public boolean isStandalone() {
        return mReader.isStandalone();
    }

    public boolean standaloneSet() {
        return mReader.standaloneSet();
    }

    public Object getProperty(String name) {
        return mReader.getProperty(name);
    }

    public int getAttributeCount() {
        return mReader.getAttributeCount();
    }

    public String getAttributeLocalName(int index) {
        return mReader.getAttributeLocalName(index);
    }

    public QName getAttributeName(int index) {
        return mReader.getAttributeName(index);
    }

    public String getAttributeNamespace(int index) {
        return mReader.getAttributeNamespace(index);
    }

    public String getAttributePrefix(int index) {
        return mReader.getAttributePrefix(index);
    }

    public String getAttributeType(int index) {
        return mReader.getAttributeType(index);
    }

    public String getAttributeValue(int index) {
        return mReader.getAttributeValue(index);
    }

    public String getAttributeValue(String nsURI, String localName) {
        return mReader.getAttributeValue(nsURI, localName);
    }

    public String getElementText()
        throws XMLStreamException
    {
        return mReader.getElementText();
    }

    public int getEventType() {
        return mReader.getEventType();
    }
    
    public String getLocalName() {
        return mReader.getLocalName();
    }

    public Location getLocation() {
        return mReader.getLocation();
    }

    public QName getName() {
        return mReader.getName();
    }

    public NamespaceContext getNamespaceContext() {
        return mReader.getNamespaceContext();
    }

    public int getNamespaceCount() {
        return mReader.getNamespaceCount();
    }

    public String getNamespacePrefix(int index) {
        return mReader.getNamespacePrefix(index);
    }

    public String getNamespaceURI() {
        return mReader.getNamespaceURI();
    }

    public String getNamespaceURI(int index) {
        return mReader.getNamespaceURI(index);
    }

    public String getNamespaceURI(String prefix) {
        return mReader.getNamespaceURI(prefix);
    }

    public String getPIData() {
        return mReader.getPIData();
    }

    public String getPITarget() {
        return mReader.getPITarget();
    }

    public String getPrefix() {
        return mReader.getPrefix();
    }

    public String getText()
    {
        return mReader.getText();
    }

    public char[] getTextCharacters()
    {
        return mReader.getTextCharacters();
    }

    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int len)
        throws XMLStreamException
    {
        return mReader.getTextCharacters(sourceStart, target, targetStart, len);
    }

    public int getTextLength() {
        return mReader.getTextLength();
    }

    public int getTextStart() {
        return mReader.getTextStart();
    }

    public boolean hasName() {
        return mReader.hasName();
    }

    public boolean hasNext()
        throws XMLStreamException
    {
        return mReader.hasNext();
    }

    public boolean hasText() {
        return mReader.hasText();
    }

    public boolean isAttributeSpecified(int index) {
        return mReader.isAttributeSpecified(index);
    }

    public boolean isCharacters() {
        return mReader.isCharacters();
    }

    public boolean isEndElement() {
        return mReader.isEndElement();
    }

    public boolean isStartElement() {
        return mReader.isStartElement();
    }

    public boolean isWhiteSpace()
    {
        return mReader.isWhiteSpace();
    }
    
    public void require(int type, String nsUri, String localName)
        throws XMLStreamException
    {
        mReader.require(type, nsUri, localName);
    }

    public void close()
        throws XMLStreamException
    {
        mReader.close();
    }

    /*
    /////////////////////////////////////////////////
    // TypedXMLStreamReader2, element
    /////////////////////////////////////////////////
     */

    public boolean getElementAsBoolean() throws XMLStreamException
    {
        return mReader.getElementAsBoolean();
    }

    public int getElementAsInt() throws XMLStreamException
    {
        return mReader.getElementAsInt();
    }

    public long getElementAsLong() throws XMLStreamException
    {
        return mReader.getElementAsLong();
    }

    public float getElementAsFloat() throws XMLStreamException
    {
        return mReader.getElementAsFloat();
    }

    public double getElementAsDouble() throws XMLStreamException
    {
        return mReader.getElementAsDouble();
    }

    public BigInteger getElementAsInteger() throws XMLStreamException
    {
        return mReader.getElementAsInteger();
    }

    public BigDecimal getElementAsDecimal() throws XMLStreamException
    {
        return mReader.getElementAsDecimal();
    }

    public QName getElementAsQName() throws XMLStreamException
    {
        return mReader.getElementAsQName();
    }

    public void getElementAs(TypedValueDecoder tvd) throws XMLStreamException
    {
        mReader.getElementAs(tvd);
    }

    public int readElementAsIntArray(int[] value, int from, int length) throws XMLStreamException
    {
        return mReader.readElementAsIntArray(value, from, length);
    }

    public int readElementAsLongArray(long[] value, int from, int length) throws XMLStreamException
    {
        return mReader.readElementAsLongArray(value, from, length);
    }
    
    public int readElementAsFloatArray(float[] value, int from, int length) throws XMLStreamException
    {
        return mReader.readElementAsFloatArray(value, from, length);
    }
    
    public int readElementAsDoubleArray(double[] value, int from, int length) throws XMLStreamException
    {
        return mReader.readElementAsDoubleArray(value, from, length);
    }

    public int readElementAsArray(TypedArrayDecoder dec) throws XMLStreamException
    {
        return mReader.readElementAsArray(dec);
    }
    
    /*
    /////////////////////////////////////////////////
    // TypedXMLStreamReader2, attribute
    /////////////////////////////////////////////////
     */

    public int getAttributeIndex(String namespaceURI, String localName)
    {
        return mReader.getAttributeIndex(namespaceURI, localName);
    }

    public boolean getAttributeAsBoolean(int index) throws XMLStreamException
    {
        return mReader.getAttributeAsBoolean(index);
    }

    public int getAttributeAsInt(int index) throws XMLStreamException
    {
        return mReader.getAttributeAsInt(index);
    }

    public long getAttributeAsLong(int index) throws XMLStreamException
    {
        return mReader.getAttributeAsLong(index);
    }

    public float getAttributeAsFloat(int index) throws XMLStreamException
    {
        return mReader.getAttributeAsFloat(index);
    }

    public double getAttributeAsDouble(int index) throws XMLStreamException
    {
        return mReader.getAttributeAsDouble(index);
    }

    public BigInteger getAttributeAsInteger(int index) throws XMLStreamException
    {
        return mReader.getAttributeAsInteger(index);
    }

    public BigDecimal getAttributeAsDecimal(int index) throws XMLStreamException
    {
        return mReader.getAttributeAsDecimal(index);
    }

    public void getAttributeAs(int index, TypedValueDecoder tvd) throws XMLStreamException
    {
        mReader.getAttributeAs(index, tvd);
    }

    public QName getAttributeAsQName(int index) throws XMLStreamException
    {
        return mReader.getAttributeAsQName(index);
    }

    /*
    /////////////////////////////////////////////////
    // XMLStreamReader2 impl
    /////////////////////////////////////////////////
     */

    public Object getFeature(String name) {
        return mReader.getFeature(name);
    }

    public void setFeature(String name, Object value) {
        mReader.setFeature(name, value);
    }

    public boolean isPropertySupported(String name) {
        return mReader.isPropertySupported(name);
    }

    public boolean setProperty(String name, Object value) {
        return mReader.setProperty(name, value);
    }

    public void skipElement() throws XMLStreamException {
        mReader.skipElement();
    }

    public DTDInfo getDTDInfo() throws XMLStreamException {
        return mReader.getDTDInfo();
    }

    public AttributeInfo getAttributeInfo() throws XMLStreamException {
        return mReader.getAttributeInfo();
    }

    public int getText(Writer w, boolean preserveContents)
        throws IOException, XMLStreamException
    {
        return mReader.getText(w, preserveContents);
    }

    public LocationInfo getLocationInfo() {
        return mReader.getLocationInfo();
    }

    public boolean isEmptyElement() throws XMLStreamException {
        return mReader.isEmptyElement();
    }

    public int getDepth() {
        return mReader.getDepth();
    }

    public NamespaceContext getNonTransientNamespaceContext() {
        return mReader.getNonTransientNamespaceContext();
    }

    public String getPrefixedName() {
        return mReader.getPrefixedName();
    }

    public void closeCompletely() throws XMLStreamException {
        mReader.closeCompletely();
    }

    /*
    /////////////////////////////////////////////////
    // XMLStreamReader2 + Validatable
    /////////////////////////////////////////////////
     */

    public XMLValidator validateAgainst(XMLValidationSchema schema)
        throws XMLStreamException
    {
        return mReader.validateAgainst(schema);
    }

    public XMLValidator stopValidatingAgainst(XMLValidationSchema schema)
        throws XMLStreamException
    {
        return mReader.stopValidatingAgainst(schema);
    }

    public XMLValidator stopValidatingAgainst(XMLValidator validator)
        throws XMLStreamException
    {
        return mReader.stopValidatingAgainst(validator);
    }

    public ValidationProblemHandler setValidationProblemHandler(ValidationProblemHandler h)
    {
        return mReader.setValidationProblemHandler(h);
    }
}

