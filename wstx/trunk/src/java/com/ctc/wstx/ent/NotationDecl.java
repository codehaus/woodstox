package com.ctc.wstx.ent;

import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import com.ctc.wstx.io.WstxInputSource;
import com.ctc.wstx.evt.WNotationDeclaration;

/**
 * Object that represents notation declarations DTD reader
 * has parsed from DTD subsets.
 */
public class NotationDecl
    extends WNotationDeclaration
{
    final Location mLocation;

    /**
     * Name/id of the notation, used to reference declaration.
     */
    final String mName;

    final String mPublicId;

    final String mSystemId;

    public NotationDecl(Location loc, String name, String pubId, String sysId)
    {
        super(loc);
        mLocation = loc;
        mName = name;
        mPublicId = pubId;
        mSystemId = sysId;
    }

    public Location getLocation() {
        return mLocation;
    }

    public String getName() {
        return mName;
    }

    public String getPublicId() {
        return mPublicId;
    }

    public String getSystemId() {
        return mSystemId;
    }

    /*
    ///////////////////////////////////////////
    // Implementation of abstract base methods
    ///////////////////////////////////////////
     */

    public void writeEnc(Writer w) throws IOException
    {
        w.write("<!NOTATION ");
        w.write(mName);
        if (mPublicId != null) {
            w.write("PUBLIC \"");
            w.write(mPublicId);
            w.write('"');
        } else {
            w.write("SYSTEM");
        }
        if (mSystemId != null) {
            w.write(" \"");
            w.write(mSystemId);
            w.write('"');
        }
        w.write('>');
    }
}
