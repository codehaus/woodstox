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

public class WEntityDeclExtParsed
    extends WEntityDeclExt
{
    public WEntityDeclExtParsed(Location loc, String name, URL ctxt,
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
            w.write("\">");
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
    
    public boolean isParsed() { return true; }
    
    public WstxInputSource createInputSource(WstxInputSource parent,
                                             WstxInputResolver res)
        throws IOException, XMLStreamException
    {
        String pubId = getPublicId();
        String sysId = getSystemId();
        URL loc = getSource();

        if (sysId == null || sysId.length() == 0) {
            // Should this ever happen?
            throw new IOException("No system id for entity '"+mName+"', can not resolve.");
        }

        loc = URLUtil.urlFromSystemId(sysId, loc);
        WstxInputSource src = (res == null) ? null :
            res.resolveReference(parent, mName, pubId, sysId, loc);

        if (src == null) {
            src = DefaultInputResolver.getInstance().resolveReference(parent, mName, pubId, sysId, loc);
        }

        return src;
    }
}
