package com.ctc.wstx.compat;

import java.util.*;

/**
 * JDK 1.2 compatible basic vanilla no-frills implementation. Used as
 * the base for most other wrappers as well.
 */
public class Jdk12Impl
    extends JdkImpl
{
    private final List mEmptyList;
    private final Map mEmptyMap;
    private final Set mEmptySet;

    /**
     * Constructor used for creating 'real' instance; creates the
     * shared data structs needed.
     */
    public Jdk12Impl() {
        mEmptyList = Collections.unmodifiableList(new ArrayList(1));
        mEmptyMap = Collections.unmodifiableMap(new HashMap(4));
        mEmptySet = Collections.unmodifiableSet(new HashSet(4));
    }

    /**
     * Constructor derived classes call to avoid creation of helper
     * Objects 'real' instance needs
     */
    protected Jdk12Impl(boolean dummy) {
        mEmptyList = null;
        mEmptyMap = null;
        mEmptySet = null;
    }

    /*
    /////////////////////////////////////////
    // Public API
    /////////////////////////////////////////
     */

    // // // Methods for accessing dummy data structures

    public List getEmptyList() {
        return mEmptyList;
    }

    public Map getEmptyMap() {
        return mEmptyMap;
    }

    public Set getEmptySet() {
        return mEmptySet;
    }

    // // // Methods for accessing 'advanced' data structs:

    /**
     * Alas, 1.2 doesn't have LinkedHashMap; only available from 1.4+, so
     * let's just create a standard HashMap.
     */
    public Map getInsertOrderedMap() {
        return new HashMap();
    }

    /**
     * Alas, 1.2 doesn't have LinkedHashMap; only available from 1.4+, so
     * let's just create a standard HashMap.
     */
    public Map getInsertOrderedMap(int initialSize) {
        return new HashMap(initialSize);
    }

    /**
     * 1.2 doesn't have LinkedHashMap, so as usual, let's just create a
     * HashMap
     */
    public Map getLRULimitMap(int maxSize) {
        return new HashMap(5 + maxSize);
    }

    // // // Methods for injecting root cause to exceptions

    /**
     * Too bad it's only 1.4+ that can set the root cause...
     * 
     * @return True if call succeeds, false if not.
     */
    public boolean setInitCause(Throwable newT, Throwable rootT)
    {
        // Nothing we can do here...
        return false;
    }
}
