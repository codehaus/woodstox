package com.ctc.wstx.stax.evt;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.EntityDeclaration;

import com.ctc.wstx.stax.io.WstxInputLocation;
import com.ctc.wstx.stax.io.WstxInputResolver;
import com.ctc.wstx.stax.io.WstxInputSource;

/**
 * Common base class for all StAX-compliact Wstx entity declaration events.
 */
public abstract class WEntityDeclaration
    extends WEvent
    implements EntityDeclaration
{
    /**
     * Name/id of the entity used to reference it.
     */
    final String mName;

    /**
     * Context that can be used to resolve references encountered from
     * expanded contents of this entity.
     */
    protected final URL mContext;

    public WEntityDeclaration(Location loc, String name, URL ctxt)
    {
        super(loc);
        mName = name;
        mContext = ctxt;
    }

    public String getBaseURI() {
        return mContext.toString();
    }

    public String getName() {
        return mName;
    }

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

    public abstract void writeAsEncodedUnicode(Writer w)
        throws XMLStreamException;

    /*
    ///////////////////////////////////////////
    // Extended API for Wstx core
    ///////////////////////////////////////////
     */

    // // // Extended location info

    public URL getSource() {
        return mContext;
    }

    // // // Access to data

    public abstract char[] getReplacementChars();

    public int getReplacementTextLength() {
        String str = getReplacementText();
        return (str == null) ? 0 : str.length();
    }

    // // // Type information

    public abstract boolean isExternal();

    public abstract boolean isParsed();

    // // // Factory methods

    /**
     * Method called to create the new input source through which expansion
     * value of the entity can be read.
     */
    public abstract WstxInputSource createInputSource(WstxInputSource parent, 
                                                      WstxInputResolver res)
        throws IOException, XMLStreamException;
}
