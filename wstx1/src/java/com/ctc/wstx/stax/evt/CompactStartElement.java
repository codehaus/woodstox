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
import com.ctc.wstx.stax.ns.BaseNsContext;
import com.ctc.wstx.util.EmptyIterator;

/**
 * Wstx {@link StartElement} implementation used when directly creating
 * events from a stream reader.
 */
public class CompactStartElement
    extends BaseStartElement
{
    /**
     * Container object that has enough information about attributes to
     * be able to implement attribute accessor methods of this class.
     */
    final WAttrList mAttrList;

    final BaseNsContext mNsContext;

    /*
    /////////////////////////////////////////////
    // Life cycle
    /////////////////////////////////////////////
     */

    protected CompactStartElement(Location loc, QName name, BaseNsContext nsCtxt,
                                  WAttrList attr)
    {
        super(loc, name);
        mNsContext = nsCtxt;
        mAttrList = attr;
    }

    public static CompactStartElement construct(Location loc, XMLStreamReader r)
    {
        WstxStreamReader wr = (WstxStreamReader) r;
        return new CompactStartElement(loc, r.getName(),
                                 wr.constructNsContext(loc),
                                 wr.buildAttrList(loc));
    }

    /*
    /////////////////////////////////////////////
    // Public API
    /////////////////////////////////////////////
     */

    public Attribute getAttributeByName(QName name)
    {
        return mAttrList.getAttr(name);
    }

    public Iterator getAttributes()
    {
        return mAttrList.getAttrs();
    }

    public Iterator getNamespaces() 
    {
        return (mNsContext == null) ? EmptyIterator.getInstance() : mNsContext.getNamespaces();
    }

    public NamespaceContext getNamespaceContext()
    {
        return mNsContext;
    }

    public String getNamespaceURI(String prefix)
    {
        return (mNsContext == null) ? null : mNsContext.getNamespaceURI(prefix);
    }

    protected void outputNsAndAttr(Writer w) throws IOException
    {
        if (mNsContext != null) {
            mNsContext.outputNamespaceDeclarations(w);
        }
        mAttrList.outputAttrs(w);
    }
}
