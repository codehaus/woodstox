package com.ctc.wstx.stax.exc;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

/**
 * Intermediate base class for actual Wstx parsing problems.
 */
public class WstxParsingException
    extends WstxException
{
    public WstxParsingException(String msg, Location loc) {
        super(msg, loc);
    }
}
