package com.ctc.wstx.util;

import java.util.*;

import com.ctc.wstx.compat.JdkFeatures;

/**
 * Simple Map implementation usable for caches where contents do not
 * expire.
 *<p>
 * TODO: for now just 'randomly' chooses entry to be removed when Map
 * overflows... should do something smarter.
 *<p>
 * Note: what sucks is that JDK 1.4 has what would be 98% what we need,
 * LinkedHashMap. But since Wstx aims to be 1.2 (or at the very least,
 * 1.3) compatible, can't use it (yet?)
 */

public final class SimpleCache
{
    final Map mItems;

    final int mMaxSize;

    public SimpleCache(int maxSize)
    {
        mItems = JdkFeatures.getInstance().getLRULimitMap(maxSize);
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
}
