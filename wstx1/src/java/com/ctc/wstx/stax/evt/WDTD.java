package com.ctc.wstx.stax.evt;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.DTD;

import com.ctc.wstx.util.DataUtil;
import com.ctc.wstx.stax.dtd.DTDSubset;

/**
 * Event that contains all StAX accessible information read from internal
 * and external DTD subsets.
 */
public class WDTD
    extends WEvent
    implements DTD
{
    final String mContent;

    /**
     * Internal DTD Object that contains combined information from internal
     * and external subsets.
     */
    final DTDSubset mDTD;

    List mEntities = null;

    List mNotations = null;

    public WDTD(Location loc, String content, DTDSubset dtd)
    {
        super(loc);
        mContent = content;
        mDTD = dtd;
    }

    public String getDocumentTypeDeclaration() {
        return mContent;
    }

    public List getEntities() {
        if (mEntities == null && (mDTD != null)) {
            mEntities = mDTD.getGeneralEntityList();
        }
        return mEntities;
    }

    public List getNotations() {
        if (mNotations == null && (mDTD != null)) {
            mNotations = mDTD.getNotationList();
        }
        return mNotations;
    }

    public Object getProcessedDTD() {
        return mDTD;
    }

    /*
    ///////////////////////////////////////////
    // Implementation of abstract base methods
    ///////////////////////////////////////////
     */

    public int getEventType() {
        return DTD;
    }

    public void writeAsEncodedUnicode(Writer w)
        throws XMLStreamException
    {
        try {
            w.write(mContent);
        } catch (IOException ie) {
            throwFromIOE(ie);
        }
    }
}
