package com.ctc.wstx.sr;

import javax.xml.stream.*;

import org.codehaus.stax2.XMLStreamReader2;

import com.ctc.wstx.ent.EntityDecl;

/**
 * Interface that defines "internal Woodstox API". It is used to decouple
 * parts of the Woodstox that need to know something more about woodstox
 * stream reader implementation, but not about implementation details.
 * Specifically, there are some simple dependencies from the stream
 * writer; they should only need to refer to this interface.
 */
public interface StreamReaderImpl
    extends XMLStreamReader2
{
    public EntityDecl getCurrentEntityDecl();

    public Object withStartElement(ElemCallback cb, Location loc);

    /**
     * @param iterateNsTwice If true, will call ns callbacks twice (once
     *   before and once after element itself): if false, will only call
     *   them once, after the element callback.
     */
    public void iterateStartElement(ElemIterCallback cb, boolean iterateNsTwice)
        throws XMLStreamException;
}
