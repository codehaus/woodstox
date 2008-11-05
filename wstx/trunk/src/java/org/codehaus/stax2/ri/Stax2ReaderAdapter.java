package org.codehaus.stax2.ri;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.util.StreamReaderDelegate;

import org.codehaus.stax2.typed.TypedArrayDecoder;
import org.codehaus.stax2.typed.TypedValueDecoder;
import org.codehaus.stax2.typed.TypedXMLStreamException;
import org.codehaus.stax2.ri.typed.StringBase64Decoder;
import org.codehaus.stax2.ri.typed.ValueDecoderFactory;

import org.codehaus.stax2.*;
import org.codehaus.stax2.validation.*;

/**
 * This adapter implements parts of {@link XMLStreamReader2}, the
 * extended stream reader defined by Stax2 extension, by wrapping
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
public class Stax2ReaderAdapter
    extends StreamReaderDelegate /* from Stax 1.0 */
    implements XMLStreamReader2 /* From Stax2 */
               ,AttributeInfo
               ,DTDInfo
               ,LocationInfo
{
    final static int INT_SPACE = 0x0020;

    final private static int MASK_GET_ELEMENT_TEXT = 
        (1 << CHARACTERS) | (1 << CDATA) | (1 << SPACE)
        | (1 << ENTITY_REFERENCE);

    final protected static int MASK_TYPED_ACCESS_BINARY =
        (1 << START_ELEMENT) //  note: END_ELEMENT handled separately
        | (1 << CHARACTERS) | (1 << CDATA) | (1 << SPACE)
        ;

    /**
     * Factory used for constructing decoders we need for typed access
     */
    protected ValueDecoderFactory mDecoderFactory;

    /**
     * Lazily-constructed decoder object for decoding base64 encoded
     * binary content.
     */
    protected StringBase64Decoder _base64Decoder = null;

    /**
     * Number of open (start) elements currently.
     */
    protected int mDepth = 0;

    /**
     * Content temporarily cached to be used for decoding typed content
     * that is in chunked mode (int/long/float/double arrays, base64
     * encoded binary data)
     */
    protected String mTypedContent;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle methods
    ////////////////////////////////////////////////////
     */

    protected Stax2ReaderAdapter(XMLStreamReader sr)
    {
        super(sr);
    }

    /**
     * Method that should be used to add dynamic support for
     * {@link XMLStreamReader2}. Method will check whether the
     * stream reader passed happens to be a {@link XMLStreamReader2};
     * and if it is, return it properly cast. If not, it will create
     * necessary wrapper.
     */
    public static XMLStreamReader2 wrapIfNecessary(XMLStreamReader sr)
    {
        if (sr instanceof XMLStreamReader2) {
            return (XMLStreamReader2) sr;
        }
        return new Stax2ReaderAdapter(sr);
    }

    /*
    ////////////////////////////////////////////////////
    // Stax 1.0 methods overridden
    ////////////////////////////////////////////////////
     */

    public int next()
        throws XMLStreamException
    {
        /* First special check: are we in the middle of chunked
         * decode operation? If so, we'll just end it...
         */
        if (mTypedContent != null) {
            mTypedContent = null;
            return XMLStreamConstants.END_ELEMENT;
        }

        int type = super.next();
        if (type == XMLStreamConstants.START_ELEMENT) {
            ++mDepth;
        } else if (type == XMLStreamConstants.END_ELEMENT) {
            --mDepth;
        }
        return type;
    }

    /*
    /////////////////////////////////////////////////
    // TypedXMLStreamReader, element access
    /////////////////////////////////////////////////
     */

    public boolean getElementAsBoolean() throws XMLStreamException
    {
        ValueDecoderFactory.BooleanDecoder dec = _decoderFactory().getBooleanDecoder();
        getElementAs(dec);
        return dec.getValue();
    }

    public int getElementAsInt() throws XMLStreamException
    {
        ValueDecoderFactory.IntDecoder dec = _decoderFactory().getIntDecoder();
        getElementAs(dec);
        return dec.getValue();
    }

    public long getElementAsLong() throws XMLStreamException
    {
        ValueDecoderFactory.LongDecoder dec = _decoderFactory().getLongDecoder();
        getElementAs(dec);
        return dec.getValue();
    }

    public float getElementAsFloat() throws XMLStreamException
    {
        ValueDecoderFactory.FloatDecoder dec = _decoderFactory().getFloatDecoder();
        getElementAs(dec);
        return dec.getValue();
    }

    public double getElementAsDouble() throws XMLStreamException
    {
        ValueDecoderFactory.DoubleDecoder dec = _decoderFactory().getDoubleDecoder();
        getElementAs(dec);
        return dec.getValue();
    }

    public BigInteger getElementAsInteger() throws XMLStreamException
    {
        ValueDecoderFactory.IntegerDecoder dec = _decoderFactory().getIntegerDecoder();
        getElementAs(dec);
        return dec.getValue();
    }

    public BigDecimal getElementAsDecimal() throws XMLStreamException
    {
        ValueDecoderFactory.DecimalDecoder dec = _decoderFactory().getDecimalDecoder();
        getElementAs(dec);
        return dec.getValue();
    }

    public QName getElementAsQName() throws XMLStreamException
    {
        ValueDecoderFactory.QNameDecoder dec = _decoderFactory().getQNameDecoder(getNamespaceContext());
        getElementAs(dec);
        return dec.getValue();
    }

    public byte[] getElementAsBinary() throws XMLStreamException
    {
        // note: code here is similar to Base64DecoderBase.aggregateAll(), see comments there
        Stax2Util.ByteAggregator aggr = _base64Decoder().getByteAggregator();
        byte[] buffer = aggr.startAggregation();
        while (true) {
            int offset = 0;
            int len = buffer.length;
            do {
                int readCount = readElementAsBinary(buffer, offset, len);
                if (readCount < 1) { // all done!
                    return aggr.aggregateAll(buffer, offset);
                }
                offset += readCount;
                len -= readCount;
            } while (len > 0);
            buffer = aggr.addFullBlock(buffer);
        }
    }

    public void getElementAs(TypedValueDecoder tvd) throws XMLStreamException
    {
        String value = getElementText();
        try {
            if (value.length() == 0) {
                // !!! TBI: call "handleEmptyValue" (or whatever)
                tvd.decode("");
            } else {
                tvd.decode(value);
            }
        } catch (IllegalArgumentException iae) {
            throw _constructTypeException(iae, value);
        }
    }

    public int readElementAsIntArray(int[] value, int from, int length) throws XMLStreamException
    {
        return readElementAsArray(_decoderFactory().getIntArrayDecoder(value, from, length));
    }

    public int readElementAsLongArray(long[] value, int from, int length) throws XMLStreamException
    {
        return readElementAsArray(_decoderFactory().getLongArrayDecoder(value, from, length));
    }

    public int readElementAsFloatArray(float[] value, int from, int length) throws XMLStreamException
    {
        return readElementAsArray(_decoderFactory().getFloatArrayDecoder(value, from, length));
    }

    public int readElementAsDoubleArray(double[] value, int from, int length) throws XMLStreamException
    {
        return readElementAsArray(_decoderFactory().getDoubleArrayDecoder(value, from, length));
    }

    public int readElementAsArray(TypedArrayDecoder tad) throws XMLStreamException
    {
        // Are we started?
        if (mTypedContent == null) { // nope, not yet (or not any more?)
            int type = getEventType();
            if (type == END_ELEMENT) { // already done
                return -1;
            }
            if (type != START_ELEMENT) {
                throw new IllegalStateException("First call to readElementAsArray() must be for a START_ELEMENT");
            }
            mTypedContent = getElementText();
            /* This will move current event to END_ELEMENT, too...
             * But should we mask it (and claim it's, say, CHARACTERS)
             * or expose as is? For now, let's do latter, simplest
             */
        }
        // Ok, so what do we have left?
        String input = mTypedContent;
        final int end = input.length();
        int ptr = 0;
        int count = 0;
        String value = null;

        try {
            decode_loop:
            while (ptr < end) {
                // First, any space to skip?
                while (input.charAt(ptr) <= INT_SPACE) {
                    if (++ptr >= end) {
                        break decode_loop;
                    }
                }
                // Then let's figure out non-space char (token)
                int start = ptr;
                ++ptr;
                while (ptr < end && input.charAt(ptr) > INT_SPACE) {
                    ++ptr;
                }
                ++count;
                // And there we have it
                value = input.substring(start, ptr);
                // Plus, can skip trailing space (or at end, just beyond it)
                ++ptr;
                if (tad.decodeValue(value)) {
                    break;
                }
            }
        } catch (IllegalArgumentException iae) {
            // Need to convert to a checked stream exception
            /* Hmmh. This is not an accurate location... but it's
             * about the best we can do
             */
            Location loc = getLocation();
            throw new TypedXMLStreamException(value, iae.getMessage(), loc, iae);
        } finally {
            int len = end-ptr;
            // null works well as the marker for complete processing
            mTypedContent = (len < 1) ? null : input.substring(ptr);
        }
        return (count < 1) ? -1 : count;
    }

    /*
    ////////////////////////////////////////////////////////
    // TypedXMLStreamReader2 implementation, binary data
    ////////////////////////////////////////////////////////
     */

    public int readElementAsBinary(byte[] resultBuffer, int offset, int maxLength)
        throws XMLStreamException
    {
        if (resultBuffer == null) {
            throw new IllegalArgumentException("resultBuffer is null");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("Illegal offset ("+offset+"), must be [0, "+resultBuffer.length+"[");
        }
        if (maxLength < 1 || (offset + maxLength) > resultBuffer.length) {
            if (maxLength == 0) { // special case, allowed, but won't do anything
                return 0;
            }
            throw new IllegalArgumentException("Illegal maxLength ("+maxLength+"), has to be positive number, and offset+maxLength can not exceed"+resultBuffer.length);
        }

        int type = getEventType();
        // First things first: must be acceptable start state:
        if (((1 << type) & MASK_TYPED_ACCESS_BINARY) == 0) {
            if (type == END_ELEMENT) {
                return -1;
            }
            throwNotStartElemOrTextual(type);
        }

        final StringBase64Decoder dec = _base64Decoder();

        // Are we just starting (START_ELEMENT)?
        if (type == START_ELEMENT) {
            // Just need to locate the first text segment (or reach END_ELEMENT)
            while (true) {
                type = next();
                if (type == END_ELEMENT) {
                    // Simple... no textual content
                    return -1;
                }
                if (type == COMMENT || type == PROCESSING_INSTRUCTION) {
                    continue;
                }
                if (((1 << type) & MASK_GET_ELEMENT_TEXT) == 0) {
                    throwNotStartElemOrTextual(type);
                }
                dec.init(true, getText());
                break;
            }
        }

        int totalCount = 0;

        main_loop:
        while (true) {
            // Ok, decode:
            int count;
            try {
                count = dec.decode(resultBuffer, offset, maxLength);
            } catch (IllegalArgumentException iae) {
                throw _constructTypeException(iae, "");
            }
            offset += count;
            totalCount += count;
            maxLength -= count;

            // And if we filled the buffer we are done
            if (maxLength < 1) {
                break;
            }
            // Otherwise need to advance to the next event
            while (true) {
                type = next();
                if (type == COMMENT || type == PROCESSING_INSTRUCTION
                    || type == SPACE) { // space is ignorable too
                    continue;
                }
                if (type == END_ELEMENT) {
                    /* just need to verify we don't have partial stuff
                     * (missing one to three characters of a full quartet
                     * that encodes 1 - 3 bytes)
                     */
                    if (!dec.okToGetEndElement()) {
                        throw _constructTypeException("Incomplete base64 triplet at the end of decoded content", "");
                    }
                    break main_loop;
                }
                if (((1 << type) & MASK_GET_ELEMENT_TEXT) == 0) {
                    throwNotStartElemOrTextual(type);
                }
                dec.init(false, getText());
                break;
            }
        }

        // If nothing was found, needs to be indicated via -1, not 0
        return (totalCount > 0) ? totalCount : -1;
    }

    /*
    /////////////////////////////////////////////////
    // TypedXMLStreamReader, attribute access
    /////////////////////////////////////////////////
     */

    public int getAttributeIndex(String namespaceURI, String localName)
    {
        return findAttributeIndex(namespaceURI, localName);
    }

    public boolean getAttributeAsBoolean(int index) throws XMLStreamException
    {
        ValueDecoderFactory.BooleanDecoder dec = _decoderFactory().getBooleanDecoder();
        getAttributeAs(index, dec);
        return dec.getValue();
    }

    public int getAttributeAsInt(int index) throws XMLStreamException
    {
        ValueDecoderFactory.IntDecoder dec = _decoderFactory().getIntDecoder();
        getAttributeAs(index, dec);
        return dec.getValue();
    }

    public long getAttributeAsLong(int index) throws XMLStreamException
    {
        ValueDecoderFactory.LongDecoder dec = _decoderFactory().getLongDecoder();
        getAttributeAs(index, dec);
        return dec.getValue();
    }

    public float getAttributeAsFloat(int index) throws XMLStreamException
    {
        ValueDecoderFactory.FloatDecoder dec = _decoderFactory().getFloatDecoder();
        getAttributeAs(index, dec);
        return dec.getValue();
    }

    public double getAttributeAsDouble(int index) throws XMLStreamException
    {
        ValueDecoderFactory.DoubleDecoder dec = _decoderFactory().getDoubleDecoder();
        getAttributeAs(index, dec);
        return dec.getValue();
    }

    public BigInteger getAttributeAsInteger(int index) throws XMLStreamException
    {
        ValueDecoderFactory.IntegerDecoder dec = _decoderFactory().getIntegerDecoder();
        getAttributeAs(index, dec);
        return dec.getValue();
    }

    public BigDecimal getAttributeAsDecimal(int index) throws XMLStreamException
    {
        ValueDecoderFactory.DecimalDecoder dec = _decoderFactory().getDecimalDecoder();
        getAttributeAs(index, dec);
        return dec.getValue();
    }

    public QName getAttributeAsQName(int index) throws XMLStreamException
    {
        ValueDecoderFactory.QNameDecoder dec = _decoderFactory().getQNameDecoder(getNamespaceContext());
        getAttributeAs(index, dec);
        return dec.getValue();
    }

    public void getAttributeAs(int index, TypedValueDecoder tvd) throws XMLStreamException
    {
        String value = getAttributeValue(index);
        try {
            tvd.decode(value);
        } catch (IllegalArgumentException iae) {
            throw _constructTypeException(iae, value);
        }
    }

    public int[] getAttributeAsIntArray(int index) throws XMLStreamException
    {
        ValueDecoderFactory.IntArrayDecoder dec = _decoderFactory().getIntArrayDecoder();
        _getAttributeAsArray(dec, getAttributeValue(index));
        return dec.getValues();
    }

    public long[] getAttributeAsLongArray(int index) throws XMLStreamException
    {
        ValueDecoderFactory.LongArrayDecoder dec = _decoderFactory().getLongArrayDecoder();
        _getAttributeAsArray(dec, getAttributeValue(index));
        return dec.getValues();
    }

    public float[] getAttributeAsFloatArray(int index) throws XMLStreamException
    {
        ValueDecoderFactory.FloatArrayDecoder dec = _decoderFactory().getFloatArrayDecoder();
        _getAttributeAsArray(dec, getAttributeValue(index));
        return dec.getValues();
    }

    public double[] getAttributeAsDoubleArray(int index) throws XMLStreamException
    {
        ValueDecoderFactory.DoubleArrayDecoder dec = _decoderFactory().getDoubleArrayDecoder();
        _getAttributeAsArray(dec, getAttributeValue(index));
        return dec.getValues();
    }

    public int getAttributeAsArray(int index, TypedArrayDecoder tad) throws XMLStreamException
    {
        return _getAttributeAsArray(tad, getAttributeValue(index));
    }

    protected int _getAttributeAsArray(TypedArrayDecoder tad, String attrValue) throws XMLStreamException
    {
        int ptr = 0;
        int start = 0;
        final int end = attrValue.length();
        String lexical = null;
        int count = 0;

        try {
            decode_loop:
            while (ptr < end) {
                // First, any space to skip?
                while (attrValue.charAt(ptr) <= INT_SPACE) {
                    if (++ptr >= end) {
                        break decode_loop;
                    }
                }
                // Then let's figure out non-space char (token)
                start = ptr;
                ++ptr;
                while (ptr < end && attrValue.charAt(ptr) > INT_SPACE) {
                    ++ptr;
                }
                int tokenEnd = ptr;
                ++ptr; // to skip trailing space (or, beyond end)
                // And there we have it
                lexical = attrValue.substring(start, tokenEnd);
                ++count;
                if (tad.decodeValue(lexical)) {
                    if (!checkExpand(tad)) {
                        break;
                    }
                }
            }
        } catch (IllegalArgumentException iae) {
            // Need to convert to a checked stream exception
            Location loc = getLocation();
            throw new TypedXMLStreamException(lexical, iae.getMessage(), loc, iae);
        }
        return count;
    }

    /**
     * Internal method used to see if we can expand the buffer that
     * the array decoder has. Bit messy, but simpler than having
     * separately typed instances; and called rarely so that performance
     * downside of instanceof is irrelevant.
     */
    private final boolean checkExpand(TypedArrayDecoder tad)
    {
        if (tad instanceof ValueDecoderFactory.BaseArrayDecoder) {
            ((ValueDecoderFactory.BaseArrayDecoder) tad).expand();
            return true;
        }
        return false;
    }

    public byte[] getAttributeAsBinary(int index) throws XMLStreamException
    {
        String lexical = getAttributeValue(index);
        final StringBase64Decoder dec = _base64Decoder();
        dec.init(true, lexical);
        try {
            return dec.decodeCompletely();
        } catch (IllegalArgumentException iae) {
            throw new TypedXMLStreamException(lexical, iae.getMessage(), getLocation(), iae);
        }
    }

    /*
    ////////////////////////////////////////////////////
    // XMLStreamReader2 (StAX2) implementation
    ////////////////////////////////////////////////////
     */

    // // // StAX2, per-reader configuration

    public Object getFeature(String name)
    {
        // No features defined
        return null;
    }

    public void setFeature(String name, Object value)
    {
        // No features defined
    }

    // NOTE: getProperty() defined in Stax 1.0 interface

    public boolean isPropertySupported(String name) {
        /* No way to cleanly implement this using just Stax 1.0
         * interface, so let's be conservative and decline any knowledge
         * of properties...
         */
        return false;
    }

    public boolean setProperty(String name, Object value)
    {
        return false; // could throw an exception too
    }

    // // // StAX2, additional traversal methods

    public void skipElement() throws XMLStreamException
    {
        if (getEventType() != START_ELEMENT) {
            throwNotStartElem(getEventType());
        }
        int nesting = 1; // need one more end elements than start elements

        while (true) {
            int type = next();
            if (type == START_ELEMENT) {
                ++nesting;
            } else if (type == END_ELEMENT) {
                if (--nesting == 0) {
                    break;
                }
            }
        }
    }

    // // // StAX2, additional attribute access

    public AttributeInfo getAttributeInfo() throws XMLStreamException
    {
        if (getEventType() != START_ELEMENT) {
            throwNotStartElem(getEventType());
        }
        return this;
    }

    // // // StAX2, Additional DTD access

    public DTDInfo getDTDInfo() throws XMLStreamException
    {
        if (getEventType() != DTD) {
            return null;
        }
        return this;
    }

    // // // StAX2, Additional location information

    /**
     * Location information is always accessible, for this reader.
     */
    public final LocationInfo getLocationInfo() {
        return this;
    }

    // // // StAX2, Pass-through text accessors

    public int getText(Writer w, boolean preserveContents)
        throws IOException, XMLStreamException
    {
        char[] cbuf = getTextCharacters();
        int start = getTextStart();
        int len = getTextLength();

        if (len > 0) {
            w.write(cbuf, start, len);
        }
        return len;
    }

    // // // StAX 2, Other accessors

    /**
     * @return Number of open elements in the stack; 0 when parser is in
     *  prolog/epilog, 1 inside root element and so on.
     */
    public int getDepth() {
        return mDepth;
    }

    /**
     * Alas, there is no way to find this out via Stax 1.0, so this
     * implementation always returns false.
     */
    public boolean isEmptyElement() throws XMLStreamException
    {
        return false;
    }

    public NamespaceContext getNonTransientNamespaceContext()
    {
        /* Too hard to construct without other info: let's bail
         * and return null; this is better than return a transient
         * one.
         */
        return null;
    }

    public String getPrefixedName()
    {
        switch (getEventType()) {
        case START_ELEMENT:
        case END_ELEMENT:
            {
                String prefix = getPrefix();
                String ln = getLocalName();

                if (prefix == null || prefix.length() == 0) {
                    return ln;
                }
                StringBuffer sb = new StringBuffer(ln.length() + 1 + prefix.length());
                sb.append(prefix);
                sb.append(':');
                sb.append(ln);
                return sb.toString();
            }
        case ENTITY_REFERENCE:
            return getLocalName();
        case PROCESSING_INSTRUCTION:
            return getPITarget();
        case DTD:
            return getDTDRootName();

        }
        throw new IllegalStateException("Current state not START_ELEMENT, END_ELEMENT, ENTITY_REFERENCE, PROCESSING_INSTRUCTION or DTD");
    }

    public void closeCompletely() throws XMLStreamException
    {
        /* As usual, Stax 1.0 offers no generic way of doing just this.
         * But let's at least call the lame basic close()
         */
        close();
    }

    /*
    ////////////////////////////////////////////////////
    // AttributeInfo implementation (StAX 2)
    ////////////////////////////////////////////////////
     */

    // Already part of XMLStreamReader
    //public int getAttributeCount();

    public int findAttributeIndex(String nsURI, String localName)
    {
        if ("".equals(nsURI)) {
            nsURI = null;
        }
        for (int i = 0, len = getAttributeCount(); i < len; ++i) {
            if (getAttributeLocalName(i).equals(localName)) {
                String otherUri = getAttributeNamespace(i);
                if (nsURI == null) {
                    if (otherUri == null || otherUri.length() == 0) {
                        return i;
                    }
                } else {
                    if (nsURI.equals(otherUri)) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    public int getIdAttributeIndex()
    {
        for (int i = 0, len = getAttributeCount(); i < len; ++i) {
            if ("ID".equals(getAttributeType(i))) {
                return i;
            }
        }
        return -1;
    }

    public int getNotationAttributeIndex()
    {
        for (int i = 0, len = getAttributeCount(); i < len; ++i) {
            if ("NOTATION".equals(getAttributeType(i))) {
                return i;
            }
        }
        return -1;
    }

    /*
    ////////////////////////////////////////////////////
    // DTDInfo implementation (StAX 2)
    ////////////////////////////////////////////////////
     */

    public Object getProcessedDTD() {
        return null;
    }

    public String getDTDRootName() {
        return null;
    }

    public String getDTDPublicId() {
        return null;
    }

    public String getDTDSystemId() {
        return null;
    }

    /**
     * @return Internal subset portion of the DOCTYPE declaration, if any;
     *   empty String if none
     */
    public String getDTDInternalSubset()
    {
        /* According to basic Stax API, getText() <b>should</b> return
         * the internal subset. Not all implementations agree, so
         * this may or may not work.
         */
        if (getEventType() == XMLStreamConstants.DTD) {
            return getText();
        }
        return null;
    }

    // // StAX2, v2.0

    public DTDValidationSchema getProcessedDTDSchema() {
        return null;
    }

    /*
    ////////////////////////////////////////////////////
    // LocationInfo implementation (StAX 2)
    ////////////////////////////////////////////////////
     */

    // // // First, the "raw" offset accessors:

    public long getStartingByteOffset() {
        return -1L;
    }

    public long getStartingCharOffset() {
        return 0;
    }

    public long getEndingByteOffset() throws XMLStreamException
    {
        return -1;
    }

    public long getEndingCharOffset() throws XMLStreamException
    {
        return -1;
    }

    // // // and then the object-based access methods:

    public XMLStreamLocation2 getStartLocation()
    {
        /* We don't really know whether location given is current,
         * start or end, but it's the best approximation we have
         * without knowing more about impl:
         */
        return getCurrentLocation();
    }

    public XMLStreamLocation2 getCurrentLocation()
    {
        // Just need to adapt; no info on parent context, if any:
        return new Stax2LocationAdapter(getLocation());
    }

    public final XMLStreamLocation2 getEndLocation()
        throws XMLStreamException
    {
        /* We don't really know whether location given is current,
         * start or end, but it's the best approximation we have
         * without knowing more about impl:
         */
        return getCurrentLocation();
    }

    /*
    ////////////////////////////////////////////////////
    // Stax2 validation
    ////////////////////////////////////////////////////
     */

    public XMLValidator validateAgainst(XMLValidationSchema schema)
        throws XMLStreamException
    {
        throwUnsupported();
        return null;
    }

    public XMLValidator stopValidatingAgainst(XMLValidationSchema schema)
        throws XMLStreamException
    {
        throwUnsupported();
        return null;
    }

    public XMLValidator stopValidatingAgainst(XMLValidator validator)
        throws XMLStreamException
    {
        throwUnsupported();
        return null;
    }

    public ValidationProblemHandler setValidationProblemHandler(ValidationProblemHandler h)
    {
        return null;
    }

    /*
    ////////////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////////////
     */

    protected ValueDecoderFactory _decoderFactory()
    {
        if (mDecoderFactory == null) {
            mDecoderFactory = new ValueDecoderFactory();
        }
        return mDecoderFactory;
    }

    protected StringBase64Decoder _base64Decoder()
    {
        if (_base64Decoder == null) {
            _base64Decoder = new StringBase64Decoder();
        }
        return _base64Decoder;
    }

    protected void throwUnsupported()
        throws XMLStreamException
    {
        throw new XMLStreamException("Unsupported method");
    }

    protected void throwNotStartElem(int type)
    {
        throw new IllegalStateException("Current event ("+Stax2Util.eventTypeDesc(type)+") not START_ELEMENT");
    }

    protected void throwNotStartElemOrTextual(int type)
    {
        throw new IllegalStateException("Current event ("+Stax2Util.eventTypeDesc(type)+") not START_ELEMENT, END_ELEMENT, CHARACTERS or CDATA");
    }

    /**
     * Method called to wrap or convert given conversion-fail exception
     * into a full {@link TypedXMLStreamException},
     *
     * @param iae Problem as reported by converter
     * @param lexicalValue Lexical value (element content, attribute value)
     *    that could not be converted succesfully.
     */
    protected TypedXMLStreamException _constructTypeException(IllegalArgumentException iae, String lexicalValue)
    {
        String msg = iae.getMessage();
        if (msg == null) {
            msg = "";
        }
        Location loc = getStartLocation();
        if (loc == null) {
            return new TypedXMLStreamException(lexicalValue, msg, iae);
        }
        return new TypedXMLStreamException(lexicalValue, msg, loc, iae);
    }

    protected TypedXMLStreamException _constructTypeException(String msg, String lexicalValue)
    {
        Location loc = getStartLocation();
        if (loc == null) {
            return new TypedXMLStreamException(lexicalValue, msg);
        }
        return new TypedXMLStreamException(lexicalValue, msg, loc);
    }
}
