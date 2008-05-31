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

    /**
     * @since 3.0
     */
    protected String mType;

    public XMLValidationProblem(Location loc, String msg)
    {
        this(loc, msg, SEVERITY_ERROR);
    }

    public XMLValidationProblem(Location loc, String msg, int severity)
    {
        this(loc, msg, severity, null);
    }

    public XMLValidationProblem(Location loc, String msg, int severity,
                                String type)
    {
        mLocation = loc;
        mMessage = msg;
        mSeverity = severity;
        mType = type;
    }

    /**
     * @since 3.0
     */
    public void setType(String t) { mType = t; }

    /**
     * @return Reference to location where problem was encountered.
     */
    public Location getLocation() { return mLocation; }

    /**
     * @return Human-readable message describing the problem 
     */
    public String getMessage() { return mMessage; }

    /**
     * @return One of <code>SEVERITY_</code> constants
     *   (such as {@link #SEVERITY_WARNING}
     */
    public int getSeverity() { return mSeverity; }

    /**
     * @return Generic type (class) of the problem; may be null
     *   if validator does not provide such details
     *
     * @since 3.0
     */
    public String getType() { return mType; }
}
