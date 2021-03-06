package com.ctc.wstx.util;

import com.ctc.wstx.compat.JdkFeatures;

public final class ExceptionUtil
{
    private ExceptionUtil() { }

    /**
     * Method that can be used to convert any Throwable to a RuntimeException;
     * conversion is only done for checked exceptions.
     */
    public static void throwRuntimeException(Throwable t)
    {
        // Unchecked? Can re-throw as is
        throwIfUnchecked(t);
        // Otherwise, let's just change its type:
        RuntimeException rex = new RuntimeException("[was "+t.getClass()+"] "+t.getMessage());
        // And if possible, indicate the root cause (1.4+ only)
        JdkFeatures.getInstance().setInitCause(rex, t);
        throw rex;
    }

    public static void throwAsIllegalArgument(Throwable t)
    {
        // Unchecked? Can re-throw as is
        throwIfUnchecked(t);
        // Otherwise, let's just change its type:
        IllegalArgumentException rex = new IllegalArgumentException("[was "+t.getClass()+"] "+t.getMessage());
        // And if possible, indicate the root cause (1.4+ only)
        JdkFeatures.getInstance().setInitCause(rex, t);
        throw rex;
    }

    public static void throwIfUnchecked(Throwable t)
    {
        // If it's not checked, let's throw it as is
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
    }

    /**
     * This method is just added for convenience, and only to be used for
     * assertion style of exceptions. For errors that actually occur, method
     * with the string arg should be called instead.
     */
    public static void throwGenericInternal()
    {
        throwInternal(null);
    }

    public static void throwInternal(String msg)
    {
        if (msg == null) {
            msg = "[no description]";
        }
        throw new RuntimeException("Internal error: "+msg);
    }

    /*
    ///////////////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////////////
     */
}

