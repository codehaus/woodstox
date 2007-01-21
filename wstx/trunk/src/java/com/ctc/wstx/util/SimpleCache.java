package com.ctc.wstx.util;

import java.util.*;

/**
 * Simple Map implementation usable for caches where contents do not
 * expire.
 *<p>
 * For JDK 1.4 and up, will use <code>LinkedHashMap</code> in LRU mode,
 * so expiration does happen using typical LRU algorithm. For 1.3 and
 * below will just discard an entry in random.
 *<p>
 * Note: we probably should use weak references, or something similar
 * to limit maximum memory usage. This could be implemented in many
 * ways, perhaps by using two areas: first, smaller one, with strong
 * refs, and secondary bigger one that uses soft references.
 */

public final class SimpleCache
{
    final LimitMap mItems;

    final int mMaxSize;

    public SimpleCache(int maxSize)
    {
        /* Note: resulting Map will take care of purging of extra
         * entries, for JDK 1.4: but for pre-1.4 there is no automatic
         * purging.
         */
        mItems = new LimitMap(maxSize);
        mMaxSize = maxSize;
    }

    public Object find(Object key) {
        return mItems.get(key);
    }

    public void add(Object key, Object value)
    {
        mItems.put(key, value);
        /* To support pre-1.4 JDKs (1.4+ handle this via LRU limit map
         * instance)
         */
        if (mItems.size() >= mMaxSize) {
            // This is crude and ugly, but...
            Iterator it = mItems.entrySet().iterator();
            while (it.hasNext()) {
                Object foo = it.next();
                it.remove();
                if (mItems.size() < mMaxSize) {
                    break;
                }
            }
        }
    }

    /*
    /////////////////////////////////////////////
    // Helper classes
    /////////////////////////////////////////////
     */

    final static class LimitMap
        extends LinkedHashMap
    {
        final int mMaxSize;

        public LimitMap(int size)
        {
            super(size, 0.8f, true);
            // Let's not allow silly low values...
            mMaxSize = size;
        }
        
        public boolean removeEldestEntry(Map.Entry eldest) {
            return (size() >= mMaxSize);
        }
    }
}
