package com.ctc.wstx.evt;

import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndDocument;

public class WEndDocument
    extends WEvent
    implements EndDocument
{
    public WEndDocument(Location loc)
    {
        super(loc);
    }

    /*
    ///////////////////////////////////////////
    // Implementation of abstract base methods
    ///////////////////////////////////////////
     */

    public int getEventType() {
        return END_DOCUMENT;
    }

    public boolean isEndDocument() {
        return true;
    }

    public void writeAsEncodedUnicode(Writer w)
        throws XMLStreamException
    {
        // Nothing to output
    }
}
