package com.ctc.wstx.stax.ns;

import javax.xml.XMLConstants;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.namespace.QName;

import com.ctc.wstx.stax.evt.WAttrList;
import com.ctc.wstx.stax.stream.StreamScanner;
import com.ctc.wstx.util.StringVector;
import com.ctc.wstx.util.TextBuilder;

/**
 * Shared base class that defines API stream reader uses to communicate
 * with the attribute collector implementation, independent of whether it's
 * operating in namespace-aware or non-namespace modes.
 * Collector class is used to build up attribute lists; for the most part
 * will just hold references to few specialized {@link TextBuilder}s that
 * are used to create efficient semi-shared value Strings.
 */
public abstract class AttributeCollector
{
    /**
     * Threshold value that indicates minimum length for lists instances
     * that need a Map structure, for fast attribute access by fully-qualified
     * name.
     */
    protected final static int LONG_ATTR_LIST_LEN = 4;

    /**
     * Expected typical maximum number of attributes for any element;
     * chosen to minimize need to resize, while trying not to waste space.
     */
    protected final static int EXP_ATTR_COUNT = 32;
    
    /*
    //////////////////////////////////////////
    // Collected attribute information:
    //////////////////////////////////////////
     */

    /*
    //////////////////////////////////////////
    // Resolved (derived) attribute information:
    //////////////////////////////////////////
     */


    /*
    ///////////////////////////////////////////////
    // Life-cycle:
    ///////////////////////////////////////////////
     */

    protected AttributeCollector() {
    }

    /**
     * Method called to allow reusing of collector, usually right before
     * starting collecting attributes for a new start tag.
     */
    protected abstract void reset();

    /*
    ///////////////////////////////////////////////
    // Public accesors (for stream reader)
    ///////////////////////////////////////////////
     */

    /**
     * @return Number of namespace declarations collected, not including
     *   possible default namespace declaration
     */
    public abstract int getNsCount();

    public abstract String getNsPrefix(int index);

    public abstract String getNsURI(int index);

    // // // Direct access to attribute/NS prefixes/localnames/URI

    public abstract int getCount();

    public abstract String getPrefix(int index);

    public abstract String getLocalName(int index);

    public abstract String getURI(int index);

    public abstract QName getQName(int index);

    public abstract String getValue(int index);

    public abstract String getValue(String nsURI, String localName);

    public abstract TextBuilder getDefaultNsBuilder();

    public abstract TextBuilder getNsBuilder(String localName);

    public abstract TextBuilder getAttrBuilder(String attrPrefix, String attrLocalName);

    /**
     * Method needed by event creating code, to build a non-transient
     * attribute container, to use with XMLEvent objects (specifically
     * implementation of StartElement event).
     */
    public abstract WAttrList buildAttrList(Location loc);

    /*
    ///////////////////////////////////////////////
    // Internal methods:
    ///////////////////////////////////////////////
     */

    protected static String[] resize(String[] old) {
        int len = old.length;
        String[] result = new String[len];
        System.arraycopy(old, 0, result, 0, len);
        return result;
    }

    protected void throwDupAttr(StreamScanner bp, int index)
        throws XMLStreamException
    {
        bp.throwParseError("Duplicate attribute '"+getQName(index)+"'.");
    }
}
