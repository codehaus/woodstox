package com.ctc.wstx.stax.evt;

import java.io.IOException;
import java.io.Writer;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;

import com.ctc.wstx.util.XMLQuoter;

public class WAttribute
    extends WEvent
    implements Attribute
{
    final QName mName;
    final String mValue;

    public WAttribute(Location loc, String localName, String uri, String prefix,
                      String value)
    {
        super(loc);
        mValue = value;
        if (prefix == null) {
            if (uri == null) {
                mName = new QName(localName);
            } else {
                mName = new QName(uri, localName);
            }
        } else {
            mName = new QName(uri, localName, prefix);
        }
    }

    public WAttribute(Location loc, QName name, String value)
    {
        super(loc);
        mName = name;
        mValue = value;
    }

    /*
    ///////////////////////////////////////////
    // Implementation of abstract base methods
    ///////////////////////////////////////////
     */

    public int getEventType() {
        return ATTRIBUTE;
    }

    public boolean isAttribute() { return true; }

    public void writeAsEncodedUnicode(Writer w)
        throws XMLStreamException
    {
        /* Specs don't really specify exactly what to output... but
         * let's do a reasonable guess:
         */
        String prefix = mName.getPrefix();
        try {
            if (prefix != null && prefix.length() > 0) {
                w.write(prefix);
                w.write(':');
            }
            w.write(mName.getLocalPart());
            w.write('=');
            w.write('"');
            XMLQuoter.outputDoubleQuotedAttr(w, mValue);
            w.write('"');
        } catch (IOException ie) {
            throwFromIOE(ie);
        }
    }

    /*
    ///////////////////////////////////////////
    // Attribute implementation
    ///////////////////////////////////////////
     */

    public String getDTDType() {
        // !!! TBI, when DTD support added
        return "CDATA";
    }

    public QName getName()
    {
        return mName;
    }

    public String getValue()
    {
        return mValue;
    }

    public boolean isSpecified()
    {
        // !!! TBI, when DTD support added
        return true;
    }
}
