package com.ctc.wstx.api.evt;

import javax.xml.stream.events.StartElement;

/**
 * Interface that extends basic {@link StartElement} with method(s)
 * that are missing from it, but necessary for exact replication
 * of the element (specifically, whether element was an empty
 * element).
 */
public interface StartElement2
    extends StartElement
{
    public boolean isEmptyElement();
}
