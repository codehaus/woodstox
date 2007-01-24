package com.ctc.wstx.evt;

import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.EntityDeclaration;

import org.codehaus.stax2.XMLStreamWriter2;
import org.codehaus.stax2.ri.evt.BaseEventImpl;

import com.ctc.wstx.ent.EntityDecl;
import com.ctc.wstx.io.WstxInputSource;

/**
 * Simple implementation of StAX entity declaration events; for the
 * most just wraps a {@link EntityDecl} instance.
 */
public abstract class WEntityDeclaration
    extends BaseEventImpl
    implements EntityDeclaration
{
    public WEntityDeclaration(Location loc)
    {
        super(loc);
    }

    public abstract String getBaseURI();

    public abstract String getName();

    public abstract String getNotationName();

    public abstract String getPublicId();

    public abstract String getReplacementText();

    public abstract String getSystemId();

    /*
    ///////////////////////////////////////////
    // Implementation of abstract base methods
    ///////////////////////////////////////////
     */

    public int getEventType() {
        return ENTITY_DECLARATION;
    }

    public abstract void writeEnc(Writer w) throws IOException;

    public void writeAsEncodedUnicode(Writer w)
        throws XMLStreamException
    {
        try {
            writeEnc(w);
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
    public void writeUsing(XMLStreamWriter2 w) throws XMLStreamException
    {
        /* Fail silently, or throw an exception? Let's do latter; at least
         * then we'll get useful (?) bug reports!
         */
        throw new XMLStreamException("Can not write entity declarations using an XMLStreamWriter");
    }
}
