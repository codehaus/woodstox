package org.codehaus.stax2.ri.evt;

import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.*;
import javax.xml.stream.events.NotationDeclaration;

import org.codehaus.stax2.XMLStreamWriter2;

public class NotationDeclarationEventImpl
    extends BaseEventImpl
    implements NotationDeclaration
{
    /**
     * Name/id of the notation, used to reference declaration.
     */
    final String mName;

    final String mPublicId;

    final String mSystemId;

    public NotationDeclarationEventImpl(Location loc,
                                        String name, String pubId, String sysId)
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

    /**
     * This method does not make much sense for this event type -- the reason
     * being that the notation declarations can only be written as part of
     * a DTD (internal or external subset), not separately. Can basically
     * choose to either skip silently (output nothing), or throw an
     * exception.
     */
    public void writeUsing(XMLStreamWriter2 w) throws XMLStreamException
    {
        /* Fail silently, or throw an exception? Let's do latter; at least
         * then we'll get useful (?) bug reports!
         */
        throw new XMLStreamException("Can not write notation declarations using an XMLStreamWriter");
    }
}
