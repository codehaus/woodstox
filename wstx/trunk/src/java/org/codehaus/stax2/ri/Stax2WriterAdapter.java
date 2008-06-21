/* Stax2 API extension for Streaming Api for Xml processing (StAX).
 *
 * Copyright (c) 2006- Tatu Saloranta, tatu.saloranta@iki.fi
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
package org.codehaus.stax2.ri;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.*;

import org.codehaus.stax2.*;
import org.codehaus.stax2.ri.typed.DefaultValueEncoder;
// Not from Stax 1.0, but Stax2 does provide it:
import org.codehaus.stax2.util.StreamWriterDelegate;
import org.codehaus.stax2.validation.ValidationProblemHandler;
import org.codehaus.stax2.validation.XMLValidationSchema;
import org.codehaus.stax2.validation.XMLValidator;

/**
 * This adapter implements parts of {@link XMLStreamWriter2}, the
 * extended stream writer defined by Stax2 extension, by wrapping
 * a vanilla Stax 1.0 {@link XMLStreamReader} implementation.
 *<p>
 * Note: the implementation is incomplete as-is, since not all
 * features needed are accessible via basic Stax 1.0 interface.
 * As such, two main use cases for this wrapper are:
 *<ul>
 * <li>Serve as convenient base class for a complete implementation,
 *    which can use native accessors provided by the wrapped Stax
 *    implementation
 *  </li>
 * <li>To be used for tasks that make limited use of Stax2 API, such
 *   that missing parts are not needed
 *  </li>
 * </ul>
 */
public class Stax2WriterAdapter
    extends StreamWriterDelegate
    implements XMLStreamWriter2 /* From Stax2 */
               ,XMLStreamConstants
{
    /**
     * Encoding we have determined to be used, according to method
     * calls (write start document etc.)
     */
    protected String mEncoding;

    protected DefaultValueEncoder mValueEncoder;

    protected final boolean mNsRepairing;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle methods
    ////////////////////////////////////////////////////
     */

    protected Stax2WriterAdapter(XMLStreamWriter sw)
    {
        super(sw);
        mDelegate = sw;
        Object value = sw.getProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES);
        mNsRepairing = (value instanceof Boolean) && ((Boolean) value).booleanValue();
    }

    /**
     * Method that should be used to add dynamic support for
     * {@link XMLStreamWriter2}. Method will check whether the
     * stream reader passed happens to be a {@link XMLStreamWriter2};
     * and if it is, return it properly cast. If not, it will create
     * necessary wrapper to support features needed by StaxMate,
     * using vanilla Stax 1.0 interface.
     */
    public static XMLStreamWriter2 wrapIfNecessary(XMLStreamWriter sw)
    {
        if (sw instanceof XMLStreamWriter2) {
            return (XMLStreamWriter2) sw;
        }
        return new Stax2WriterAdapter(sw);
    }

    /*
    /////////////////////////////////////////////////
    // TypedXMLStreamWriter2 implementation
    // (Typed Access API, Stax v3.0)
    /////////////////////////////////////////////////
     */

    // // // Typed element content write methods

    public void writeBoolean(boolean b) throws XMLStreamException
    {
        mDelegate.writeCharacters(b ? "true" : "false");
    }

    public void writeInt(int value) throws XMLStreamException
    {
        mDelegate.writeCharacters(String.valueOf(value));
    }

    public void writeLong(long value) throws XMLStreamException
    {
        mDelegate.writeCharacters(String.valueOf(value));
    }

    public void writeFloat(float value) throws XMLStreamException
    {
        mDelegate.writeCharacters(String.valueOf(value));
    }

    public void writeDouble(double value) throws XMLStreamException
    {
        mDelegate.writeCharacters(String.valueOf(value));
    }

    public void writeInteger(BigInteger value) throws XMLStreamException
    {
        mDelegate.writeCharacters(value.toString());
    }

    public void writeDecimal(BigDecimal value) throws XMLStreamException
    {
        mDelegate.writeCharacters(value.toString());
    }

    public void writeQName(QName name) throws XMLStreamException
    {
        String value = name.getLocalPart();
        String prefix = name.getPrefix();
        if (prefix != null && prefix.length() > 0) {
            value = prefix+":"+value;
        }
        mDelegate.writeCharacters(value);
    }

    public void writeIntArray(int[] value, int from, int length)
        throws XMLStreamException
    {
        // true -> start with space, for multiple segments
        mDelegate.writeCharacters(getValueEncoder().encodeAsString(true, value, from, length));
    }

    public void writeLongArray(long[] value, int from, int length)
        throws XMLStreamException
    {
        // true -> start with space, for multiple segments
        mDelegate.writeCharacters(getValueEncoder().encodeAsString(true, value, from, length));
    }

    public void writeFloatArray(float[] value, int from, int length)
        throws XMLStreamException
    {
        // true -> start with space, for multiple segments
        mDelegate.writeCharacters(getValueEncoder().encodeAsString(true, value, from, length));
    }

    public void writeDoubleArray(double[] value, int from, int length)
        throws XMLStreamException
    {
        // true -> start with space, for multiple segments
        mDelegate.writeCharacters(getValueEncoder().encodeAsString(true, value, from, length));
    }

    // // // Typed attribute value write methods

    public void writeBooleanAttribute(String prefix, String nsURI, String localName, boolean b) throws XMLStreamException
    {
        mDelegate.writeAttribute(prefix, nsURI, localName, b ? "true" : "false");
    }

    public void writeIntAttribute(String prefix, String nsURI, String localName, int value) throws XMLStreamException
    {
        mDelegate.writeAttribute(prefix, nsURI, localName, String.valueOf(value));
    }

    public void writeLongAttribute(String prefix, String nsURI, String localName, long value) throws XMLStreamException
    {
        mDelegate.writeAttribute(prefix, nsURI, localName, String.valueOf(value));
    }

    public void writeFloatAttribute(String prefix, String nsURI, String localName, float value) throws XMLStreamException
    {
        mDelegate.writeAttribute(prefix, nsURI, localName, String.valueOf(value));
    }

    public void writeDoubleAttribute(String prefix, String nsURI, String localName, double value) throws XMLStreamException
    {
        mDelegate.writeAttribute(prefix, nsURI, localName, String.valueOf(value));
    }

    public void writeIntegerAttribute(String prefix, String nsURI, String localName, BigInteger value) throws XMLStreamException
    {
        mDelegate.writeAttribute(prefix, nsURI, localName, value.toString());
    }

    public void writeDecimalAttribute(String prefix, String nsURI, String localName, BigDecimal value) throws XMLStreamException
    {
        mDelegate.writeAttribute(prefix, nsURI, localName, value.toString());
    }

    public void writeQNameAttribute(String prefix, String nsURI, String localName, QName name) throws XMLStreamException
    {
        String value = name.getLocalPart();
        String vp = name.getPrefix();
        if (vp != null && vp.length() > 0) {
            value = vp+":"+value;
        }
        mDelegate.writeAttribute(prefix, nsURI, localName, value);
    }

    public void writeIntArrayAttribute(String prefix, String nsURI, String localName, int[] value) throws XMLStreamException
    {
        // false -> no need to start with a space
        mDelegate.writeAttribute(prefix, nsURI, localName,
                                 getValueEncoder().encodeAsString(false, value, 0, value.length));
    }

    public void writeLongArrayAttribute(String prefix, String nsURI, String localName, long[] value) throws XMLStreamException
    {
        // false -> no need to start with a space
        mDelegate.writeAttribute(prefix, nsURI, localName,
                                 getValueEncoder().encodeAsString(false, value, 0, value.length));
    }

    public void writeFloatArrayAttribute(String prefix, String nsURI, String localName, float[] value) throws XMLStreamException
    {
        // false -> no need to start with a space
        mDelegate.writeAttribute(prefix, nsURI, localName,
                                 getValueEncoder().encodeAsString(false, value, 0, value.length));
    }

    public void writeDoubleArrayAttribute(String prefix, String nsURI, String localName, double[] value) throws XMLStreamException
    {
        // false -> no need to start with a space
        mDelegate.writeAttribute(prefix, nsURI, localName,
                                 getValueEncoder().encodeAsString(false, value, 0, value.length));
    }

     /*
    ////////////////////////////////////////////////////
    // XMLStreamWriter2 (StAX2) implementation
    ////////////////////////////////////////////////////
     */

    public boolean isPropertySupported(String name)
    {
        /* No real clean way to check this, so let's just fake by
         * claiming nothing is supported
         */
        return false;
    }

    public boolean setProperty(String name, Object value)
    {
        throw new IllegalArgumentException("No settable property '"+name+"'");
    }

    public XMLStreamLocation2 getLocation()
    {
        // No easy way to keep track of it, without impl support
        return null;
    }

    public String getEncoding()
    {
        // We may have been able to infer it... if so:
        return mEncoding;
    }

    public void writeCData(char[] text, int start, int len)
        throws XMLStreamException
    {
        writeCData(new String(text, start, len));
    }

    public void writeDTD(String rootName, String systemId, String publicId,
                         String internalSubset)
        throws XMLStreamException
    {
        /* This may or may not work... depending on how well underlying
         * implementation follows stax 1.0 spec (it should work)
         */
        StringBuffer sb = new StringBuffer();
        sb.append("<!DOCTYPE");
        sb.append(rootName);
        if (systemId != null) {
            if (publicId != null) {
                sb.append(" PUBLIC \"");
                sb.append(publicId);
                sb.append("\" \"");
            } else {
                sb.append(" SYSTEM \"");
            }
            sb.append(systemId);
            sb.append('"');
        }
        // Hmmh. Should we output empty internal subset?
        if (internalSubset != null && internalSubset.length() > 0) {
            sb.append(" [");
            sb.append(internalSubset);
            sb.append(']');
        }
        sb.append('>');
        writeDTD(sb.toString());
    }

    public void writeFullEndElement()
        throws XMLStreamException
    {
        /* It may be possible to fake it, by pretending to write
         * character output, which in turn should prevent writing of
         * an empty element...
         */
        mDelegate.writeCharacters("");
        mDelegate.writeEndElement();
    }

    public void writeSpace(String text)
        throws XMLStreamException
    {
        /* Hmmh. Two choices: either try to write as regular characters,
         * or output as is via raw calls. Latter would be safer, if we
         * had access to it; former may escape incorrectly.
         * While this may not be optimal, let's try former
         */
        writeRaw(text);
    }

    public void writeSpace(char[] text, int offset, int length)
        throws XMLStreamException
    {
        // See comments above...
        writeRaw(text, offset, length);
    }

    public void writeStartDocument(String version, String encoding,
                                   boolean standAlone)
        throws XMLStreamException
    {
        // No good way to do it, so let's do what we can...
        writeStartDocument(encoding, version);
    }
    
    /*
    ///////////////////////////////
    // Stax2, Pass-through methods
    ///////////////////////////////
    */

    public void writeRaw(String text)
        throws XMLStreamException
    {
        writeRaw(text, 0, text.length());
    }

    public void writeRaw(String text, int offset, int len)
        throws XMLStreamException
    {
        // There is no clean way to implement this via Stax 1.0, alas...
        throw new UnsupportedOperationException("Not implemented");
    }

    public void writeRaw(char[] text, int offset, int length)
        throws XMLStreamException
    {
        writeRaw(new String(text, offset, length));
    }

    public void copyEventFromReader(XMLStreamReader2 sr, boolean preserveEventData)
        throws XMLStreamException
    {
        switch (sr.getEventType()) {
        case START_DOCUMENT:
            {
                String version = sr.getVersion();
                /* No real declaration? If so, we don't want to output
                 * anything, to replicate as closely as possible the
                 * source document
                 */
                if (version == null || version.length() == 0) {
                    ; // no output if no real input
                } else {
                    if (sr.standaloneSet()) {
                        writeStartDocument(sr.getVersion(),
                                           sr.getCharacterEncodingScheme(),
                                           sr.isStandalone());
                    } else {
                        writeStartDocument(sr.getCharacterEncodingScheme(),
                                           sr.getVersion());
                    }
                }
            }
            return;
            
        case END_DOCUMENT:
            writeEndDocument();
            return;
            
            // Element start/end events:
        case START_ELEMENT:
            /* Start element is bit trickier to output since there
             * may be differences between repairing/non-repairing
             * writers. But let's try a generic handling here.
             */
            copyStartElement(sr);
            return;
            
        case END_ELEMENT:
            writeEndElement();
            return;
            
        case SPACE:
            writeSpace(sr.getTextCharacters(), sr.getTextStart(), sr.getTextLength());
            return;
            
        case CDATA:
            writeCData(sr.getTextCharacters(), sr.getTextStart(), sr.getTextLength());
            return;
            
        case CHARACTERS:
            writeCharacters(sr.getTextCharacters(), sr.getTextStart(), sr.getTextLength());
            return;
            
        case COMMENT:
            writeComment(sr.getText());
            return;
            
        case PROCESSING_INSTRUCTION:
            writeProcessingInstruction(sr.getPITarget(), sr.getPIData());
            return;
            
        case DTD:
            {
                DTDInfo info = sr.getDTDInfo();
                if (info == null) {
                    /* Hmmmh. Can this happen for non-DTD-aware readers?
                     * And if so, what should we do?
                     */
                    throw new XMLStreamException("Current state DOCTYPE, but not DTDInfo Object returned -- reader doesn't support DTDs?");
                }
                writeDTD(info.getDTDRootName(), info.getDTDSystemId(),
                         info.getDTDPublicId(), info.getDTDInternalSubset());
            }
            return;
            
        case ENTITY_REFERENCE:
            writeEntityRef(sr.getLocalName());
            return;
            
        case ATTRIBUTE:
        case NAMESPACE:
        case ENTITY_DECLARATION:
        case NOTATION_DECLARATION:
            // Let's just fall back to throw the exception
        }
        throw new XMLStreamException("Unrecognized event type ("
                                     +sr.getEventType()+"); not sure how to copy");
    }

    /*
    ///////////////////////////////
    // Stax2, validation
    ///////////////////////////////
    */

    public XMLValidator validateAgainst(XMLValidationSchema schema)
        throws XMLStreamException
    {
        // !!! TODO: try to implement?
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public XMLValidator stopValidatingAgainst(XMLValidationSchema schema)
        throws XMLStreamException
    {
        return null;
    }

    public XMLValidator stopValidatingAgainst(XMLValidator validator)
        throws XMLStreamException
    {
        return null;
    }

    public ValidationProblemHandler setValidationProblemHandler(ValidationProblemHandler h)
    {
        /* Not a real problem: although we can't do anything with it
         * (without real validator integration)
         */
        return null;
    }

    /*
    ///////////////////////////////
    // Helper methods
    ///////////////////////////////
    */

    protected void copyStartElement(XMLStreamReader sr)
        throws XMLStreamException
    {
        // Any namespace declarations/bindings?
        int nsCount = sr.getNamespaceCount();
        if (nsCount > 0) { // yup, got some...
            /* First, need to (or at least, should?) add prefix bindings:
             * (may not be 100% required, but probably a good thing to do,
             * just so that app code has access to prefixes then)
             */
            for (int i = 0; i < nsCount; ++i) {
                String prefix = sr.getNamespacePrefix(i);
                String uri = sr.getNamespaceURI(i);
                if (prefix == null || prefix.length() == 0) { // default NS
                    setDefaultNamespace(uri);
                } else {
                    setPrefix(prefix, uri);
                }
            }
        }
        writeStartElement(sr.getPrefix(), sr.getLocalName(), sr.getNamespaceURI());
        
        if (nsCount > 0) {
            // And then output actual namespace declarations:
            for (int i = 0; i < nsCount; ++i) {
                String prefix = sr.getNamespacePrefix(i);
                String uri = sr.getNamespaceURI(i);
                
                if (prefix == null || prefix.length() == 0) { // default NS
                    writeDefaultNamespace(uri);
                } else {
                    writeNamespace(prefix, uri);
                }
            }
        }
        
        /* And then let's just output attributes. But should we copy the
         * implicit attributes (created via attribute defaulting?)
         */
        int attrCount = sr.getAttributeCount();
        if (attrCount > 0) {
            for (int i = 0; i < attrCount; ++i) {
                writeAttribute(sr.getAttributePrefix(i),
                               sr.getAttributeNamespace(i),
                               sr.getAttributeLocalName(i),
                               sr.getAttributeValue(i));
            }
        }
    }

    protected DefaultValueEncoder getValueEncoder()
    {
        if (mValueEncoder == null) {
            mValueEncoder = new DefaultValueEncoder();
        }
        return mValueEncoder;
    }
}
