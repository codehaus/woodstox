package com.ctc.wstx.stax.evt;

import java.io.IOException;
import java.io.Writer;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.*;

import com.ctc.wstx.compat.JdkFeatures;
import com.ctc.wstx.stax.exc.WstxException;
import com.ctc.wstx.stax.exc.WstxIOException;

public abstract class WEvent
    implements XMLEvent
{
    /**
     * Location where token started; exact definition may depends
     * on event type.
     */
    protected final Location mLocation;

    protected WEvent(Location loc) {
        mLocation = loc;
    }

    /*
    //////////////////////////////////////////////
    // Skeleton XMLEvent API
    //////////////////////////////////////////////
     */

    public Characters asCharacters() {
        return (Characters) this;
    }

    public EndElement asEndElement() {
        return (EndElement) this;
    }

    public StartElement asStartElement() {
        return (StartElement) this;
    }

    public abstract int getEventType();

    public Location getLocation() {
        return mLocation;
    }

    public QName getSchemaType() {
        return null;
    }

    public boolean isAttribute()
    {
        return false;
    }

    public boolean isCharacters()
    {
        return false;
    }

    public boolean isEndDocument()
    {
        return false;
    }

    public boolean isEndElement()
    {
        return false;
    }

    public boolean isEntityReference()
    {
        return false;
    }

    public boolean isNamespace()
    {
        return false;
    }

    public boolean isProcessingInstruction()
    {
        return false;
    }

    public boolean isStartDocument()
    {
        return false;
    }

    public boolean isStartElement()
    {
        return false;
    }

    public abstract void writeAsEncodedUnicode(Writer w)
        throws XMLStreamException;

    /*
    //////////////////////////////////////////////
    // Helper methods
    //////////////////////////////////////////////
     */

    protected void throwFromIOE(IOException ioe)
        throws WstxException
    {
        throw new WstxIOException(ioe);
    }
}
