package org.codehaus.stax2.validation;

import javax.xml.stream.Location;

/**
 * Simple container class used to store validation problem (error),
 * either to be returned as is, or to use for creating and throwing
 * a validation exception.
 */
public class XMLValidationProblem
{
    public final static int SEVERITY_WARNING = 1;
    public final static int SEVERITY_ERROR = 2;
    public final static int SEVERITY_FATAL = 3;

    protected final Location mLocation;
    protected final String mMessage;
    protected final int mSeverity;

    public XMLValidationProblem(Location loc, String msg)
    {
        this(loc, msg, SEVERITY_ERROR);
    }

    public XMLValidationProblem(Location loc, String msg, int severity)
    {
        mLocation = loc;
        mMessage = msg;
        mSeverity = severity;
    }

    public Location getLocation() { return mLocation; }
    public String getMessage() { return mMessage; }
    public int getSeverity() { return mSeverity; }
}
