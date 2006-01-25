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

package com.ctc.wstx.io;

/**
 * Base class used by readers (specifically, by
 * {@link com.ctc.wstx.sr.StreamScanner}, and its sub-classes)
 * to encapsulate input buffer portion of the class. Philosophically
 * this should probably be done via containment (composition), not
 * sub-classing but for performance reason, this "core" class is generally
 * extended from instead.
 *<p>
 * Main reason for the input data portion to be factored out of main
 * class is that this way it can also be passed to nested input handling
 * Objects, which can then manipulate input buffers of the caller,
 * efficiently.
 */
public class WstxInputData
{
    // // // Some well-known chars:

    /**
     * Null-character is used as return value from some method(s), since
     * it is not a legal character in an XML document.
     */
    public final static char CHAR_NULL = '\u0000';
    public final static char INT_NULL = 0;

    public final static char CHAR_SPACE = (char) 0x0020;
    public final static char INT_SPACE = 0x0020;

    /**
     * This constant defines the highest Unicode character allowed
     * in XML content.
     */
    public final static int MAX_UNICODE_CHAR = 0x10FFFF;

    /*
    ////////////////////////////////////////////////////
    // Character validity constants, structs
    ////////////////////////////////////////////////////
     */

    /**
     * We will only use validity array for first 256 characters, mostly
     * because after those characters it's easier to do fairly simple
     * block checks.
     */
    private final static int VALID_CHAR_COUNT = 0x100;

    private final static byte NAME_CHAR_INVALID_B = (byte) 0;
    private final static byte NAME_CHAR_ALL_VALID_B = (byte) 1;
    private final static byte NAME_CHAR_VALID_NONFIRST_B = (byte) -1;

    private final static int NAME_CHAR_INVALID_I = (byte) 0;
    private final static int NAME_CHAR_ALL_VALID_I = (byte) 1;
    private final static int NAME_CHAR_VALID_NONFIRST_I = (byte) -1;

    private final static byte[] sCharValidity = new byte[VALID_CHAR_COUNT];

    static {
        /* First, since all valid-as-first chars are also valid-as-other chars,
         * we'll initialize common chars:
         */
        sCharValidity['_'] = NAME_CHAR_ALL_VALID_B;
        for (int i = 0, last = ('z' - 'a'); i <= last; ++i) {
            sCharValidity['A' + i] = NAME_CHAR_ALL_VALID_B;
            sCharValidity['a' + i] = NAME_CHAR_ALL_VALID_B;
        }
        for (int i = 0xC0; i < 0xF6; ++i) { // not all are fully valid, but
            sCharValidity[i] = NAME_CHAR_ALL_VALID_B;
        }
        // ... now we can 'revert' ones not fully valid:
        sCharValidity[0xD7] = NAME_CHAR_INVALID_B;
        sCharValidity[0xF7] = NAME_CHAR_INVALID_B;

        /* And then we can proceed with ones only valid-as-other.
         */
        sCharValidity['-'] = NAME_CHAR_VALID_NONFIRST_B;
        sCharValidity['.'] = NAME_CHAR_VALID_NONFIRST_B;
        sCharValidity[0xB7] = NAME_CHAR_VALID_NONFIRST_B;
        for (int i = '0'; i <= '9'; ++i) {
            sCharValidity[i] = NAME_CHAR_VALID_NONFIRST_B;
        }
    }

    /**
     * Public identifiers only use 7-bit ascii range.
     */
    private final static int VALID_PUBID_CHAR_COUNT = 0x80;
    private final static byte[] sPubidValidity = new byte[VALID_PUBID_CHAR_COUNT];
    private final static byte PUBID_CHAR_INVALID_B = (byte) 0;
    private final static byte PUBID_CHAR_VALID_B = (byte) 1;
    static {
        for (int i = 0, last = ('z' - 'a'); i <= last; ++i) {
            sPubidValidity['A' + i] = PUBID_CHAR_VALID_B;
            sPubidValidity['a' + i] = PUBID_CHAR_VALID_B;
        }
        for (int i = '0'; i <= '9'; ++i) {
            sPubidValidity[i] = PUBID_CHAR_VALID_B;
        }

        // 3 main white space types are valid
        sPubidValidity[0x0A] = PUBID_CHAR_VALID_B;
        sPubidValidity[0x0D] = PUBID_CHAR_VALID_B;
        sPubidValidity[0x20] = PUBID_CHAR_VALID_B;

        // And many of punctuation/separator ascii chars too:
        sPubidValidity['-'] = PUBID_CHAR_VALID_B;
        sPubidValidity['\''] = PUBID_CHAR_VALID_B;
        sPubidValidity['('] = PUBID_CHAR_VALID_B;
        sPubidValidity[')'] = PUBID_CHAR_VALID_B;
        sPubidValidity['+'] = PUBID_CHAR_VALID_B;
        sPubidValidity[','] = PUBID_CHAR_VALID_B;
        sPubidValidity['.'] = PUBID_CHAR_VALID_B;
        sPubidValidity['/'] = PUBID_CHAR_VALID_B;
        sPubidValidity[':'] = PUBID_CHAR_VALID_B;
        sPubidValidity['='] = PUBID_CHAR_VALID_B;
        sPubidValidity['?'] = PUBID_CHAR_VALID_B;
        sPubidValidity[';'] = PUBID_CHAR_VALID_B;
        sPubidValidity['!'] = PUBID_CHAR_VALID_B;
        sPubidValidity['*'] = PUBID_CHAR_VALID_B;
        sPubidValidity['#'] = PUBID_CHAR_VALID_B;
        sPubidValidity['@'] = PUBID_CHAR_VALID_B;
        sPubidValidity['$'] = PUBID_CHAR_VALID_B;
        sPubidValidity['_'] = PUBID_CHAR_VALID_B;
        sPubidValidity['%'] = PUBID_CHAR_VALID_B;
    }

    /*
    ////////////////////////////////////////////////////
    // Current input data
    ////////////////////////////////////////////////////
     */

    /**
     * Current buffer from which data is read; generally data is read into
     * buffer from input source, but not always (especially when using nested
     * input contexts when expanding parsed entity references etc).
     */
    protected char[] mInputBuffer;

    /**
     * Pointer to next available character in buffer
     */
    protected int mInputPtr = 0;

    /**
     * Index of character after last available one in the buffer.
     */
    protected int mInputLen = 0;

    /*
    ////////////////////////////////////////////////////
    // Current input location information
    ////////////////////////////////////////////////////
     */

    /**
     * Current number of characters that were processed in previous blocks,
     * before contents of current input buffer.
     */
    protected long mCurrInputProcessed = 0L;

    /**
     * Current row location of current point in input buffer, starting
     * from 1
     */
    protected int mCurrInputRow = 1;

    /**
     * Current index of the first character of the current row in input
     * buffer. Needed to calculate column position, if necessary; benefit
     * of not having column itself is that this only has to be updated
     * once per line.
     */
    protected int mCurrInputRowStart = 0;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////////////////
     */

    protected WstxInputData() {
    }

    /**
     * Note: Only public due to sub-classes needing to call this on
     * base class instance from different package (confusing?)
     */
    public void copyBufferStateFrom(WstxInputData src)
    {
        mInputBuffer = src.mInputBuffer;
        mInputPtr = src.mInputPtr;
        mInputLen = src.mInputLen;

        mCurrInputProcessed = src.mCurrInputProcessed;
        mCurrInputRow = src.mCurrInputRow;
        mCurrInputRowStart = src.mCurrInputRowStart;
    }

    /*
    ////////////////////////////////////////////////////
    // Public/package API, character classes
    ////////////////////////////////////////////////////
     */

    /**
     * Method that can be used to check whether specified character
     * is a valid first character of an XML 1.1 name; except that
     * colon (:) is not recognized as a start char here: caller has
     * to verify it separately (since it generally affects namespace
     * mapping of a qualified name).
     */
    public final static boolean is11NameStartChar(char c)
    {
        // ISO-Latin can be checked via type array:
        if (c < VALID_CHAR_COUNT) {
            if (c <= CHAR_SPACE) {
                return false;
            }
            return (sCharValidity[c] == NAME_CHAR_ALL_VALID_B);
        }

        // Others are checked block-by-block:
        if (c <= 0x2FEF) {
            if (c < 0x300 || c >= 0x2C00) {
                // 0x100 - 0x2FF, 0x2C00 - 0x2FEF are ok
                return true;
            }
            if (c < 0x370 || c > 0x218F) {
                // 0x300 - 0x36F, 0x2190 - 0x2BFF invalid
                return false;
            }
            if (c < 0x2000) {
                // 0x370 - 0x37D, 0x37F - 0x1FFF are ok
                return (c != 0x37E);
            }
            if (c >= 0x2070) {
                // 0x2070 - 0x218F are ok
                return (c <= 0x218F);
            }
            // And finally, 0x200C - 0x200D
            return (c == 0x200C || c == 0x200D);
        }

        // 0x3000 and above:
        if (c >= 0x3001) {
            /* Hmmh, let's allow high surrogates here, without checking
             * that they are properly followed... crude basic support,
             * I know, but allow valid combinations, just doesn't catch
             * invalid ones
             */
            if (c <= 0xDBFF) { // 0x3001 - 0xD7FF (chars),
                // 0xD800 - 0xDBFF (high surrogate) are ok:
                return true;
            }
            if (c >= 0xF900 && c <= 0xFFFD) {
                /* Check above removes low surrogate (since one can not
                 * START an identifier), and byte-order markers..
                 */
                return (c <= 0xFDCF || c >= 0xFDF0);
            }
        }

        return false;
    }

    /**
     * Method that can be used to check whether specified character
     * is a valid character of an XML 1.1 name as any other char than
     * the first one; except that colon (:) is not recognized as valid here:
     * caller has to verify it separately (since it generally affects namespace
     * mapping of a qualified name).
     */
    public final static boolean is11NameChar(char c)
    {
        // ISO-Latin can be checked via type array:
        if (c < VALID_CHAR_COUNT) {
            if (c <= CHAR_SPACE) {
                return false;
            }
            return (sCharValidity[c] != NAME_CHAR_INVALID_B);
        }

        // Others are checked block-by-block:
        if (c <= 0x2FEF) {
            if (c < 0x2000 || c >= 0x2C00) {
                // 0x100 - 0x1FFF, 0x2C00 - 0x2FEF are ok
                return true;
            }
            if (c < 0x200C || c > 0x218F) {
                // 0x2000 - 0x200B, 0x2190 - 0x2BFF invalid
                return false;
            }
            if (c >= 0x2070) {
                // 0x2070 - 0x218F are ok
                return true;
            }
            // And finally, 0x200C - 0x200D, 0x203F - 0x2040 are ok
            return (c == 0x200C || c == 0x200D
                || c == 0x203F || c == 0x2040);
        }

        // 0x3000 and above:
        if (c >= 0x3001) {
            /* Hmmh, let's allow surrogate heres, without checking that
             * they have proper ordering. For non-first name chars, both are
             * ok, for valid names. Crude basic support,
             * I know, but allows valid combinations, just doesn't catch
             * invalid ones
             */
            if (c <= 0xDFFF) { // 0x3001 - 0xD7FF (chars),
                // 0xD800 - 0xDFFF (high, low surrogate) are ok:
                return true;
            }
            if (c >= 0xF900 && c <= 0xFFFD) {
                /* Check above removes other invalid chars (below valid
                 * range), and byte-order markers (0xFFFE, 0xFFFF).
                 */
                return (c <= 0xFDCF || c >= 0xFDF0);
            }
        }

        return false;
    }

    public final static boolean isSpaceChar(char c)
    {
        return (c <= CHAR_SPACE);
    }

    public static String getCharDesc(char c)
    {
        int i = (int) c;
        if (Character.isISOControl(c)) {
            return "(CTRL-CHAR, code "+i+")";
        }
        if (i > 255) {
            return "'"+c+"' (code "+i+" / 0x"+Integer.toHexString(i)+")";
        }
        return "'"+c+"' (code "+i+")";
    }

}
