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

import javax.xml.stream.XMLReporter;

import com.ctc.wstx.stax.exc.WstxException;

/**
 * This is the abstract base class that defines API for Objects that contain
 * specification read from DTDs (internal and external subsets).
 *<p>
 * API is separated from its implementations so that different XML reader
 * subsets can be created; specifically ones with no DTD processing 
 * functionality.
 */
public abstract class DTDSubset
{
    /*
    //////////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////////
     */

    protected DTDSubset() { }

    /**
     * Method that will combine definitions from this internal subset with
     * definitions from passed-in external subset, producing a new combined
     * DTDSubset instance.
     */
    public abstract DTDSubset combineWithExternalSubset(DTDSubset extSubset,
                                                        XMLReporter rep)
        throws WstxException;

    /*
    //////////////////////////////////////////////////////
    // Public API
    //////////////////////////////////////////////////////
     */

    public abstract boolean isCachable();
    
    public abstract Map getGeneralEntityMap();

    public abstract List getGeneralEntityList();

    public abstract Map getParameterEntityMap();

    public abstract Map getNotationMap();

    public abstract List getNotationList();

    public abstract Map getElementMap();

    /**
     * Method used in determining whether cached external subset instance
     * can be used with specified internal subset. If ext. subset references
     * any parameter entities int subset (re-)defines, it can not; otherwise
     * it can be used.
     *
     * @return True if this (external) subset refers to a parameter entity
     *    defined in passed-in internal subset.
     */
    public abstract boolean isReusableWith(DTDSubset intSubset);
}
