package org.codehaus.staxmate.sr;

import javax.xml.stream.XMLStreamException;

/**
 * Simple factory class that can be used to customize instances of
 * {@link SMElementInfo} that iterators construct and store when element
 * tracking is enabled.
 */
public interface ElementInfoFactory
{
    public SMElementInfo constructElementInfo(SMIterator it,
                                              SMElementInfo parent,
                                              SMElementInfo prevSibling)
        throws XMLStreamException;
}

