package com.ctc.wstx.stax.exc;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

/**
 * Exception class used for notifying about well-formedness errors that
 * writers would create. Such exceptions are thrown when strict output
 * validation is enabled.
 */
public class WstxOutputException
    extends WstxException
{
    public WstxOutputException(String msg) {
        super(msg);
    }
}
