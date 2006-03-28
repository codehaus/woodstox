package com.ctc.wstx.compat;

import java.util.*;

/**
 * JDK 1.4 compatible implementation; adds support for setting Exception
 * root cause, as well as for <code>java.util.LinkedHashMap</code>.
 */
public class Jdk14Impl
    extends Jdk13Impl
{
    /**
     * Constructor used for creating 'real' instance.
     */
    public Jdk14Impl() {
        super(true); // to prevent 1.3 and 1.2 from creating any obs we don't need
    }

    /**
     * Constructor derived classes call to avoid creation of helper
     * Objects 'real' instance needs (if any)
     */
    protected Jdk14Impl(boolean dummy) {
        super(true);
    }

    /*
    /////////////////////////////////////////
    // Public API
    /////////////////////////////////////////
     */

    // // // Simple accessors

    /**
     * 1.4 finally has correct AND fast ThreadLocal implementation
     * (see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4414045
     * for details); and following versions should be fine.
     */
    public final boolean leakingThreadLocal() {
        return false;
    }

    // // // Methods for accessing dummy data structures

    // 1.3 had them right already
    //public List getEmptyList();
    //public Map getEmptyMap();
    //public Set getEmptySet();

    // // // Methods for accessing 'advanced' data structs:

    public HashMap getInsertOrderedMap() {
        return new LinkedHashMap();
    }

    public HashMap getInsertOrderedMap(int initialSize) {
        return new LinkedHashMap(initialSize);
    }

    public HashMap getLRULimitMap(int maxSize) {
        int initSize = maxSize;
        return new LimitMap(initSize, maxSize, 0.8f);
    }

    // // // Methods for injecting root cause to exceptions

    public boolean setInitCause(Throwable newT, Throwable rootT)
    {
        newT.initCause(rootT);
        return true;
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

        public LimitMap(int initialSize, int maxSize, float loadFactor) {
            super(initialSize, loadFactor, true);
            // Let's not allow silly low values...
            mMaxSize = (maxSize < 4) ? 4 : maxSize;
        }
        
        public boolean removeEldestEntry(Map.Entry eldest) {
            return (size() >= mMaxSize);
        }
    }
}

