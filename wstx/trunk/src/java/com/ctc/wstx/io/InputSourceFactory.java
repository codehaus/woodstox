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
    public static ReaderSource constructReaderSource
        (WstxInputSource parent, String fromEntity, InputBootstrapper bs,
         String pubId, String sysId, URL src,
         Reader r, boolean realClose, int bufSize) 
    {
        ReaderSource rs = new ReaderSource
            (parent, fromEntity, pubId, sysId, src, r, realClose, bufSize);
        if (bs != null) {
            rs.setInputOffsets(bs.getInputTotal(), bs.getInputRow(),
                               -bs.getInputColumn());
        }
        return rs;
    }

    public static BranchingReaderSource constructBranchingSource
        (WstxInputSource parent, String fromEntity, InputBootstrapper bs,
         String pubId, String sysId, URL src,
         Reader r, boolean realClose, int bufSize) 
    {
        BranchingReaderSource rs = new BranchingReaderSource
            (parent, fromEntity, pubId, sysId, src, r, realClose, bufSize);
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
