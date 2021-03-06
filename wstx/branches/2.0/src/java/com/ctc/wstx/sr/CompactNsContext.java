package com.ctc.wstx.sr;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.stream.Location;

import com.ctc.wstx.util.BaseNsContext;
import com.ctc.wstx.util.EmptyIterator;
import com.ctc.wstx.util.SingletonIterator;
// !!! 27-Jul-2004, TSa: Should remove this dependency!
import com.ctc.wstx.evt.WNamespace;

/**
 * Simple implementation of separate non-transient namespace context
 * object. Created for start-element event by transient namespace
 * instance updated by stream reader.
 */
public final class CompactNsContext
    extends BaseNsContext
{
    final Location mLocation;

    /**
     * Array that contains 2 Strings for each declared default namespace
     * (including default namespace declarations); first is the prefix,
     * second URI.
     */
    final String[] mNamespaces;

    /**
     * Index of first namespace pair in mNamespaces that is declared
     * in scope of element for which this context was constructed.
     */
    final int mFirstLocalNs;

    /**
     * List only needed to support List accessor from start-element event;
     * created lazily if/as needed.
     */
    ArrayList mNsList;

    public CompactNsContext(Location loc, String defaultNsURI, String[] namespaces,
                            int firstLocal)
    {
        mLocation = loc;
        mNamespaces = namespaces;
        mFirstLocalNs = firstLocal;
    }

    public String doGetNamespaceURI(String prefix)
    {
        /* Let's search from beginning towards end; this way we'll first
         * find the innermost (or, in case of same-level declaration, last)
         * declaration for prefix.
         */
        // (note: default namespace will be there too...)
        String[] ns = mNamespaces;
        if (prefix == null || prefix.length() == 0) {
            for (int i = ns.length-2; i >= 0; i -= 2) {
                if (ns[i].length() == 0) {
                    return ns[i+1];
                }
            }
            return "";
        }
        for (int i = ns.length-2; i >= 0; i -= 2) {
            if (prefix.equals(ns[i])) {
                return ns[i+1];
            }
        }
        return null;
    }

    public String doGetPrefix(String nsURI)
    {
        // Note: base class checks for 'known' problems and prefixes:

        String[] ns = mNamespaces;
        int len = ns.length;

        main_loop:
        for (int i = len-1; i > 0; i -= 2) {
            if (nsURI.equals(ns[i])) {
                /* 29-Sep-2004, TSa: Actually, need to make sure that this
                 *    declaration is not masked by a later declaration.
                 *    This happens when same prefix is declared on a later
                 *    entry (ie. for child element)
                 */
                String prefix = ns[i-1];
                for (int j = i+1; j < len; j += 2) {
                    // Prefixes are interned, can do straight equality check
                    if (ns[j] == prefix) {
                        continue main_loop; // was masked!
                    }
                }
                return mNamespaces[i-1];
            }
        }
        return null;
    }

    public Iterator doGetPrefixes(String nsURI)
    {
        // Note: base class checks for 'known' problems and prefixes:

        String[] ns = mNamespaces;
        int len = ns.length;
        String first = null;
        ArrayList all = null;

        main_loop:
        for (int i = len-1; i > 0; i -= 2) {
            String currNS = ns[i];
            if (currNS == nsURI || currNS.equals(nsURI)) {
                /* 29-Sep-2004, TSa: Need to ensure it's not masked by
                 *    a later ns declaration in a child element.
                 */
                String prefix = ns[i-1];
                for (int j = i+1; j < len; j += 2) {
                    // Prefixes are interned, can do straight equality check
                    if (ns[j] == prefix) {
                        continue; // was masked!
                    }
                }
                if (first == null) {
                    first = prefix;
                } else {
                    if (all == null) {
                        all = new ArrayList();
                        all.add(first);
                    }
                    all.add(prefix);
                }
            }
        }
        if (all != null) {
            return all.iterator();
        }
        if (first != null) {
            return new SingletonIterator(first);
        }
        return EmptyIterator.getInstance();
    }

    /*
    ///////////////////////////////////////////////////////
    // Extended API, needed by Wstx classes
    ///////////////////////////////////////////////////////
     */

    public Iterator getNamespaces()
    {
        if (mNsList == null) {
            int firstLocal = mFirstLocalNs;
            int len = mNamespaces.length - firstLocal;
            if (len == 0) { // can this happen?
                return EmptyIterator.getInstance();
            }
            if (len == 2) { // only one NS
                return new SingletonIterator(new WNamespace
                                             (mLocation,
                                              mNamespaces[firstLocal],
                                              mNamespaces[firstLocal+1]));
            }
            ArrayList l = new ArrayList(len >> 1);
            String[] ns = mNamespaces;
            for (len = mNamespaces.length; firstLocal < len;
                 firstLocal += 2) {
                l.add(new WNamespace(mLocation, ns[firstLocal],
                                     ns[firstLocal+1]));
            }
            mNsList = l;
        }
        return mNsList.iterator();
    }
    
    /**
     * Method called by {@link com.ctc.wstx.evt.CompactStartElement}
     * to output all 'local' namespace declarations active in current
     * namespace scope, if any. Local means that declaration was done in
     * scope of current element, not in a parent element.
     */
    public void outputNamespaceDeclarations(Writer w) throws IOException
    {
        String[] ns = mNamespaces;
        for (int i = mFirstLocalNs, len = ns.length; i < len; i += 2) {
            w.write(' ');
            w.write(XMLConstants.XMLNS_ATTRIBUTE);
            String prefix = ns[i];
            if (prefix.length() > 0) {
                w.write(':');
                w.write(prefix);
            }
            w.write("=\"");
            w.write(ns[i+1]);
            w.write('"');
        }
    }
}
