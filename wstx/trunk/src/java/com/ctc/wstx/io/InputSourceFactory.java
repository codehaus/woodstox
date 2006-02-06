package com.ctc.wstx.io;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;

import javax.xml.stream.Location;

import com.ctc.wstx.util.TextBuffer;

/**
 * Factory class that creates instances of {@link WstxInputSource} to allow
 * reading input from various sources.
 */
public final class InputSourceFactory
{
    final static int DEFAULT_BUFFER_LENGTH = 4000;

    /**
     * @param parent
     * @param entityName Name of the entity expanded to create this input
     *    source: null when source created for the (main level) external
     *    DTD subset entity.
     * @param xmlVersion Optional xml version identifier of the main parsed
     *   document. Currently only relevant for checking that XML 1.0 document
     *   does not include XML 1.1 external parsed entities.
     *   If null, no checks will be done.
     */
    public static ReaderSource constructEntitySource
        (WstxInputSource parent, String entityName, InputBootstrapper bs,
         String pubId, String sysId, String xmlVersion,
         URL src, Reader r)
    {
        // true -> do close the underlying Reader at EOF
        int bufLen = (parent == null) ? DEFAULT_BUFFER_LENGTH : parent.getInputBufferLength();
        ReaderSource rs = new ReaderSource
            (parent, entityName, pubId, sysId, src, r, true, bufLen);
        if (bs != null) {
            rs.setInputOffsets(bs.getInputTotal(), bs.getInputRow(),
                               -bs.getInputColumn());
        }
        return rs;
    }

    /**
     * Factory method used for creating the main-level document reader
     * source.
     */
    public static BranchingReaderSource constructDocumentSource
        (InputBootstrapper bs, String pubId, String sysId, URL src,
         Reader r, boolean realClose, int bufSize) 
    {
        BranchingReaderSource rs = new BranchingReaderSource
            (pubId, sysId, src, r, realClose, bufSize);
        if (bs != null) {
            rs.setInputOffsets(bs.getInputTotal(), bs.getInputRow(),
                               -bs.getInputColumn());
        }
        return rs;
    }

    /**
     * Factory method usually used to expand internal parsed entities; in
     * which case context remains mostly the same.
     */
    public static WstxInputSource constructCharArraySource
        (WstxInputSource parent, String fromEntity,
         char[] text, int offset, int len, Location loc, URL src)
    {
        return new CharArraySource(parent, fromEntity, text, offset, len,
                                   loc, src);
    }
}
