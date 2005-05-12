package com.ctc.wstx.dtd;

import java.io.Writer;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.ctc.wstx.api.ReaderConfig;
import com.ctc.wstx.dtd.DTDReaderProxy;
import com.ctc.wstx.dtd.DTDSubset;
import com.ctc.wstx.exc.WstxException;
import com.ctc.wstx.io.WstxInputSource;
import com.ctc.wstx.sr.StreamScanner;

/**
 * Proxy implementation that will act as a dummy DTD reader that can only
 * skip internal subset, but not do any actual reading. Used by stream readers
 * that do not need to support actual DTD handling.
 */
public final class MinimalDTDReaderProxy
    extends DTDReaderProxy
{
    final static MinimalDTDReaderProxy sInstance = new MinimalDTDReaderProxy();

    final static String NOT_IMPL = "Method not implemented by non-DTD-handling stream readers";

    /*
    ////////////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////////////
     */

    private MinimalDTDReaderProxy() { }

    public static MinimalDTDReaderProxy getInstance() {
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
        throw new WstxException(NOT_IMPL);
    }
    
    /**
     * Method called to read in the external subset definition.
     */
    public DTDSubset readExternalSubset
        (StreamScanner master, WstxInputSource src, ReaderConfig cfg,
         DTDSubset intSubset)
        throws IOException, XMLStreamException
    {
        throw new WstxException(NOT_IMPL);
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

