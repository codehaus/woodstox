package org.codehaus.stax2;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;

/**
 * Extended interface that implements functionality that is missing
 * from {@link XMLEventReader}, based on findings on trying to
 * implement StAX API v1.0.
 */
public interface XMLEventReader2
    extends XMLEventReader
{
    /**
     * Method that is similar to {@link #hasNext}, except that it can
     * throw a {@link XMLStreamException}. This is important distinction,
     * since the underlying stream reader is allowed to throw such an
     * exception when its
     * <code>hasNext()</code> gets called.
     *
     */
    public boolean hasNextEvent() throws XMLStreamException;
}
