package com.ctc.wstx.util;

import java.lang.reflect.Array;
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

    public static Object growArrayBy50Pct(Object arr)
    {
        if (arr == null) {
            throw new Error("Illegal to pass null; can not determine component type");
        }
        Object old = arr;
        int len = Array.getLength(arr);
        arr = Array.newInstance(arr.getClass().getComponentType(), len + (len >> 1));
        System.arraycopy(old, 0, arr, 0, len);
        return arr;
    }

    public static String[] growArrayBy(String[] arr, int more)
    {
        if (arr == null) {
            return new String[more];
        }
        String[] old = arr;
        int len = arr.length;
        arr = new String[len + more];
        System.arraycopy(old, 0, arr, 0, len);
        return arr;
    }

    public static int[] growArrayBy(int[] arr, int more)
    {
        if (arr == null) {
            return new int[more];
        }
        int[] old = arr;
        int len = arr.length;
        arr = new int[len + more];
        System.arraycopy(old, 0, arr, 0, len);
        return arr;
    }
}
 
