package com.ctc.wstx.stax.evt;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.EntityDeclaration;

import com.ctc.wstx.stax.io.DefaultInputResolver;
import com.ctc.wstx.stax.io.InputSourceFactory;
import com.ctc.wstx.stax.io.WstxInputResolver;
import com.ctc.wstx.stax.io.WstxInputSource;
import com.ctc.wstx.util.URLUtil;

public class WEntityDeclExtUnparsed
    extends WEntityDeclExt
{
    final String mNotationId;

    public WEntityDeclExtUnparsed(Location loc, String name, URL ctxt,
                                String pubId, String sysId,
                                String notationId)
    {
        super(loc, name, ctxt, pubId, sysId);
        mNotationId = notationId;
    }

    public String getNotationName() {
        return mNotationId;
    }

    /*
    ///////////////////////////////////////////
    // Implementation of abstract base methods
    ///////////////////////////////////////////
     */

    public void writeAsEncodedUnicode(Writer w)
        throws XMLStreamException
    {
        try {
            w.write("<!ENTITY ");
            w.write(mName);
            String pubId = getPublicId();
            if (pubId == null) {
                w.write("PUBLIC \"");
                w.write(pubId);
                w.write("\" ");
            } else {
                w.write("SYSTEM ");
            }
            w.write('"');
            w.write(getSystemId());
            w.write("\" NDATA ");
            w.write(mNotationId);
            w.write('>');
        } catch (IOException ie) {
            throw new XMLStreamException(ie);
        }
    }

    /*
    ///////////////////////////////////////////
    // Extended API for Wstx core
    ///////////////////////////////////////////
     */

    // // // Type information
    
    public boolean isParsed() { return false; }
    
    public WstxInputSource createInputSource(WstxInputSource parent,
                                             WstxInputResolver res)
    {
        // Should never get called, actually...
        throw new Error("Internal error: createInputSource() called for unparsed (external) entity.");
    }
}
