/* StAX2 extension for StAX API (JSR-173).
 *
 * Copyright (c) 2005- Tatu Saloranta, tatu.saloranta@iki.fi
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

package org.codehaus.stax2.util;

import java.io.IOException;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Similar to {@link javax.xml.stream.util.StreamReaderDelegate},
 * but implements a proxy for {@link XMLStreamWriter}.
 * The only additional methods are ones for setting and accessing
 * the delegate to forward requests to.
 *<p>
 * Note: such class really should exist in core Stax API
 * (in package <code>javax.xml.stream.util</code>), but since
 * it does not, it is implemented within Stax2 extension API
 *
 * @since 3.0
 */
public class StreamWriterDelegate
	implements XMLStreamWriter
{
    protected XMLStreamWriter mParent;

    /*
    //////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////
     */

    public StreamWriterDelegate(XMLStreamWriter parentWriter)
    {
        mParent = parentWriter;
    }

    public void setParent(XMLStreamWriter parentWriter)
    {
        mParent = parentWriter;
    }

    public XMLStreamWriter getParent()
    {
        return mParent;
    }

    /*
    //////////////////////////////////////////////
    // XMLStreamWriter implementation
    //////////////////////////////////////////////
     */

    public void close() throws XMLStreamException {
        mParent.close();
    }

    public void flush() throws XMLStreamException {
        mParent.flush();

    }

    public NamespaceContext getNamespaceContext() {
        return mParent.getNamespaceContext();
    }

    public String getPrefix(String ns) throws XMLStreamException {
        return mParent.getPrefix(ns);
    }

    public Object getProperty(String pname) throws IllegalArgumentException {
        return mParent.getProperty(pname);
    }

    public void setDefaultNamespace(String ns) throws XMLStreamException {
        mParent.setDefaultNamespace(ns);

    }

    public void setNamespaceContext(NamespaceContext nc)
        throws XMLStreamException
    {
        mParent.setNamespaceContext(nc);

    }

    public void setPrefix(String prefix, String uri) throws XMLStreamException {
        mParent.setPrefix(prefix, uri);

    }

    public void writeAttribute(String arg0, String arg1) throws XMLStreamException {
        mParent.writeAttribute(arg0, arg1);

    }

    public void writeAttribute(String arg0, String arg1, String arg2) throws XMLStreamException {
        mParent.writeAttribute(arg0, arg1, arg2);
    }

    public void writeAttribute(String arg0, String arg1, String arg2, String arg3) throws XMLStreamException {
        mParent.writeAttribute(arg0, arg1, arg2, arg3);
    }

    public void writeCData(String arg0) throws XMLStreamException {
        mParent.writeCData(arg0);

    }

    public void writeCharacters(String arg0) throws XMLStreamException {
        mParent.writeCharacters(arg0);

    }

    public void writeCharacters(char[] arg0, int arg1, int arg2)
        throws XMLStreamException {
        mParent.writeCharacters(arg0, arg1, arg2);

    }

    public void writeComment(String arg0) throws XMLStreamException {
        mParent.writeComment(arg0);

    }

    public void writeDTD(String arg0) throws XMLStreamException {
        mParent.writeDTD(arg0);

    }

    public void writeDefaultNamespace(String arg0) throws XMLStreamException {
        mParent.writeDefaultNamespace(arg0);

    }

    public void writeEmptyElement(String arg0) throws XMLStreamException {
        mParent.writeEmptyElement(arg0);

    }

    public void writeEmptyElement(String arg0, String arg1) throws XMLStreamException {
        mParent.writeEmptyElement(arg0, arg1);

    }

    public void writeEmptyElement(String arg0, String arg1, String arg2)
        throws XMLStreamException {
        mParent.writeEmptyElement(arg0, arg1, arg2);

    }

    public void writeEndDocument() throws XMLStreamException {
        mParent.writeEndDocument();

    }

    public void writeEndElement() throws XMLStreamException {
        mParent.writeEndElement();

    }

    public void writeEntityRef(String arg0) throws XMLStreamException {
        mParent.writeEntityRef(arg0);

    }

    public void writeNamespace(String arg0, String arg1)
        throws XMLStreamException {
        mParent.writeNamespace(arg0, arg1);

    }

    public void writeProcessingInstruction(String arg0)
        throws XMLStreamException {
        mParent.writeProcessingInstruction(arg0);

    }

    public void writeProcessingInstruction(String arg0, String arg1)
        throws XMLStreamException {
        mParent.writeProcessingInstruction(arg0, arg1);

    }

    public void writeStartDocument() throws XMLStreamException {
        mParent.writeStartDocument();

    }

    public void writeStartDocument(String arg0) throws XMLStreamException {
        mParent.writeStartDocument(arg0);

    }

    public void writeStartDocument(String arg0, String arg1)
        throws XMLStreamException {
        mParent.writeStartDocument(arg0, arg1);

    }

    public void writeStartElement(String arg0) throws XMLStreamException {
        mParent.writeStartElement(arg0);

    }

    public void writeStartElement(String arg0, String arg1)
        throws XMLStreamException {
        mParent.writeStartElement(arg0, arg1);
    }

    public void writeStartElement(String arg0, String arg1, String arg2)
        throws XMLStreamException {
        mParent.writeStartElement(arg0, arg1, arg2);
    }
}

