package com.ctc.wstx.evt;

import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.EntityDeclaration;

import com.ctc.wstx.ent.EntityDecl;

public class WEntityReference
    extends WEvent
    implements EntityReference
{
    final EntityDeclaration mDecl;

    final String mName;

    public WEntityReference(Location loc, EntityDeclaration decl)
    {
        super(loc);
        mDecl = decl;
        mName = null;
    }

    /**
     * This constructor gets called for undeclared/defined entities: we will
     * still know the name (from the reference), but not how it's defined
     * (since it is not defined).
     */
    public WEntityReference(Location loc, String name)
    {
        super(loc);
        mDecl = null;
        mName = name;
    }

    public EntityDeclaration getDeclaration() {
        return mDecl;
    }

    public String getName()
    {
        if (mName != null) {
            return mName;
        }
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

    public void writeUsing(XMLStreamWriter w) throws XMLStreamException
    {
        w.writeEntityRef(getName());
    }
}
