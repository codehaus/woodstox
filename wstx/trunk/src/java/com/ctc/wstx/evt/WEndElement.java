package com.ctc.wstx.evt;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.Namespace;

import com.ctc.wstx.util.EmptyIterator;

public class WEndElement
    extends WEvent
    implements EndElement
{
    final QName mName;
    final ArrayList mNamespaces;

    /**
     * Constructor usually used when reading events from a stream reader.
     */
    public WEndElement(Location loc, XMLStreamReader r)
    {
        super(loc);
        mName = r.getName();

        // Let's figure out if there are any namespace declarations...
        int nsCount = r.getNamespaceCount();
        if (nsCount == 0) {
            mNamespaces = null;
        } else {
            ArrayList l = new ArrayList(nsCount);
            for (int i = 0; i < nsCount; ++i) {
                String prefix = r.getNamespacePrefix(i);
                if (prefix == null || prefix.length() == 0) { //default ns
                    l.add(new WNamespace(loc, r.getNamespaceURI(i)));
                } else {
                    l.add(new WNamespace(loc, prefix, r.getNamespaceURI(i)));
                }
            }
            mNamespaces = l;
        }
    }

    /**
     * Constructor used by the event factory.
     */
    public WEndElement(Location loc, QName name, Iterator namespaces)
    {
        super(loc);
        mName = name;
        if (namespaces == null || !namespaces.hasNext()) {
            mNamespaces = null;
        } else {
            ArrayList l = new ArrayList();
            while (namespaces.hasNext()) {
                /* Let's do typecast here, to catch any cast errors early;
                 * not strictly required, but helps in preventing later
                 * problems
                 */
                l.add((Namespace) namespaces.next());
            }
            mNamespaces = l;
        }
    }

    /*
    /////////////////////////////////////////////
    // Public API
    /////////////////////////////////////////////
     */

    public QName getName() {
        return mName;
    }

    public Iterator getNamespaces() 
    {
        return (mNamespaces == null) ? EmptyIterator.getInstance()
            : mNamespaces.iterator();
    }

    /*
    /////////////////////////////////////////////////////
    // Implementation of abstract base methods, overrides
    /////////////////////////////////////////////////////
     */

    public EndElement asEndElement() { // overriden to save a cast
        return this;
    }

    public int getEventType() {
        return END_ELEMENT;
    }

    public boolean isEndElement() {
        return true;
    }

    public void writeAsEncodedUnicode(Writer w)
        throws XMLStreamException
    {
        try {
            w.write("</");
            String prefix = mName.getPrefix();
            if (prefix != null && prefix.length() > 0) {
                w.write(prefix);
                w.write(':');
            }
            w.write(mName.getLocalPart());
            w.write('>');
        } catch (IOException ie) {
            throwFromIOE(ie);
        }
    }
}
