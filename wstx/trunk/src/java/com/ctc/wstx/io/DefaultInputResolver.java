package com.ctc.wstx.io;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import com.ctc.wstx.exc.WstxException;
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
    // Factory methods
    ////////////////////////////
    */

    /**
     * Factory method that accepts various types of Objects, and tries to
     * create a {@link WstxInputSource} from it.
     *
     * @param parent Input source context active when resolving a new
     *    "sub-source".
     * @param refId Identifier of the entity to be expanded, if any; may be
     *    null
     * @param o Object that should provide the new input source; non-type safe
     */
    public static WstxInputSource sourceFrom(WstxInputSource parent, String refId, Object o)
        throws IllegalArgumentException, IOException, WstxException
    {
        int bufLen = (parent == null) ? DEFAULT_BUFFER_SIZE : parent.getInputBufferLength();

        if (o instanceof Source) {
            if (o instanceof StreamSource) {
                StreamSource src = (StreamSource) o;
                // First; maybe we have a Reader?
                // !!! TBI
            }
            throw new IllegalArgumentException("Can not use other Source objects than StreamSource: got "+o.getClass());
        }
        if (o instanceof URL) {
            return sourceFromURL(parent, refId, bufLen, (URL) o);
        }
        if (o instanceof InputStream) {
            return sourceFromIS(parent, refId, bufLen, (InputStream) o, null, null);
        }
        if (o instanceof Reader) {
            return sourceFromR(parent, refId, bufLen, (Reader) o, null, null);
        }
        if (o instanceof String) {
            return sourceFromString(parent, refId, bufLen, (String) o);
        }

        throw new IllegalArgumentException("Unrecognized input argument type for sourceFrom(): "+o.getClass());
    }

    public static WstxInputSource sourceFromURL(WstxInputSource parent, String refId,
                                                int bufLen, URL url)
        throws IOException, WstxException
    {
        /* And then create the input source. Note that by default URL's
         * own input stream creation creates buffered reader -- for us
         * that's useless and wasteful (adds one unnecessary level of
         * caching, halving the speed due to copy operations needed), so
         * let's avoid it.
         */
        InputStream in = URLUtil.optimizedStreamFromURL(url);
        String sysId = url.toExternalForm();
        StreamBootstrapper bs = StreamBootstrapper.getInstance(in, null, sysId, bufLen);
        /* !!! TBI: Should try to figure out how to pass XMLReporter here,
         *   so that warnings could be reported?
         */
        Reader r = bs.bootstrapInput(false, null);
        return InputSourceFactory.constructReaderSource
            (parent, refId, bs, null, sysId, url, r, true, bufLen);
    }

    public static WstxInputSource sourceFromString(WstxInputSource parent, String refId,
                                                   int bufLen, String sysId)
        throws IOException, WstxException
    {
        URL url = (parent == null) ? null : parent.getSource();
        url = URLUtil.urlFromSystemId(sysId, url);

        InputStream in = URLUtil.optimizedStreamFromURL(url);
        StreamBootstrapper bs = StreamBootstrapper.getInstance(in, null, sysId, bufLen);
        Reader r = bs.bootstrapInput(false, null);
        return InputSourceFactory.constructReaderSource
            (parent, refId, bs, null, sysId, url, r, true, bufLen);
    }

    public static WstxInputSource sourceFromIS(WstxInputSource parent, String refId,
                                               int bufLen, InputStream is,
                                               String pubId, String sysId)
        throws IOException, WstxException
    {
        StreamBootstrapper bs = StreamBootstrapper.getInstance
            (is, pubId, sysId, bufLen);
        Reader r = bs.bootstrapInput(false, null);
        URL ctxt = parent.getSource();

        // If we got a real sys id, we do know the source...
        if (sysId != null && sysId.length() > 0) {
            ctxt = URLUtil.urlFromSystemId(sysId, ctxt);
        }
        return InputSourceFactory.constructReaderSource
            (parent, refId, bs, pubId, sysId, ctxt, r, true, bufLen);
    }

    public static WstxInputSource sourceFromR(WstxInputSource parent, String refId,
                                              int bufLen, Reader r,
                                              String pubId, String sysId)
        throws IOException, WstxException
    {
        /* Last null -> no app-provided encoding (doesn't matter for non-
         * main-level handling)
         */
        ReaderBootstrapper rbs = ReaderBootstrapper.getInstance
            (r, pubId, sysId, bufLen, null);
        // null -> no xml reporter... should have one?
        r = rbs.bootstrapInput(false, null);
        URL ctxt = parent.getSource();
        if (sysId != null && sysId.length() > 0) {
            ctxt = URLUtil.urlFromSystemId(sysId, ctxt);
        }
        return InputSourceFactory.constructReaderSource
            (parent, refId, rbs, pubId, sysId, ctxt, r, true, bufLen);
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
        InputStream in = URLUtil.optimizedStreamFromURL(resolvedURL);
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
