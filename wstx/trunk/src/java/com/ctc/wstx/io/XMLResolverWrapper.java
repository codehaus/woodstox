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

            if (src == null) { // null -> should use the default mechanism
                return null;
            }

            int bufLen = (refCtxt == null) ? DEFAULT_BUFFER_SIZE : refCtxt.getInputBufferLength();
            InputBootstrapper bs = null;

            if (src instanceof InputStream) {
                bs = StreamBootstrapper.getInstance
                    ((InputStream) src, publicId, systemId, bufLen);
            } else if (src instanceof Reader) {
                /* 10-Feb-2005, TSa: Strictly speaking, StAX 1.0 API does not
                 *   allow a Reader to be returned... but it really should,
                 *   so let's just allow it here (it's easy to implement, too)
                 */
                bs = ReaderBootstrapper.getInstance
                    ((Reader) src, publicId, systemId, bufLen, null);
            } else {
                throw new IOException("Unimplemented type of input source: "+src.getClass()+".");
            }

            /* !!! TBI: Should try to figure out how to pass XMLReporter
             *   here, so that warnings could be reported?
             */
            Reader r = bs.bootstrapInput(false, null);
            
            // true -> close input source after finished reading
            return InputSourceFactory.constructReaderSource
                (refCtxt, entityId, bs, publicId, systemId, assumedLoc, r, true, bufLen);
        } catch (XMLStreamException wex) {
            IOException ioe = new IOException(wex.toString());
            JdkFeatures.getInstance().setInitCause(ioe, wex);
            throw ioe;
        }
    }
}
