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
import java.io.Writer;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.*;

import com.ctc.wstx.api.WriterConfig;
import com.ctc.wstx.cfg.OutputConfigFlags;
import com.ctc.wstx.sw.BaseStreamWriter;
import com.ctc.wstx.sw.NonNsStreamWriter;
import com.ctc.wstx.sw.RepairingNsStreamWriter;
import com.ctc.wstx.sw.SimpleNsStreamWriter;

/**
 * Simple implementation of {@link XMLEventWriter}. The only 'special' thing
 * is that since this writer can make full use of the matching
 * {@link BaseStreamWriter}, it tries to call methods that
 * allow full validation of output (if enabled by output settings).
 */
public class WstxEventWriter
    implements XMLEventWriter,
               XMLStreamConstants,
               OutputConfigFlags
{
    final XMLStreamWriter mWriter;

    /**
     * Since we may be able to use Woodstox-specific short cuts,
     * occasionally, let's store a reference to type-safe instance,
     * if one passed.
     */
    final BaseStreamWriter mWstxWriter;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle (ctors)
    ////////////////////////////////////////////////////
     */

    public WstxEventWriter(XMLStreamWriter sw)
    {
        mWriter = sw;
        mWstxWriter = (sw instanceof BaseStreamWriter) ?
            ((BaseStreamWriter) sw) : null;
    }

    /*
    ////////////////////////////////////////////////////
    // XMLEventWriter API
    ////////////////////////////////////////////////////
     */

    /**
     *<p>
     * Note: ALL events (except for custom ones Wstx itself doesn't produce,
     * and thus can not deal with) are routed through stream writer. This
     * because it may want to do different kinds of validation
     */
    public void add(XMLEvent event)
        throws XMLStreamException
    {
        switch (event.getEventType()) {
            /* First events that we have to route via stream writer, to
             * get and/or update namespace information:
             */

        case ATTRIBUTE: // need to pass to stream writer, to get namespace info
            {
                Attribute attr = (Attribute) event;
                QName name = attr.getName();
                mWriter.writeAttribute(name.getPrefix(), name.getNamespaceURI(),
                                       name.getLocalPart(), attr.getValue());
            }
            break;

        case END_DOCUMENT:
            mWriter.writeEndDocument();
            break;

        case END_ELEMENT:
            // Let's call method that can check that element matches start element...
            {
                if (mWstxWriter != null) {
                    mWstxWriter.writeEndElement(event.asEndElement().getName());
                } else {
                    mWriter.writeEndElement();
                }
            }
            break;
            
        case NAMESPACE:
            {
                Namespace ns = (Namespace) event;
                mWriter.writeNamespace(ns.getPrefix(), ns.getNamespaceURI());
            }
            break;
            
        case START_DOCUMENT:
            {
                StartDocument sd = (StartDocument) event;
                if (!sd.encodingSet()) { // encoding defined?
                    mWriter.writeStartDocument(sd.getVersion());
                } else {
                    mWriter.writeStartDocument(sd.getCharacterEncodingScheme(),
                                               sd.getVersion());
                }
            }
            break;
            
        case START_ELEMENT:
            {
                StartElement se = event.asStartElement();
                if (mWstxWriter != null) {
                    /* Woodstox-specific stream writer has some short
                     * cuts...
                     */
                    mWstxWriter.writeStartElement(se);
                } else {
                    QName n = se.getName();
                    mWriter.writeStartElement(n.getPrefix(), n.getLocalPart(),
                                              n.getNamespaceURI());
                    Iterator it = se.getNamespaces();
                    while (it.hasNext()) {
                        Namespace ns = (Namespace) it.next();
                        add(ns);
                    }
                    it = se.getAttributes();
                    while (it.hasNext()) {
                        Attribute attr = (Attribute) it.next();
                        add(attr);
                    }
                }
            }
            break;
            
            /* Then events we could output directly if necessary... but that
             * make sense to route via stream writer, for validation
             * purposes.
             */
            
        case CHARACTERS: // better pass to stream writer, for prolog/epilog validation
            {
                Characters ch = event.asCharacters();
                String text = ch.getData();
                if (ch.isCData()) {
                    mWriter.writeCData(text);
                } else {
                    mWriter.writeCharacters(text);
                }
            }
            break;

        case CDATA:
            mWriter.writeCData(event.asCharacters().getData());
            break;
 
        case COMMENT:
            mWriter.writeComment(((Comment) event).getText());
            break;
            
        case DTD:
            mWriter.writeDTD(((DTD) event).getDocumentTypeDeclaration());
            break;

        case ENTITY_REFERENCE:
            mWriter.writeEntityRef(((EntityReference) event).getName());
            break;

        case PROCESSING_INSTRUCTION: // let's just write directly
            {
                ProcessingInstruction pi = (ProcessingInstruction) event;
                mWriter.writeProcessingInstruction(pi.getTarget(), pi.getData());
            }
            break;
            
            /* And then finally types not produced by Wstx; can/need to
             * ask event to just output itself
             */
        case ENTITY_DECLARATION: // not yet produced by Wstx
        case NOTATION_DECLARATION: // not yet produced by Wstx
        case SPACE: // usually only CHARACTERS events exist...

        default:
            /* Default handling; ok, it's possible to create one's own
             * event types... if so, let's just by-pass any checks, and
             * let event output itself.
             */
            if (mWstxWriter != null) {
                /* Related to [WSTX-141]: need to force closing of open
                 * start elements (if any), first. Simplest way (and the
                 * only generic one I know of) is to make a dummy write.
                 */
                mWstxWriter.writeCharacters("");
                event.writeAsEncodedUnicode(mWstxWriter.wrapAsRawWriter());
            } else {
                /* 08-Aug-2006, TSa: Not quite sure what to do with this.
                 *    For now, choices are throwing an exception, or
                 *    silently failing. Latter is bigger of evils, unless
                 *    proven otherwise.
                 */
                throw new XMLStreamException("Unrecognized event type ("+event.getEventType()+"), for XMLEvent of type "+event.getClass());
            }
        }
    }

    public void add(XMLEventReader reader)
        throws XMLStreamException
    {
        while (reader.hasNext()) {
            add(reader.nextEvent());
        }
    }

    public void close()
        throws XMLStreamException
    {
        mWriter.close();
    }

    public void flush()
        throws XMLStreamException
    {
        mWriter.flush();
    }

    public NamespaceContext getNamespaceContext() {
        return mWriter.getNamespaceContext();
    }

    public String getPrefix(String uri)
        throws XMLStreamException
    {
        return mWriter.getPrefix(uri);
    }

    public void setDefaultNamespace(String uri)
        throws XMLStreamException
    {
        mWriter.setDefaultNamespace(uri);
    }

    public void setNamespaceContext(NamespaceContext ctxt)
        throws XMLStreamException
    {
        mWriter.setNamespaceContext(ctxt);
    }

    public void setPrefix(String prefix, String uri)
        throws XMLStreamException
    {
        mWriter.setPrefix(prefix, uri);
    }
}
