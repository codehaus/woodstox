package com.ctc.wstx.stax.exc;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

/**
 * Exception thrown during parsing, if an unexpected EOF is encountered.
 * Location usually signals starting position of current Node.
 */
public class WstxEOFException
    extends WstxParsingException
{
    public WstxEOFException(String msg, Location loc) {
        super(msg, loc);
    }
}
