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
 * Content specification that defines content model that has
 * multiple alternative elements.
 */
public class ChoiceContentSpec
    extends ContentSpec
{
    /*
    ///////////////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////////////
     */

    public ChoiceContentSpec(int arity)
    {
        super(arity);
    }

    public static ChoiceContentSpec construct(int arity, List subSpecs)
    {
        // !!! TBI
        return new ChoiceContentSpec(arity);
    }

    /*
    ///////////////////////////////////////////////////
    // Public API
    ///////////////////////////////////////////////////
     */

    public boolean hasMixed() { return false; }

    /*
    ///////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////
     */

}
