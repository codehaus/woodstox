package com.ctc.wstx.api;

import javax.xml.stream.XMLStreamReader;

/**
 * Extended interface that implements functionality that is necessary
 * to properly build event API on top of {@link XMLStreamReader}.
 * It also adds limited number of methods that are important for
 * efficient pass-through processing (such as one needed when routing
 * SOAP-messages).
 */
public interface XMLStreamReader2
    extends XMLStreamReader
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
    ///////////////////////////
    // DOCTYPE info accessors
    ///////////////////////////
    */

    /**
     * @return If current event is DTD, DTD support is enabled,
     *   and reader supports DTD processing, returns an internal
     *   Object implementation uses for storing/processing DTD;
     *   otherwise returns null.
     */
    public Object getProcessedDTD();

    /**
     * @return If current event is DTD, returns the full root name
     *   (including prefix, if any); otherwise returns null
     */
    public String getDTDRootName();

    /**
     * @return If current event is DTD, and has a system id, returns the
     *   system id; otherwise returns null.
     */
    public String getDTDSystemId();

    /**
     * @return If current event is DTD, and has a public id, returns the
     *   public id; otherwise returns null.
     */
    public String getDTDPublicId();

    /**
     * @return If current event is DTD, and has an internal subset,
     *   returns the internal subset; otherwise returns null.
     */
    public String getDTDInternalSubset();

    /*
    ///////////////////////////
    // Other accessors
    ///////////////////////////
    */

    /**
     * @return True, if current event is START_ELEMENT
     *   and is based on a parsed empty element.
     */
    public boolean isEmptyElement();

    /**
     * @return Number of open elements in the stack; 0 when parser is in
     *  prolog/epilog, 1 inside root element and so on. Depth is same
     *  for matching start/end elements.
     */
    public int getDepth();

}

