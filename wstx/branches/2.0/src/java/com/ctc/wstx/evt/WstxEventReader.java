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

import org.codehaus.stax2.XMLEventReader2;

import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.exc.WstxParsingException;
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
    implements XMLEventReader2,
               XMLStreamConstants
{
    protected final static int STATE_INITIAL = 1;
    protected final static int STATE_EOD = 2;
    protected final static int STATE_CONTENT = 3;

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

    /**
     * This variable keeps track of the type of the 'previous' event
     * when peeking for the next Event. It is needed for some functionality,
     * to remember state even when underlying parser has to move to peek
     * the next event.
     */
    protected int mPrePeekEvent = START_DOCUMENT;

    public WstxEventReader(XMLEventAllocator a, XMLStreamReader r)
    {
        mAllocator = a;
        mReader = r;
    }

    /*
    //////////////////////////////////////////////////////
    // XMLEventReader API
    //////////////////////////////////////////////////////
     */

    public void close()
        throws XMLStreamException
    {
        /* Nothing much we can do -- sure, could make factory and reader
         * non-final, clear them up, but what's the point?
         */
        /*
        try { // Stupid StAX 1.0 incompatibilities
            mReader.close();
        } catch (XMLStreamException sex) {
            throwFromSex(sex); 
        }
        */

        /* 05-Dec-2004, TSa: Actually, looks like we can just throw
         *   the XMLStreamReader ok?
         */
        mReader.close();
    }

    public String getElementText()
        throws XMLStreamException
    {
        /* Simple, if no peeking occured -- can just forward this to the
         * underlying parser
         */
        if (mPeekedEvent == null) {
            return mReader.getElementText();
        }

        XMLEvent evt = mPeekedEvent;
        mPeekedEvent = null;

        /* Otherwise need to verify that we are currently over START_ELEMENT.
         * Problem is we have already went past it...
         */
        if (mPrePeekEvent != START_ELEMENT) {
            throw new WstxParsingException
                (ErrorConsts.ERR_STATE_NOT_STELEM, evt.getLocation());
        }

        String str = null;
        StringBuffer sb = null;

        /* Ok, fine, then just need to loop through and get all the
         * text...
         */
        for (; true; evt = nextEvent()) {
            if (evt.isEndElement()) {
                break;
            }
            int type = evt.getEventType();
            if (type == COMMENT || type == PROCESSING_INSTRUCTION) {
                ; // can/should just ignore them
            }
            if (!evt.isCharacters()) {
                throw new WstxParsingException("Expected a text token, got "
                                               +ErrorConsts.tokenTypeDesc(type),
                                               evt.getLocation());
            }
            String curr = evt.asCharacters().getData();
            if (str == null) {
                str = curr;
            } else {
                if (sb == null) {
                    sb = new StringBuffer(str.length() + curr.length());
                    sb.append(str);
                }
                sb.append(curr);
            }
        }
        
        if (sb != null) {
            return sb.toString();
        }
        return (str == null) ? "" : str;
    }

    public Object getProperty(String name) {
        return mReader.getProperty(name);
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
                /* !!! 07-Dec-2004, TSa: Specs are mum about Comments and PIs.
                 *  But why would they not be skipped just like what
                 *  the stream reader does?
                 */
            case COMMENT:
            case PROCESSING_INSTRUCTION:
                break;
            case CDATA:
            case CHARACTERS:
                {
                    if (((Characters) evt).isWhiteSpace()) {
                        break;
                    }
                }
                throwParseError("Received non-all-whitespace CHARACTERS or CDATA event in nextTag().");
		break; // never gets here, but some compilers whine without...
            case START_ELEMENT:
            case END_ELEMENT:
                return evt;

            default:
                throwParseError("Received event "+evt.getEventType()+", instead of START_ELEMENT or END_ELEMENT.");
            }
        } else {
            /* 13-Sep-2005, TSa: As pointed out by Patrick, we may need to
             *   initialize the state here, too; otherwise peek() won't work
             *   correctly. The problem is that following loop's get method
             *   does not use event reader's method but underlying reader's.
             *   As such, it won't update state: most importantly, initial
             *   state may not be changed to non-initial.
             */
            if (mState == STATE_INITIAL) {
                mState = STATE_CONTENT;
            }
        }

        while (true) {
            int next = mReader.next();

            switch (next) {
            case END_DOCUMENT:
                return null;
            case SPACE:
                /* !!! 07-Dec-2004, TSa: Specs are mum about Comments and PIs.
                 *  But why would they not be skipped just like what
                 *  the stream reader does?
                 */
            case COMMENT:
            case PROCESSING_INSTRUCTION:
                continue;
            case CDATA:
            case CHARACTERS:
                if (mReader.isWhiteSpace()) {
                    continue;
                }
                throwParseError("Received non-all-whitespace CHARACTERS or CDATA event in nextTag().");
		break; // just to keep Jikes happy...

            case START_ELEMENT:
            case END_ELEMENT:
                return createNextEvent(false, next);

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
                // Not sure what it should be... but this should do:
                mPrePeekEvent = START_DOCUMENT;
                mPeekedEvent = createStartEvent();
                mState = STATE_CONTENT;
            } else {
                mPrePeekEvent = mReader.getEventType();
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
    //////////////////////////////////////////////////////
    // XMLEventReader2 API
    //////////////////////////////////////////////////////
     */

    /**
     *<p>
     * Note: although the interface allows implementations to
     * throw an {@link XMLStreamException}, Woodstox doesn't currently need
     * to. It's still declared, in case in future there is need to throw
     * such an exception.
     */
    public boolean hasNextEvent()
        throws XMLStreamException
    {
        return (mState != STATE_EOD);
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
        throwParseError(msg, mReader.getLocation());
    }

    protected void throwParseError(String msg, Location loc)
        throws XMLStreamException
    {
        throw new WstxParsingException(msg, loc);
    }
}


