package com.ctc.wstx.evt;

import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.StartDocument;

import com.ctc.wstx.api.WstxOutputProperties;

public class WStartDocument
    extends WEvent
    implements StartDocument
{
    private final boolean mStandaloneSet;
    private final boolean mIsStandalone;
    private final String mVersion;
    private final boolean mEncodingSet;
    private final String mEncodingScheme;
    private final String mSystemId;

    public WStartDocument(Location loc, XMLStreamReader r)
    {
        super(loc);
        mStandaloneSet = r.standaloneSet();
        mIsStandalone = r.isStandalone();
        mVersion = r.getVersion();
        mEncodingScheme = r.getCharacterEncodingScheme();
        mEncodingSet = (mEncodingScheme != null && mEncodingScheme.length() > 0);
        mSystemId = loc.getSystemId();
    }

    /**
     * Method called by event factory, when constructing start document
     * event.
     */
    public WStartDocument(Location loc)
    {
        this(loc, (String) null);
    }

    public WStartDocument(Location loc, String encoding)
    {
        this(loc, encoding, null);
    }

    public WStartDocument(Location loc, String encoding, String version)
    {
        this(loc, encoding, version, false, false);
    }

    public WStartDocument(Location loc, String encoding, String version,
                          boolean standaloneSet, boolean isStandalone)
    {
        super(loc);
        mEncodingScheme = encoding;
        mEncodingSet = (encoding != null && encoding.length() > 0);
        mVersion = version;
        mStandaloneSet = standaloneSet;
        mIsStandalone = isStandalone;
        mSystemId = "";
    }

    public boolean encodingSet() {
        return mEncodingSet;
    }

    public String getCharacterEncodingScheme() {
        return mEncodingScheme;
    }

    public String getSystemId() {
        return mSystemId;
    }

    public String getVersion() {
        return mVersion;
    }

    public boolean isStandalone() {
        return mIsStandalone;
    }

    public boolean standaloneSet() {
        return mStandaloneSet;
    }

    /*
    ///////////////////////////////////////////
    // Implementation of abstract base methods
    ///////////////////////////////////////////
     */

    public int getEventType() {
        return START_DOCUMENT;
    }

    public boolean isStartDocument() {
        return true;
    }

    public void writeAsEncodedUnicode(Writer w)
        throws XMLStreamException
    {
        // Need to output the XML declaration?
        try {
            w.write("<?xml version=\"");
            if (mVersion == null || mVersion.length() == 0) {
                w.write(WstxOutputProperties.DEFAULT_XML_VERSION);
            } else {
                w.write(mVersion);
            }
            w.write('"');
            if (mEncodingSet) {
                w.write(" encoding=\"");
                w.write(mEncodingScheme);
                w.write('"');
            }
            if (mStandaloneSet) {
                if (mIsStandalone) {
                    w.write(" standalone=\"yes\"");
                } else {
                    w.write(" standalone=\"no\"");
                }
            }
            w.write(" ?>");
        } catch (IOException ie) {
            throwFromIOE(ie);
        }
    }
}
