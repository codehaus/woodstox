/* Woodstox XML processor
 *
 * Copyright (c) 2004- Tatu Saloranta, tatu.saloranta@iki.fi
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

package com.ctc.wstx.sr;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.ri.typed.ValueDecoderFactory;
import org.codehaus.stax2.typed.TypedArrayDecoder;
import org.codehaus.stax2.typed.TypedValueDecoder;
import org.codehaus.stax2.typed.TypedXMLStreamException;

import com.ctc.wstx.api.ReaderConfig;
import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.cfg.XmlConsts;
import com.ctc.wstx.io.BranchingReaderSource;
import com.ctc.wstx.io.InputBootstrapper;
import com.ctc.wstx.io.WstxInputData;

/**
 * Completed implementation of {@link XMLStreamReader2}, including
 * Typed Access API (Stax2 v3.0) implementation. Only functionality
 * missing is DTD validation, which is provided by a specialized
 * sub-class.
 */
public class TypedStreamReader
    extends BasicStreamReader
{
    /**
     * Factory used for constructing decoders we need for typed access
     */
    protected ValueDecoderFactory mDecoderFactory;

    /*
    ////////////////////////////////////////////////////
    // Instance construction
    ////////////////////////////////////////////////////
     */

    protected TypedStreamReader(InputBootstrapper bs,
                                BranchingReaderSource input, ReaderCreator owner,
                                ReaderConfig cfg, InputElementStack elemStack,
                                boolean forER)
        throws XMLStreamException
    {
        super(bs, input, owner, cfg, elemStack, forER);
    }

    /**
     * Factory method for constructing readers.
     *
     * @param owner "Owner" of this reader, factory that created the reader;
     *   needed for returning updated symbol table information after parsing.
     * @param input Input source used to read the XML document.
     * @param cfg Object that contains reader configuration info.
     */
    public static TypedStreamReader createStreamReader
        (BranchingReaderSource input, ReaderCreator owner, ReaderConfig cfg,
         InputBootstrapper bs, boolean forER)
        throws XMLStreamException
    {

        TypedStreamReader sr = new TypedStreamReader
            (bs, input, owner, cfg, createElementStack(cfg), forER);
        return sr;
    }


    /*
    ////////////////////////////////////////////////////////
    // TypedXMLStreamReader2 implementation, scalar elements
    ////////////////////////////////////////////////////////
     */

    public boolean getElementAsBoolean() throws XMLStreamException
    {
        ValueDecoderFactory.BooleanDecoder dec = decoderFactory().getBooleanDecoder();
        decodeElementText(dec);
        return dec.getValue();
    }

    public int getElementAsInt() throws XMLStreamException
    {
        ValueDecoderFactory.IntDecoder dec = decoderFactory().getIntDecoder();
        decodeElementText(dec);
        return dec.getValue();
    }

    public long getElementAsLong() throws XMLStreamException
    {
        ValueDecoderFactory.LongDecoder dec = decoderFactory().getLongDecoder();
        decodeElementText(dec);
        return dec.getValue();
    }

    public float getElementAsFloat() throws XMLStreamException
    {
        ValueDecoderFactory.FloatDecoder dec = decoderFactory().getFloatDecoder();
        decodeElementText(dec);
        return dec.getValue();
    }

    public double getElementAsDouble() throws XMLStreamException
    {
        ValueDecoderFactory.DoubleDecoder dec = decoderFactory().getDoubleDecoder();
        decodeElementText(dec);
        return dec.getValue();
    }

    public BigInteger getElementAsInteger() throws XMLStreamException
    {
        ValueDecoderFactory.IntegerDecoder dec = decoderFactory().getIntegerDecoder();
        decodeElementText(dec);
        return dec.getValue();
    }

    public BigDecimal getElementAsDecimal() throws XMLStreamException
    {
        ValueDecoderFactory.DecimalDecoder dec = decoderFactory().getDecimalDecoder();
        decodeElementText(dec);
        return dec.getValue();
    }

    public QName getElementAsQName() throws XMLStreamException
    {
        ValueDecoderFactory.QNameDecoder dec = decoderFactory().getQNameDecoder(getNamespaceContext());
        decodeElementText(dec);
        return verifyQName(dec.getValue());
    }

    public void getElementAs(TypedValueDecoder tvd) throws XMLStreamException
    {
        decodeElementText(tvd);
    }

    private final void decodeElementText(TypedValueDecoder dec)
        throws XMLStreamException
    {
        if (mCurrToken != START_ELEMENT) {
            throwParseError(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        /* Ok, now: with START_ELEMENT we know that it's not partially
         * processed; that we are in-tree (not prolog or epilog).
         * The only possible complication would be:
         */
        if (mStEmptyElem) {
            /* And if so, we'll then get 'virtual' close tag; things
             * are simple as location info was set when dealing with
             * empty start element; and likewise, validation (if any)
             * has been taken care of
             */
            mStEmptyElem = false;
            mCurrToken = END_ELEMENT;
            handleEmptyValue(dec);
            return;
        }
        // First need to find a textual event
        while (true) {
            int type = next();
            if (type == END_ELEMENT) {
                try {
                    dec.decode("");
                } catch (IllegalArgumentException iae) {
                    throw constructTypeException(iae, "");
                }
                return;
            }
            if (type == COMMENT || type == PROCESSING_INSTRUCTION) {
                continue;
            }
            if (((1 << type) & MASK_GET_ELEMENT_TEXT) == 0) {
                throwParseError("Expected a text token, got "+tokenTypeDesc(type)+".");
            }
            break;
        }
        if (mTokenState < TOKEN_FULL_SINGLE) {
            readCoalescedText(mCurrToken, false);
        }
        /* Ok: then a quick check; if it looks like we are directly
         * followed by the end tag, we need not construct String
         * quite yet.
         */
        if ((mInputPtr + 1) < mInputEnd &&
            mInputBuffer[mInputPtr] == '<' && mInputBuffer[mInputPtr+1] == '/') {
            // Note: next() has validated text, no need for more validation
            mInputPtr += 2;
            mCurrToken = END_ELEMENT;
            // Can by-pass next(), nextFromTree(), in this case:
            readEndElem();
            // And buffer, then, has data for conversion, so:
            try {
                mTextBuffer.decode(dec);
            } catch (IllegalArgumentException iae) {
                throw constructTypeException(iae, mTextBuffer.contentsAsString());
            }
            return;
        }

        // Otherwise, we'll need to do slower processing
        int extra = 1 + (mTextBuffer.size() >> 1); // let's add 50% space
        StringBuffer sb = mTextBuffer.contentsAsStringBuffer(extra);
        int type;
        
        while ((type = next()) != END_ELEMENT) {
            if (((1 << type) & MASK_GET_ELEMENT_TEXT) != 0) {
                if (mTokenState < mStTextThreshold) {
                    finishToken(false);
                }
                mTextBuffer.contentsToStringBuffer(sb);
                continue;
            }
            if (type != COMMENT && type != PROCESSING_INSTRUCTION) {
                throwParseError("Expected a text token, got "+tokenTypeDesc(type)+".");
            }
        }
        // Note: calls next() have validated text, no need for more validation
        String str = sb.toString();
        try {
            dec.decode(str);
        } catch (IllegalArgumentException iae) {
            throw constructTypeException(iae, str);
        }
    }

    /**
     * Method called to handle value that has empty String
     * as representation. This will usually either lead to an
     * exception, or parsing to the default value for the
     * type in question (null for nullable types and so on).
     */
    private void handleEmptyValue(TypedValueDecoder dec)
        throws XMLStreamException
    {
        try {
            // !!! TBI: call "handleEmptyValue" (or whatever)
            dec.decode("");
        } catch (IllegalArgumentException iae) {
            throw constructTypeException(iae, "");
        }
    }

    /*
    ////////////////////////////////////////////////////////
    // TypedXMLStreamReader2 implementation, array elements
    ////////////////////////////////////////////////////////
     */

    public int readElementAsIntArray(int[] value, int from, int length) throws XMLStreamException
    {
        return readElementAsArray(decoderFactory().getIntArrayDecoder(value, from, length));
    }

    public int readElementAsLongArray(long[] value, int from, int length) throws XMLStreamException
    {
        return readElementAsArray(decoderFactory().getLongArrayDecoder(value, from, length));
    }

    public int readElementAsFloatArray(float[] value, int from, int length) throws XMLStreamException
    {
        return readElementAsArray(decoderFactory().getFloatArrayDecoder(value, from, length));
    }

    public int readElementAsDoubleArray(double[] value, int from, int length) throws XMLStreamException
    {
        return readElementAsArray(decoderFactory().getDoubleArrayDecoder(value, from, length));
    }

    public void readElementAs(TypedArrayDecoder tad) throws XMLStreamException
    {
        readElementAsArray(tad);
    }


    /**
     * Method called to parse array of primitives.
     *<p>
     * !!! 05-Sep-2008, tatu: Current implementation is not optimal
     *   either performance-wise, or from getting accurate Location
     *   for decoding problems. But it works otherwise, and we need
     *   to get Woodstox 4.0 out by the end of the year... so it'll
     *   do, for now.
     *
     * @return Number of elements decoded (if any were decoded), or
     *   -1 to indicate that no more values can be decoded.
     */
    private final int readElementAsArray(TypedArrayDecoder dec)
        throws XMLStreamException
    {
        // First: check the state.
        if (mCurrToken == END_ELEMENT) {
            // END_ELEMENT is ok: nothing more to decode
            return -1;
        }

        /* Otherwise either we are just starting (START_ELEMENT), or
         * have collected all the stuff into mTextBuffer.
         */
        if (mCurrToken == START_ELEMENT) {
            // Empty? Not common, but can short-cut handling if occurs
            if (mStEmptyElem) {
                mStEmptyElem = false;
                mCurrToken = END_ELEMENT;
                return -1;
            }
            /* Otherwise we'll need to collect all the contents.
             * It may be relatively simple (a single text segment),
             * or get complicated...
             */
            while (true) {
                int type = next();
                if (type == END_ELEMENT) {
                    // Simple... no textul content
                    return -1;
                }
                if (type == COMMENT || type == PROCESSING_INSTRUCTION) {
                    continue;
                }
                if (((1 << type) & MASK_GET_ELEMENT_TEXT) == 0) {
                    throwParseError("Expected a text token, got "+tokenTypeDesc(type)+".");
                }
                break;
            }
            // Ok, got a text segment, need to complete & coalesce
            if (mTokenState < TOKEN_FULL_SINGLE) {
                readCoalescedText(mCurrToken, false);
            }
        } else { // not START_ELEMENT, must be textual
            if (mCurrToken != CHARACTERS && mCurrToken != CDATA) {
                /* Will occur if entities are unexpanded, too... which
                 * is probably ok (can add more meaningful specific
                 * error, if not)
                 */
                throwParseError(ErrorConsts.ERR_STATE_NOT_ELEM_OR_TEXT, tokenTypeDesc(mCurrToken), null);
            }
        }

        /* Ok now: we do have a completely read textual event, when we
         * start.
         */
        int count = 0;

        decode_loop:
        while (true) {
            count += mTextBuffer.decodeElements(dec);
            if (!dec.hasRoom()) {
                break;
            }
            // Ok, result buffer can hold more; need next textual event!
            while (true) {
                int type = next();
                if (type == END_ELEMENT) {
                    break decode_loop;
                }
                if (type == COMMENT || type == PROCESSING_INSTRUCTION) {
                    continue;
                }
                if (((1 << type) & MASK_GET_ELEMENT_TEXT) == 0) {
                    throwParseError("Expected a text token, got "+tokenTypeDesc(type)+".");
                }
                break;
            }
            // Ok, got a text segment, need to complete & coalesce
            if (mTokenState < TOKEN_FULL_SINGLE) {
                readCoalescedText(mCurrToken, false);
            }
        }

        // If nothing was found, needs to be indicated via -1, not 0
        return (count > 0) ? count : -1;
    }

    /*
    ///////////////////////////////////////////////////////////
    // TypedXMLStreamReader2 implementation, scalar attributes
    ///////////////////////////////////////////////////////////
     */

    public int getAttributeIndex(String namespaceURI, String localName)
    {
        // Note: cut'n pasted from "getAttributeInfo()"
        if (mCurrToken != START_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        return mElementStack.findAttributeIndex(namespaceURI, localName);
    }

    public boolean getAttributeAsBoolean(int index) throws XMLStreamException
    {
        ValueDecoderFactory.BooleanDecoder dec = decoderFactory().getBooleanDecoder();
        decodeAttrText(index, dec);
        return dec.getValue();
    }

    public int getAttributeAsInt(int index) throws XMLStreamException
    {
        ValueDecoderFactory.IntDecoder dec = decoderFactory().getIntDecoder();
        decodeAttrText(index, dec);
        return dec.getValue();
    }

    public long getAttributeAsLong(int index) throws XMLStreamException
    {
        ValueDecoderFactory.LongDecoder dec = decoderFactory().getLongDecoder();
        decodeAttrText(index, dec);
        return dec.getValue();
    }

    public float getAttributeAsFloat(int index) throws XMLStreamException
    {
        ValueDecoderFactory.FloatDecoder dec = decoderFactory().getFloatDecoder();
        decodeAttrText(index, dec);
        return dec.getValue();
    }

    public double getAttributeAsDouble(int index) throws XMLStreamException
    {
        ValueDecoderFactory.DoubleDecoder dec = decoderFactory().getDoubleDecoder();
        decodeAttrText(index, dec);
        return dec.getValue();
    }

    public BigInteger getAttributeAsInteger(int index) throws XMLStreamException
    {
        ValueDecoderFactory.IntegerDecoder dec = decoderFactory().getIntegerDecoder();
        decodeAttrText(index, dec);
        return dec.getValue();
    }

    public BigDecimal getAttributeAsDecimal(int index) throws XMLStreamException
    {
        ValueDecoderFactory.DecimalDecoder dec = decoderFactory().getDecimalDecoder();
        decodeAttrText(index, dec);
        return dec.getValue();
    }

    public QName getAttributeAsQName(int index) throws XMLStreamException
    {
        ValueDecoderFactory.QNameDecoder dec = decoderFactory().getQNameDecoder(getNamespaceContext());
        decodeAttrText(index, dec);
        return verifyQName(dec.getValue());
    }

    public void getAttributeAs(int index, TypedValueDecoder tvd) throws XMLStreamException
    {
        decodeAttrText(index, tvd);
    }

    private final void decodeAttrText(int index, TypedValueDecoder tvd)
        throws XMLStreamException
    {
        if (mCurrToken != START_ELEMENT) {
            throw new IllegalStateException(ErrorConsts.ERR_STATE_NOT_STELEM);
        }
        try {
            mAttrCollector.decodeValue(index, tvd);
        } catch (IllegalArgumentException iae) {
            throw constructTypeException(iae, mAttrCollector.getValue(index));
        }
    }

    /*
    /////////////////////////////////////////////////////
    // Internal helper methods
    /////////////////////////////////////////////////////
     */

    /**
     * Method called to verify validity of the parsed QName element
     * or attribute value. At this point binding of a prefixed name
     * (if qname has a prefix) has been verified, and thereby prefix
     * also must be valid (since there must have been a preceding
     * declaration). But local name might still not be a legal
     * well-formed xml name, so let's verify that.
     */
    protected QName verifyQName(QName n)
        throws TypedXMLStreamException
    {
        String ln = n.getLocalPart();
        int ix = WstxInputData.findIllegalNameChar(ln, mCfgNsEnabled, mXml11);
        if (ix >= 0) {
            String prefix = n.getPrefix();
            String pname = (prefix != null && prefix.length() > 0) ?
                (prefix + ":" +ln) : ln;
            throw constructTypeException("Invalid local name \""+ln+"\" (character at #"+ix+" is invalid)", pname);
        }
        return n;
    }

    protected ValueDecoderFactory decoderFactory()
    {
        if (mDecoderFactory == null) {
            mDecoderFactory = new ValueDecoderFactory();
        }
        return mDecoderFactory;
    }

    /**
     * Method called to wrap or convert given conversion-fail exception
     * into a full {@link TypedXMLStreamException),
     *
     * @param iae Problem as reported by converter
     * @param lexicalValue Lexical value (element content, attribute value)
     *    that could not be converted succesfully.
     */
    protected TypedXMLStreamException constructTypeException(IllegalArgumentException iae, String lexicalValue)
    {
        return new TypedXMLStreamException(lexicalValue, iae.getMessage(), getStartLocation(), iae);
    }

    protected TypedXMLStreamException constructTypeException(String msg, String lexicalValue)
    {
        return new TypedXMLStreamException(lexicalValue, msg, getStartLocation());
    }

}

