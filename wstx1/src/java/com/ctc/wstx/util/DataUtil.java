package com.ctc.wstx.util;

import java.util.*;

public final class DataUtil
{
    private DataUtil() { }

    /*
    ////////////////////////////////////////////////////////////
    // Methods for common operations on std data structs
    ////////////////////////////////////////////////////////////
    */

    /**
     * Method that can be used to efficiently check if 2 collections
     * share at least one common element.
     *
     * @return True if there is at least one element that's common
     *   to both Collections, ie. that is contained in both of them.
     */
    public static boolean anyValuesInCommon(Collection c1, Collection c2)
    {
        // Let's always iterate over smaller collection:
        if (c1.size() > c2.size()) {
            Collection tmp = c1;
            c1 = c2;
            c2 = tmp;
        }
        Iterator it = c1.iterator();
        while (it.hasNext()) {
            if (c2.contains(it.next())) {
                return true;
            }
        }
        return false;
    }

    public static Object[] growArrayBy50Pct(Object[] arr)
    {
        if (arr == null) {
            return new Object[16];
        }
        Object[] old = arr;
        int len = arr.length;
        arr = new Object[len + (len >> 1)];
        System.arraycopy(old, 0, arr, 0, len);
        return arr;
    }
}
 
