package org.codehaus.stax2;

import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.validation.XMLValidationProblem;

/**
 * Extension of {@link XMLReporter} to allow for better access to
 * information about the actual problem.
 *
 * @since 3.0
 */
public interface XMLReporter2
    extends XMLReporter
{
    // From base interface:
    //public void report(String message, String errorType, Object relatedInformation, Location location)

    /**
     * Reporting method called with reference to object that defines
     * exact problem being encountered. Implementor is free to
     * quietly handle the problem, or to throw an exception
     * to cause abnormal termination of xml processing.
     */
    public void report(XMLValidationProblem problem) throws XMLStreamException;
}
