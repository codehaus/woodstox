package com.ctc.wstx.dtd;

import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * Implementation of {@link NameKeySet} suitable for storing large number
 * of entries; basically anything above trivially small sets (4 or less).
 *<p>
 * Notes about usage:
 * <ul>
 *  <li>All Strings contained in {@link NameKey} instances are assumed
 *   interned, so that equality comparison can be done (both for values
 *   stored and keys used)
 *   </li>
 *  <li>It is assumed that sets are never empty, ie. always contain at
 *    least one entry.
 *   </li>
 *  <li>It is assumed that caller has ensured that there are no duplicates
 *    in the set -- this data structure does no further validation.
 *   </li>
 * </ul>
 */
public final class LargeNameKeySet
    extends NameKeySet
{
    /**
     * Let's not bother creating tiny hash areas; should seldom be a problem
     * as smaller sets are usually created using different impl. class.
     */
    final static int MIN_HASH_AREA = 8;

    final boolean mNsAware;

    /**
     * Primary hash area in which NameKeys are added. Sized to be the smallest
     * power of two bigger than number of entries; but at least 4 (it doesn't
     * make sense to create smaller arrays)
     */
    final NameKey[] mNames;

    /**
     * Secondary (spill) area, in which keys whose hash values collide
     * with primary ones are added. Number of buckets is 1/4 of number
     * of primary entries,
     */
    final Bucket[] mBuckets;

    public LargeNameKeySet(boolean nsAware, NameKey[] names)
    {
        mNsAware = nsAware;
        int len = names.length;

        // Let's find the size first... let's except 1/8 slack (88% fill rate)
        int minSize = len + ((len + 7) >> 3);
        // Let's not create hash areas smaller than certain limit
        int tableSize = MIN_HASH_AREA;

        while (tableSize < minSize) {
            tableSize += tableSize;
        }

        mNames = new NameKey[tableSize];
        // and 1/4 of that for spill area... but let's do that lazily

        Bucket[] buckets = null;
        int mask = (tableSize - 1);

        for (int i = 0; i < len; ++i) {
            NameKey nk = names[i];
            int ix = (nk.hashCode() & mask);
            if (mNames[ix] == null) { // no collision
                mNames[ix] = nk;
            } else { // collision, need to add a bucket
                ix >>= 2;

                Bucket old;
                if (buckets == null) {
                    buckets = new Bucket[tableSize >> 2];
                    old = null;
                } else {
                    old = buckets[ix];
                }
                buckets[ix] = new Bucket(nk, old);
            }
        }

        mBuckets = buckets;
    }

    /**
     * @return True if the set contains specified name; false if not.
     */
    public boolean contains(NameKey name)
    {
        NameKey[] hashArea = mNames;
        int index = name.hashCode() & (hashArea.length - 1);
        NameKey res = hashArea[index];

        if (res != null && res.equals(name)) {
            return true;
        }

        Bucket[] buckets = mBuckets;
        if (buckets != null) {
            for (Bucket bucket = buckets[index >> 2]; bucket != null;
                 bucket = bucket.getNext()) {
                res = bucket.getName();
                if (res.equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Method called by debug/error handling code, to get a list of
     * all names contained.
     */
    public void appendNames(StringBuffer sb, String sep)
    {
        // Let's first get the alphabetized list of all names from main hash
        TreeSet ts = new TreeSet();
        for (int i = 0; i < mNames.length; ++i) {
            NameKey name = mNames[i];
            if (name != null) {
                ts.add(name);
            }
        }

        // then spill area
        if (mBuckets != null) {
            for (int i = 0; i < (mNames.length >> 2); ++i) {
                Bucket b = mBuckets[i];
                while (b != null) {
                    ts.add(b.getName());
                    b = b.getNext();
                }
            }
        }

        // And then append them:
        Iterator it = ts.iterator();
        boolean first = true;
        while (it.hasNext()) {
            if (first) {
                first = false;
            } else {
                sb.append(sep);
            }
            sb.append(it.next().toString());
        }
    }

    /*
    ///////////////////////////////////////////////////////////
    // Helper class(es)
    ///////////////////////////////////////////////////////////
     */

    private final static class Bucket
    {
        final NameKey mName;
        
        final Bucket mNext;

        public Bucket(NameKey name, Bucket next) {
            mName = name;
            mNext = next;
        }

        public NameKey getName() { return mName; }
        public Bucket getNext() { return mNext; }

        public boolean contains(NameKey n) {
            return mName.equals(n);
        }
    }
}
