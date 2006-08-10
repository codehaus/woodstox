package com.ctc.wstx.evt;

import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.EntityDeclaration;

import com.ctc.wstx.ent.EntityDecl;
import com.ctc.wstx.io.WstxInputSource;

/**
 * Simple implementation of StAX entity declaration events; for the
 * most just wraps a {@link EntityDecl} instance.
 */
public final class WEntityDeclaration
    extends WEvent
    implements EntityDeclaration
{
    /**
     * Woodstox object that actually contains all information
     * about this event
     */
    final EntityDecl mDecl;

    public WEntityDeclaration(EntityDecl decl)
    {
        super(decl.getLocation());
        mDecl = decl;
    }

    public String getBaseURI() {
        return mDecl.getBaseURI();
    }

    public String getName() {
        return mDecl.getName();
    }

    public String getNotationName() {
        return mDecl.getNotationName();
    }

    public String getPublicId() {
        return mDecl.getPublicId();
    }

    public String getReplacementText() {
        return mDecl.getReplacementText();
    }

    public String getSystemId() {
        return mDecl.getSystemId();
    }

    /*
    ///////////////////////////////////////////
    // Implementation of abstract base methods
    ///////////////////////////////////////////
     */

    public int getEventType() {
        return ENTITY_DECLARATION;
    }

    public void writeAsEncodedUnicode(Writer w)
        throws XMLStreamException
    {
        try {
            mDecl.writeEnc(w);
        } catch (IOException ie) {
            throw new XMLStreamException(ie);
        }
    }

    /**
     * This method does not make much sense for this event type -- the reason
     * being that the entity declarations can only be written as part of
     * a DTD (internal or external subset), not separately. Can basically
     * choose to either skip silently (output nothing), or throw an
     * exception.
     */
    public void writeUsing(XMLStreamWriter w) throws XMLStreamException
    {
        /* Fail silently, or throw an exception? Let's do latter; at least
         * then we'll get useful (?) bug reports!
         */
        throw new XMLStreamException("Can not write entity declarations using an XMLStreamWriter");
    }
}
