package com.ctc.wstx.stax.evt;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

import com.ctc.wstx.stax.WstxStreamReader;
import com.ctc.wstx.util.EmptyIterator;

/**
 * Shared base class of {@link StartElement} implementations Wstx uses.
 */
abstract class BaseStartElement
    extends WEvent
    implements StartElement
{
    final QName mName;

    /*
    /////////////////////////////////////////////
    // Life cycle
    /////////////////////////////////////////////
     */

    protected BaseStartElement(Location loc, QName name)
    {
        super(loc);
        mName = name;
    }

    /*
    /////////////////////////////////////////////
    // Public API
    /////////////////////////////////////////////
     */

    public abstract Attribute getAttributeByName(QName name);

    public abstract Iterator getAttributes();

    public QName getName() {
        return mName;
    }

    public abstract Iterator getNamespaces();

    public abstract NamespaceContext getNamespaceContext();

    public abstract String getNamespaceURI(String prefix);

    /*
    /////////////////////////////////////////////////////
    // Implementation of abstract base methods, overrides
    /////////////////////////////////////////////////////
     */

    public StartElement asStartElement() { // overriden to save a cast
        return this;
    }

    public int getEventType() {
        return START_ELEMENT;
    }

    public boolean isStartElement() {
        return true;
    }

    public void writeAsEncodedUnicode(Writer w)
        throws XMLStreamException
    {
        try {
            w.write('<');
            String prefix = mName.getPrefix();
            if (prefix != null && prefix.length() > 0) {
                w.write(prefix);
                w.write(':');
            }
            w.write(mName.getLocalPart());

            // Base class can output namespaces and attributes:
            outputNsAndAttr(w);

            w.write('>');
        } catch (IOException ie) {
            throw new XMLStreamException(ie);
        }
    }

    protected abstract void outputNsAndAttr(Writer w) throws IOException;
}
