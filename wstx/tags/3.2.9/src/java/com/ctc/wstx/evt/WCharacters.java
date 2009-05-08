package com.ctc.wstx.evt;

import java.io.IOException;
import java.io.Writer;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Characters;

import com.ctc.wstx.io.TextEscaper;

public class WCharacters
    extends WEvent
    implements Characters
{
    final String mContent;

    final boolean mIsCData;
    final boolean mIgnorableWS;

    boolean mWhitespaceChecked = false;
    boolean mIsWhitespace = false;

    public WCharacters(Location loc, String content, boolean cdata)
    {
        super(loc);
        mContent = content;
        mIsCData = cdata;
        mIgnorableWS = false;
    }

    /**
     * Constructor for creating white space characters...
     */
    private WCharacters(Location loc, String content,
                        boolean cdata, boolean allWS, boolean ignorableWS)
    {
        super(loc);
        mContent = content;
        mIsCData = cdata;
        mIsWhitespace = allWS;
        if (allWS) {
            mWhitespaceChecked = true;
            mIgnorableWS = ignorableWS;
        } else {
            mWhitespaceChecked = false;
            mIgnorableWS = false;
        }
    }

    public final static WCharacters createIgnorableWS(Location loc, String content) {
        return new WCharacters(loc, content, false, true, true);
    }

    public final static WCharacters createNonIgnorableWS(Location loc, String content) {
        return new WCharacters(loc, content, false, true, false);
    }

    /*
    /////////////////////////////////////////////////////
    // Implementation of abstract base methods, overrides
    /////////////////////////////////////////////////////
     */

    public Characters asCharacters() { // overriden to save a cast
        return this;
    }

    public int getEventType() {
        return mIsCData ? CDATA : CHARACTERS;
    }

    public boolean isCharacters() { return true; }

    public void writeAsEncodedUnicode(Writer w)
        throws XMLStreamException
    {
        try {
            if (mIsCData) {
                w.write("<![CDATA[");
                w.write(mContent);
                w.write("]]>");
            } else {
                TextEscaper.writeEscapedXMLText(w, mContent);
            }
        } catch (IOException ie) {
            throwFromIOE(ie);
        }
    }

    public void writeUsing(XMLStreamWriter w) throws XMLStreamException
    {
        if (mIsCData) {
            w.writeCData(mContent);
        } else {
            w.writeCharacters(mContent);
        }
    }

    /*
    ///////////////////////////////////////////
    // Attribute implementation
    ///////////////////////////////////////////
     */

    public String getData() {
        return mContent;
    }

    public boolean isCData() {
        return mIsCData;
    }

    public boolean isIgnorableWhiteSpace() {
        return mIgnorableWS;
    }

    public boolean isWhiteSpace() {
        // Better only do white space check, if it's done already...
        if (!mWhitespaceChecked) {
            mWhitespaceChecked = true;
            String str = mContent;
            int i = 0;
            int len = str.length();
            for (; i < len; ++i) {
                if (str.charAt(i) > 0x0020) {
                    break;
                }
            }
            mIsWhitespace = (i == len);
        }
        return mIsWhitespace;
    }

    /*
    ///////////////////////////////////////////
    // Package access...
    ///////////////////////////////////////////
     */

    public void setWhitespaceStatus(boolean status) {
        mWhitespaceChecked = true;
        mIsWhitespace = status;
    }

    /*
    ///////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////
     */

}
