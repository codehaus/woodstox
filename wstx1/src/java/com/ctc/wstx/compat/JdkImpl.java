package com.ctc.wstx.compat;

import java.util.*;

/**
 * This is the interface used to access JDK-dependant functionality; generally
 * things that later JDKs have in their APIs, but that can be simulated
 * with earlier JDKs to some degree.
 */
public abstract class JdkImpl
{
    protected JdkImpl() { }

    /*
    /////////////////////////////////////////
    // Public API
    /////////////////////////////////////////
     */

    // // // Methods for accessing dummy data structures

    public abstract List getEmptyList();
    public abstract Map getEmptyMap();
    public abstract Set getEmptySet();

    // // // Methods for accessing 'advanced' data structs:

    public abstract Map getInsertOrderedMap();
    public abstract Map getInsertOrderedMap(int initialSize);

    public abstract Map getLRULimitMap(int maxSize);

    // // // Methods for injecting root cause to exceptions

    /**
     * Method that sets init cause of the specified Throwable to be
     * another specified Throwable. Note: not all JDKs support such
     * functionality.
     * 
     * @return True if call succeeds, false if not.
     */
    public abstract boolean setInitCause(Throwable newT, Throwable rootT);
}

