/* Woodstox XML processor
 *
 * Copyright (c) 2004 Tatu Saloranta, tatu.saloranta@iki.fi
 *
 * Licensed under the License specified in file LICENSE, included with
 * the source code.
 * You may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ctc.wstx.evt;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.EntityDeclaration;
import javax.xml.stream.util.XMLEventAllocator;
import javax.xml.stream.util.XMLEventConsumer;

import com.ctc.wstx.api.XMLStreamReader2;
import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.dtd.DTDSubset;
import com.ctc.wstx.ent.EntityDecl;
import com.ctc.wstx.exc.WstxException;
import com.ctc.wstx.sr.ElemAttrs;
import com.ctc.wstx.sr.ElemCallback;
import com.ctc.wstx.sr.WstxStreamReader;
import com.ctc.wstx.util.BaseNsContext;

/**
 * Straight-forward implementation of {@link XMLEventAllocator}, to be
 * used with Woodstox' event reader.
 *<p>
 * One of few complications here is the way start elements are constructed.
 * The pattern used is double-indirection, needed to get a callback from
 * the stream reader, with data we need for constructing even Object...
 * but without stream reader having any understanding of event Objects
 * per se.
 */
public class DefaultEventAllocator
    extends ElemCallback
    implements XMLEventAllocator, XMLStreamConstants
{
    final static DefaultEventAllocator sRootInstance = new DefaultEventAllocator();

    public DefaultEventAllocator() { }

    public static DefaultEventAllocator rootInstance() {
        return sRootInstance;
    }

    /*
    //////////////////////////////////////////////////////////
    // XMLEventAllocator implementation
    //////////////////////////////////////////////////////////
     */

    public XMLEvent allocate(XMLStreamReader r)
        throws XMLStreamException
    {
        Location loc = r.getLocation();

        switch (r.getEventType()) {
        case CDATA:
            return new WCharacters(loc, r.getText(), true);
        case CHARACTERS:
            return new WCharacters(loc, r.getText(), false);
        case COMMENT:
            return new WComment(loc, r.getText());
        case DTD:
            // Not sure if we really need this defensive coding but...
            if (r instanceof XMLStreamReader2) {
                XMLStreamReader2 sr2 = (XMLStreamReader2) r;
                return new WDTD(loc,
                                sr2.getDTDRootName(),
                                sr2.getDTDSystemId(), sr2.getDTDPublicId(),
                                sr2.getDTDInternalSubset(),
                                (DTDSubset) sr2.getProcessedDTD());
            }
            /* No way to get all information... the real big problem is
             * that of how to access root name: it's obligatory for
             * DOCTYPE construct. :-/
             */
            return new WDTD(loc, null, r.getText());

        case END_DOCUMENT:
            return new WEndDocument(loc);

        case END_ELEMENT:
            return new WEndElement(loc, r);

        case PROCESSING_INSTRUCTION:
            return new WProcInstr(loc, r.getPITarget(), r.getPIData());
        case SPACE:
            {
                WCharacters ch = new WCharacters(loc, r.getText(), false);
                ch.setWhitespaceStatus(true);
                return ch;
            }
        case START_DOCUMENT:
            return new WStartDocument(loc, r);

        case START_ELEMENT:
            {
                /* Creating the event is bit complicated, as the stream
                 * reader is not to know anything about event objects.
                 * To do this, we do double-indirection, which means that
                 * this object actually gets a callback:
                 */
                WstxStreamReader sr = (WstxStreamReader) r;
                BaseStartElement be = (BaseStartElement) sr.withStartElement(this, loc);
                if (be == null) { // incorrect state
                    throw new WstxException("Trying to create START_ELEMENT when current event is "
                                            +ErrorConsts.tokenTypeDesc(sr.getEventType()),
                                            loc);
                }
                return be;
            }

        case ENTITY_REFERENCE:
            {
                EntityDecl ed = ((WstxStreamReader) r).getCurrentEntityDecl();
                return new WEntityReference(loc, ed);
            }


            /* Following 2 types should never get in here; they are directly
             * handled by DTDReader, and can only be accessed via DTD event
             * element.
             */
        case ENTITY_DECLARATION:
        case NOTATION_DECLARATION:
            /* Following 2 types should never get in here; they are directly
             * handled by the reader, and can only be accessed via start
             * element.
             */
        case NAMESPACE:
        case ATTRIBUTE:
            throw new WstxException("Internal error: should not get "
                                    +ErrorConsts.tokenTypeDesc(r.getEventType()));
        default:
            throw new Error("Unrecognized event type "+r.getEventType()+".");
        }
    }
    
    public void allocate(XMLStreamReader r, XMLEventConsumer consumer)
        throws XMLStreamException
    {
        consumer.add(allocate(r));
    }

    /**
     * Default implementation just returns the shared default instance;
     * sub-classes may wish to override this.
     */
    public XMLEventAllocator newInstance() {
        return sRootInstance;
    }
    
    /*
    //////////////////////////////////////////////////////////
    // ElemCallback implementation
    //////////////////////////////////////////////////////////
     */

    public Object withStartElement(Location loc, QName name,
                                   BaseNsContext nsCtxt, ElemAttrs attrs)
    {
        return new CompactStartElement(loc, name, nsCtxt, attrs);
    }

    /*
    //////////////////////////////////////////////////////////
    // Internal methods:
    //////////////////////////////////////////////////////////
     */
}
