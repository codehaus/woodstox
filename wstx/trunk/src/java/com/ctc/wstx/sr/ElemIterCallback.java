package com.ctc.wstx.sr;

import javax.xml.stream.XMLStreamException;

/**
 * Abstract base class that defines set of simple callbacks to be
 * called by the stream reader, passing information about element
 * that the stream currently points to, if any.
 *<p>
 * Note: the reason why this is an abstract class (not interface) is
 * performance: calling abstract methods is somewhat faster than calling
 * methods via interface. Normally this might not be that big of a deal,
 * but since this callback interface is internally used when copying
 * (start) elements, the performance optimization is worth the limitations,
 * since design was done such way that having to extend this class is
 * not a problem.
 *<p>
 * Also note that since this class is not defined as part
 * of the public API,
 * it's not expected to be normally used by the application code.
 */
public abstract class ElemIterCallback
{
    public abstract void iterateElement(String prefix, String localName,
                                        String nsURI, boolean isEmpty)
        throws XMLStreamException;

    public abstract void iterateNamespace(String prefix, String nsURI)
        throws XMLStreamException;

    public abstract void iterateAttribute(String prefix, String localName,
                                          String nsURI, boolean isSpecified,
                                          String value)
        throws XMLStreamException;
}
