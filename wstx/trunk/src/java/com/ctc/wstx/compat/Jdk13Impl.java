package com.ctc.wstx.compat;

import java.util.*;

/**
 * JDK 1.3 compatible implementation; adds off-the-shelf simple data struct
 * instances on top of 1.2.
 */
public class Jdk13Impl
    extends Jdk12Impl
{
    /**
     * Constructor used for creating 'real' instance.
     */
    public Jdk13Impl() {
        super(true); // to prevent 1.2 from creating any obs we don't need
    }

    /**
     * Constructor derived classes call to avoid creation of helper
     * Objects 'real' instance needs (if any)
     */
    protected Jdk13Impl(boolean dummy) {
        super(true);
    }

    /*
    /////////////////////////////////////////
    // Public API
    /////////////////////////////////////////
     */

    // // // Simple accessors

    /**
     * For 1.3, ThreadLocal has problems, and can leak memory (check
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4414045
     * for details, or google for 'ThreadLocal "memory leaks"')
     */
    public boolean leakingThreadLocal() {
        return true;
    }

    // // // Methods for accessing dummy data structures

    public List getEmptyList() {
        return Collections.EMPTY_LIST;
    }

    public Map getEmptyMap() {
        return Collections.EMPTY_MAP;
    }

    public Set getEmptySet() {
        return Collections.EMPTY_SET;
    }

    // // // Methods for accessing 'advanced' data structs:

    // No improvements over 1.2...
    //public HashMap getInsertOrderedMap();
    //public HashMap getInsertOrderedMap(int initialSize);

    // // // Methods for injecting root cause to exceptions

    // No improvements over 1.2...
    //public boolean setInitCause(Throwable newT, Throwable rootT);
}
