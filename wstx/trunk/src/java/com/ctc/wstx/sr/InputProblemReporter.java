package com.ctc.wstx.sr;

import javax.xml.stream.Location;

import com.ctc.wstx.exc.WstxException;

/**
 * Interface implemented by input reader, and used by other components to
 * report problem that are related to current input position.
 */
public interface InputProblemReporter
{
    /*
    ////////////////////////////////////////////////////
    // Methods for reporting "hard" errors:
    ////////////////////////////////////////////////////
     */

    public void throwParseError(String msg)
        throws WstxException;

    /**
     * Similar to {@link #throwParseError(String)}, but specifically defines
     * location where the error happened. Used usually when validity of
     * a declaration can not be verified at the point of declaration but
     * only later on (reference to undefined id value, for example)
     */
    public void throwParseError(Location loc, String msg)
        throws WstxException;

    public void throwParseError(String msg, Object arg)
        throws WstxException;

    public void throwParseError(String msg, Object arg, Object arg2)
        throws WstxException;

    /*
    ///////////////////////////////////////////////////////
    // Methods for reporting "soft" (recoverable) problems
    ///////////////////////////////////////////////////////
     */

    public void reportProblem(String probType, String msg);

    public void reportProblem(String probType, String msg, Location loc);

    public void reportProblem(String probType, String format, Object arg);

    public void reportProblem(String probType, String format, Object arg,
                              Object arg2);

    public void reportProblem(String probType, String format, Object arg,
                              Object arg2, Location loc);

    /*
    ////////////////////////////////////////////////////
    // Supporting methods needed by reporting
    ////////////////////////////////////////////////////
     */

    public Location getLocation();
}
