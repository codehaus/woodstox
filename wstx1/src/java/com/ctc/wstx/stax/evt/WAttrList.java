package com.ctc.wstx.stax.evt;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;

import com.ctc.wstx.util.EmptyIterator;
import com.ctc.wstx.util.SingletonIterator;
import com.ctc.wstx.util.XMLQuoter;

/**
 * Container class that is constructed with enough raw attribute information,
 * to be able to lazily construct full attribute objects, to be accessed
 * via Iterator, or fully-qualified name.
 *<p>
 * Implementation note: code for using Map-like structure is unfortunately
 * cut'n pasted from {@link com.ctc.wstx.stax.ns.AttributeCollector}. Problem
 * with refactoring is that it's 90% the same code, but not 100%.
 */
public final class WAttrList
{
    protected final static String DEFAULT_NS_URI = "";

    private final static int OFFSET_LOCAL_NAME = 0;
    private final static int OFFSET_NS_URI = 1;
    private final static int OFFSET_NS_PREFIX = 2;
    private final static int OFFSET_VALUE = 3;

    /**
     * Location of the start element that contains attributes in this list.
     * Used as the approximation of location of the attribute.
     */
    private final Location mLocation;
    
    /**
     * Array that contains 4 Strings for each attribute;
     * localName, URI, prefix, value. Can be used to lazily construct
     * structure(s) needed to return Iterator for accessing all
     * attributes.
     */
    private final String[] mRawAttrs;

    /**
     * Lazily created List that contains Attribute instances contained
     * in this list. Created only if there are at least 2 attributes.
     */
    private ArrayList mAttrList = null;

    /*
    //////////////////////////////////////////////////////////////
    // Information that defines "Map-like" data structure used for
    // quick access to attribute values by fully-qualified name
    // (only used for "long" lists)
    //////////////////////////////////////////////////////////////
     */

    // // // For full explanation, see source for AttributeCollector

    private final int[] mAttrMap;

    private final int mAttrHashSize;

    private final int mAttrSpillEnd;

    /**
     * Constructor for generating "no attributes" list.
     */
    public WAttrList(Location loc)
    {
        mLocation = loc;
        String[] strs = EmptyIterator.getEmptyStringArray();
        mRawAttrs = strs;
        mAttrMap = null;
        mAttrHashSize = 0;
        mAttrSpillEnd = 0;
    }

    /**
     * Method called to create "short" attribute list; list that has
     * only few entries, and can thus be searched for attributes using
     * linear search, without using any kind of Map structure.
     *<p>
     * Currently the limit is 4 attributes; 1, 2 or 3 attribute lists are
     * considered short, 4 or more 'long'.
     *
     * @param loc Location of the start element that contains attributes
     *   this container contains.
     * @param rawAttrs Array that contains 4 Strings for each attribute;
     *    localName, URI, prefix, value. Can be used to lazily construct
     *    structure(s) needed to return Iterator for accessing all
     *    attributes.
     */
    public WAttrList(Location loc, String[] rawAttrs) {
        mLocation = loc;
        mRawAttrs = rawAttrs;
        mAttrMap = null;
        mAttrHashSize = 0;
        mAttrSpillEnd = 0;
    }

    /**
     * Method called to create "long" attribute list; list that has
     * a few entries, and efficient access by fully-qualified name should
     * not be done by linear search.
     *
     * @param loc Location of the start element that contains attributes
     *   this container contains.
     * @param rawAttrs Array that contains 4 Strings for each attribute;
     *    localName, URI, prefix, value. Can be used to lazily construct
     *    structure(s) needed to return Iterator for accessing all
     *    attributes.
     */
    public WAttrList(Location loc, String[] rawAttrs,
                     int[] attrMap, int hashSize, int spillEnd)
    {
        mLocation = loc;
        mRawAttrs = rawAttrs;
        mAttrMap = attrMap;
        mAttrHashSize = hashSize;
        mAttrSpillEnd = spillEnd;
    }

    /*
    ////////////////////////////////////////////////////
    // Public API
    ////////////////////////////////////////////////////
     */

    public Attribute getAttr(QName name)
    {
        // Can/need to use linear search?
        if (mAttrMap == null) {
            String ln = name.getLocalPart();
            String uri = name.getNamespaceURI();
            boolean defaultNs = (uri == null || uri.length() == 0);
            String[] raw = mRawAttrs;

            for (int i = 0, len = raw.length; i < len; i += 4) {
                if (!ln.equals(raw[i])) {
                    continue;
                }
                String thisUri = raw[i+1];
                if (defaultNs) {
                    if (thisUri == null || thisUri.length() == 0) {
                        return createAttr(i);
                    }
                } else { // non-default NS
                    if (thisUri != null &&
                        (thisUri == uri || thisUri.equals(uri))) {
                        return createAttr(i);
                    }
                }
            }
            return null; // no match
        }

        // Ok, better use the Map...
        return getAttrByMap(name.getNamespaceURI(), name.getLocalPart());
    }

    public Iterator getAttrs() {
        if (mAttrList == null) { // list is lazily created, if needed
            int rawLen = mRawAttrs.length;
            if (rawLen == 0) { // no attributes
                return EmptyIterator.getInstance();
            }
            if (rawLen == 4) {
                return new SingletonIterator(createAttr(0));
            }
            ArrayList l = new ArrayList(rawLen >> 2);
            for (int i = 0; i < rawLen; i += 4) {
                l.add(createAttr(i));
            }
            mAttrList = l;
        }
        return mAttrList.iterator();

    }

    public void outputAttrs(Writer w) throws IOException
    {
        String[] raw = mRawAttrs;
        for (int i = 0, len = raw.length; i < len; i += 4) {
            w.write(' ');
            String prefix = raw[i + OFFSET_NS_PREFIX];
            if (prefix != null && prefix.length() > 0) {
                w.write(prefix);
                w.write(':');
            }
            w.write(raw[i]); // local name
            w.write("=\"");
            XMLQuoter.outputDoubleQuotedAttr(w, raw[i + OFFSET_VALUE]);
            w.write('"');
        }
    }

    /*
    ////////////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////////////
     */

    private final WAttribute createAttr(int rawIndex) {
        String[] raw = mRawAttrs;
        return new WAttribute(mLocation, raw[rawIndex], raw[rawIndex+1],
                              raw[rawIndex+2], raw[rawIndex+3]);
    }

    /**
     *<p>
     * Note: this method is very similar to
     * {@link com.ctc.wstx.stax.AttributeCollector#getAttrValue}; basically
     * most of it was cut'n pasted. Would be nice to refactor, but it's
     * bit hard to do that since data structures are not 100% identical
     * (mostly attribute storage, not Map structure itself).
     */
    private final WAttribute getAttrByMap(String nsURI, String localName)
    {
        // Primary hit?
        int hash = localName.hashCode();
        if (nsURI == null) {
            nsURI = DEFAULT_NS_URI;
        } else if (nsURI.length() > 0) {
            hash ^= nsURI.hashCode();
        }
        int ix = mAttrMap[hash & (mAttrHashSize - 1)];
        if (ix == 0) { // nothing in here; no spills either
            return null;
        }
        // Index is "one off" (since 0 indicates 'null), 4 Strings per attr
        ix = (ix - 1) << 2;

        // Is primary candidate match?
        String[] raw = mRawAttrs;
        String thisName = raw[ix];
        /* Equality first, since although equals() checks that too, it's
         * very likely to match (if interning Strings), and we can save
         * a method call.
         */
        if (thisName == localName || thisName.equals(localName)) {
            String thisURI = raw[ix+1];
            if (thisURI == nsURI || thisURI.equals(nsURI)) {
                return createAttr(ix);
            }
        }

        /* Nope, need to traverse spill list, which has 2 entries for
         * each spilled attribute id; first for hash value, second index.
         */
        for (int i = mAttrHashSize, len = mAttrSpillEnd; i < len; i += 2) {
            if (mAttrMap[i] != hash) {
                continue;
            }
            /* Note: spill indexes are not off-by-one, since there's no need
             * to mask 0
             */
            ix = mAttrMap[i+1] << 2; // ... but there are 4 Strings for each attr
            thisName = raw[ix];
            if (thisName == localName || thisName.equals(localName)) {
                String thisURI = raw[ix+1];
                if (thisURI == nsURI || thisURI.equals(nsURI)) {
                    return createAttr(ix);
                }
            }
        }

        return null;
    }
}
