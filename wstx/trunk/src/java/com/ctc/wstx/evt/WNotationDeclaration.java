package com.ctc.wstx.evt;

import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.NotationDeclaration;

import com.ctc.wstx.ent.NotationDecl;
import com.ctc.wstx.io.WstxInputSource;

/**
 * Woodstox implementation of StAX {@link NotationDeclaration}; basically
 * just wraps the internal notation declaration object woodstox uses
 */
public class WNotationDeclaration
    extends WEvent
    implements NotationDeclaration
{
    final NotationDecl mDecl;

    public WNotationDeclaration(NotationDecl decl)
    {
        super(decl.getLocation());
        mDecl = decl;
    }

    public String getName() {
        return mDecl.getName();
    }

    public String getPublicId() {
        return mDecl.getPublicId();
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
        return NOTATION_DECLARATION;
    }

    public void writeAsEncodedUnicode(Writer w)
        throws XMLStreamException
    {
        try {
            mDecl.writeEnc(w);
        } catch (IOException ie) {
            throwFromIOE(ie);
        }
    }
}
