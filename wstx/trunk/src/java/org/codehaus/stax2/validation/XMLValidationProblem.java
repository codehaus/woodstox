package org.codehaus.stax2.validation;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

/**
 * Simple container class used to store validation problem (error),
 * either to be returned as is, or to use for creating and throwing
 * a validation exception.
 */
public class XMLValidationProblem
{
    protected final Location mLocation;
    protected final String mMessage;

    public XMLValidationProblem(Location loc, String msg)
    {
        mLocation = loc;
        mMessage = msg;
    }

    public Location getLocation() { return mLocation; }
    public String getMessage() { return mMessage; }
}
