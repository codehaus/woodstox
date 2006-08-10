package com.ctc.wstx.exc;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

/**
 * Intermediate base class for reporting actual Wstx parsing problems.
 */
public class WstxParsingException
    extends WstxException
{
    public WstxParsingException(String msg, Location loc) {
        super(msg, loc);
    }
}
