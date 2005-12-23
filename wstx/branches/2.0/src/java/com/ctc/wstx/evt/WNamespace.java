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
        super(loc, XMLConstants.XML_NS_PREFIX, "", "", nsURI, true);
        mPrefix = "";
        mURI = nsURI;
    }

    /**
     * Constructor non-default namespace declaration. Such declarations
     * belong to "XML namespace" namespace.
     */
    public WNamespace(Location loc, String nsPrefix, String nsURI)
    {
        super(loc, nsPrefix, XMLConstants.XML_NS_URI,
              XMLConstants.XML_NS_PREFIX, nsURI, true);
        mPrefix = nsPrefix;
        mURI = nsURI;
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
