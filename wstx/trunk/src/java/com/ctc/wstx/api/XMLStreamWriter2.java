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

    public void writeFromReader(XMLStreamReader2 r)
        throws XMLStreamException;
}
