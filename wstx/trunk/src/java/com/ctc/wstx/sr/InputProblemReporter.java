package com.ctc.wstx.sr;

import javax.xml.stream.Location;

import com.ctc.wstx.exc.WstxException;
import com.ctc.wstx.exc.WstxValidationException;

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

    public void throwParseError(String msg, Object arg)
        throws WstxException;
    public void throwParseError(String msg, Object arg, Object arg2)
        throws WstxException;


    public void throwValidationError(String msg)
        throws WstxValidationException;
    public void throwValidationError(Location loc, String msg)
        throws WstxValidationException;
    public void throwValidationError(String msg, Object arg)
        throws WstxValidationException;
    public void throwValidationError(String msg, Object arg, Object arg2)
        throws WstxValidationException;

    /*
    ///////////////////////////////////////////////////////
    // Methods for reporting "soft" (recoverable) problems
    ///////////////////////////////////////////////////////
     */

    public void reportProblem(String probType, String msg);

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
