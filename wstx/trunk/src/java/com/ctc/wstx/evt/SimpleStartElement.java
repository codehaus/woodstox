package com.ctc.wstx.evt;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;

import com.ctc.wstx.compat.JdkFeatures;
import com.ctc.wstx.io.AttrValueEscapingWriter;
import com.ctc.wstx.util.BaseNsContext;
import com.ctc.wstx.util.EmptyIterator;

/**
 * Wstx {@link StartElement} implementation used when event is constructed
 * from already objectified data, for example when constructed by the event
 * factory.
 */
public class SimpleStartElement
    extends BaseStartElement
{
    final Map mAttrs;

    /*
    /////////////////////////////////////////////
    // Life cycle
    /////////////////////////////////////////////
     */

    protected SimpleStartElement(Location loc, QName name, BaseNsContext nsCtxt,
                                 Map attr, boolean wasEmpty)
    {
        super(loc, name, nsCtxt, wasEmpty);
        mAttrs = attr;
    }

    public static SimpleStartElement construct(Location loc, QName name,
                                               Iterator attrs, Iterator ns,
                                               NamespaceContext nsCtxt,
                                               boolean wasEmpty)
    {
        Map attrMap;
        if (attrs == null || !attrs.hasNext()) {
            attrMap = null;
        } else {
            attrMap = JdkFeatures.getInstance().getInsertOrderedMap();
            do {
                Attribute attr = (Attribute) attrs.next();
                attrMap.put(attr.getName(), attr);
            } while (attrs.hasNext());
        }

        BaseNsContext myCtxt;
        if (ns != null && ns.hasNext()) {
            ArrayList l = new ArrayList();
            do {
                l.add((Namespace) ns.next()); // cast to catch type problems early
            } while (ns.hasNext());
            myCtxt = MergedNsContext.construct(nsCtxt, l);
        } else {
            /* Doh. Need specificially 'our' namespace context, to get them
             * output properly...
             */
            if (nsCtxt == null) {
                myCtxt = null;
            } else if (nsCtxt instanceof BaseNsContext) {
                myCtxt = (BaseNsContext) nsCtxt;
            } else {
                myCtxt = MergedNsContext.construct(nsCtxt, null);
            }
        }
        return new SimpleStartElement(loc, name, myCtxt, attrMap, wasEmpty);
    }

    /*
    /////////////////////////////////////////////
    // Public API
    /////////////////////////////////////////////
     */

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
            mNsCtxt.outputNamespaceDeclarations(w);
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
                    AttrValueEscapingWriter.writeEscapedAttrValue(w, val);
                }
                w.write('"');
            }
        }
    }
}
