package com.ctc.wstx.evt;

import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
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
}
