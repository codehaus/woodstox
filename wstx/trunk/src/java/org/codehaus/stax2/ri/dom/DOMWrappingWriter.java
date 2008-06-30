/* Stax2 API extension for Streaming Api for Xml processing (StAX).
 *
 * Copyright (c) 2006- Tatu Saloranta, tatu.saloranta@iki.fi
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

package org.codehaus.stax2.ri.dom;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.MessageFormat;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.*;

import org.w3c.dom.*;

import org.codehaus.stax2.XMLStreamLocation2;
import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.XMLStreamWriter2;
import org.codehaus.stax2.ri.typed.SimpleValueEncoder;
import org.codehaus.stax2.validation.*;

/**
 * This is an adapter class that implements {@link XMLStreamWriter}
 * as a facade on top of  a DOM document or Node, allowing one
 * to basically construct DOM trees via Stax API.
 *<p>
 * Note that the implementation is only to be used for use with
 * <code>javax.xml.transform.dom.DOMSResult</code>. It can however be
 * used for both full documents, and single element root fragments,
 * depending on what node is passed as the argument.
 *
 * @since 3.0
 */
public abstract class DOMWrappingWriter
    implements XMLStreamWriter2
{
    /*
    ////////////////////////////////////////////////////
    // Configuration
    ////////////////////////////////////////////////////
     */

    protected final boolean mNsAware;

    protected final boolean mNsRepairing;

    /**
     * This member variable is to keep information about encoding
     * that seems to be used for the document (or fragment) to output,
     * if known.
     */
    protected String mEncoding = null;

    /**
     * If we are being given info about existing bindings, it'll come
     * as a NamespaceContext.
     */
    protected NamespaceContext mNsContext;

    /*
    ////////////////////////////////////////////////////
    // State
    ////////////////////////////////////////////////////
     */

    /**
     * We need a reference to the document hosting nodes to
     * be able to create new nodes
     */
    protected final Document mDocument;

    /*
    ////////////////////////////////////////////////////
    // Helper objects
    ////////////////////////////////////////////////////
     */

    /**
     * Encoding of typed values is used the standard encoder
     * included in RI.
     */
    protected SimpleValueEncoder mValueEncoder;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////////////////
     */
    
    protected DOMWrappingWriter(Node treeRoot,
                                boolean nsAware, boolean nsRepairing)
        throws XMLStreamException
    {
        if (treeRoot == null) {
            throw new IllegalArgumentException("Can not pass null Node for constructing a DOM-based XMLStreamWriter");
        }
        mNsAware = nsAware;
        mNsRepairing = nsRepairing;

        /* Ok; we need a document node; or an element node; or a document
         * fragment node.
         */
        switch (treeRoot.getNodeType()) {
        case Node.DOCUMENT_NODE: // fine
            mDocument = (Document) treeRoot;

            /* Should try to find encoding, version and stand-alone
             * settings... but is there a standard way of doing that?
             */
            break;

        case Node.ELEMENT_NODE: // can make sub-tree... ok
            mDocument = treeRoot.getOwnerDocument();
            break;

        case Node.DOCUMENT_FRAGMENT_NODE: // as with element...
            mDocument = treeRoot.getOwnerDocument();
            // Above types are fine
            break;

        default: // other Nodes not usable
            throw new XMLStreamException("Can not create an XMLStreamWriter for a DOM node of type "+treeRoot.getClass());
        }
        if (mDocument == null) {
            throw new XMLStreamException("Can not create an XMLStreamWriter for given node (of type "+treeRoot.getClass()+"): did not have owner document");
        }
    }

    /*
    ////////////////////////////////////////////////////
    // Partial XMLStreamWriter API (Stax 1.0) impl
    ////////////////////////////////////////////////////
     */

    public void close() {
        // NOP
    }

    public void flush() {
        // NOP
    }

    public void setNamespaceContext(NamespaceContext context) {
        mNsContext = context;
    }

    /*
    ////////////////////////////////////////////////////
    // XMLStreamWriter2 API (Stax2 v2.0)
    ////////////////////////////////////////////////////
     */

    public XMLValidator validateAgainst(XMLValidationSchema schema)
        throws XMLStreamException
    {
        // !!! TBI
        return null;
    }

    public XMLValidator stopValidatingAgainst(XMLValidationSchema schema)
        throws XMLStreamException
    {
        // !!! TBI
        return null;
    }

    public XMLValidator stopValidatingAgainst(XMLValidator validator)
        throws XMLStreamException
    {
        // !!! TBI
        return null;
    }

    public ValidationProblemHandler setValidationProblemHandler(ValidationProblemHandler h)
    {
        // !!! TBI
        return null;
    }

    public XMLStreamLocation2 getLocation() {
        // !!! TBI
        return null;
    }

    public String getEncoding() {
        return mEncoding;
    }

    public void writeCData(char[] text, int start, int len)
        throws XMLStreamException
    {
        writeCData(new String(text, start, len));
    }

    //public void writeDTD(String rootName, String systemId, String publicId, String internalSubset)

    public void writeFullEndElement() throws XMLStreamException
    {
        // No difference with DOM
        writeEndElement();
    }

    public void writeStartDocument(String version, String encoding, boolean standAlone)
        throws XMLStreamException
    {
        writeStartDocument(encoding, version);
    }

    /*
    ///////////////////////////////
    // Stax2, pass-through methods
    ///////////////////////////////
     */

    public void writeRaw(String text)
        throws XMLStreamException
    {
        reportUnsupported("writeRaw()");
    }

    public void writeRaw(String text, int start, int offset)
        throws XMLStreamException
    {
        reportUnsupported("writeRaw()");
    }

    public void writeRaw(char[] text, int offset, int length)
        throws XMLStreamException
    {
        reportUnsupported("writeRaw()");
    }

    public void copyEventFromReader(XMLStreamReader2 r, boolean preserveEventData)
        throws XMLStreamException
    {
        // !!! TBI
    }


    /*
    /////////////////////////////////////////////////
    // TypedXMLStreamWriter2 implementation
    // (Typed Access API, Stax v3.0)
    /////////////////////////////////////////////////
     */

    // // // Typed element content write methods

    public void writeBoolean(boolean value) throws XMLStreamException
    {
        writeCharacters(value ? "true" : "false");
    }

    public void writeInt(int value) throws XMLStreamException
    {
        writeCharacters(String.valueOf(value));
    }

    public void writeLong(long value) throws XMLStreamException
    {
        writeCharacters(String.valueOf(value));
    }

    public void writeFloat(float value) throws XMLStreamException
    {
        writeCharacters(String.valueOf(value));
    }

    public void writeDouble(double value) throws XMLStreamException
    {
        writeCharacters(String.valueOf(value));
    }

    public void writeInteger(BigInteger value) throws XMLStreamException
    {
        writeCharacters(value.toString());
    }

    public void writeDecimal(BigDecimal value) throws XMLStreamException
    {
        writeCharacters(value.toString());
    }

    public void writeQName(QName name) throws XMLStreamException
    {
        String value = name.getLocalPart();
        String prefix = name.getPrefix();
        if (prefix != null && prefix.length() > 0) {
            value = prefix+":"+value;
        }
        writeCharacters(value);
    }

    public void writeIntArray(int[] value, int from, int length)
        throws XMLStreamException
    {
        /* true -> start with space, to allow for multiple consecutive
         * to be written
         */
        writeCharacters(getValueEncoder().encodeAsString(value, from, length));
    }

    public void writeLongArray(long[] value, int from, int length)
        throws XMLStreamException
    {
        // true -> start with space, for multiple segments
        writeCharacters(getValueEncoder().encodeAsString(value, from, length));
    }

    public void writeFloatArray(float[] value, int from, int length)
        throws XMLStreamException
    {
        // true -> start with space, for multiple segments
        writeCharacters(getValueEncoder().encodeAsString(value, from, length));
    }

    public void writeDoubleArray(double[] value, int from, int length)
        throws XMLStreamException
    {
        // true -> start with space, for multiple segments
        writeCharacters(getValueEncoder().encodeAsString(value, from, length));
    }

    public void writeBinary(byte[] value, int from, int length)
        throws XMLStreamException
    {
        writeCharacters(getValueEncoder().encodeAsString(value, from, length));
    }

    // // // Typed attribute value write methods

    public void writeBooleanAttribute(String prefix, String nsURI, String localName, boolean value)
        throws XMLStreamException
    {
        writeAttribute(prefix, nsURI, localName, value ? "true" : "false");
    }

    public void writeIntAttribute(String prefix, String nsURI, String localName, int value)
        throws XMLStreamException
    {
        writeAttribute(prefix, nsURI, localName, String.valueOf(value));
    }

    public void writeLongAttribute(String prefix, String nsURI, String localName, long value)
        throws XMLStreamException
    {
        writeAttribute(prefix, nsURI, localName, String.valueOf(value));
    }

    public void writeFloatAttribute(String prefix, String nsURI, String localName, float value)
        throws XMLStreamException
    {
        writeAttribute(prefix, nsURI, localName, String.valueOf(value));
    }

    public void writeDoubleAttribute(String prefix, String nsURI, String localName, double value)
        throws XMLStreamException
    {
        writeAttribute(prefix, nsURI, localName, String.valueOf(value));
    }

    public void writeIntegerAttribute(String prefix, String nsURI, String localName, BigInteger value)
        throws XMLStreamException
    {
        writeAttribute(prefix, nsURI, localName, value.toString());
    }

    public void writeDecimalAttribute(String prefix, String nsURI, String localName, BigDecimal value)
        throws XMLStreamException
    {
        writeAttribute(prefix, nsURI, localName, value.toString());
    }

    public void writeQNameAttribute(String prefix, String nsURI, String localName, QName name)
        throws XMLStreamException
    {
        String value = name.getLocalPart();
        String vp = name.getPrefix();
        if (vp != null && vp.length() > 0) {
            value = vp+":"+value;
        }
        writeAttribute(prefix, nsURI, localName, value);
    }

    public void writeIntArrayAttribute(String prefix, String nsURI, String localName, int[] value)
        throws XMLStreamException
    {
        writeAttribute(prefix, nsURI, localName,
                       getValueEncoder().encodeAsString(value, 0, value.length));
    }

    public void writeLongArrayAttribute(String prefix, String nsURI, String localName, long[] value) throws XMLStreamException
    {
        writeAttribute(prefix, nsURI, localName,
                       getValueEncoder().encodeAsString(value, 0, value.length));
    }
    
    public void writeFloatArrayAttribute(String prefix, String nsURI, String localName, float[] value) throws XMLStreamException
    {
        writeAttribute(prefix, nsURI, localName,
                       getValueEncoder().encodeAsString(value, 0, value.length));
    }
    
    public void writeDoubleArrayAttribute(String prefix, String nsURI, String localName, double[] value) throws XMLStreamException
    {
        writeAttribute(prefix, nsURI, localName,
                       getValueEncoder().encodeAsString(value, 0, value.length));
    }

    public void writeBinaryAttribute(String prefix, String nsURI, String localName, byte[] value) throws XMLStreamException
    {
        writeAttribute(prefix, nsURI, localName,
                       getValueEncoder().encodeAsString(value, 0, value.length));
    }


    /*
    ////////////////////////////////////////////////////
    // Abstract methods for sub-classes to implement
    ////////////////////////////////////////////////////
     */

    protected abstract void appendLeaf(Node n)
        throws IllegalStateException;

    /*
    ////////////////////////////////////////////////////
    // Shared package methods
    ////////////////////////////////////////////////////
     */

    protected SimpleValueEncoder getValueEncoder()
    {
        if (mValueEncoder == null) {
            mValueEncoder = new SimpleValueEncoder();
        }
        return mValueEncoder;
    }

    /*
    ////////////////////////////////////////////////////
    // Package methods, basic output problem reporting
    ////////////////////////////////////////////////////
     */

    protected static void throwOutputError(String msg)
        throws XMLStreamException
    {
        throw new XMLStreamException(msg);
    }

    protected static void throwOutputError(String format, Object arg)
        throws XMLStreamException
    {
        String msg = MessageFormat.format(format, new Object[] { arg });
        throwOutputError(msg);
    }

    protected void reportUnsupported(String operName)
    {
        throw new UnsupportedOperationException(operName+" can not be used with DOM-backed writer");
    }
}


