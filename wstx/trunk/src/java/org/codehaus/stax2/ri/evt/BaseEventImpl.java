package org.codehaus.stax2.ri.evt;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.*;

import org.codehaus.stax2.*;
import org.codehaus.stax2.evt.XMLEvent2;

/**
 * This abstract base class implements common functionality for
 * Stax2 reference implementation's event API part.
 *
 * @author Tatu Saloranta
 */
public abstract class BaseEventImpl
    implements XMLEvent2
{
    /**
     * Location where token started; exact definition may depends
     * on event type.
     */
    protected final Location mLocation;

    protected BaseEventImpl(Location loc)
    {
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
    // XMLEvent2 (StAX2)
    //////////////////////////////////////////////
     */

    public abstract void writeUsing(XMLStreamWriter2 w) throws XMLStreamException;

    /*
    ///////////////////////////////////////////
    // Overridden standard methods
    ///////////////////////////////////////////
     */

    public String toString() {
        return "[Stax Event #"+getEventType()+"]";
    }

    /*
    //////////////////////////////////////////////
    // Helper methods
    //////////////////////////////////////////////
     */

    protected void throwFromIOE(IOException ioe)
        throws XMLStreamException
    {
        throw new XMLStreamException(ioe.getMessage(), ioe);
    }
}
