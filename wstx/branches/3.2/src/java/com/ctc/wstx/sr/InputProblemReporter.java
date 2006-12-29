package com.ctc.wstx.sr;

import javax.xml.stream.Location;

import org.codehaus.stax2.validation.XMLValidationException;
import org.codehaus.stax2.validation.XMLValidationProblem;

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

    public void reportProblem(String probType, String format, Object arg);

    public void reportProblem(String probType, String format, Object arg,
                              Object arg2);

    public void reportProblem(String probType, String format, Object arg,
                              Object arg2, Location loc);

    /*
    ///////////////////////////////////////////////////////
    // Reporting validation problems
    ///////////////////////////////////////////////////////
     */

    public void reportValidationProblem(XMLValidationProblem prob)
        throws XMLValidationException;
    public void reportValidationProblem(String msg)
        throws XMLValidationException;
    public void reportValidationProblem(String msg, Location loc, int severity)
        throws XMLValidationException;
    public void reportValidationProblem(String msg, Object arg)
        throws XMLValidationException;
    public void reportValidationProblem(String msg, Object arg, Object arg2)
        throws XMLValidationException;

    /*
    ////////////////////////////////////////////////////
    // Supporting methods needed by reporting
    ////////////////////////////////////////////////////
     */

    public Location getLocation();
}
