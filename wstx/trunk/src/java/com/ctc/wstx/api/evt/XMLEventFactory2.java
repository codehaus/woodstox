package com.ctc.wstx.api.evt;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.events.*;

/**
 * Interface that adds missing (but required) methods to
 * {@link XMLEventFactory}; especially ones for creating actual
 * well-behaving DOCTYPE events.
 */
public abstract class XMLEventFactory2
    extends XMLEventFactory
{
    public abstract DTD2 createDTD(String rootName, String sysId, String pubId,
                                   String intSubset);

    public abstract DTD2 createDTD(String rootName, String sysId, String pubId,
                                   String intSubset, Object processedDTD);
}

