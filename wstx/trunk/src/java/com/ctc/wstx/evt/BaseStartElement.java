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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

import com.ctc.wstx.api.evt.StartElement2;
import com.ctc.wstx.util.BaseNsContext;
import com.ctc.wstx.util.EmptyIterator;

/**
 * Shared base class of {@link StartElement} implementations Wstx uses.
 */
abstract class BaseStartElement
    extends WEvent
    implements StartElement2
{
    protected final QName mName;

    protected final BaseNsContext mNsCtxt;

    protected final boolean mWasEmpty;

    /*
    /////////////////////////////////////////////
    // Life cycle
    /////////////////////////////////////////////
     */

    protected BaseStartElement(Location loc, QName name, BaseNsContext nsCtxt,
                               boolean wasEmpty)
    {
        super(loc);
        mName = name;
        mNsCtxt = nsCtxt;
        mWasEmpty = wasEmpty;
    }

    /*
    /////////////////////////////////////////////
    // StartElement API
    /////////////////////////////////////////////
     */

    public abstract Attribute getAttributeByName(QName name);

    public abstract Iterator getAttributes();

    public final QName getName() {
        return mName;
    }

    public Iterator getNamespaces() 
    {
        if (mNsCtxt == null) {
            return EmptyIterator.getInstance();
        }
        /* !!! 28-Sep-2004: Should refactor, since now it's up to ns context
         *   to construct namespace events... which adds unnecessary
         *   up-dependency from stream level to event objects.
         */
        return mNsCtxt.getNamespaces();
    }

    public NamespaceContext getNamespaceContext()
    {
        return mNsCtxt;
    }

    public String getNamespaceURI(String prefix)    {
        return (mNsCtxt == null) ? null : mNsCtxt.getNamespaceURI(prefix);
    }

    /*
    /////////////////////////////////////////////
    // StartElement2 implementation
    /////////////////////////////////////////////
     */

    public boolean isEmptyElement() {
        return mWasEmpty;
    }

    /*
    /////////////////////////////////////////////////////
    // Implementation of abstract base methods, overrides
    /////////////////////////////////////////////////////
     */

    public StartElement asStartElement() { // overriden to save a cast
        return this;
    }

    public int getEventType() {
        return START_ELEMENT;
    }

    public boolean isStartElement() {
        return true;
    }

    public void writeAsEncodedUnicode(Writer w)
        throws XMLStreamException
    {
        try {
            w.write('<');
            String prefix = mName.getPrefix();
            if (prefix != null && prefix.length() > 0) {
                w.write(prefix);
                w.write(':');
            }
            w.write(mName.getLocalPart());

            // Base class can output namespaces and attributes:
            outputNsAndAttr(w);

            w.write('>');
        } catch (IOException ie) {
            throw new XMLStreamException(ie);
        }
    }

    protected abstract void outputNsAndAttr(Writer w) throws IOException;
}
