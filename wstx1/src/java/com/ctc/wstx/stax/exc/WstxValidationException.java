package com.ctc.wstx.stax.exc;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

/**
 * Specific exception thrown when document has validation (DTD) errors;
 * things that are not wellformedness problems.
 */
public class WstxValidationException
    extends WstxException
{
    public WstxValidationException(String msg, Location loc) {
        super(msg, loc);
    }
}
