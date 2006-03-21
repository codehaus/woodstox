package com.ctc.wstx.dtd;

import javax.xml.stream.Location;

import org.codehaus.stax2.validation.XMLValidationException;

import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.io.WstxInputData;
import com.ctc.wstx.sr.InputProblemReporter;
import com.ctc.wstx.util.WordResolver;

/**
 * Attribute class for attributes that contain references
 * to elements that have matching identifier specified.
 */
public final class DTDIdRefAttr
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
    public DTDIdRefAttr(NameKey name, int defValueType, DefaultAttrValue defValue,
                        int specIndex)
    {
        super(name, defValueType, defValue, specIndex);
    }

    public DTDAttribute cloneWith(int specIndex)
    {
        return new DTDIdRefAttr(mName, mDefValueType, mDefValue, specIndex);
    }

    /*
    ///////////////////////////////////////////////////
    // Public API
    ///////////////////////////////////////////////////
     */

    public int getValueType() {
        return TYPE_IDREF;
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
        /* Let's skip leading/trailing white space, even if we are not
         * to normalize visible attribute value. This allows for better
         * round-trip handling, but still allow validation.
         */
        while (start < end && WstxInputData.isSpaceChar(cbuf[start])) {
            ++start;
        }

        if (start >= end) { // empty (all white space) value?
            return reportValidationProblem(v, "Empty IDREF value");
        }

        --end; // so that it now points to the last char
        while (end > start && WstxInputData.isSpaceChar(cbuf[end])) {
            --end;
        }

        // Ok, need to check char validity, and also calc hash code:
        char c = cbuf[start];
        if (!WstxInputData.is11NameStartChar(c) && c != ':') {
            return reportInvalidChar(v, c, "not valid as the first IDREF character");
        }
        int hash = (int) c;
        for (int i = start+1; i <= end; ++i) {
            c = cbuf[i];
            if (!WstxInputData.is11NameChar(c)) {
                return reportInvalidChar(v, c, "not valid as an IDREF character");
            }
            hash = (hash * 31) + (int) c;
        }

        // Ok, let's check and update id ref list...
        ElementIdMap m = v.getIdMap();
        Location loc = v.getLocation();
        ElementId id = m.addReferenced(cbuf, start, (end - start + 1), hash,
                                       loc, v.getElemName(), mName);
        // and that's all; no more checks needed here
        return normalize ? id.getId() : null;
    }

    /**
     * Method called by the validator
     * to ask attribute to verify that the default it has (if any) is
     * valid for such type.
     */
    public void validateDefault(InputProblemReporter rep, boolean normalize)
        throws XMLValidationException
    {
        mDefValue.setValue(validateDefaultName(rep, normalize));
    }

    /*
    ///////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////
     */

}
