package com.ctc.wstx.stax.evt;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.EntityDeclaration;
import javax.xml.stream.util.XMLEventAllocator;
import javax.xml.stream.util.XMLEventConsumer;

// Too bad we have to rely on this...
import com.ctc.wstx.stax.dtd.DTDSubset;
import com.ctc.wstx.stax.stream.BasicStreamReader;

/**
 * Straight-forward implementation of {@link XMLEventAllocator}, to be
 * used with Woodstox' event reader.
 */
public class DefaultEventAllocator
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
            {
                DTDSubset ss;
                String fullText;

                // Not sure if we really need this defensive coding but...
                if (r instanceof BasicStreamReader) {
                    BasicStreamReader wr = (BasicStreamReader) r;
                    ss = wr.getDTD();
                    fullText = wr.getDTDText();
                } else {
                    ss = null;
                    /* 16-Aug-2004, TSa: There's really no good way to find
                     *   the correct full replacement... so can either
                     *   assign null, or just the internal subset?
                     */
                    // ... let's choose the internal subset, for now...
                    fullText = r.getText();
                }
                return new WDTD(loc, fullText, ss);
            }
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
            return CompactStartElement.construct(loc, r);

        case ENTITY_REFERENCE:
            {
                WEntityDeclaration ed = ((BasicStreamReader) r).getCurrentEntityDecl();
                return new WEntityReference(loc, ed.getName(), ed);
            }


            /* Following 2 types should never get in here; they are directly
             * handled by DTDReader, and can only be accessed via DTD event
             * element.
             */
        case ENTITY_DECLARATION:
            throw new Error("Internal error: should not get ENTITY_DECLARATION.");
        case NOTATION_DECLARATION:
            throw new Error("Internal error: should not get NOTATION_DECLARATION.");

            /* Following 2 types should never get in here; they are directly
             * handled by the reader, and can only be accessed via start
             * element.
             */
        case NAMESPACE:
            throw new Error("Internal error: should not get NAMESPACE.");
        case ATTRIBUTE:
            throw new Error("Internal error: should not get ATTRIBUTE.");

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
    // Internal methods:
    //////////////////////////////////////////////////////////
     */
}
