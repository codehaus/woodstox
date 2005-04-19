package com.ctc.wstx.dtd;

import javax.xml.stream.Location;

import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.exc.WstxException;
import com.ctc.wstx.io.WstxInputData;
import com.ctc.wstx.sr.AttributeCollector;
import com.ctc.wstx.sr.InputProblemReporter;
import com.ctc.wstx.util.TextBuilder;
import com.ctc.wstx.util.WordResolver;

/**
 * Specific attribute class for attributes that contain (unique)
 * identifiers.
 */
public final class DTDNmTokensAttr
    extends DTDAttribute
{
    /*
    ///////////////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////////////
     */

    /**
     * Main constructor.
     */
    public DTDNmTokensAttr(NameKey name, int defValueType, String defValue,
                          int specIndex)
    {
        super(name, defValueType, defValue, specIndex);
    }

    public DTDAttribute cloneWith(int specIndex)
    {
        return new DTDNmTokensAttr(mName, mDefValueType, mDefValue, specIndex);
    }

    /*
    ///////////////////////////////////////////////////
    // Public API
    ///////////////////////////////////////////////////
     */

    public int getValueType() {
        return TYPE_NMTOKENS;
    }

    /*
    ///////////////////////////////////////////////////
    // Public API, validation
    ///////////////////////////////////////////////////
     */

    /**
     * Method called by the {@link ElementValidator}
     * to let the attribute do necessary normalization and/or validation
     * for the value.
     */
    public void validate(ElementValidator v, boolean normalize, AttributeCollector ac,
                         int index)
        throws WstxException
    {
        TextBuilder tb = ac.getAttrBuilder();
        char[] ch = tb.getCharBuffer();
        int start = tb.getOffset(index);
        int origStart = start;
        int last = tb.getOffset(index+1) - 1;

        /* First things first; let's ensure value is not empty (all
         * white space)...
         */
        while (start <= last && WstxInputData.isSpaceChar(ch[start])) {
            ++start;
        }
        // Empty value?
        if (start > last) {
            reportParseError(v, "Empty NMTOKENS value");
        }

        /* Then, let's have separate handling for normalizing and
         * non-normalizing case, since latter is trivially easy case:
         */
        if (!normalize) {
            for (; start <= last; ++start) {
                char c = ch[start];
                if (!WstxInputData.isSpaceChar(c) 
                    && !WstxInputData.is11NameChar(c)) {
                    reportInvalidChar(v, c, "not valid as NMTOKENS character");
                }
            }
            return; // ok, all good
        }

        boolean trimmed = (origStart != start);
        origStart = start;

        // Wouldn't absolutely have to trim trailing... but is easy to do
        while (last > start && WstxInputData.isSpaceChar(ch[last])) {
            --last;
            trimmed = true;
        }

        /* Ok, now, need to check we only have valid chars, and maybe
         * also coalesce multiple spaces, if any.
         */
        StringBuffer sb = null;

        while (start <= last) {
            int i = start;
            for (; i <= last; ++i) {
                char c = ch[i];
                if (WstxInputData.isSpaceChar(c)) {
                    break;
                }
                if (!WstxInputData.is11NameChar(c)) {
                    reportInvalidChar(v, c, "not valid as an NMTOKENS character");
                }
            }

            if (sb == null) {
                sb = new StringBuffer(last - start + 1);
            } else {
                sb.append(' ');
            }
            sb.append(ch, start, (i - start));

            start = i + 1;
            // Ok, any white space to skip?
            while (start <= last && WstxInputData.isSpaceChar(ch[start])) {
                ++start;
            }
        }

        ac.setNormalizedValue(index, sb.toString());
    }

    /**
     * Method called by the {@link ElementValidator}
     * to ask attribute to verify that the default it has (if any) is
     * valid for such type.
     */
    public void validateDefault(InputProblemReporter rep, boolean normalize)
        throws WstxException
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

            int i = start+1;

            do {
                if (!WstxInputData.is11NameChar(c)) {
                    reportParseError(rep, "Invalid default value '"+defValue
                                     +"'; character #"+i+" ("
                                     +WstxInputData.getCharDesc(c)
                                     +") not a valid NMTOKENS character");
                }
                if (++i >= len) {
                    break;
                }
                c = defValue.charAt(i);
            } while (!WstxInputData.isSpaceChar(c));
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
            reportParseError(rep, "Invalid default value '"+defValue
                             +"'; empty String is not a valid NMTOKENS value");
        }

        if (normalize) {
            mDefValue = sb.toString();
        }
    }

    /*
    ///////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////
     */

}
