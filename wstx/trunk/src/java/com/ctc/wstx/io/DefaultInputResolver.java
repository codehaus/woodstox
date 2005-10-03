package com.ctc.wstx.io;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;

import javax.xml.stream.XMLReporter;
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
    private final static int DEFAULT_BUFFER_LENGTH = 4000;

    /*
    ////////////////////////////
    // Life-cycle
    ////////////////////////////
    */

    private DefaultInputResolver() { }

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
     * @param rep Report object that can be used to report non-fatal problems
     *   that occur during entity source resolution
     *
     * @return Input source, if entity could be resolved; null if it could
     *   not be resolved. In latter case processor may use its own default
     *   resolution mechanism.
     */
    public static WstxInputSource resolveEntity
        (WstxInputSource refCtxt, String entityName,
         String publicId, String systemId,
         XMLResolver customResolver, XMLReporter rep)
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
                return sourceFrom(refCtxt, entityName, source, rep);
            }
        }
            
        // Have to have a system id, then...
        if (systemId == null) {
            throw new XMLStreamException("Can not resolve "
                                         +((entityName == null) ? "[External DTD subset]" : ("entity '"+entityName+"'"))+" without a system id (public id '"
                                         +publicId+"')");
        }
        URL url = URLUtil.urlFromSystemId(systemId, ctxt);
        return sourceFromURL(refCtxt, rep, entityName, url, publicId, systemId);
    }

    /**
     * A very simple utility expansion method used generally when the
     * only way to resolve an entity is via passed resolver; and where
     * failing to resolve it is not fatal.
     */
    public static WstxInputSource resolveEntityUsing
        (WstxInputSource refCtxt, String entityName,
         String publicId, String systemId,
         XMLResolver resolver, XMLReporter rep)
        throws IOException, XMLStreamException
    {
        URL ctxt = (refCtxt == null) ? null : refCtxt.getSource();
        if (ctxt == null) {
            ctxt = URLUtil.urlFromCurrentDir();
        }
        Object source = resolver.resolveEntity(publicId, systemId, ctxt.toExternalForm(), entityName);
        return (source == null) ? null : sourceFrom(refCtxt, entityName, source, rep);
    }

    /**
     * Factory method that accepts various types of Objects, and tries to
     * create a {@link WstxInputSource} from it. Currently it's only called
     * to locate external DTD subsets, when overriding default DOCTYPE
     * declarations; not for entity expansion or for locating the main
     * document entity.
     *
     * @param parent Input source context active when resolving a new
     *    "sub-source"; usually the main document source.
     * @param refName Name of the entity to be expanded, if any; may be
     *    null (and currently always is)
     * @param o Object that should provide the new input source; non-type safe
     */
    public static WstxInputSource sourceFrom(WstxInputSource parent, String refName, Object o,
                                             XMLReporter rep)
        throws IllegalArgumentException, IOException, WstxException
    {
        if (o instanceof Source) {
            if (o instanceof StreamSource) {
                return sourceFromSS(parent, rep, refName, (StreamSource) o);
            }
            throw new IllegalArgumentException("Can not use other Source objects than StreamSource: got "+o.getClass());
        }
        if (o instanceof URL) {
            return sourceFromURL(parent, rep, refName, (URL) o, null, null);
        }
        if (o instanceof InputStream) {
            return sourceFromIS(parent, rep, refName, (InputStream) o, null, null);
        }
        if (o instanceof Reader) {
            return sourceFromR(parent, rep, refName, (Reader) o, null, null);
        }
        if (o instanceof String) {
            return sourceFromString(parent, rep, refName, (String) o);
        }

        throw new IllegalArgumentException("Unrecognized input argument type for sourceFrom(): "+o.getClass());
    }

    /*
    ////////////////////////////
    // Internal methods
    ////////////////////////////
    */

    private static WstxInputSource sourceFromSS(WstxInputSource parent, XMLReporter rep,
                                               String refName,
                                               StreamSource ssrc)
        throws IOException, WstxException
    {
        InputBootstrapper bs;
        Reader r = ssrc.getReader();
        String pubId = ssrc.getPublicId();
        String sysId = ssrc.getSystemId();
        URL ctxt = (parent == null) ? null : parent.getSource();
        URL url = (sysId == null || sysId.length() == 0) ? null
            : URLUtil.urlFromSystemId(sysId, ctxt);

        if (r == null) {
            InputStream in = ssrc.getInputStream();
            if (in == null) { // Need to try just resolving the system id then
                if (url == null) {
                    throw new IllegalArgumentException("Can not create StAX reader for a StreamSource -- neither reader, input stream nor system id was set.");
                }
                in = URLUtil.optimizedStreamFromURL(url);
            }
            bs = StreamBootstrapper.getInstance
                (in, pubId, sysId, getInputBufferLength(parent));
        } else {
            bs = ReaderBootstrapper.getInstance
                (r, pubId, sysId, getInputBufferLength(parent), null);
        }
        
        Reader r2 = bs.bootstrapInput(false, rep);
        return InputSourceFactory.constructEntitySource
            (parent, refName, bs, pubId, sysId, ((url == null) ? ctxt : url),
             r2);
    }

    private static WstxInputSource sourceFromURL(WstxInputSource parent, XMLReporter rep,
                                                 String refName, URL url,
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
        StreamBootstrapper bs = StreamBootstrapper.getInstance(in, pubId, sysId,
                                                               getInputBufferLength(parent));
        Reader r = bs.bootstrapInput(false, rep);
        return InputSourceFactory.constructEntitySource
            (parent, refName, bs, pubId, sysId, url, r);
    }

    /**
     * We have multiple ways to look at what would it mean to get a String
     * as the resolved result. The most straight-forward is to consider
     * it literal replacement (with possible embedded entities), so let's
     * use that (alternative would be to consider it to be a reference
     * like URL -- those need to be returned as appropriate objects
     * instead).
     */
    private static WstxInputSource sourceFromString(WstxInputSource parent, XMLReporter rep, 
                                                    String refName, String refContent)
        throws IOException, WstxException
    {
        /* Last null -> no app-provided encoding (doesn't matter for non-
         * main-level handling)
         */
        return sourceFromR(parent, rep, refName,
                           new StringReader(refContent),
                           null, refName);

        /* 11-Aug-2005, TSa: Used to use this code, which assumes it is
         *    actually a reference...
         */
        /*
         URL url = (parent == null) ? null : parent.getSource();
         url = URLUtil.urlFromSystemId(sysId, url);

         InputStream in = URLUtil.optimizedStreamFromURL(url);
         StreamBootstrapper bs = StreamBootstrapper.getInstance(in, null, sysId,
                                                                getInputBufferLength(parent));
         Reader r = bs.bootstrapInput(false, rep);
         return InputSourceFactory.constructEntitySource
            (parent, refName, bs, null, sysId, url, r);
        */
    }

    private static WstxInputSource sourceFromIS(WstxInputSource parent,
                                                XMLReporter rep,
                                                String refName,
                                                InputStream is,
                                                String pubId, String sysId)
        throws IOException, WstxException
    {
        StreamBootstrapper bs = StreamBootstrapper.getInstance
            (is, pubId, sysId, getInputBufferLength(parent));
        Reader r = bs.bootstrapInput(false, rep);
        URL ctxt = parent.getSource();

        // If we got a real sys id, we do know the source...
        if (sysId != null && sysId.length() > 0) {
            ctxt = URLUtil.urlFromSystemId(sysId, ctxt);
        }
        return InputSourceFactory.constructEntitySource
            (parent, refName, bs, pubId, sysId, ctxt, r);
    }

    private static WstxInputSource sourceFromR(WstxInputSource parent, XMLReporter rep,
                                               String refName, Reader r,
                                               String pubId, String sysId)
        throws IOException, WstxException
    {
        /* Last null -> no app-provided encoding (doesn't matter for non-
         * main-level handling)
         */
        ReaderBootstrapper rbs = ReaderBootstrapper.getInstance
            (r, pubId, sysId, getInputBufferLength(parent), null);
        // null -> no xml reporter... should have one?
        r = rbs.bootstrapInput(false, rep);
        URL ctxt = parent.getSource();
        if (sysId != null && sysId.length() > 0) {
            ctxt = URLUtil.urlFromSystemId(sysId, ctxt);
        }
        return InputSourceFactory.constructEntitySource
            (parent, refName, rbs, pubId, sysId, ctxt, r);
    }

    private static int getInputBufferLength(WstxInputSource parent)
    {
        return (parent == null) ?
            DEFAULT_BUFFER_LENGTH : parent.getInputBufferLength();
    }
}
