package com.ctc.wstx.stax.evt;

import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.EntityDeclaration;

public class WEntityReference
    extends WEvent
    implements EntityReference
{
    final EntityDeclaration mDeclaration;

    final String mName;

    public WEntityReference(Location loc, String name, EntityDeclaration decl)
    {
        super(loc);
        mName = name;
        mDeclaration = decl;
    }

    public EntityDeclaration getDeclaration() {
        return mDeclaration;
    }

    public String getName() {
        return mName;
    }

    /*
    ///////////////////////////////////////////
    // Implementation of abstract base methods
    ///////////////////////////////////////////
     */

    public int getEventType() {
        return ENTITY_DECLARATION;
    }

    public boolean isEntityReference() {
        return true;
    }

    public void writeAsEncodedUnicode(Writer w)
        throws XMLStreamException
    {
        try {
            w.write('&');
            w.write(mName);
            w.write(';');
        } catch (IOException ie) {
            throwFromIOE(ie);
        }
    }
}
