package com.ctc.wstx.evt;

import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.NotationDeclaration;

import com.ctc.wstx.ent.NotationDecl;
import com.ctc.wstx.io.WstxInputSource;

/**
 * Woodstox implementation of StAX {@link NotationDeclaration}; basically
 * just wraps the internal notation declaration object woodstox uses
 */
public abstract class WNotationDeclaration
    extends WEvent
    implements NotationDeclaration
{
    public WNotationDeclaration(Location loc)
    {
        super(loc);
    }

    public abstract String getName();

    public abstract String getPublicId();

    public abstract String getSystemId();

    /*
    ///////////////////////////////////////////
    // Implementation of abstract base methods
    ///////////////////////////////////////////
     */

    public int getEventType() {
        return NOTATION_DECLARATION;
    }

    public abstract void writeEnc(Writer w)
        throws IOException;

    public void writeAsEncodedUnicode(Writer w)
        throws XMLStreamException
    {
        try {
            writeEnc(w);
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
    public void writeUsing(XMLStreamWriter w) throws XMLStreamException
    {
        /* Fail silently, or throw an exception? Let's do latter; at least
         * then we'll get useful (?) bug reports!
         */
        throw new XMLStreamException("Can not write notation declarations using an XMLStreamWriter");
    }
}
