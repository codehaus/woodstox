package com.ctc.wstx.io;

import java.io.*;
import java.net.URL;

import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;

import com.ctc.wstx.util.URLUtil;
import com.ctc.wstx.compat.JdkFeatures;

/**
 * Simple wrapper around standard StAX XMLResolver.
 */
public final class XMLResolverWrapper
    implements WstxInputResolver
{
    /**
     * Default buffer size should never really be needed, when input
     * sources are properly chained, but let's have some sane default
     * for weird cases. 1000k chars means 2kByte buffer.
     */
    final static int DEFAULT_BUFFER_SIZE = 1000;

    final static String DEFAULT_ENCODING = "UTF-8";

    final XMLResolver mResolver;

    public XMLResolverWrapper(XMLResolver r) {
        mResolver = r;
    }

    public WstxInputSource resolveReference(WstxInputSource refCtxt, String entityId,
                                            String publicId, String systemId,
                                            URL assumedLoc)
        throws IOException
    {
        /* ??? Not sure what 'namespace' arg (last one) should be; for now,
         *   will just pass the entity name.
         */
        Object src;
        try {
            src = mResolver.resolveEntity(publicId, systemId,
                                          assumedLoc.toString(), entityId);

            if (src == null) {
                return null;
            }
            if (!(src instanceof InputStream)) {
                throw new IOException("Unimplemented type of input source: "+src.getClass()+".");
            }
            
            int bufLen = (refCtxt == null) ? DEFAULT_BUFFER_SIZE : refCtxt.getInputBufferLength();
            StreamBootstrapper bs = StreamBootstrapper.getInstance
                ((InputStream) src, publicId, systemId, bufLen);
            /* !!! TBI: Should try to figure out how to pass XMLReporter here,
             *   so that warnings could be reported?
             */
            Reader r = bs.bootstrapInput(false, null);

            // true -> close input source after finished reading
            ReaderSource rsrc = InputSourceFactory.constructReaderSource
                (refCtxt, entityId, bs, publicId, systemId, assumedLoc, r, true, bufLen);
            return rsrc;
        } catch (XMLStreamException wex) {
            IOException ioe = new IOException(wex.toString());
            JdkFeatures.getInstance().setInitCause(ioe, wex);
            throw ioe;
        }
    }
}
