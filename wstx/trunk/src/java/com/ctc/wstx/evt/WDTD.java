package com.ctc.wstx.evt;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.DTD;

import com.ctc.wstx.dtd.DTDSubset;

/**
 * Event that contains all StAX accessible information read from internal
 * and external DTD subsets.
 */
public class WDTD
    extends WEvent
    implements DTD
{
    final String mRootName;

    final String mSystemId;

    final String mPublicId;

    final String mInternalSubset;

    /**
     * Internal DTD Object that contains combined information from internal
     * and external subsets.
     */
    final DTDSubset mDTD;

    List mEntities = null;

    List mNotations = null;

    /*
    /////////////////////////////////////////////////////
    // Temporary objects
    /////////////////////////////////////////////////////
     */

    /**
     * String built from parts on (if) needed
     */
    transient String mFullText = null;

    /*
    /////////////////////////////////////////////////////
    // Constuctors
    /////////////////////////////////////////////////////
     */

    public WDTD(Location loc, DTDSubset dtd, String rootName,
                String sysId, String pubId, String intSubset)
    {
        super(loc);
        mDTD = dtd;
        mRootName = rootName;
        mSystemId = sysId;
        mPublicId = pubId;
        mInternalSubset = intSubset;
    }

    /**
     * Constructor used when only partial information is available...
     */
    public WDTD(Location loc, String intSubset)
    {
        this(loc, null, null, null, null, intSubset);
    }

    /*
    /////////////////////////////////////////////////////
    // Constuctors
    /////////////////////////////////////////////////////
     */
    public String getDocumentTypeDeclaration()
    {
        if (mFullText == null) {
            int len = 60;
            if (mInternalSubset != null) {
                len += mInternalSubset.length() + 4;
            }
            StringBuffer sb = new StringBuffer(len);
            sb.append("<!DOCTYPE");
            if (mRootName != null) {
                sb.append(' ');
                sb.append(mRootName);
            }
            if (mSystemId != null) {
                if (mPublicId != null) {
                    sb.append(" PUBLIC \"");
                    sb.append(mPublicId);
                    sb.append("\" ");
                } else {
                    sb.append(" SYSTEM \"");
                }
                sb.append(mSystemId);
                sb.append('"');
            }
            if (mInternalSubset != null) {
                sb.append(" [");
                sb.append(mInternalSubset);
                sb.append(']');
            }
            sb.append(">");
            mFullText = sb.toString();
        }
        return mFullText;
    }

    public List getEntities() {
        /* !!! 28-Sep-2004, TSa: Need to rewrite to convert Object
         *   types contained from EntityDecl to WEntityDeclaration
         */
        if (mEntities == null && (mDTD != null)) {
            mEntities = mDTD.getGeneralEntityList();
        }
        return mEntities;
    }

    public List getNotations() {
        /* !!! 28-Sep-2004, TSa: Need to rewrite to convert Object
         *   types contained from NotationDecl to WNotationDeclaration
         */
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
            // !!! TBI
            w.write(' ');
        } catch (IOException ie) {
            throwFromIOE(ie);
        }
    }

    /*
    ///////////////////////////////////////////
    // Extended interface (DTD2)
    ///////////////////////////////////////////
     */
}
