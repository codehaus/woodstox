package com.ctc.wstx.evt;

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

import com.ctc.wstx.io.AttrValueEscapingWriter;
import com.ctc.wstx.sr.ElemAttrs;
import com.ctc.wstx.util.BaseNsContext;
import com.ctc.wstx.util.EmptyIterator;
import com.ctc.wstx.util.SingletonIterator;

/**
 * Wstx {@link StartElement} implementation used when directly creating
 * events from a stream reader.
 */
public class CompactStartElement
    extends BaseStartElement
{
    // Need to be in sync with ones from ElemAttrs
    private final static int OFFSET_LOCAL_NAME = 0;
    private final static int OFFSET_NS_URI = 1;
    private final static int OFFSET_NS_PREFIX = 2;
    private final static int OFFSET_VALUE = 3;

    /*
    /////////////////////////////////////////////
    // Basic element properties
    /////////////////////////////////////////////
     */


    /*
    /////////////////////////////////////////////
    // Attribute information
    /////////////////////////////////////////////
     */

    /**
     * Container object that has enough information about attributes to
     * be able to implement attribute accessor methods of this class.
     */
    final ElemAttrs mAttrs;

    /**
     * Array needed for accessing actual String components of the attributes
     */
    final String[] mRawAttrs;

    /**
     * Lazily created List that contains Attribute instances contained
     * in this list. Created only if there are at least 2 attributes.
     */
    private ArrayList mAttrList = null;


    /*
    /////////////////////////////////////////////
    // Life cycle
    /////////////////////////////////////////////
     */

    protected CompactStartElement(Location loc, QName name, BaseNsContext nsCtxt,
                                  ElemAttrs attrs)
    {
        super(loc, name, nsCtxt);
        mAttrs = attrs;
        mRawAttrs = (attrs == null) ? null : attrs.getRawAttrs();
    }

    /*
    /////////////////////////////////////////////
    // StartElement implementation
    /////////////////////////////////////////////
     */

    public Attribute getAttributeByName(QName name)
    {
        if (mAttrs == null) {
            return null;
        }
        int ix = mAttrs.findIndex(name);
        if (ix < 0) {
            return null;
        }
        return constructAttr(mRawAttrs, ix, mAttrs.isDefault(ix));
    }

    public Iterator getAttributes()
    {
        if (mAttrList == null) { // List is lazily constructed as needed
            if (mAttrs == null) {
                return EmptyIterator.getInstance();
            }
            String[] rawAttrs = mRawAttrs;
            int rawLen = rawAttrs.length;
            int defOffset = mAttrs.getFirstDefaultOffset();
            if (rawLen == 4) {
                return new SingletonIterator
                    (constructAttr(rawAttrs, 0, (defOffset == 0)));
            }
            ArrayList l = new ArrayList(rawLen >> 2);
            for (int i = 0; i < rawLen; i += 4) {
                l.add(constructAttr(rawAttrs, i, (i >= defOffset)));
            }
            mAttrList = l;
        }
        return mAttrList.iterator();
    }

    protected void outputNsAndAttr(Writer w) throws IOException
    {
        if (mNsCtxt != null) {
            mNsCtxt.outputNamespaceDeclarations(w);
        }

        String[] raw = mRawAttrs;
        if (raw != null) {
            for (int i = 0, len = raw.length; i < len; i += 4) {
                w.write(' ');
                String prefix = raw[i + OFFSET_NS_PREFIX];
                if (prefix != null && prefix.length() > 0) {
                    w.write(prefix);
                    w.write(':');
                }
                w.write(raw[i]); // local name
                w.write("=\"");
                AttrValueEscapingWriter.writeEscapedAttrValue(w, raw[i + OFFSET_VALUE]);
                w.write('"');
            }
        }
    }

    /*
    /////////////////////////////////////////////
    // Internal methods
    /////////////////////////////////////////////
     */

    public WAttribute constructAttr(String[] raw, int rawIndex, boolean isDef)
    {
        return new WAttribute(mLocation, raw[rawIndex], raw[rawIndex+1],
                              raw[rawIndex+2], raw[rawIndex+3], isDef);
    }
}
