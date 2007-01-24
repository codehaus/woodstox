package org.codehaus.stax2.ri.evt;

import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.*;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.EntityDeclaration;

import org.codehaus.stax2.XMLStreamWriter2;

public class EntityReferenceEventImpl
    extends BaseEventImpl
    implements EntityReference
{
    protected final EntityDeclaration mDecl;

    public EntityReferenceEventImpl(Location loc, EntityDeclaration decl)
    {
        super(loc);
        mDecl = decl;
    }

    public EntityDeclaration getDeclaration()
    {
        return mDecl;
    }

    public String getName()
    {
        return mDecl.getName();
    }

    /*
    ///////////////////////////////////////////
    // Implementation of abstract base methods
    ///////////////////////////////////////////
     */

    public int getEventType() {
        return ENTITY_REFERENCE;
    }

    public boolean isEntityReference() {
        return true;
    }

    public void writeAsEncodedUnicode(Writer w)
        throws XMLStreamException
    {
        try {
            w.write('&');
            w.write(getName());
            w.write(';');
        } catch (IOException ie) {
            throwFromIOE(ie);
        }
    }

    public void writeUsing(XMLStreamWriter2 w) throws XMLStreamException
    {
        w.writeEntityRef(getName());
    }
}
