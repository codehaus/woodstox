package com.ctc.wstx.stax.ns;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;

import com.ctc.wstx.util.SingletonIterator;

/**
 * Abstract base class that defines extra features defined by most
 * NamespaceContext implementations Wstx uses.
 */
public abstract class BaseNsContext
    implements NamespaceContext
{
    /*
    /////////////////////////////////////////////
    // NamespaceContext API
    /////////////////////////////////////////////
     */

    public final String getNamespaceURI(String prefix)
    {
        /* First the known offenders; invalid args, 2 predefined xml namespace
         * prefixes
         */
        if (prefix == null) {
            throw new IllegalArgumentException("Illegal to pass null as argument.");
        }
        if (prefix.length() > 0) {
            if (prefix.equals(XMLConstants.XML_NS_PREFIX)) {
                return XMLConstants.XML_NS_URI;
            }
            if (prefix.equals(XMLConstants.XMLNS_ATTRIBUTE)) {
                return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
            }
        }
        return doGetNamespaceURI(prefix);
    }

    public final String getPrefix(String nsURI)
    {
        /* First the known offenders; invalid args, 2 predefined xml namespace
         * prefixes
         */
        if (nsURI == null || nsURI.length() == 0) {
            throw new IllegalArgumentException("Illegal to pass null/empty prefix as argument.");
        }
        if (nsURI.equals(XMLConstants.XML_NS_URI)) {
            return XMLConstants.XML_NS_PREFIX;
        }
        if (nsURI.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
            return XMLConstants.XMLNS_ATTRIBUTE;
        }
        return doGetPrefix(nsURI);
    }

    public final Iterator getPrefixes(String nsURI)
    {
        /* First the known offenders; invalid args, 2 predefined xml namespace
         * prefixes
         */
        if (nsURI == null || nsURI.length() == 0) {
            throw new IllegalArgumentException("Illegal to pass null/empty prefix as argument.");
        }
        if (nsURI.equals(XMLConstants.XML_NS_URI)) {
            return new SingletonIterator(XMLConstants.XML_NS_PREFIX);
        }
        if (nsURI.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
            return new SingletonIterator(XMLConstants.XMLNS_ATTRIBUTE);
        }

        return doGetPrefixes(nsURI);
    }

    /*
    /////////////////////////////////////////////
    // Extended API
    /////////////////////////////////////////////
     */

    public abstract Iterator getNamespaces();

    /**
     * Method called by the matching start element class to
     * output all namespace declarations active in current namespace
     * scope, if any.
     */
    public abstract void outputNamespaceDeclarations(Writer w) throws IOException;

    /*
    /////////////////////////////////////////////////
    // Template methods sub-classes need to implement
    /////////////////////////////////////////////////
     */

    public abstract String doGetNamespaceURI(String prefix);

    public abstract String doGetPrefix(String nsURI);

    public abstract Iterator doGetPrefixes(String nsURI);
}
