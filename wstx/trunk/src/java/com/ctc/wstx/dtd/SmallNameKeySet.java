package com.ctc.wstx.dtd;

import java.util.Collection;
import java.util.Iterator;

/**
 * NameKeySet implementation suitable for storing small set of NameKey
 * values (generally 8 or less). Uses linear search, and is thus the
 * most compact presentation for a set
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
public final class SmallNameKeySet
    extends NameKeySet
{
    final boolean mNsAware;

    final String[] mStrings;

    public SmallNameKeySet(boolean nsAware, NameKey[] names)
    {
        mNsAware = nsAware;
        int len = names.length;
        if (len == 0) { // sanity check
            throw new Error("Trying to construct empty NameKeySet");
        }
        mStrings = new String[nsAware ? (len+len) : len];
        for (int out = 0, in = 0; in < len; ++in) {
            NameKey nk = names[in];
            if (nsAware) {
                mStrings[out++] = nk.getPrefix();
            }
            mStrings[out++] = nk.getLocalName();
        }
    }

    public boolean hasMultiple() {
        return mStrings.length > 1;
    }

    /**
     * @return True if the set contains specified name; false if not.
     */
    public boolean contains(NameKey name)
    {
        int len = mStrings.length;
        String ln = name.getLocalName();
        String[] strs = mStrings;

        if (mNsAware) {
            String prefix = name.getPrefix();
            if (strs[1] == ln && strs[0] == prefix) {
                return true;
            }
            for (int i = 2; i < len; i += 2) {
                if (strs[i+1] == ln && strs[i] == prefix) {
                    return true;
                }
            }
        } else {
            if (strs[0] == ln) {
                return true;
            }
            for (int i = 1; i < len; ++i) {
                if (strs[i] == ln) {
                    return true;
                }
            }
        }

        return false;
    }

    public void appendNames(StringBuffer sb, String sep)
    {
        for (int i = 0; i < mStrings.length; ) {
            if (i > 0) {
                sb.append(sep);
            }
            if (mNsAware) {
                String prefix = mStrings[i++];
                if (prefix != null) {
                    sb.append(prefix);
                    sb.append(':');
                }
            }
            sb.append(mStrings[i++]);
        }
    }
}
