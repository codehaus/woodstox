package com.ctc.wstx.exc;

import java.io.IOException;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

/**
 * Simple wrapper for {@link IOException}s; needed when StAX does not expose
 * underlying I/O exceptions via its methods.
 */
public class WstxIOException
    extends WstxException
{
    public WstxIOException(IOException ie) {
        super(ie);
    }
}
