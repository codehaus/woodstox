package com.ctc.wstx.evt;

import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.EntityDeclaration;

import com.ctc.wstx.ent.EntityDecl;

public class WEntityReference
    extends WEvent
    implements EntityReference
{
    final EntityDecl mDecl;

    EntityDeclaration mDeclEvt;

    public WEntityReference(Location loc, com.ctc.wstx.ent.EntityDecl decl)
    {
        super(loc);
        mDecl = decl;
        mDeclEvt = null;
    }

    public WEntityReference(Location loc, javax.xml.stream.events.EntityDeclaration decl)
    {
        super(loc);
        mDecl = null;
        mDeclEvt = decl;
    }

    public EntityDeclaration getDeclaration() {
        if (mDeclEvt == null) {
            mDeclEvt = new WEntityDeclaration(mDecl);
        }
        return mDeclEvt;
    }

    public String getName() {
        return (mDecl == null) ? mDeclEvt.getName() : mDecl.getName();
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
            w.write(getName());
            w.write(';');
        } catch (IOException ie) {
            throwFromIOE(ie);
        }
    }
}
