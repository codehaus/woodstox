package com.ctc.wstx.dtd;

import java.util.*;

/**
 * Simple content model validator that accepts no elements, ever; this
 * is true for pure #PCDATA content model as well as EMPTY content model.
 * Can be used as a singleton, since all info needed for diagnostics
 * is passed via methods.
 */
public class EmptyValidator
    extends StructValidator
{
    final static EmptyValidator sInstance = new EmptyValidator();

    private EmptyValidator() {
    }

    public static EmptyValidator getInstance() {
        return sInstance;
    }

    /**
     * Simple; can always (re)use instance itself; no state information
     * is kept.
     */
    public StructValidator newInstance() {
        return this;
    }

    public String tryToValidate(NameKey elemName)
    {
        /* Note: this assumes that this validator is only used for
         * pure #PCDATA elements
         */
        return "No elements allowed in pure #PCDATA content model";
    }

    /**
     * If we ever get as far as element closing, things are all good;
     * can just return null.
     */
    public String fullyValid()
    {
        return null;
    }
}
