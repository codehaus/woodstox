package com.ctc.wstx.stax.io;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;

import javax.xml.stream.XMLStreamException;

import com.ctc.wstx.util.URLUtil;

/**
 * Default non-caching implementation of {@link WstxInputResolver}.
 */
public final class DefaultInputResolver
    implements WstxInputResolver
{
    /**
     * Default buffer size should never really be needed, when input
     * sources are properly chained, but let's have some sane default
     * for weird cases. 1000k chars means 2Kbyte buffer.
     */
    final static int DEFAULT_BUFFER_SIZE = 1000;

    private final static DefaultInputResolver sInstance = new DefaultInputResolver();

    /*
    ////////////////////////////
    // Life-cycle
    ////////////////////////////
    */

    protected DefaultInputResolver() {
    }

    public static DefaultInputResolver getInstance() {
        return sInstance;
    }


    /*
    ////////////////////////////
    // Public API
    ////////////////////////////
    */

    /**
     * Basic external resource resolver implementation; usable both with
     * DTD and entity resolution.
     *
     * @param refCtxt Input context, relative to which reference was made.
     *   May be null, if context is not known.
     * @param entityId Name/id of the entity being expanded, if this is an
     *   entity expansion; null otherwise (for example, when resolving external
     *   subset).
     * @param publicId Public identifier of the resource, if known; null/empty
     *   otherwise. Default implementation just ignores the identifier.
     * @param systemId System identifier of the resource. Although interface
     *   allows null/empty, default implementation considers this an error.
     * @param assumedLoc Assumed default location. Default resolver happily
     *   just uses this location.
     *
     * @return Input source, if entity could be resolved; null if it could
     *   not be resolved. In latter case processor may use its own default
     *   resolution mechanism.
     */
    public WstxInputSource resolveReference(WstxInputSource refCtxt, String entityId,
                                            String publicId, String systemId,
                                            URL assumedLoc)
        throws IOException, XMLStreamException
    {
        // First, let's do actual location resolution:
        URL resolvedURL = resolveURL(refCtxt, entityId, publicId, systemId,
                                     assumedLoc);

        if (resolvedURL == null) {
            return null;
        }

        /* And then create the input source. Note that by default URL's
         * own input stream creation creates buffered reader -- for us
         * that's useless and wasteful (adds one unnecessary level of
         * caching, halving the speed due to copy operations needed), so
         * let's avoid it.
         */
        InputStream in;

        if ("file".equals(resolvedURL.getProtocol())) {
            in = new FileInputStream(resolvedURL.getPath());
        } else {
            in = resolvedURL.openStream();
        }

        int bufLen = (refCtxt == null) ? DEFAULT_BUFFER_SIZE : refCtxt.getInputBufferLength();
        StreamBootstrapper bs = StreamBootstrapper.getInstance(in, publicId, systemId, bufLen);
        /* !!! TBI: Should try to figure out how to pass XMLReporter here,
         *   so that warnings could be reported?
         */
        Reader r = bs.bootstrapInput(false, null);

        // true -> close input source after finished reading
        ReaderSource rsrc = InputSourceFactory.constructReaderSource
            (refCtxt, entityId, bs, publicId, systemId, assumedLoc, r, true, bufLen);
        return rsrc;
    }

    /*
    ////////////////////////////
    // Overridable methods:
    ////////////////////////////
    */

    /**
     * Overridable URL resolution mechanism; default implementation returns
     * the assumed location as the resolved location.
     */
    protected URL resolveURL(WstxInputSource refCtxt, String entityId,
                             String publicId, String systemId,
                             URL assumedLoc)
        throws IOException
    {
        return assumedLoc;
    }
}
