package com.ctc.wstx.evt;

import java.io.Writer;
import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Namespace;

/**
 * Implementation of {@link Namespace}. Only returned via accessors in
 * actual "first class" event objects (start element, end element); never
 * directly via event reader.
 */
public class WNamespace
    extends WAttribute
    implements Namespace
{
    final String mPrefix;
    final String mURI;

    /**
     * Constructor default namespace declaration. Such declarations don't
     * have namespace prefix/URI, although semantically it would belong
     * to XML namespace URI...
     */
    public WNamespace(Location loc, String nsURI)
    {
        super(loc, XMLConstants.XML_NS_PREFIX, XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
              null,
              nsURI, true);
        mPrefix = "";
        mURI = nsURI;
    }

    /**
     * Constructor non-default namespace declaration. Such declarations
     * belong to "XML namespace" namespace.
     */
    public WNamespace(Location loc, String nsPrefix, String nsURI)
    {
        super(loc, nsPrefix,  XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
              XMLConstants.XMLNS_ATTRIBUTE,
              nsURI, true);
        mPrefix = nsPrefix;
        mURI = nsURI;
    }

    public static WNamespace constructFor(Location loc, String nsPrefix, String nsURI)
    {
        if (nsPrefix == null || nsPrefix.length() == 0) { // default NS:
            return new WNamespace(loc, nsURI);
        }
        return new WNamespace(loc, nsPrefix, nsURI);
    }

    public String getNamespaceURI() {
        return mURI;
    }

    public String getPrefix() {
        return mPrefix;
    }

    public boolean isDefaultNamespaceDeclaration() {
        return (mPrefix.length() == 0);
    }

    /*
    ///////////////////////////////////////////
    // Implementation of abstract base methods
    ///////////////////////////////////////////
     */

    public int getEventType() {
        return NAMESPACE;
    }

    public boolean isNamespace() {
        return true;
    }

    // Attribute's implementation should be ok already:
    //public void writeAsEncodedUnicode(Writer w) throws XMLStreamException
}
