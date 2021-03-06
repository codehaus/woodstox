package com.ctc.wstx.stax.evt;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.EntityDeclaration;

import com.ctc.wstx.stax.io.InputSourceFactory;
import com.ctc.wstx.stax.io.WstxInputLocation;
import com.ctc.wstx.stax.io.WstxInputResolver;
import com.ctc.wstx.stax.io.WstxInputSource;
import com.ctc.wstx.util.XMLQuoter;

public class WEntityDeclInt
    extends WEntityDeclaration
{
    /**
     * Location where entity content definition started;
     * points to the starting/opening quote for internal
     * entities.
     */
    protected final Location mContentLocation;

    /**
     * Replacement text of the entity; full array contents.
     */
    final char[] mRepl;

    String mReplText = null;

    public WEntityDeclInt(Location loc, String name, URL ctxt,
                          char[] repl, Location defLoc)
    {
        super(loc, name, ctxt);
        mRepl = repl;
        mContentLocation = defLoc;
    }

    public static WEntityDeclInt create(String id, String repl)
    {
        return create(id, repl.toCharArray());
    }

    public static WEntityDeclInt create(String id, char[] val)
    {
        WstxInputLocation loc = WstxInputLocation.getEmptyLocation();
        return new WEntityDeclInt(loc, id, null, val, loc);
    }
    
    public String getNotationName() {
        return null;
    }

    public String getPublicId() {
        return null;
    }

    public String getReplacementText()
    {
        String repl = mReplText;
        if (repl == null) {
            repl = (mRepl.length == 0) ? "" : new String(mRepl);
            mReplText = repl;
        }
        return mReplText;
    }

    public String getSystemId() {
        return null;
    }

    /*
    ///////////////////////////////////////////
    // Implementation of abstract base methods
    ///////////////////////////////////////////
     */

    public void writeAsEncodedUnicode(Writer w)
        throws XMLStreamException
    {
        try {
            w.write("<!ENTITY ");
            w.write(mName);
            w.write(" \"");
            XMLQuoter.outputDTDText(w, mRepl, 0, mRepl.length);
            w.write("\">");
        } catch (IOException ie) {
            throw new XMLStreamException(ie);
        }
    }

    /*
    ///////////////////////////////////////////
    // Extended API for Wstx core
    ///////////////////////////////////////////
     */

    // // // Access to data

    /**
     * Gives raw access to replacement text data...
     *<p>
     * Note: this is not really safe, as caller can modify the array, but
     * since this method is thought to provide fast access, let's avoid making
     * copy here.
     */
    public char[] getReplacementChars() {
        return mRepl;
    }

    // // // Type information
    
    public boolean isExternal() { return false; }
    
    public boolean isParsed() { return true; }
    
    public WstxInputSource createInputSource(WstxInputSource parent,
                                             WstxInputResolver res)
    {
        return InputSourceFactory.constructCharArraySource
            (parent, mName, mRepl, 0, mRepl.length, mContentLocation, getSource());
    }
}
