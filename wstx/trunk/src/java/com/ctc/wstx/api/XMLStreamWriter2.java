package com.ctc.wstx.api;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Extended interface that implements functionality that is necessary
 * to properly build event API on top of {@link XMLStreamWriter},
 * as well as to configure individual instances.
 * It also adds limited number of methods that are important for
 * efficient pass-through processing (such as one needed when routing
 * SOAP-messages).
 */
public interface XMLStreamWriter2
    extends XMLStreamWriter
{
    /*
    ///////////////////////////
    // Configuration
    ///////////////////////////
    */

    /**
     * @param name Name of the feature of which value to get
     *
     * @return Value of the feature (possibly null), if supported; null
     *     otherwise
     */
    public Object getFeature(String name);

    /**
     * @param name Name of the feature to set
     * @param value Value to set feature to.
     */
    public void setFeature(String name, Object value);

    /*
    //////////////////////////////////////////
    // Write methods base interface is missing
    //////////////////////////////////////////
    */

    public void writeDTD(String rootName, String systemId, String publicId,
                         String internalSubset)
        throws XMLStreamException;

    /*
    ///////////////////////////
    // Pass-through methdods
    ///////////////////////////
    */

    /**
     * Method that writes specified content as is, without encoding or
     * deciphering it in any way. It will not update state of the writer
     * (except by possibly flushing output of previous writes, like
     * finishing a start element),
     * nor be validated in any way. As such, care must be taken, if this
     * method is used.
     *<p>
     * Method is usually used when encapsulating output from another writer
     * as a sub-tree, or when passing through XML fragments.
     */
    public void writeRaw(String text)
        throws XMLStreamException;

    /**
     * Method that writes specified content as is, without encoding or
     * deciphering it in any way. It will not update state of the writer
     * (except by possibly flushing output of previous writes, like
     * finishing a start element),
     * nor be validated in any way. As such, care must be taken, if this
     * method is used.
     *<p>
     * Method is usually used when encapsulating output from another writer
     * as a sub-tree, or when passing through XML fragments.
     */
    public void writeRaw(char[] text, int offset, int length)
        throws XMLStreamException;

    public void writeFromReader(XMLStreamReader2 r)
        throws XMLStreamException;
}
