package org.codehaus.stax2.ri.evt;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;

import org.codehaus.stax2.XMLStreamWriter2;

/**
 * Wstx {@link StartElement} implementation used when event is constructed
 * from already objectified data, for example when constructed by the event
 * factory.
 */
public class StartElementEventImpl
    extends BaseEventImpl
    implements StartElement
{
    protected final QName mName;

    protected final Map mAttrs;

    //protected final NamespaceContext mNsCtxt;
    protected NamespaceContext mNsCtxt;

    /*
    /////////////////////////////////////////////
    // Life cycle
    /////////////////////////////////////////////
     */

    protected StartElementEventImpl(Location loc, QName name, Map attrs)
    {
        super(loc);
        mName = name;
        mAttrs = attrs;
    }

    /**
     * Factory method called when a start element needs to be constructed
     * from an external source (most likely, non-woodstox stream reader).
     */
    public static StartElementEventImpl construct(Location loc, QName name,
                                             Map attrs, List ns)
    {
        return new StartElementEventImpl(loc, name, attrs);
    }

    public static StartElementEventImpl construct(Location loc, QName name,
                                             Iterator attrs, Iterator ns,
                                             NamespaceContext nsCtxt)
    {
        Map attrMap;
        if (attrs == null || !attrs.hasNext()) {
            attrMap = null;
        } else {
            attrMap = new LinkedHashMap();
            do {
                Attribute attr = (Attribute) attrs.next();
                attrMap.put(attr.getName(), attr);
            } while (attrs.hasNext());
        }

        /*
        BaseNsContext myCtxt;
        if (ns != null && ns.hasNext()) {
            ArrayList l = new ArrayList();
            do {
                l.add((Namespace) ns.next()); // cast to catch type problems early
            } while (ns.hasNext());
            myCtxt = MergedNsContext.construct(nsCtxt, l);
        } else {
            if (nsCtxt == null) {
                myCtxt = null;
            } else {
                myCtxt = MergedNsContext.construct(nsCtxt, null);
            }
        }
        */
        return new StartElementEventImpl(loc, name, attrMap);
    }

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

    public void writeUsing(XMLStreamWriter2 w) throws XMLStreamException
    {
        QName n = mName;
        w.writeStartElement(n.getPrefix(), n.getLocalPart(),
                            n.getNamespaceURI());
        outputNsAndAttr(w);
    }

    /*
    /////////////////////////////////////////////
    // Public API
    /////////////////////////////////////////////
     */

    public final QName getName() {
        return mName;
    }

    public Iterator getNamespaces() 
    {
        if (mNsCtxt == null) {
            return EmptyIterator.getInstance();
        }
        /*
        return mNsCtxt.getNamespaces();
        */
        return null;
    }

    public NamespaceContext getNamespaceContext()
    {
        return mNsCtxt;
    }

    public String getNamespaceURI(String prefix)
    {
        return (mNsCtxt == null) ? null : mNsCtxt.getNamespaceURI(prefix);
    }

    public Attribute getAttributeByName(QName name)
    {
        if (mAttrs == null) {
            return null;
        }
        return (Attribute) mAttrs.get(name);
    }

    public Iterator getAttributes()
    {
        if (mAttrs == null) {
            return EmptyIterator.getInstance();
        }
        return mAttrs.values().iterator();
    }

    protected void outputNsAndAttr(Writer w) throws IOException
    {
        // First namespace declarations, if any:
        if (mNsCtxt != null) {
            /*
            mNsCtxt.outputNamespaceDeclarations(w);
            */
        }
        // Then attributes, if any:
        if (mAttrs != null && mAttrs.size() > 0) {
            Iterator it = mAttrs.values().iterator();
            while (it.hasNext()) {
                Attribute attr = (Attribute) it.next();
                // Let's only output explicit attribute values:
                if (!attr.isSpecified()) {
                    continue;
                }

                w.write(' ');
                QName name = attr.getName();
                String prefix = name.getPrefix();
                if (prefix != null && prefix.length() > 0) {
                    w.write(prefix);
                    w.write(':');
                }
                w.write(name.getLocalPart());
                w.write("=\"");
                String val =  attr.getValue();
                if (val != null && val.length() > 0) {
                    AttributeEventImpl.writeEscapedAttrValue(w, val);
                }
                w.write('"');
            }
        }
    }

    protected void outputNsAndAttr(XMLStreamWriter w) throws XMLStreamException
    {
        // First namespace declarations, if any:
        /* 
        if (mNsCtxt != null) {
            mNsCtxt.outputNamespaceDeclarations(w);
        }
        */
        // Then attributes, if any:
        if (mAttrs != null && mAttrs.size() > 0) {
            Iterator it = mAttrs.values().iterator();
            while (it.hasNext()) {
                Attribute attr = (Attribute) it.next();
                // Let's only output explicit attribute values:
                if (!attr.isSpecified()) {
                    continue;
                }
                QName name = attr.getName();
                String prefix = name.getPrefix();
                String ln = name.getLocalPart();
                String nsURI = name.getNamespaceURI();
                w.writeAttribute(prefix, nsURI, ln, attr.getValue());
            }
        }
    }
}
