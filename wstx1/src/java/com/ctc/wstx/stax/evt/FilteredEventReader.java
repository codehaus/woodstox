package com.ctc.wstx.stax;

import java.io.IOException;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.*;
import javax.xml.stream.util.XMLEventAllocator;

import com.ctc.wstx.util.ExceptionUtil;

/**
 *<p>
 * Some notes about implemention:
 *<ul>
 * <li>There is no way to filter out values of peek(), so we'll just straight
 *    dispatch the call to underlying reader
 *  </li>
 *</ul>
 */
public class FilteredEventReader
    implements XMLEventReader,
               XMLStreamConstants
{
    final XMLEventReader mReader;
    final EventFilter mFilter;

    public FilteredEventReader(XMLEventReader r, EventFilter f) {
        mReader = r;
        mFilter = f;
    }

    /*
    ////////////////////////////////////////////////////
    // XMLEventReader implementation
    ////////////////////////////////////////////////////
     */

    public void close()
        throws XMLStreamException
    {
        mReader.close();
    }

    public String getElementText()
        throws XMLStreamException
    {
        // Should be ok to forward, does not iterate
        return mReader.getElementText();
    }

    public Object getProperty(String name) {
        return mReader.getProperty(name);
    }

    public boolean hasNext() {
        return mReader.hasNext();
    }

    public XMLEvent nextEvent()
        throws XMLStreamException
    {
        while (true) {
            XMLEvent evt = mReader.nextEvent();
            if (evt == null || mFilter.accept(evt)) {
                return evt;
            }
            /* ??? 11-May-2004, TSa: Should we take some precautions for
             *   END_DOCUMENT event? Or is above null check enough.
             */
        }
    }

    public Object next()
    {
        try {
            return nextEvent();
        } catch (XMLStreamException sex) {
            ExceptionUtil.throwRuntimeException(sex);
            return null; // never gets here
        }
    }

    public XMLEvent nextTag()
        throws XMLStreamException
    {
        // This can be implemented very similar to next()...

        while (true) {
            XMLEvent evt = mReader.nextTag();
            if (evt == null || mFilter.accept(evt)) {
                return evt;
            }
        }
    }

    /**
     * Note: there is no way to do any filtering here; will simply dispatch
     * the call to the underlying reader.
     */
    public XMLEvent peek()
        throws XMLStreamException
    {
        return mReader.peek();
    }

    /**
     * Note: only here because we implement Iterator interface
     */
    public void remove() {
        mReader.remove();
    }
}

