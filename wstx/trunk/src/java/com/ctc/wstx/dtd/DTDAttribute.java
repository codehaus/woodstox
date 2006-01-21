/* Woodstox XML processor
 *
 * Copyright (c) 2004- Tatu Saloranta, tatu.saloranta@iki.fi
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

package com.ctc.wstx.dtd;

import java.util.Map;

import org.codehaus.stax2.validation.XMLValidationException;

import com.ctc.wstx.ent.EntityDecl;
import com.ctc.wstx.exc.*;
import com.ctc.wstx.io.WstxInputData;
import com.ctc.wstx.sr.InputProblemReporter;
import com.ctc.wstx.util.StringUtil;
import com.ctc.wstx.util.WordResolver;

/**
 * Base class for objects that contain attribute definitions from DTD.
 * Sub-classes exists for specific typed attributes (enumeration-valued,
 * non-CDATA ones); base class itself is used for attributes of type
 * CDATA.
 */
public abstract class DTDAttribute
{
    final static char CHAR_SPACE = (char) 0x0020;

    /*
    ///////////////////////////////////////////////////
    // Type constants
    ///////////////////////////////////////////////////
     */

    // // // Value types

    public final static int TYPE_CDATA = 0; // default...
    public final static int TYPE_ENUMERATED = 1;

    public final static int TYPE_ID = 2;
    public final static int TYPE_IDREF = 3;
    public final static int TYPE_IDREFS = 4;

    public final static int TYPE_ENTITY = 5;
    public final static int TYPE_ENTITIES = 6;

    public final static int TYPE_NOTATION = 7;
    public final static int TYPE_NMTOKEN = 8;
    public final static int TYPE_NMTOKENS = 9;

    // // // Default value types

    public final static int DEF_DEFAULT = 1;
    public final static int DEF_IMPLIED = 2;
    public final static int DEF_REQUIRED = 3;
    public final static int DEF_FIXED = 4;

    /**
     * Array that has String constants matching above mentioned
     * value types
     */
    final static String[] sTypes = new String[] {
        "CDATA",
        "ENUMERATED", // !!! 23-Jan-2005, TSa: What's the official type constant? CDATA?
        "ID",
        "IDREF",
        "IDREFS",
        "ENTITY",
        "ENTITIES",
        "NOTATION",
        "NMTOKEN",
        "NMTOKENS",
    };

    /*
    ///////////////////////////////////////////////////
    // Information about the attribute itself
    ///////////////////////////////////////////////////
     */

    protected final NameKey mName;

    /**
     * Index number amongst "special" attributes (required ones, attributes
     * that have default values), if attribute is one: -1 if not.
     */
    protected final int mSpecialIndex;

    protected final int mDefValueType;

    /**
     *<p>
     * Note: Can not be made final since validation code may want to trim
     * it down a bit...
     */
    protected String mDefValue;

    /*
    ///////////////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////////////
     */

    public DTDAttribute(NameKey name, int defValueType,
                        String defValue, int specIndex)
    {
        mName = name;
        mDefValueType = defValueType;
        mDefValue = defValue;
        mSpecialIndex = specIndex;
    }

    public abstract DTDAttribute cloneWith(int specIndex);

    /*
    ///////////////////////////////////////////////////
    // Public API, accessors
    ///////////////////////////////////////////////////
     */

    public final NameKey getName() { return mName; }

    public final String toString() {
        return mName.toString();
    }

    public final int getDefaultType() {
        return mDefValueType;
    }

    public final String getDefaultValue() {
        return mDefValue;
    }

    /**
     * Method used by the element to figure out if attribute needs "special"
     * checking; basically if it's required, and/or has a default value.
     * In both cases missing the attribute has specific consequences, either
     * exception or addition of a default value.
     */
    public final boolean isSpecial() {
        return isSpecial(mDefValueType);
    }

    public static boolean isSpecial(int defValueType) {
        return (defValueType == DEF_DEFAULT)
            || (defValueType == DEF_FIXED)
            || (defValueType == DEF_REQUIRED);
    }

    public final boolean hasDefaultValue() {
        return (mDefValueType == DEF_DEFAULT)
            || (mDefValueType == DEF_FIXED);
    }

    public final boolean isRequired() {
        return (mDefValueType == DEF_REQUIRED);
    }

    public final boolean isFixed() {
        return (mDefValueType == DEF_FIXED);
    }

    public final int getSpecialIndex() {
        return mSpecialIndex;
    }

    public final boolean needsValidation() {
        return (getValueType() != TYPE_CDATA);
    }

    /**
     * Returns the value type of this attribute as an enumerated int
     * to match type (CDATA, ...)
     *<p>
     * Note: 
     */
    public int getValueType() {
        return TYPE_CDATA;
    }

    public String getValueTypeString()
    {
        return sTypes[getValueType()];
    }

    public boolean typeIsId() {
        return false;
    }

    public boolean typeIsNotation() {
        return false;
    }

    /*
    ///////////////////////////////////////////////////
    // Public API, validation
    ///////////////////////////////////////////////////
     */

    public abstract String validate(DTDValidatorBase v, char[] cbuf, int start, int end, boolean normalize)
        throws XMLValidationException;

    /**
     *<p>
     * Note: the default implementation is not optimized, as it does
     * a potentially unnecessary copy of the contents. It is expected that
     * this method is seldom called (Woodstox never directly calls it; it
     * only gets called for chained validators when one validator normalizes
     * the value, and then following validators are passed a String, not
     * char array)
     */
    public String validate(DTDValidatorBase v, String value, boolean normalize)
        throws XMLValidationException
    {
        int len = value.length();
        /* Temporary buffer has to come from the validator itself, since
         * attribute objects are stateless and shared...
         */
        char[] cbuf = v.getTempAttrValueBuffer(value.length());
        if (len > 0) {
            value.getChars(0, len, cbuf, 0);
        }
        return validate(v, cbuf, 0, len, normalize);
    }

    /**
     * Method called by the {@link DTDValidator}
     * to ask attribute to verify that the default it has (if any) is
     * valid for such type.
     */
    public abstract void validateDefault(InputProblemReporter rep, boolean normalize)
        throws javax.xml.stream.XMLStreamException;

    /**
     * Method called when no validation is to be done, but value is still
     * to be normalized as much as it can. What this usually means is that
     * all regular space (parser earlier on converts other white space to
     * spaces, except for specific character entities; and these special
     * cases are NOT to be normalized).
     *<p>
     * The only exception is that CDATA will not do any normalization. But
     * for now, let's implement basic functionality that CDTA instance will
     * override
     */
    public String normalize(DTDValidatorBase v, char[] cbuf, int start, int end)
        throws XMLValidationException
    {
        return StringUtil.normalizeSpaces(cbuf, start, end);
    }

    /*
    ///////////////////////////////////////////////////
    // Package methods, validation helper methods
    ///////////////////////////////////////////////////
     */

    protected String validateDefaultName(InputProblemReporter rep, boolean normalize)
        throws XMLValidationException
    {
        String defValue = mDefValue.trim();

        if (defValue.length() == 0) {
            reportValidationProblem(rep, "Invalid default value '"+defValue
                             +"'; empty String is not a valid name");
        }

        // Ok, needs to be a valid XML name:
        char c = defValue.charAt(0);
        if (!WstxInputData.is11NameChar(c) && c != ':') {
            reportValidationProblem(rep, "Invalid default value '"+defValue+"'; character "
                                +WstxInputData.getCharDesc(c)
                                +") not valid first character of a name");
        }

        for (int i = 1, len = defValue.length(); i < len; ++i) {
            if (!WstxInputData.is11NameChar(defValue.charAt(i))) {
                reportValidationProblem(rep, "Invalid default value '"+defValue+"'; character #"+i+" ("
                                    +WstxInputData.getCharDesc(defValue.charAt(i))
                                   +") not valid name character");
            }
        }

        // Ok, cool it's ok...
        return normalize ? defValue : mDefValue;
    }

    protected String validateDefaultNames(InputProblemReporter rep, boolean normalize)
        throws XMLValidationException
    {
        String defValue = mDefValue;
        int len = defValue.length();

        // Then code similar to actual value validation:
        StringBuffer sb = null;
        int count = 0;
        int start = 0;

        main_loop:
        while (start < len) {
            char c = defValue.charAt(start);

            // Ok, any white space to skip?
            while (true) {
                if (!WstxInputData.isSpaceChar(c)) {
                    break;
                }
                if (++start >= len) {
                    break main_loop;
                }
                c = defValue.charAt(start);
            }

            if (!WstxInputData.is11NameStartChar(c) && c != ':') {
                reportValidationProblem(rep, "Invalid default value '"+defValue
                                 +"'; character "
                                 +WstxInputData.getCharDesc(c)
                                 +") not valid first character of a name token");
            }
            int i = start+1;
            for (; i < len; ++i) {
                c = defValue.charAt(i);
                if (WstxInputData.isSpaceChar(c)) {
                    break;
                }
                if (!WstxInputData.is11NameChar(c)) {
                    reportValidationProblem(rep, "Invalid default value '"+defValue
                                     +"'; character "
                                     +WstxInputData.getCharDesc(c)
                                     +") not a valid name character");
                }
            }

            ++count;

            if (normalize) {
                if (sb == null) {
                    sb = new StringBuffer(i - start + 32);
                } else {
                    sb.append(' ');
                }
                sb.append(defValue.substring(start, i));
            }
            start = i+1;
        }

        if (count == 0) {
            reportValidationProblem(rep, "Invalid default value '"+defValue
                             +"'; empty String is not a valid name value");
        }

        return normalize ? sb.toString() : mDefValue;
    }

    protected String validateDefaultNmToken(InputProblemReporter rep, boolean normalize)
        throws XMLValidationException
    {
        String defValue = mDefValue.trim();

        if (defValue.length() == 0) {
            reportValidationProblem(rep, "Invalid default value '"+defValue+"'; empty String is not a valid NMTOKEN");
        }

        // Ok, needs to be a valid NMTOKEN:
        for (int i = 0, len = defValue.length(); i < len; ++i) {
            if (!WstxInputData.is11NameChar(defValue.charAt(i))) {
                reportValidationProblem(rep, "Invalid default value '"+defValue
                                    +"'; character #"+i+" ("
                                   +WstxInputData.getCharDesc(defValue.charAt(i))
                                   +") not valid NMTOKEN character");
            }
        }
        // Ok, cool it's ok...
        return normalize ? defValue : mDefValue;
    }

    /**
     * Method called by validation/normalization code for enumeration-valued
     * attributes, to trim
     * specified attribute value (full normalization not needed -- called
     * for values that CAN NOT have spaces inside; such values can not
     * be legal), and then check whether it is included
     * in set of words (tokens) passed in. If actual value was included,
     * will return the normalized word (as well as store shared String
     * locally); otherwise will return null.
     */
    public String validateEnumValue(char[] cbuf, int start, int end,
                                    boolean normalize,
                                    WordResolver res)
    {
        /* Better NOT to build temporary Strings quite yet; can resolve
         * matches via resolver more efficiently.
         */
        // Note: at this point, should only have real spaces...
        if (normalize) {
            while (start < end && cbuf[start] <= CHAR_SPACE) {
                ++start;
            }
            while (--end > start && cbuf[end] <= CHAR_SPACE) {
                ;
            }
            ++end; // so it'll point to the first char (or beyond end of buffer)
        }

        // Empty String is never legal for enums:
        if (start >= end) {
            return null;
        }
        return res.find(cbuf, start, end);
    }

    protected EntityDecl findEntityDecl(DTDValidatorBase v,
                                        char[] ch, int start, int len, int hash)
        throws XMLValidationException
    {
        Map entMap = v.getEntityMap();
        /* !!! 13-Nov-2005, TSa: If this was to become a bottle-neck, we
         *   could use/share a symbol table. Or at least reuse Strings...
         */
        String id = new String(ch, start, len);
        EntityDecl ent = (EntityDecl) entMap.get(id);

        if (ent == null) {
            reportValidationProblem(v, "Referenced entity '"+id+"' not defined");
        } else if (ent.isParsed()) {
            reportValidationProblem(v, "Referenced entity '"+id+"' is not an unparsed entity");
        }
        return ent;
    }

    /* Too bad this method can not be combined with previous segment --
     * the reason is that DTDValidator does not implement
     * InputProblemReporter...
     */

    protected void checkEntity(InputProblemReporter rep, String id, EntityDecl ent)
        throws XMLValidationException
    {
        if (ent == null) {
            rep.reportValidationProblem("Referenced entity '"+id+"' not defined");
        } else if (ent.isParsed()) {
            rep.reportValidationProblem("Referenced entity '"+id+"' is not an unparsed entity");
        }
    }

    /*
    ///////////////////////////////////////////////////
    // Package methods, error reporting
    ///////////////////////////////////////////////////
     */

    protected String reportInvalidChar(DTDValidatorBase v, char c, String msg)
        throws XMLValidationException
    {
        reportValidationProblem(v, "Invalid character "+WstxInputData.getCharDesc(c)+": "+msg);
        return null;
    }

    protected String reportValidationProblem(DTDValidatorBase v, String msg)
        throws XMLValidationException
    {
        v.reportValidationProblem("Attribute '"+mName+"': "+msg);
        return null;
    }

    /**
     * Method called during parsing of DTD schema, to report a problem.
     * Note that unlike during actual validation, we have no option of
     * just gracefully listing problems and ignoring them; an exception
     * is always thrown.
     */
    protected String reportValidationProblem(InputProblemReporter rep, String msg)
        throws XMLValidationException
    {
        rep.reportValidationProblem("Attribute definition '"+mName+"': "+msg);
        return null;
    }

    /*
    ///////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////
     */

}
