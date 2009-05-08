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

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.*;

import org.codehaus.stax2.evt.XMLEvent2;

import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.compat.JdkFeatures;
import com.ctc.wstx.exc.WstxException;
import com.ctc.wstx.exc.WstxIOException;

public abstract class WEvent
    implements XMLEvent2
{
    /**
     * Location where token started; exact definition may depends
     * on event type.
     */
    protected final Location mLocation;

    protected WEvent(Location loc) {
        mLocation = loc;
    }

    /*
    //////////////////////////////////////////////
    // Skeleton XMLEvent API
    //////////////////////////////////////////////
     */

    public Characters asCharacters() {
        return (Characters) this;
    }

    public EndElement asEndElement() {
        return (EndElement) this;
    }

    public StartElement asStartElement() {
        return (StartElement) this;
    }

    public abstract int getEventType();

    public Location getLocation() {
        return mLocation;
    }

    public QName getSchemaType() {
        return null;
    }

    public boolean isAttribute()
    {
        return false;
    }

    public boolean isCharacters()
    {
        return false;
    }

    public boolean isEndDocument()
    {
        return false;
    }

    public boolean isEndElement()
    {
        return false;
    }

    public boolean isEntityReference()
    {
        return false;
    }

    public boolean isNamespace()
    {
        return false;
    }

    public boolean isProcessingInstruction()
    {
        return false;
    }

    public boolean isStartDocument()
    {
        return false;
    }

    public boolean isStartElement()
    {
        return false;
    }

    public abstract void writeAsEncodedUnicode(Writer w)
        throws XMLStreamException;

    /*
    //////////////////////////////////////////////
    // XMLEvent2 (StAX2)
    //////////////////////////////////////////////
     */

    public abstract void writeUsing(XMLStreamWriter w) throws XMLStreamException;

    /*
    ///////////////////////////////////////////
    // Overridden standard methods
    ///////////////////////////////////////////
     */

    public String toString() {
        return "["+ErrorConsts.tokenTypeDesc(getEventType())+"]";
    }

    /*
    //////////////////////////////////////////////
    // Helper methods
    //////////////////////////////////////////////
     */

    protected void throwFromIOE(IOException ioe)
        throws WstxException
    {
        throw new WstxIOException(ioe);
    }
}
