package com.ctc.wstx.sr;

import java.io.IOException;
import java.io.Writer;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import com.ctc.wstx.exc.WstxException;
import com.ctc.wstx.util.DataUtil;
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
    ///////////////////////////////////////////////
    // Life-cycle:
    ///////////////////////////////////////////////
     */

    protected NonNsAttributeCollector(boolean normAttrs)
    {
        super(normAttrs);
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
    protected void resolveValues(InputProblemReporter rep)
        throws WstxException
    {
        int attrCount = mAttrCount;

        /* Let's now set number of 'real' attributes, to allow figuring
         * out number of attributes created via default value expansion
         */
        mNonDefCount = attrCount;

        if (attrCount < 1) {
            // Checked if doing access by FQN:
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
                        throwDupAttr(rep, currIndex);
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

    public int findIndex(String localName)
    {
        /* Note: most of the code is from getValue().. could refactor
         * code, performance is bit of concern (one more method call
         * if index access was separate).
         * See comments on that method, for logics.
         */

        // Primary hit?
        int hashSize = mAttrHashSize;
        if (hashSize == 0) { // sanity check, for 'no attributes'
            return -1;
        }
        int hash = localName.hashCode();
        int ix = mAttrMap[hash & (hashSize-1)];
        if (ix == 0) { // nothing in here; no spills either
            return -1;
        }
        --ix;

        // Is primary candidate match?
        String thisName = mAttrNames.getString(ix);
        if (thisName == localName || thisName.equals(localName)) {
            return ix;
        }

        /* Nope, need to traverse spill list, which has 2 entries for
         * each spilled attribute id; first for hash value, second index.
         */
        for (int i = hashSize, len = mAttrSpillEnd; i < len; i += 2) {
            if (mAttrMap[i] != hash) {
                continue;
            }
            ix = mAttrMap[i+1];
            thisName = mAttrNames.getString(ix);
            if (thisName == localName || thisName.equals(localName)) {
                return ix;
            }
        }
        return -1;
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
    public ElemAttrs buildAttrOb()
    {
        int count = mAttrCount;
        if (count == 0) {
            return null;
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
            return new ElemAttrs(raw, mNonDefCount);
        }

        // Ok, nope; we need to also pass the Map information...
        return new ElemAttrs(raw, mNonDefCount,
                             mAttrMap, mAttrHashSize, mAttrSpillEnd);
                             
    }

    /*
    ///////////////////////////////////////////////
    // Validation methods:
    ///////////////////////////////////////////////
     */

    /**
     * Method called by validator to insert an attribute that has a default
     * value and wasn't yet included in collector's attribute set.
     *
     * @return Index of the newly added attribute, if added; -1 to indicate
     *    this was a duplicate
     */
    public int addDefaultAttribute(String localName, String value)
    {
        mAttrNames.addString(localName); // First, the name
        int attrIndex = mAttrCount;

        /* Then the value. First, need to make sure value array exists and
         * is big enough:
         */
        if (mAttrValues == null) {
            mAttrValues = new String[attrIndex + 8];
        } else if (attrIndex >= mAttrValues.length) {
            mAttrValues = DataUtil.growArrayBy(mAttrValues, 8);
        }
        mAttrValues[attrIndex] = value;
        /* Could also add a dummy entry to value builder, but why bother;
         * cached value set above should effectively mask it for this entry.
         */

        /* However, we do need to add an entry to the access Map;
         * this code is modelled after resolveValues().
         */
        int hash = localName.hashCode();
        int index = hash & (mAttrHashSize - 1);
        // Note: at this point mAttrCount has been added by one
        int[] map = mAttrMap;
        if (map[index] == 0) { // whoa, have room...
            map[index] = attrIndex+1; // add 1 to get 1-based index (0 is empty marker)
        } else { // nah, collision...
            /* No point in calling spillAttr(), unfortunately, as it's
             * been specifically tuned for needs of resolveXxx method...
             * plus we don't need to check for dups at this point (wouldn't
             * add default value if such attr was encountered)
             */
            if ((mAttrSpillEnd + 1) >= map.length) {
                mAttrMap = map = DataUtil.growArrayBy(map, 8);
            }
            map[mAttrSpillEnd] = hash;
            map[mAttrSpillEnd+1] = attrIndex;
            mAttrSpillEnd += 2;
        }

        return mAttrCount++;
    }

    /**
     * Method that basically serializes the specified (read-in) attribute
     * using Writers provided
     */
    public void writeAttribute(int index, char quoteChar, Writer mainWriter,
                               Writer attrValueWriter)
        throws IOException
    {
        // Note: here we assume index checks have been done by caller
        mainWriter.write(mAttrNames.getString(index));
        mainWriter.write('=');
        mainWriter.write(quoteChar);
        writeValue(index, attrValueWriter);
        mainWriter.write(quoteChar);
    }

    /*
    ///////////////////////////////////////////////
    // Internal methods:
    ///////////////////////////////////////////////
     */

    private void throwInternal() {
        throw new Error("Internal error: shouldn't call this method.");
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

        /* Is there room to spill into? (need to have 2 int spaces; one for
         * hash, the other for index)
         */
        if ((spillIndex + 1)>= map.length) {
            // Let's just add room for 4 spills...
            map = DataUtil.growArrayBy(map, 8);
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
