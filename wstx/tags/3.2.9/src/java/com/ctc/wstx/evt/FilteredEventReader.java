package com.ctc.wstx.evt;

import java.io.IOException;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.*;
import javax.xml.stream.util.XMLEventAllocator;

import com.ctc.wstx.exc.WstxLazyException;
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

    /**
     * Actually, we need to do local buffering; that's the only way
     * to reliably implement filtering with peeking.
     */
    XMLEvent mNextEvent;

    public FilteredEventReader(XMLEventReader r, EventFilter f)
    {
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
        /* 09-May-2006, TSa: Not sure if this is good enough: might
         *   need to improve.
         */
        return mReader.getElementText();
    }

    public Object getProperty(String name) {
        return mReader.getProperty(name);
    }

    public boolean hasNext()
    {
        try {
            return (peek() != null);
        } catch (XMLStreamException sex) { // shouldn't happen, but...
            WstxLazyException.throwLazily(sex);
            return false; // never gets this far
        }
    }

    public XMLEvent nextEvent()
        throws XMLStreamException
    {
        while (true) {
            XMLEvent evt = mReader.nextEvent();
            if (evt == null || mFilter.accept(evt)) {
                // should never get null, actually, but...
                return evt;
            }
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
     * This is bit tricky to implement, but it should filter out
     * events just as nextEvent() would.
     */
    public XMLEvent peek()
        throws XMLStreamException
    {
        while (true) {
            XMLEvent evt = mReader.peek();
            if (evt == null || mFilter.accept(evt)) {
                return evt;
            }
            // Need to discard as long as we have events:
            mReader.nextEvent();
        }
    }

    /**
     * Note: only here because we implement Iterator interface
     */
    public void remove() {
        mReader.remove();
    }
}

