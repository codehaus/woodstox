/* Woodstox XML processor
 *
 * Copyright (c) 2004 Tatu Saloranta, tatu.saloranta@iki.fi
 *
 * Licensed under the License specified in the file LICENSE which is
 * included with the source code.
 * You may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ctc.wstx.evt;

import java.io.IOException;
import java.io.Reader;
import java.util.NoSuchElementException;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.*;
import javax.xml.stream.util.XMLEventAllocator;

import com.ctc.wstx.util.ExceptionUtil;

/**
 * Implementation of {@link XMLEventReader}, built on top of
 * {@link com.ctc.wstx.sr.WstxStreamReader} (using composition).
 * Since there is not much to optimize at this
 * level (API and underlying stream reader pretty much define functionality
 * and optimizations that can be done), implementation is fairly straight
 * forward, with not many surprises.
 *<p>
 * Implementation notes: trickiest things to implement are:
 * <ul>
 *  <li>Peek() functionality! Geez, why did that have to be part of StAX
 *    specs???!
 *   </li>
 *  <li>Adding START_DOCUMENT event that cursor API does not return
 *    explicitly.
 *   </li>
 * </ul>
 */
public class WstxEventReader
    implements XMLEventReader,
               XMLStreamConstants
{
    protected final int STATE_INITIAL = 1;
    protected final int STATE_EOD = 2;
    protected final int STATE_CONTENT = 3;

    private final XMLEventAllocator mAllocator;

    private final XMLStreamReader mReader;

    /**
     * Event that has been peeked, ie. loaded without call to
     * {@link #nextEvent}; will be returned and cleared by
     * call to {@link #nextEvent} (or, returned again if peeked
     * again)
     */
    private XMLEvent mPeekedEvent = null;

    /**
     * High-level state indicator, with currently three values: 
     * whether we are initializing (need to synthetize START_DOCUMENT),
     * at EOD (end-of-doc), or otherwise, normal operation.
     * Useful in simplifying some methods, as well as to make sure
     * that independent of how stream reader handles things, event reader
     * can reliably detect End-Of-Document.
     */
    protected int mState = STATE_INITIAL;

    public WstxEventReader(XMLEventAllocator a, XMLStreamReader r)
    {
        mAllocator = a;
        mReader = r;
    }

    public void close() {
        /* Nothing much we can do -- sure, could make factory and reader
         * non-final, clear them up, but what's the point?
         */
        try { // Stupid StAX 1.0 incompatibilities
            mReader.close();
        } catch (XMLStreamException sex) {
            throwFromSex(sex); 
        }
    }

    public String getElementText()
        throws XMLStreamException
    {
        if (mPeekedEvent != null) {
            XMLEvent evt = mPeekedEvent;
            mPeekedEvent = null;
            if (evt.isCharacters()) {
                String str = evt.asCharacters().getData();
                /* !!! TBI: doesn't yet guarantee we'll point to END_ELEMENT;
                 *   should peek until it does, and coalesce if we happen
                 *   to have multiple text segments...
                 */
                return str;
            }
            throw new XMLStreamException("Expected a text token, got "
                                         +evt.getEventType()+".");
        }
        return mReader.getElementText();
    }

    public Object getProperty(String name) {
        // !!! TBI
        return null;
    }

    public boolean hasNext() {
        return (mState != STATE_EOD);
    }

    public XMLEvent nextEvent()
        throws XMLStreamException
    {
        if (mState == STATE_EOD) {
            throw new NoSuchElementException();
        } else if (mState == STATE_INITIAL) {
            mState = STATE_CONTENT;
            return createStartEvent();
        }
        if (mPeekedEvent != null) {
            XMLEvent evt = mPeekedEvent;
            mPeekedEvent = null;
            if (evt.isEndDocument()) {
                mState = STATE_EOD;
            }
            return evt;
        }
        return createNextEvent(true, mReader.next());
    }

    public Object next() {
        try {
            return nextEvent();
        } catch (XMLStreamException sex) {
            throwFromSex(sex);
            return null;
        }
    }

    public XMLEvent nextTag()
        throws XMLStreamException
    {
        // If we have peeked something, need to process it
        if (mPeekedEvent != null) {
            XMLEvent evt = mPeekedEvent;
            mPeekedEvent = null;
            switch (evt.getEventType()) {
            case END_DOCUMENT:
                return null;
            case SPACE:
                break;
            case CDATA:
            case CHARACTERS:
                {
                    if (((Characters) mPeekedEvent).isWhiteSpace()) {
                        break;
                    }
                }
                throwParseError("Received non-all-whitespace CHARACTERS or CDATA event in nextTag().");
            case START_ELEMENT:
            case END_ELEMENT:
                return evt;
            case COMMENT:
            case PROCESSING_INSTRUCTION:
            default:
                throwParseError("Received event "+mPeekedEvent.getEventType()+", instead of START_ELEMENT or END_ELEMENT.");
            }
        }
        while (true) {
            int next = mReader.next();

            switch (next) {
            case END_DOCUMENT:
                return null;
            case SPACE:
                continue;
            case CDATA:
            case CHARACTERS:
                if (mReader.isWhiteSpace()) {
                    continue;
                }
                throwParseError("Received non-all-whitespace CHARACTERS or CDATA event in nextTag().");
            case START_ELEMENT:
            case END_ELEMENT:
                return createNextEvent(false, next);
            case COMMENT:
            case PROCESSING_INSTRUCTION:
            default:
                throwParseError("Received event "+next+", instead of START_ELEMENT or END_ELEMENT.");
            }
        }
    }

    public XMLEvent peek()
        throws XMLStreamException
    {
        if (mPeekedEvent == null) {
            if (mState == STATE_EOD) {
                throwEOD();
            }
            if (mState == STATE_INITIAL) {
                mPeekedEvent = createStartEvent();
                mState = STATE_CONTENT;
            } else {
                mPeekedEvent = createNextEvent(false, mReader.next());
            }
        }
        return mPeekedEvent;
    }

    /**
     * Note: only here because we implement Iterator interface
     */
    public void remove() {
        throw new UnsupportedOperationException("Can not remove events from XMLEventReader.");
    }

    /*
    ///////////////////////////////////////////////
    // Private/package methods
    ///////////////////////////////////////////////
     */

    protected XMLEvent createNextEvent(boolean checkEOD, int type)
        throws XMLStreamException
    {
        XMLEvent evt = mAllocator.allocate(mReader);
        if (checkEOD && type == END_DOCUMENT) {
            mState = STATE_EOD;
        }
        return evt;
    }

    /**
     * Method called to create the very first START_DOCUMENT event.
     */
    protected XMLEvent createStartEvent()
        throws XMLStreamException
    {
        XMLEvent start = mAllocator.allocate(mReader);
        return start;
    }

    private void throwEOD()
    {
        throw new NoSuchElementException();
    }

    protected void throwFromSex(XMLStreamException sex)
    {
        /* Not sure what the best unchecked exception type is (esp. since some
         * of more logical types have no constructor that takes root cause),
         * but hopefully this will do
         */
        ExceptionUtil.throwRuntimeException(sex);
    }

    protected void throwParseError(String msg)
        throws XMLStreamException
    {
        throw new XMLStreamException(msg, mReader.getLocation());
    }
}


