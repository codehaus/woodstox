package com.ctc.wstx.stax.dtd;

import java.util.*;

/**
 * Class that encapsulates full content specification for a single
 * ELEMENT definition from DTD.
 */
public abstract class ContentSpec
{
    final static int ARITY_ZERO = 0;
    final static int ARITY_ONE = 1; // no modifier
    final static int ARITY_ZERO_OR_ONE = 2; // '?'
    final static int ARITY_ZERO_OR_MORE = 3; // '*'
    final static int ARITY_ONE_OR_MORE = 4; // '+'

    final int mArity;

    /*
    ///////////////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////////////
     */

    public ContentSpec(int arity)
    {
        mArity = arity;
    }

    /*
    ///////////////////////////////////////////////////
    // Public API
    ///////////////////////////////////////////////////
     */

    public final int getArity() {
        return mArity;
    }

    public abstract boolean hasMixed();

    /*
    ///////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////
     */

}
