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
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.*;

import com.ctc.wstx.cfg.OutputConfigFlags;
import com.ctc.wstx.sw.WriterConfig;
import com.ctc.wstx.sw.WstxStreamWriter;
import com.ctc.wstx.sw.WstxNonNsStreamWriter;
import com.ctc.wstx.sw.WstxNsStreamWriter;

/**
 * Simple implementation of {@link XMLEventWriter}. The only 'special' thing
 * is that since this writer can make full use of the matching
 * {@link WstxStreamWriter}, it tries to call methods that
 * allow full validation of output (if enabled by output settings).
 */
public class WstxEventWriter
    implements XMLEventWriter,
               XMLStreamConstants,
               OutputConfigFlags
{
    final WstxStreamWriter mWriter;

    // // // Specific config flags we are interested in often:

    final boolean mEnableNS;

    final boolean mAutomaticNS;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle (ctors)
    ////////////////////////////////////////////////////
     */

    public WstxEventWriter(Writer w, WriterConfig cfg)
    {
        int flags = cfg.getConfigFlags();
        mEnableNS = (flags & CFG_ENABLE_NS) != 0;
        if (mEnableNS) {
            mWriter = new WstxNsStreamWriter(w, cfg);
        } else {
            mWriter = new WstxNonNsStreamWriter(w, cfg);
        }
        mAutomaticNS = (flags & CFG_AUTOMATIC_NS) != 0;
    }

    public WstxEventWriter(WstxStreamWriter sw, WriterConfig cfg)
    {
        int flags = cfg.getConfigFlags();
        mEnableNS = (flags & CFG_ENABLE_NS) != 0;
        mWriter = sw;
        mAutomaticNS = (flags & CFG_AUTOMATIC_NS) != 0;
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
                mWriter.writeEndElement(event.asEndElement().getName());
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
                if (sd.encodingSet()) { // encoding defined?
                    mWriter.writeStartDocument(sd.getVersion());
                } else {
                    mWriter.writeStartDocument(sd.getCharacterEncodingScheme(),
                                               sd.getVersion());
                }
            }
            break;
            
        case START_ELEMENT:
            mWriter.writeStartElement(event.asStartElement());
            break;
            
            /* Then events we could output directly if necessary... but that
             * make sense to route via stream writer, for validation
             * purposes.
             */
            
        case CHARACTERS: // better pass to stream writer, for prolog/epilog validation
            mWriter.writeCharacters(event.asCharacters());
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
        case CDATA: // usually only CHARACTERS events exist...
        case ENTITY_DECLARATION: // not yet produced by Wstx
        case NOTATION_DECLARATION: // not yet produced by Wstx
        case SPACE: // usually only CHARACTERS events exist...

        default:
            /* Default handling; ok, it's possible to create one's own
             * event types... if so, let's just by-pass any checks, and
             * let event output itself.
             */
            event.writeAsEncodedUnicode(mWriter.getWriter());
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

    public String getPrefix(String uri) {
        return mWriter.getPrefix(uri);
    }

    public void setDefaultNamespace(String uri)
        throws XMLStreamException
    {
        mWriter.setDefaultNamespace(uri);
    }

    public void setNamespaceContext(NamespaceContext ctxt) {
        mWriter.setNamespaceContext(ctxt);
    }

    public void setPrefix(String prefix, String uri)
        throws XMLStreamException
    {
        mWriter.setPrefix(prefix, uri);
    }
}
