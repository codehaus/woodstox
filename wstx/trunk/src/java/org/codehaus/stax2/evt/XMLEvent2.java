package org.codehaus.stax2.evt;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;

import javax.xml.stream.events.StartElement;

/**
 * Interface that extends basic {@link XMLEvent2} with method(s)
 * that are missing from it; specifically linkage that allows using
 * a stream/event writer for outputting.
 *<p>
 * NOTE: Unfortunately there is no way to cleanly retrofit this interface
 * to actual implementation classes, so some casting is necessary to
 * make use of new features.
 */
public interface XMLEvent2
    extends XMLEvent
{
    public void writeUsing(XMLStreamWriter w) throws XMLStreamException;
}
