package com.ctc.wstx.dtd;

import javax.xml.stream.Location;

import org.codehaus.stax2.validation.XMLValidationException;

import com.ctc.wstx.exc.WstxValidationException;
import com.ctc.wstx.io.WstxInputData;
import com.ctc.wstx.sr.InputProblemReporter;

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
     * Method called by the validator
     * to let the attribute do necessary normalization and/or validation
     * for the value.
     */
    public String validate(DTDValidatorBase v, char[] cbuf, int start, int end, boolean normalize)
        throws XMLValidationException
    {
        int origStart = start;

        /* First things first; let's ensure value is not empty (all
         * white space)...
         */
        while (start < end && WstxInputData.isSpaceChar(cbuf[start])) {
            ++start;
        }
        // Empty value?
        if (start >= end) {
            return reportValidationProblem(v, "Empty NMTOKENS value");
        }

        /* Then, let's have separate handling for normalizing and
         * non-normalizing case, since latter is trivially easy case:
         */
        if (!normalize) {
            for (; start < end; ++start) {
                char c = cbuf[start];
                if (!WstxInputData.isSpaceChar(c) 
                    && !WstxInputData.is11NameChar(c)) {
                    return reportInvalidChar(v, c, "not valid as NMTOKENS character");
                }
            }
            return null; // ok, all good
        }

        boolean trimmed = (origStart != start);
        //origStart = start;

        --end; // so that it now points to the last char
        // Wouldn't absolutely have to trim trailing... but is easy to do
        while (end > start && WstxInputData.isSpaceChar(cbuf[end])) {
            --end;
            trimmed = true;
        }

        /* Ok, now, need to check we only have valid chars, and maybe
         * also coalesce multiple spaces, if any.
         */
        StringBuffer sb = null;

        while (start <= end) {
            int i = start;
            for (; i <= end; ++i) {
                char c = cbuf[i];
                if (WstxInputData.isSpaceChar(c)) {
                    break;
                }
                if (!WstxInputData.is11NameChar(c)) {
                    return reportInvalidChar(v, c, "not valid as an NMTOKENS character");
                }
            }

            if (sb == null) {
                sb = new StringBuffer(end - start + 1);
            } else {
                sb.append(' ');
            }
            sb.append(cbuf, start, (i - start));

            start = i + 1;
            // Ok, any white space to skip?
            while (start <= end && WstxInputData.isSpaceChar(cbuf[start])) {
                ++start;
            }
        }

        /* 27-Nov-2005, TSa: Could actually optimize trimming, and often
         *   avoid using StringBuffer... but let's only do it if it turns
         *   out dealing with NMTOKENS normalization shows up on profiling...
         */
        return sb.toString();
    }

    /**
     * Method called by the validator
     * to ask attribute to verify that the default it has (if any) is
     * valid for such type.
     */
    public void validateDefault(InputProblemReporter rep, boolean normalize)
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

            int i = start+1;

            do {
                if (!WstxInputData.is11NameChar(c)) {
                    reportValidationProblem(rep, "Invalid default value '"+defValue
                                            +"'; character #"+i+" ("
                                            +WstxInputData.getCharDesc(c)
                                            +") not a valid NMTOKENS character");
                    return;
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
            reportValidationProblem(rep, "Invalid default value '"+defValue
                                    +"'; empty String is not a valid NMTOKENS value");
            return;
        }

        if (normalize) {
            mDefValue = sb.toString();
        }
    }
}
