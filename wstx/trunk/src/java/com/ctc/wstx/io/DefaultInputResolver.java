package com.ctc.wstx.io;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;

import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import com.ctc.wstx.exc.WstxException;
import com.ctc.wstx.util.URLUtil;

/**
 * Static utility class that implements the entity (external DTD subset,
 * external parsed entities) resolution logics.
 */
public final class DefaultInputResolver
{
    /**
     * Default buffer size should never really be needed, when input
     * sources are properly chained, but let's have some sane default
     * for weird cases. 1000k chars means 2Kbyte buffer.
     */
    final static int DEFAULT_BUFFER_SIZE = 1000;

    /*
    ////////////////////////////
    // Life-cycle
    ////////////////////////////
    */

    private DefaultInputResolver() { }

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
     * @param refName Name of the entity to be expanded, if any; may be
     *    null
     * @param o Object that should provide the new input source; non-type safe
     */
    public static WstxInputSource sourceFrom(WstxInputSource parent, String refName, Object o)
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
            return sourceFromURL(parent, refName, bufLen, (URL) o,
                                 null, null);
        }
        if (o instanceof InputStream) {
            return sourceFromIS(parent, refName, bufLen, (InputStream) o, null, null);
        }
        if (o instanceof Reader) {
            return sourceFromR(parent, refName, bufLen, (Reader) o, null, null);
        }
        if (o instanceof String) {
            return sourceFromString(parent, refName, bufLen, (String) o);
        }

        throw new IllegalArgumentException("Unrecognized input argument type for sourceFrom(): "+o.getClass());
    }

    public static WstxInputSource sourceFromURL(WstxInputSource parent, String refName,
                                                int bufLen, URL url,
                                                String pubId, String sysId)
        throws IOException, WstxException
    {
        /* And then create the input source. Note that by default URL's
         * own input stream creation creates buffered reader -- for us
         * that's useless and wasteful (adds one unnecessary level of
         * caching, halving the speed due to copy operations needed), so
         * let's avoid it.
         */
        InputStream in = URLUtil.optimizedStreamFromURL(url);
        if (sysId == null) {
            sysId = url.toExternalForm();
        }
        StreamBootstrapper bs = StreamBootstrapper.getInstance(in, pubId, sysId, bufLen);
        /* !!! TBI: Should try to figure out how to pass XMLReporter here,
         *   so that warnings could be reported?
         */
        Reader r = bs.bootstrapInput(false, null);
        return InputSourceFactory.constructReaderSource
            (parent, refName, bs, pubId, sysId, url, r, true, bufLen);
    }

    public static WstxInputSource sourceFromString(WstxInputSource parent, String refName,
                                                   int bufLen, String sysId)
        throws IOException, WstxException
    {
        URL url = (parent == null) ? null : parent.getSource();
        url = URLUtil.urlFromSystemId(sysId, url);

        InputStream in = URLUtil.optimizedStreamFromURL(url);
        StreamBootstrapper bs = StreamBootstrapper.getInstance(in, null, sysId, bufLen);
        Reader r = bs.bootstrapInput(false, null);
        return InputSourceFactory.constructReaderSource
            (parent, refName, bs, null, sysId, url, r, true, bufLen);
    }

    public static WstxInputSource sourceFromIS(WstxInputSource parent, String refName,
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
            (parent, refName, bs, pubId, sysId, ctxt, r, true, bufLen);
    }

    public static WstxInputSource sourceFromR(WstxInputSource parent, String refName,
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
            (parent, refName, rbs, pubId, sysId, ctxt, r, true, bufLen);
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
     * @param entityName Name/id of the entity being expanded, if this is an
     *   entity expansion; null otherwise (for example, when resolving external
     *   subset).
     * @param publicId Public identifier of the resource, if known; null/empty
     *   otherwise. Default implementation just ignores the identifier.
     * @param systemId System identifier of the resource. Although interface
     *   allows null/empty, default implementation considers this an error.
     * @param customResolver Custom resolver to use first for resolution,
     *   if any (may be null).
     *
     * @return Input source, if entity could be resolved; null if it could
     *   not be resolved. In latter case processor may use its own default
     *   resolution mechanism.
     */
    public static WstxInputSource resolveReference
        (WstxInputSource refCtxt, String entityName,
         String publicId, String systemId, XMLResolver customResolver)
        throws IOException, XMLStreamException
    {
        URL ctxt = (refCtxt == null) ? null : refCtxt.getSource();
        if (ctxt == null) {
            ctxt = URLUtil.urlFromCurrentDir();
        }

        // Do we have a custom resolver that may be able to resolve it?
        if (customResolver != null) {
            Object source = (customResolver == null) ? null :
                customResolver.resolveEntity(publicId, systemId, ctxt.toExternalForm(), entityName);
            if (source != null) {
                return sourceFrom(refCtxt, entityName, source);
            }
        }
            
        // Have to have a system id, then...
        if (systemId == null) {
            throw new XMLStreamException("Can not resolve "
                                         +((entityName == null) ? "[External DTD subset]" : ("entity '"+entityName+"'"))+" without a system id (public id '"
                                         +publicId+"')");
        }
        URL url = URLUtil.urlFromSystemId(systemId, ctxt);
        int bufLen = (refCtxt == null) ? DEFAULT_BUFFER_SIZE : refCtxt.getInputBufferLength();
        return sourceFromURL(refCtxt, entityName, bufLen, url,
                             publicId, systemId);
    }
}
