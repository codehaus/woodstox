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

import java.io.Writer;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ctc.wstx.stax.cfg.ReaderConfig;
import com.ctc.wstx.stax.io.WstxInputSource;
import com.ctc.wstx.stax.stream.StreamScanner;

/**
 * Interface that defines functionality for Objects that act as DTD reader
 * proxies. Proxy approach is just used to allow code insulation; it would
 * have been possible to use factory/instance approach too, but this way
 * we end up with few more classes.
 */
public abstract class DTDReaderProxy
{
    /**
     * Method called to read in the internal subset definition.
     */
    public abstract DTDSubset readInternalSubset(StreamScanner master, WstxInputSource input,
                                        ReaderConfig cfg)
        throws IOException, XMLStreamException;
    
    /**
     * Method called to read in the external subset definition.
     */
    public abstract DTDSubset readExternalSubset
        (StreamScanner master, WstxInputSource src, ReaderConfig cfg,
         DTDSubset intSubset)
        throws IOException, XMLStreamException;
    
    /**
     * Method similar to {@link #readInternalSubset}, in that it skims
     * through structure of internal subset, but without doing any sort
     * of validation, or parsing of contents. Method may still throw an
     * exception, if skipping causes EOF or there's an I/O problem.
     */
    public abstract void skipInternalSubset(StreamScanner master, WstxInputSource input,
                                            ReaderConfig cfg)
        throws IOException, XMLStreamException;
}

