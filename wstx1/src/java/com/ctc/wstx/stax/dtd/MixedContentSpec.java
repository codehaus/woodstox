/* Woodstox XML processor.
 *<p>
 * Copyright (c) 2004 Tatu Saloranta, tatu.saloranta@iki.fi
 *<p>
 * You can redistribute this work and/or modify it under the terms of
 * LGPL (Lesser Gnu Public License), as published by
 * Free Software Foundation (http://www.fsf.org). No warranty is
 * implied. See LICENSE for details about licensing.
 */

package com.ctc.wstx.stax.dtd;

import java.util.*;

import com.ctc.wstx.util.StringVector;

/**
 * Content specification that defines content model that contains mixed
 * content, that is, has the leading #PCDATA declaration.
 */
public class MixedContentSpec
    extends ContentSpec
{
    /*
    ///////////////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////////////
     */

    public MixedContentSpec(boolean any)
    {
        super(any ? ARITY_ZERO : ARITY_ZERO_OR_MORE);
    }

    public static MixedContentSpec construct(StringVector elems)
    {
        // !!! TBI
        return new MixedContentSpec(false);
    }

    public static MixedContentSpec constructAny()
    {
        return new MixedContentSpec(true);
    }

    /*
    ///////////////////////////////////////////////////
    // Public API
    ///////////////////////////////////////////////////
     */

    public boolean hasMixed() {
        return true;
    }

    /*
    ///////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////
     */

}
