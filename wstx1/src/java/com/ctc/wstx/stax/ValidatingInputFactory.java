/* Woodstox XML processor.
 *<p>
 * Copyright (c) 2004 Tatu Saloranta, tatu.saloranta@iki.fi
 *<p>
 * You can redistribute this work and/or modify it under the terms of
 * LGPL (Lesser Gnu Public License), as published by
 * Free Software Foundation (http://www.fsf.org). No warranty is
 * implied. See LICENSE for details about licensing.
 */

package com.ctc.wstx.stax;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.*;

import javax.xml.stream.*;
import javax.xml.stream.util.XMLEventAllocator;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import com.ctc.wstx.util.SimpleCache;
import com.ctc.wstx.stax.cfg.ReaderConfig;
import com.ctc.wstx.stax.dtd.DTDId;
import com.ctc.wstx.stax.dtd.DTDSubset;
import com.ctc.wstx.stax.dtd.FullDTDReaderProxy;
import com.ctc.wstx.stax.exc.WstxIOException;
import com.ctc.wstx.stax.io.WstxInputResolver;
import com.ctc.wstx.stax.io.WstxInputSource;
import com.ctc.wstx.stax.stream.ReaderCreator;

/**
 * Input factory that contains full set of cursor API functionality,
 * including full DTD handling. It does not contain Event API, which
 * means that this is a subset of full StAX specification; subset that
 * is useful if event API is not needed. It builds on top of
 * {@link MinimalInputFactory}, basically just adding the connecting
 * code to use real DTD handling implementation.
 *
 * @see MinimalInputFactory
 */
public final class ValidatingInputFactory
    extends MinimalInputFactory
{
    /*
    /////////////////////////////////////////////////////
    // Actual storage of configuration settings
    /////////////////////////////////////////////////////
     */

    // // // Other configuration objects:

    protected SimpleCache mDTDCache = null;

    /*
    /////////////////////////////////////////////////////
    // Life-cycle:
    /////////////////////////////////////////////////////
     */

    public ValidatingInputFactory() {
        super(FullDTDReaderProxy.getInstance(), false);
    }

    /**
     * Need to add this method, since we have no base class to do it...
     */
    public static ValidatingInputFactory newValidatingInstance() {
        return new ValidatingInputFactory();
    }

    /*
    /////////////////////////////////////////////////////
    // ReaderCreator implementation
    /////////////////////////////////////////////////////
     */

    // // // Configuration access methods:

    /**
     * Method readers created by this factory call, if DTD caching is
     * enabled, to see if an external DTD (subset) has been parsed
     * and cached earlier.
     */
    public synchronized DTDSubset findCachedDTD(DTDId id)
    {
        return (mDTDCache == null) ?
            null : (DTDSubset) mDTDCache.find(id);
    }

    // // // Callbacks for updating shared information

    // Base class has proper implementation already
    //public synchronized void updateSymbolTable(SymbolTable t)

    public synchronized void addCachedDTD(DTDId id, DTDSubset extSubset)
    {
        if (mDTDCache == null) {
            mDTDCache = new SimpleCache(mConfig.getDtdCacheSize());
        }
        mDTDCache.add(id, extSubset);
    }

    /*
    /////////////////////////////////////////////////////
    // Subset of XMLInputFactory API:
    /////////////////////////////////////////////////////
     */

    public Object getProperty(String name)
    {
        // !!! TBI: Add support for validation etc
        return super.getProperty(name);
    }

    //public XMLEventAllocator getEventAllocator();
    
    //public XMLReporter getXMLReporter()
    //public XMLResolver getXMLResolver()
    
    public boolean isPropertySupported(String name)
    {
        // !!! TBI: Add support for validation etc
        return super.isPropertySupported(name);
    }

    //public void setEventAllocator(XMLEventAllocator allocator);

    public void setProperty(String propName, Object value)
    {
        // !!! TBI: Add support for validation etc
        super.setProperty(propName, value);
    } 

    /*
    /////////////////////////////////////////
    // Type-safe configuration access:
    /////////////////////////////////////////
     */

    /*
    /////////////////////////////////////////////////////
    // Internal methods:
    /////////////////////////////////////////////////////
     */

    /*
    /////////////////////////////////////////////////////
    // Trivial test driver, to check loading of the
    // class and instance creation work
    /////////////////////////////////////////////////////
     */

    public static void main(String[] args)
        throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java "+ValidatingInputFactory.class+" [input file]");
            System.exit(1);
        }
        ValidatingInputFactory f = new ValidatingInputFactory();

        System.out.println("Creating Val. str. reader for file '"+args[0]+"'.");
        XMLStreamReader r = f.createXMLStreamReader(new java.io.FileInputStream(args[0]));
        r.close();
        System.out.println("Reader created and closed ok, exiting.");
    }
}

