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
 * Proxy implementation that will act as a full-featured DTD reader. 
 * Used by stream readers that do support full DTD handling.
 */
public final class FullDTDReaderProxy
    extends DTDReaderProxy
{
    final static FullDTDReaderProxy sInstance = new FullDTDReaderProxy();

    /*
    ////////////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////////////
     */

    private FullDTDReaderProxy() { }

    public static FullDTDReaderProxy getInstance() {
        return sInstance;
    }

    /*
    ////////////////////////////////////////////////
    // Public API
    ////////////////////////////////////////////////
     */

    /**
     * Method called to read in the internal subset definition.
     */
    public DTDSubset readInternalSubset(StreamScanner master, WstxInputSource input,
                                        ReaderConfig cfg)
        throws IOException, XMLStreamException
    {
        return FullDTDReader.readInternalSubset(master, input, cfg);
    }
    
    /**
     * Method called to read in the external subset definition.
     */
    public DTDSubset readExternalSubset
        (StreamScanner master, WstxInputSource src, ReaderConfig cfg,
         DTDSubset intSubset)
        throws IOException, XMLStreamException
    {
        return FullDTDReader.readExternalSubset(master, src, cfg, intSubset);
    }
    
    /**
     * Method similar to {@link #readInternalSubset}, in that it skims
     * through structure of internal subset, but without doing any sort
     * of validation, or parsing of contents. Method may still throw an
     * exception, if skipping causes EOF or there's an I/O problem.
     */
    public void skipInternalSubset(StreamScanner master, WstxInputSource input,
                                   ReaderConfig cfg)
        throws IOException, XMLStreamException
    {
        MinimalDTDReader.skipInternalSubset(master, input, cfg);
    }
}

