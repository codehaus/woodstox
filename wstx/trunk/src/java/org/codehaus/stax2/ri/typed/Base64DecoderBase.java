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

package org.codehaus.stax2.ri.typed;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import org.codehaus.stax2.ri.Stax2Util;

/**
 * Abstract base class used to share functionality between concrete
 * base64 decoders.
 *<p>
 * Mostly what follows is just shared definitions of the state machine
 * states to use, but there is also shared convenience functionality
 * for convenience decoding into simple byte arrays.
 */
abstract class Base64DecoderBase
{
    // // // Constants for the simple state machine used for decoding

    /**
     * Initial state is where we start, and where white space
     * is accepted.
     */
    final static int STATE_INITIAL = 0;

    /**
     * State in which we have gotten one valid non-padding base64 encoded
     * character
     */
    final static int STATE_VALID_1 = 1;

    /**
     * State in which we have gotten two valid non-padding base64 encoded
     * characters.
     */
    final static int STATE_VALID_2 = 2;

    /**
     * State in which we have gotten three valid non-padding base64 encoded
     * characters.
     */
    final static int STATE_VALID_3 = 3;

    /**
     * State in which we have succesfully decoded a full triplet, but not
     * yet output any characters
     */
    final static int STATE_OUTPUT_3 = 4;

    /**
     * State in which we have 2 decoded bytes to output (either due to
     * partial triplet, or having output one byte from full triplet).
     */
    final static int STATE_OUTPUT_2 = 5;

    /**
     * State in which we have 1 decoded byte to output (either due to
     * partial triplet, or having output some of decoded bytes earlier)
     */
    final static int STATE_OUTPUT_1 = 6;

    /**
     * State in which we have gotten two valid non-padding base64 encoded
     * characters, followed by a single padding character. This means
     * that we must get one more padding character to be able to decode
     * the single encoded byte
     */
    final static int STATE_VALID_2_AND_PADDING = 7;

    // // // Charaacter constants

    final static int INT_SPACE = 0x0020;

    /**
     * Base64 uses equality sign as padding char
     */
    final static char CHAR_PADDING = '=';

    /**
     * Array containing 6-bit values indexed by ascii characters (for
     * valid base64 characters). Invalid entries are marked by -1.
     */
    final static int[] BASE64_BY_CHAR = new int[128];
    static {
        Arrays.fill(BASE64_BY_CHAR, -1);
        String base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        for (int i = 0; i < base64Chars.length(); ++i) {
            BASE64_BY_CHAR[base64Chars.charAt(i)] = i;
        }
    }

    // // // Decoding State

    /**
     * State of the state machine
     */
    int mState = STATE_INITIAL;

    /**
     * Data decoded and/or ready to be output. Alignment and storage format
     * depend on state: during decoding things are appended from lowest
     * significant bits, and during output, flushed from more significant
     * bytes.
     */
    int mDecodedData;

    // // // Reused state for convenience byte[] accessors

    Stax2Util.ByteAggregator mByteAggr = null;

    // // // Constructor(s)

    protected Base64DecoderBase() { }

    /*
    //////////////////////////////////////////////////////////////
    // Shared base API
    //////////////////////////////////////////////////////////////
     */

    public abstract int decode(byte[] resultBuffer, int resultOffset, int maxLength)
        throws IllegalArgumentException;

    /**
     * Method that can be called to check whether decoder state is
     * such that hitting an END_ELEMENT is acceptable. This is the
     * case if decoder state is not one where we only have partially
     * decoded triplet: such as in the initial state, or state
     * where we are to output bytes (which also implies decoding
     * for a triplet has succeeded)
     */
    public final boolean okToGetEndElement()
    {
        return (mState == STATE_INITIAL)
            || (mState == STATE_OUTPUT_3)
            || (mState == STATE_OUTPUT_2)
            || (mState == STATE_OUTPUT_1)
            ;
    }

    /*
    //////////////////////////////////////////////////////////////
    // Convenience accessors
    //////////////////////////////////////////////////////////////
     */

    /**
     * Method that can be called to completely decode content that this
     * decoder has been initialized with.
     */
    public byte[] decodeCompletely()
    {
        Stax2Util.ByteAggregator aggr = getByteAggregator();
        byte[] buffer = aggr.startAggregation();
        while (true) {
            // Ok let's read full buffers each round
            int offset = 0;
            int len = buffer.length;

            do {
                int readCount = decode(buffer, offset, len);
                // note: can return 0; converted to -1 by front-end
                if (readCount < 1) { // all done!
                    // but we must be in a valid state too:
                    if (!okToGetEndElement()) {
                        throw new IllegalArgumentException("Incomplete base64 triplet at the end of decoded content");
                    }
                    return aggr.aggregateAll(buffer, offset);
                }
                offset += readCount;
                len -= readCount;
            } while (len > 0);

            // and if we got it, hand out results, get a new buffer
            buffer = aggr.addFullBlock(buffer);
        }
    }

    public Stax2Util.ByteAggregator getByteAggregator()
    {
        if (mByteAggr == null) {
            mByteAggr = new Stax2Util.ByteAggregator();
        }
        return mByteAggr;
    }
        
    /*
    //////////////////////////////////////////////////////////////
    // Internal helper methods error reporting
    //////////////////////////////////////////////////////////////
     */

    protected IllegalArgumentException reportInvalidChar(char ch, int bindex)
        throws IllegalArgumentException
    {
        return reportInvalidChar(ch, bindex, null);
    }

    /**
     * @param bindex Relative index within base64 character unit; between 0
     *   and 3 (as unit has exactly 4 characters)
     */
    protected IllegalArgumentException reportInvalidChar(char ch, int bindex, String msg)
        throws IllegalArgumentException
    {
        String base;
        if (ch <= INT_SPACE) {
            base = "Illegal white space character (code 0x"+Integer.toHexString(ch)+") as character #"+(bindex+1)+" of 4-char base64 unit: can only used between units";
        } else if (ch == CHAR_PADDING) {
            base = "Unexpected padding character ('"+CHAR_PADDING+"') as character #"+(bindex+1)+" of 4-char base64 unit: padding only legal as 3rd or 4th character";
        } else if (!Character.isDefined(ch) || Character.isISOControl(ch)) {
            // Not sure if we can really get here... ? (most illegal xml chars are caught at lower level)
            base = "Illegal character (code 0x"+Integer.toHexString(ch)+") in base64 content";
        } else {
            base = "Illegal character '"+((char)ch)+"' (code 0x"+Integer.toHexString(ch)+") in base64 content";
        }
        if (msg != null) {
            base = base + ": " + msg;
        }
        return new IllegalArgumentException(base);
    }
}
