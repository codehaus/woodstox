package com.ctc.wstx.ent;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;

import javax.xml.stream.Location;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.EntityDeclaration;

import com.ctc.wstx.io.DefaultInputResolver;
import com.ctc.wstx.io.InputSourceFactory;
import com.ctc.wstx.io.WstxInputSource;
import com.ctc.wstx.util.URLUtil;

public class ParsedExtEntity
    extends ExtEntity
{
    public ParsedExtEntity(Location loc, String name, URL ctxt,
                           String pubId, String sysId)
    {
        super(loc, name, ctxt, pubId, sysId);
    }

    public String getNotationName() {
        return null;
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
        if (pubId == null) {
            w.write("PUBLIC \"");
            w.write(pubId);
            w.write("\" ");
        } else {
            w.write("SYSTEM ");
        }
        w.write('"');
        w.write(getSystemId());
        w.write("\">");
    }

    /*
    ///////////////////////////////////////////
    // Extended API for Wstx core
    ///////////////////////////////////////////
     */

    // // // Type information
    
    public boolean isParsed() { return true; }
    
    public WstxInputSource createInputSource(WstxInputSource parent,
                                             XMLResolver res)
        throws IOException, XMLStreamException
    {
        return DefaultInputResolver.resolveReference
            (parent, mName, getPublicId(), getSystemId(), res);
    }
}
