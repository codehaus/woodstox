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

/**
 * Content specification that defines model that has sequence of one or more
 * elements that have to come in the specified order.
 */
public class SeqContentSpec
    extends ContentSpec
{
    /*
    ///////////////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////////////
     */

    public SeqContentSpec(int arity)
    {
        super(arity);
    }

    public static SeqContentSpec construct(int arity, List subSpecs)
    {
        // !!! TBI
        return new SeqContentSpec(arity);
    }

    /*
    ///////////////////////////////////////////////////
    // Public API
    ///////////////////////////////////////////////////
     */

    public boolean hasMixed() {
        // No, only choice has mixed stuff
        return false;
    }

    /*
    ///////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////
     */

}
