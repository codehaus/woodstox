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
 * Attribute collector class used in non-namespace parsing mode; much
 * simpler than the one that has to handle namespaces.
 */
final class NonNsAttributeCollector
    extends AttributeCollector
{
    /**
     * Default URI that is returned in non-namespace mode for all elements
     * and attributes
     */
    protected final static String DEFAULT_NS_URI = null;

    protected final static String DEFAULT_PREFIX = null;

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
     * Vector in which full (non-namespace) attributes names are added,
     * in order they are parsed from the source document.
     */
    private final StringVector mAttrNames = new StringVector(EXP_ATTR_COUNT);

    /*
    //////////////////////////////////////////
    // Resolved (derived) attribute information:
    //////////////////////////////////////////
     */

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
     * Encoding of a data structure that contains mapping from
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
     * primary hash hits and spills, eventual comparison with the local
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

    protected NonNsAttributeCollector()
    {
        super();
    }

    /**
     * Method called to allow reusing of collector, usually right before
     * starting collecting attributes for a new start tag.
     */
    protected void reset()
    {
        mValueBuffer.reset();
        /* No need to clear attr name, or NS prefix Strings; they are
         * canonicalized and will be referenced by symbol table in any
         * case... so we can save trouble of cleaning them up. This Object
         * will get GC'ed soon enough, after parser itself gets disposed of.
         */
        mAttrNames.clear(false);
        mAttrCount = 0;

        /* Note: attribute values will be cleared later on, when validating
         * namespaces. This so that we know how much to clean up; and
         * occasionally can also just avoid clean up (when resizing)
         */
    }

    /**
     * Method called to by the input element stack when all attributes for
     * the element have been parsed. Now collector can build data structures
     * it needs, if any.
     */
    protected void resolveValues(StreamScanner sc)
        throws XMLStreamException
    {
        int attrCount = mAttrCount;

        if (attrCount < 1) {
            // Checked if do access by FQN:
            mAttrHashSize = 0;
        }
        String[] attrNames = mAttrNames.getInternalArray();

        // Also, we better clean up values now
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

        /* And then let's create attribute map, to allow efficient storage
         * and access by name, without creating full hash map instance.
         * Could do it on-demand,
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
                String name = attrNames[i];
                int hash = name.hashCode();
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
                    map = spillAttr(name, map, currIndex, spillIndex, attrCount,
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

    public int getNsCount() {
        return 0;
    }

    public String getNsPrefix(int index) {
        return DEFAULT_PREFIX;
    }

    public String getNsURI(int index) {
        return DEFAULT_NS_URI;
    }

    // // // Direct access to attribute/NS prefixes/localnames/URI

    public int getCount() {
        return mAttrCount;
    }

    public String getPrefix(int index) {
        if (index < 0 || index >= mAttrCount) {
            throwIndex(index);
        }
        return DEFAULT_PREFIX;
    }

    public String getLocalName(int index) {
        if (index < 0 || index >= mAttrCount) {
            throwIndex(index);
        }
        return mAttrNames.getString(index);
    }

    public String getURI(int index)
    {
        if (index < 0 || index >= mAttrCount) {
            throwIndex(index);
        }
        return DEFAULT_NS_URI;
    }

    public QName getQName(int index) {
        return new QName(getLocalName(index));
    }

    public String getValue(int index) {
        if (index < 0 || index >= mAttrCount) {
            throwIndex(index);
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
        /* Not allowed to have a namespace, except let's not mind the
         * default (empty) namespace URI
         */
        if (nsURI != null && nsURI.length() > 0) {
            return null;
        }

        // Primary hit?
        int hashSize = mAttrHashSize;
        if (hashSize == 0) { // sanity check, for 'no attributes'
            return null;
        }
        int hash = localName.hashCode();
        int ix = mAttrMap[hash & (hashSize-1)];
        if (ix == 0) { // nothing in here; no spills either
            return null;
        }
        --ix;

        // Is primary candidate match?
        String thisName = mAttrNames.getString(ix);
        /* Equality first, since although equals() checks that too, it's
         * very likely to match (if interning Strings), and we can save
         * a method call.
         */
        if (thisName == localName || thisName.equals(localName)) {
            return getValue(ix);
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
            thisName = mAttrNames.getString(ix);
            if (thisName == localName || thisName.equals(localName)) {
                return getValue(ix);
            }
        }

        return null;
    }

    public TextBuilder getDefaultNsBuilder()
    {
        throwInternal();
        return null;
    }

    /**
     * @return null if prefix has been already declared; TextBuilder to
     *   add value to if not.
     */
    public TextBuilder getNsBuilder(String localName)
    {
        throwInternal();
        return null;
    }

    public TextBuilder getAttrBuilder(String attrPrefix, String attrLocalName)
    {
        // 'normal' attribute:
        ++mAttrCount;
        mAttrNames.addString(attrLocalName);
        return mValueBuffer;
    }

    public TextBuilder getNsURIs() {
        throwInternal();
        return null;
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
            raw[ix] = mAttrNames.getString(i);
            raw[ix+1] = DEFAULT_NS_URI;
            raw[ix+2] = DEFAULT_PREFIX;
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

    private void throwInternal() {
        throw new Error("Internal error: shouldn't call this method.");
    }

    private void throwIndex(int index) {
        throw new IllegalArgumentException("No attribute with index "+index+"; element only has "+getCount()+" attributes.");
    }


    /**
     * @return Null, if attribute is a duplicate (to indicate error);
     *    map itself, or resized version, otherwise.
     */
    private int[] spillAttr(String name,
                            int[] map, int currIndex, int spillIndex, int attrCount,
                            int hash, int hashCount)
    {
        // Do we have a dup with primary entry?
        /* Can do equality comp for local name, as they
         * are always canonicalized:
         */
        if (mAttrNames.getString(currIndex) == name) {
            return null;
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
                if (mAttrNames.getString(currIndex) == name) {
                    return null;
                }
            }
        }
        map[spillIndex] = hash;
        return map;
    }
}
