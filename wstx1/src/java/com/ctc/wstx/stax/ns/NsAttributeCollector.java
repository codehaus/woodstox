package com.ctc.wstx.stax.ns;

import javax.xml.XMLConstants;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.namespace.QName;

import com.ctc.wstx.stax.evt.WAttrList;
import com.ctc.wstx.stax.stream.StreamScanner;
import com.ctc.wstx.util.StringVector;
import com.ctc.wstx.util.TextBuilder;

/**
 * Attribute collector class used in namespace-aware parsing mode
 */
final class NsAttributeCollector
    extends AttributeCollector
{
    /**
     * Default URI that root element has, if none is explicitly defined.
     */
    protected final static String DEFAULT_NS_URI = "";

    /**
     * Expected typical maximum number of namespace declarations for any
     * elmement;
     * chosen to minimize need to resize, while trying not to waste space.
     */
    final static int EXP_NS_COUNT = 12;

    /**
     * Canonicalized prefix string "xml"
     */
    final String mXmlPrefix;

    /**
     * Canonicalized prefix string "xmlns"
     */
    final String mXmlnsPrefix;
    
    /*
    //////////////////////////////////////////
    // Collected namespace information:
    //////////////////////////////////////////
     */

    /**
     * TextBuilder into which values of namespace URIs are added (including
     * URI for the default namespace, if one defined)
     */
    private final TextBuilder mNamespaceURIs = new TextBuilder(EXP_NS_COUNT);

    /**
     * StringVector in which namespace prefixes are added; can contain a
     * single null to indicate the default namespace entry
     */
    private final StringVector mNsPrefixes = new StringVector(EXP_NS_COUNT);

    /**
     * Flag to indicate whether the default namespace has already been declared
     * for the current element.
     */
    private boolean mDefaultNsDeclared = false;

    /*
    //////////////////////////////////////////
    // Collected attribute information:
    //////////////////////////////////////////
     */
 
    /**
     * Actual number of attributes collected.
     */
    protected int mAttrCount;

    /**
     * TextBuilder into which values of non-namespace attributes are appended
     * to.
     */
    protected final TextBuilder mValueBuffer = new TextBuilder(EXP_ATTR_COUNT);

    /**
     * Vector in which two Strings (prefix, local name) are added for each
     * attribute, in order they are parsed from source document.
     */
    private final StringVector mAttrNames = new StringVector(EXP_ATTR_COUNT * 2);

    /*
    //////////////////////////////////////////
    // Resolved (derived) attribute information:
    //////////////////////////////////////////
     */

    /**
     * Array in which fully resolved attribute namespace URIs are added,
     * when resolved.
     */
    private String[] mAttrURIs = null;

    /**
     * Array in which attribute value Strings are added, first time they
     * are requested. Values are first added to <code>mValueBuffer</code>,
     * from which a String is created, and finally substring created as
     * needed and added to this array.
     */
    private String[] mAttrValues = null;

    /*
    //////////////////////////////////////////////////////////////
    // Information that defines "Map-like" data structure used for
    // quick access to attribute values by fully-qualified name
    //////////////////////////////////////////////////////////////
     */

    /**
     * Encoding of a data structure that contains mapping from qualified
     * attribute names to attribute index in main attribute name arrays.
     *<p>
     * Data structure contains two separate areas; main hash area (with
     * size <code>mAttrHashSize</code>), and remaining spillover area
     * that follows hash area up until (but not including)
     * <code>mAttrSpillEnd</code> index.
     * Main hash area only contains indexes (index+1; 0 signifying empty slot)
     * to actual attributes; spillover area has both hash and index for
     * any spilled entry. Spilled entries are simply stored in order
     * added, and need to be searched using linear search. In case of both
     * primary hash hits and spills, eventual comparison with URI and local
     * name needs to be done with actual name array.
     */
    private int[] mAttrMap = null;

    /**
     * Size of hash area in <code>mAttrMap</code>; generally at least 20%
     * more than number of attributes (<code>mAttrCount</code>).
     */
    private int mAttrHashSize;

    /**
     * Pointer to int slot right after last spill entr, in
     * <code>mAttrMap</code> array.
     */
    private int mAttrSpillEnd;

    /*
    ///////////////////////////////////////////////
    // Life-cycle:
    ///////////////////////////////////////////////
     */

    public NsAttributeCollector(String xmlPrefix, String xmlnsPrefix)
    {
        mXmlPrefix = xmlPrefix;
        mXmlnsPrefix = xmlnsPrefix;
    }

    /**
     * Method called to allow reusing of collector, usually right before
     * starting collecting attributes for a new start tag.
     */
    protected void reset()
    {
        mValueBuffer.reset();
        mNamespaceURIs.reset();
        mDefaultNsDeclared = false;

        /* No need to clear attr name, or NS prefix Strings; they are
         * canonicalized and will be referenced by symbol table in any
         * case... so we can save trouble of cleaning them up. This Object
         * will get GC'ed soon enough, after parser itself gets disposed of.
         */
        mAttrNames.clear(false);
        mNsPrefixes.clear(false);
        mAttrCount = 0;

        /* Note: attribute values will be cleared later on, when validating
         * namespaces. This so that we know how much to clean up; and
         * occasionally can also just avoid clean up (when resizing)
         */
    }

    /**
     * Method called to resolve namespace URIs from attribute prefixes.
     */
    protected void resolveNamespaces(StreamScanner sc, StringVector ns)
        throws XMLStreamException
    {
        int attrCount = mAttrCount;

        if (attrCount < 1) {
            // Checked if doing access by FQN:
            mAttrHashSize = 0;
        }

        // Need to have room for URIs:
        String[] attrURIs = mAttrURIs;
        if (attrURIs == null || attrURIs.length < attrCount) {
            mAttrURIs = attrURIs = new String[attrCount];
        }
        String[] attrNames = mAttrNames.getInternalArray();
        for (int i = 0; i < attrCount; ++i) {
            String prefix = attrNames[i+i];
            // Attributes do NOT use default namespace:
            /* (note: should never have empty string, ie. second check
             * should not be needed!)
             */
            if (prefix == null || prefix.length() == 0) {
                attrURIs[i] = DEFAULT_NS_URI;
                // xml:lang etc? no need for mapping
            } else if (prefix == mXmlPrefix) {
                attrURIs[i] = XMLConstants.XML_NS_URI;
            } else {
                String uri = ns.findLastFromMap(prefix);
                if (uri == null) {
                    sc.throwParseError("Undeclared namespace prefix '"
                                       +prefix+"' for attribute '"+attrNames[i+i+1]+"'.");
                }
                attrURIs[i] = uri;
            }
        }
        // Also, do we need to clear values first?
        if (mAttrValues != null) {
            // If array is too small, let's just discard it now:
            if (mAttrValues.length < attrCount) {
                mAttrValues = null;
            } else {
                // Otherwise, need to clear value entries from last element
                for (int i = 0; i < attrCount; ++i) {
                    mAttrValues[i] = null;
                }
            }
        }

        /* Ok, finally, let's create attribute map, to allow efficient
         * access by prefix+localname combination. Could do it on-demand,
         * but this way we can check for duplicates right away.
         */
        int[] map = mAttrMap;

        /* What's minimum size, to contain at most 80% full hash area,
         * plus 1/8 spill area (12.5% spilled entries, two ints each)?
         */
        int hashCount = 4;
        {
            int min = attrCount + (attrCount >> 2); // == 80% fill rate
            while (hashCount < min) {
                hashCount += hashCount; // 2x
            }
            // And then the spill area
            mAttrHashSize = hashCount;
            min = hashCount + (hashCount >> 4); // 12.5 x 2 ints
            if (map == null || map.length < min) {
                map = new int[min];
            } else { // need to clear old hash entries:
                for (int i = 0; i < hashCount; ++i) {
                    map[i] = 0;
                }
            }
        }

        {
            int mask = hashCount-1;
            int spillIndex = hashCount;

            // Ok, array's fine, let's hash 'em in!
            for (int i = 0; i < attrCount; ++i) {
                String uri = attrURIs[i];
                String name = attrNames[i+i+1];
                int hash = name.hashCode();
                if (uri.length() > 0) {
                    hash ^= uri.hashCode();
                }
                int index = hash & mask;
                // Hash slot available?
                if (map[index] == 0) {
                    map[index] = i+1; // since 0 is marker
                } else {
                    int currIndex = map[index]-1;
                    /* nope, need to spill; let's extract most of that code to
                     * a separate method for clarity (and maybe it'll be
                     * easier to inline by JVM too)
                     */
                    map = spillAttr(uri, name, map, currIndex, spillIndex, attrCount,
                                    hash, hashCount);
                    if (map == null) {
                        throwDupAttr(sc, currIndex);
                    }
                    map[++spillIndex] = i; // no need to specifically avoid 0
                    ++spillIndex;
                }
            }
            mAttrSpillEnd = spillIndex;
        }
        mAttrMap = map;
    }

    /*
    ///////////////////////////////////////////////
    // Public accesors (for stream reader)
    ///////////////////////////////////////////////
     */

    /**
     * @return Number of namespace declarations collected, not including
     *   possible default namespace declaration
     */
    public int getNsCount() {
        return mNamespaceURIs.size();
    }

    public boolean hasDefaultNs() {
        return mDefaultNsDeclared;
    }

    public String getNsPrefix(int index) {
        return mNsPrefixes.getString(index);
    }

    public String getNsURI(int index) {
        return mNamespaceURIs.getEntry(index);
    }

    // // // Direct access to attribute/NS prefixes/localnames/URI

    public int getCount() {
        return mAttrCount;
    }

    public String getPrefix(int index) {
        return mAttrNames.getString(index << 1);
    }

    public String getLocalName(int index) {
        return mAttrNames.getString((index << 1) + 1);
    }

    public String getURI(int index) {
        return mAttrURIs[index];
    }

    public QName getQName(int index) {
        String prefix = getPrefix(index);
        if (prefix == null) { // QName barfs on null...
            prefix = "";
        }
        return new QName(getURI(index), getLocalName(index), prefix);
    }

    public String getValue(int index) {
        if (index < 0 || index >= mAttrCount) {
            throw new IllegalArgumentException("Invalid index "+index
                                               +"; current element has only "
                                               +mAttrCount+" attributes.");
        }
        if (mAttrValues == null) {
            mAttrValues = new String[mAttrCount];
        }
        String str = mAttrValues[index];
        if (str == null) {
            str = mValueBuffer.getEntry(index);
            mAttrValues[index] = str;
        }
        return str;
    }

    public String getValue(String nsURI, String localName)
    {
        // Primary hit?
        int hashSize = mAttrHashSize;
        if (hashSize == 0) { // sanity check, for 'no attributes'
            return null;
        }
        int hash = localName.hashCode();
        if (nsURI == null) {
            nsURI = DEFAULT_NS_URI;
        } else if (nsURI.length() > 0) {
            hash ^= nsURI.hashCode();
        }
        int ix = mAttrMap[hash & (hashSize-1)];
        if (ix == 0) { // nothing in here; no spills either
            return null;
        }
        --ix;

        // Is primary candidate match?
        String thisName = mAttrNames.getString(ix+ix+1);
        /* Equality first, since although equals() checks that too, it's
         * very likely to match (if interning Strings), and we can save
         * a method call.
         */
        if (thisName == localName || thisName.equals(localName)) {
            String thisURI = mAttrURIs[ix];
            if (thisURI == nsURI || thisURI.equals(nsURI)) {
                return getValue(ix);
            }
        }

        /* Nope, need to traverse spill list, which has 2 entries for
         * each spilled attribute id; first for hash value, second index.
         */
        for (int i = hashSize, len = mAttrSpillEnd; i < len; i += 2) {
            if (mAttrMap[i] != hash) {
                continue;
            }
            /* Note: spill indexes are not off-by-one, since there's no need
             * to mask 0
             */
            ix = mAttrMap[i+1];
            thisName = mAttrNames.getString(ix+ix+1);
            if (thisName == localName || thisName.equals(localName)) {
                String thisURI = mAttrURIs[ix];
                if (thisURI == nsURI || thisURI.equals(nsURI)) {
                    return getValue(ix);
                }
            }
        }

        return null;
    }

    /**
     * @return null if the default namespace URI has been already declared
     *   for the current element; TextBuilder to add URI to if not.
     */
    public TextBuilder getDefaultNsBuilder()
    {
        if (mDefaultNsDeclared) {
            return null;
        }
        mDefaultNsDeclared = true;
        mNsPrefixes.addString(null);
        return mNamespaceURIs;
    }

    /**
     * @return null if prefix has been already declared; TextBuilder to
     *   add value to if not.
     */
    public TextBuilder getNsBuilder(String prefix)
    {
        if (mNsPrefixes.containsInterned(prefix)) {
            return null;
        }
        mNsPrefixes.addString(prefix);
        return mNamespaceURIs;
    }

    /**
     * Method called to get TextBuilder instance, into which value
     * String should be built, when starting to read attribute
     * value.
     *<p>
     * Note: It is assumed that all Strings have been canonicalized
     * via default symbol table
     *
     * @param attrPrefix canonicalized attribute prefix
     * @param attrLocalName canonicalized local name of attribute
     */
    public TextBuilder getAttrBuilder(String attrPrefix, String attrLocalName)
    {
        // 'normal' attribute:
        ++mAttrCount;
        mAttrNames.addStrings(attrPrefix, attrLocalName);
        /* Can't yet create attribute map by name, since we only know
         * name prefix, not necessarily matching URI.
         */ 
        return mValueBuffer;
    }

    /**
     *<p>
     * Note: only called by {@link InputElementStack}
     */
    protected String[] getNsPrefixes() {
        return mNsPrefixes.getInternalArray();
    }

    /**
     *<p>
     * Note: only called by {@link InputElementStack}
     */
    public TextBuilder getNsURIs() {
        return mNamespaceURIs;
    }

    /**
     * Method needed by event creating code, to build a non-transient
     * attribute container, to use with XMLEvent objects (specifically
     * implementation of StartElement event).
     */
    public WAttrList buildAttrList(Location loc) {
        int count = mAttrCount;
        if (count == 0) { // if there are no attrs, we can use a shorthand:
            return new WAttrList(loc);
        }
        /* If we have actual attributes, let's first just create the
         * raw array that has all attribute information:
         */
        String[] raw = new String[count << 2];
        for (int i = 0; i < count; ++i) {
            int ix = (i << 2);
            raw[ix] = mAttrNames.getString(i + i + 1);
            raw[ix+1] = mAttrURIs[i];
            raw[ix+2] = mAttrNames.getString(i + i);
            raw[ix+3] = getValue(i);
        }

        // Do we have a "short" list?
        if (count < LONG_ATTR_LIST_LEN) {
            return new WAttrList(loc, raw);
        }

        // Ok, nope; we need to also pass the Map information...
        return new WAttrList(loc, raw,
                             mAttrMap, mAttrHashSize, mAttrSpillEnd);
    }

    /*
    ///////////////////////////////////////////////
    // Internal methods:
    ///////////////////////////////////////////////
     */

    /**
     * @return Null, if attribute is a duplicate (to indicate error);
     *    map itself, or resized version, otherwise.
     */
    private int[] spillAttr(String uri, String name,
                            int[] map, int currIndex, int spillIndex, int attrCount,
                            int hash, int hashCount)
    {
        // Do we have a dup with primary entry?
        /* Can do equality comp for local name, as they
         * are always canonicalized:
         */
        if (mAttrNames.getString(currIndex+currIndex+1) == name) {
            // URIs may or may not be interned though:
            String currURI = mAttrURIs[currIndex];
            if (currURI == uri || currURI.equals(uri)) {
                return null;
            }
        }

        /* Is there room to spill into? (need to 2 int spaces; one for hash,
         * the other for index)
         */
        if ((spillIndex + 1)>= map.length) {
            // Let's just add room for 4 spills...
            int[] old = map;
            map = new int[old.length + 8];
            System.arraycopy(old, 0, map, 0, old.length);
        }
        // Let's first ensure we aren't adding a dup:
        for (int j = hashCount; j < spillIndex; j += 2) {
            if (map[j] == hash) {
                currIndex = map[j+1];
                if (mAttrNames.getString(currIndex+currIndex+1) == name) {
                    String currURI = mAttrURIs[currIndex];
                    if (currURI == uri || currURI.equals(uri)) {
                        return null;
                    }
                }
            }
        }
        map[spillIndex] = hash;
        return map;
    }
}
