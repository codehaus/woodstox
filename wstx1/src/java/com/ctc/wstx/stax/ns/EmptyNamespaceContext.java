package com.ctc.wstx.stax.ns;

import java.io.Writer;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;

import com.ctc.wstx.util.EmptyIterator;

/**
 * Dummy {@link NamespaceContext} (and {@link BaseNsContext})
 * implementation that is usually used in
 * non-namespace-aware mode.
 */
public final class EmptyNamespaceContext
    extends BaseNsContext
{
    final static EmptyNamespaceContext sInstance = new EmptyNamespaceContext();
    
    private EmptyNamespaceContext() { }

    public static EmptyNamespaceContext getInstance() { return sInstance; }

    /*
    /////////////////////////////////////////////
    // Extended API
    /////////////////////////////////////////////
     */

    public Iterator getNamespaces() {
        return EmptyIterator.getInstance();
    }

    /**
     * Method called by the matching start element class to
     * output all namespace declarations active in current namespace
     * scope, if any.
     */
    public void outputNamespaceDeclarations(Writer w)
    {
        ; // nothing to output
    }

    /*
    /////////////////////////////////////////////////
    // Template methods sub-classes need to implement
    /////////////////////////////////////////////////
     */

    public String doGetNamespaceURI(String prefix) {
        return null;
    }

    public String doGetPrefix(String nsURI) {
        return null;
    }

    public Iterator doGetPrefixes(String nsURI) {
        return EmptyIterator.getInstance();
    }
}
