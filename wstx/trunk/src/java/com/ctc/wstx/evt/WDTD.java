package com.ctc.wstx.evt;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.DTD;

import com.ctc.wstx.api.evt.DTD2;
import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.dtd.DTDSubset;

/**
 * Event that contains all StAX accessible information read from internal
 * and external DTD subsets.
 */
public class WDTD
    extends WEvent
    implements DTD2
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

    /*
    /////////////////////////////////////////////////////
    // Lazily constructed objects
    /////////////////////////////////////////////////////
     */

    List mEntities = null;

    List mNotations = null;

    /**
     * Full textual presentation of the DOCTYPE event; usually only
     * constructed when needed, but sometimes (when using 'broken'
     * older StAX interfaces), may be the only piece that's actually
     * passed.
     */
    String mFullText = null;

    /*
    /////////////////////////////////////////////////////
    // Constuctors
    /////////////////////////////////////////////////////
     */

    public WDTD(Location loc, String rootName,
                String sysId, String pubId, String intSubset,
                DTDSubset dtd)
    {
        super(loc);
        mDTD = dtd;
        mRootName = rootName;
        mSystemId = sysId;
        mPublicId = pubId;
        mInternalSubset = intSubset;
        mFullText = null;
    }

    public WDTD(Location loc, String rootName,
                String sysId, String pubId, String intSubset)
    {
        this(loc, rootName, sysId, pubId, intSubset, null);
    }

    /**
     * Constructor used when only partial information is available...
     */
    public WDTD(Location loc, String rootName, String intSubset)
    {
        this(loc, rootName, null, null, intSubset, null);
    }

    public WDTD(Location loc, String fullText)
    {
        this(loc, null, null, null, null, null);
        mFullText = fullText;
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
            StringWriter sw = new StringWriter(len);
            try {
                writeAsEncodedUnicode(sw);
            } catch (XMLStreamException sex) { // should never happen
                throw new Error(ErrorConsts.ERR_INTERNAL + ": "+sex);
            }
            mFullText = sw.toString();
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
            w.write("<!DOCTYPE");
            if (mRootName != null) {
                /* Can only be null for plain XMLStreamReader interface...
                 * which hopefully never happens (need to use Woodstox
                 * Event implementation on top of non-Woodstox cursor
                 * implementation)
                 */
                w.write(' ');
                w.write(mRootName);
            }
            if (mSystemId != null) {
                if (mPublicId != null) {
                    w.write(" PUBLIC \"");
                    w.write(mPublicId);
                    w.write("\" ");
                } else {
                    w.write(" SYSTEM \"");
                }
                w.write(mSystemId);
                w.write('"');
            }
            if (mInternalSubset != null) {
                w.write(" [");
                w.write(mInternalSubset);
                w.write(']');
            }
            w.write(">");
        } catch (IOException ie) {
            throwFromIOE(ie);
        }
    }

    /*
    ///////////////////////////////////////////
    // Extended interface (DTD2)
    ///////////////////////////////////////////
     */

    public String getRootName() {
        return mRootName;
    }

    public String getSystemId() {
        return mSystemId;
    }

    public String getPublicId() {
        return mPublicId;
    }

    public String getInternalSubset() {
        return mInternalSubset;
    }
}
