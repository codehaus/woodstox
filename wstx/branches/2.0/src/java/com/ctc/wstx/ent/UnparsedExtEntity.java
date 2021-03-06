package com.ctc.wstx.ent;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;

import javax.xml.stream.Location;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.EntityDeclaration;

import com.ctc.wstx.io.WstxInputSource;
import com.ctc.wstx.util.URLUtil;

public class UnparsedExtEntity
    extends ExtEntity
{
    final String mNotationId;

    public UnparsedExtEntity(Location loc, String name, URL ctxt,
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

    public void writeEnc(Writer w) throws IOException
    {
        w.write("<!ENTITY ");
        w.write(mName);
        String pubId = getPublicId();
        if (pubId != null) {
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
    }

    /*
    ///////////////////////////////////////////
    // Extended API for Wstx core
    ///////////////////////////////////////////
     */

    // // // Type information
    
    public boolean isParsed() { return false; }
    
    public WstxInputSource expand(WstxInputSource parent,
                                  XMLResolver res, XMLReporter rep)
    {
        // Should never get called, actually...
        throw new Error("Internal error: createInputSource() called for unparsed (external) entity.");
    }
}
