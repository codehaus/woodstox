package com.ctc.stax.staxmate;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Simple bit-set based filter that can be configured by specifying allowed
 * event types. Such filters are immutable and can be easily shared, but
 * can only be used if
 *
 * @author Tatu Saloranta
 */
public class SimpleFilter
    extends SMFilter
{
    final int mAllowedTypes;

    /*
    /////////////////////////////////////////////////////
    // Life-cycle
    /////////////////////////////////////////////////////
     */

    public SimpleFilter(int typeFlags) {
        mAllowedTypes = typeFlags;
    }

    public SimpleFilter extend(int additionalFlags, int removeFlags)
    {
        int newTypes = mAllowedTypes | additionalFlags & ~removeFlags;
        return new SimpleFilter(newTypes);
    }

    /*
    /////////////////////////////////////////////////////
    // SMFilter implementation
    /////////////////////////////////////////////////////
     */

    public boolean accept(int type, SMIterator caller)
        throws XMLStreamException
    {
        return (mAllowedTypes & (1 << type)) != 0;
    }
}

