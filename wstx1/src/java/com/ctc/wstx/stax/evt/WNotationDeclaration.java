package com.ctc.wstx.stax.evt;

import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.NotationDeclaration;

import com.ctc.wstx.stax.io.WstxInputSource;

/**
 * Common base class for all StAX-compliact Wstx entity declaration events.
 */
public class WNotationDeclaration
    extends WEvent
    implements NotationDeclaration
{
    /**
     * Name/id of the notation, used to reference declaration.
     */
    final String mName;

    final String mPublicId;

    final String mSystemId;

    public WNotationDeclaration(Location loc, String name,
                                String pubId, String sysId)
    {
        super(loc);
        mName = name;
        mPublicId = pubId;
        mSystemId = sysId;
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

    public int getEventType() {
        return NOTATION_DECLARATION;
    }

    public void writeAsEncodedUnicode(Writer w)
        throws XMLStreamException
    {
        try {
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
        } catch (IOException ie) {
            throwFromIOE(ie);
        }
    }
}
