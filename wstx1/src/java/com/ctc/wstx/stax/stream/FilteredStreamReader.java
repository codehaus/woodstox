package com.ctc.wstx.stax.stream;

import java.io.IOException;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.*;
import javax.xml.stream.util.XMLEventAllocator;

public class FilteredStreamReader
    implements XMLStreamReader,
               XMLStreamConstants
{
    final XMLStreamReader mReader;
    final StreamFilter mFilter;

    public FilteredStreamReader(XMLStreamReader r, StreamFilter f) {
        mReader = r;
        mFilter = f;
    }

    /*
    ////////////////////////////////////////////////////
    // XMLStreamReader implementation
    ////////////////////////////////////////////////////
     */

    /*
    /////////////////////////////////////////////////
    // API implementation that needs special handling
    /////////////////////////////////////////////////
     */

    public int next()
        throws XMLStreamException
    {
        while (true) {
            int type = mReader.next();
            if (mFilter.accept(this)) {
                return type;
            }
            /* ??? 11-May-2004, TSa: Should we take some precautions for
             *   END_DOCUMENT event?
             */
        }
    }

    public int nextTag()
        throws XMLStreamException
    {
        // Can be implemented very much like next()

        while (true) {
            int type = mReader.nextTag();
            if (mFilter.accept(this)) {
                return type;
            }
            /* ??? 11-May-2004, TSa: Should we take some precautions for
             *   END_DOCUMENT event?
             */
        }
    }

    /*
    /////////////////////////////////////////////////
    // Simple pass-through methods:
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

}

