package com.ctc.wstx.dtd;

import javax.xml.stream.Location;

import org.codehaus.stax2.validation.XMLValidationException;

import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.io.WstxInputData;
import com.ctc.wstx.sr.InputProblemReporter;
import com.ctc.wstx.util.WordResolver;

/**
 * Specific attribute class for attributes that contain (unique)
 * identifiers.
 */
public final class DTDIdAttr
    extends DTDAttribute
{
    /*
    ///////////////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////////////
     */

    /**
     * Main constructor. Note that id attributes can never have
     * default values.
     *<p>
     * note: although ID attributes are not to have default value,
     * this is 'only' a validity constraint, and in dtd-aware-but-
     * not-validating mode it is apparently 'legal' to add default
     * values. Bleech.
     */
    public DTDIdAttr(NameKey name, int defValueType, DefaultAttrValue defValue,
                     int specIndex)
    {
        super(name, defValueType, defValue, specIndex);
    }

    public DTDAttribute cloneWith(int specIndex)
    {
        return new DTDIdAttr(mName, mDefValueType, mDefValue, specIndex);
    }

    /*
    ///////////////////////////////////////////////////
    // Public API
    ///////////////////////////////////////////////////
     */

    public int getValueType() {
        return TYPE_ID;
    }

    public boolean typeIsId() {
        return true;
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
        // Let's trim leading white space first...
        while (start < end && WstxInputData.isSpaceChar(cbuf[start])) {
            ++start;
        }

        // No id?
        if (start >= end) {
            return reportValidationProblem(v, "Empty ID value");
        }
        --end; // so that it now points to the last char
        while (end > start && WstxInputData.isSpaceChar(cbuf[end])) {
            --end;
        }

        // Ok, need to check char validity, and also calc hash code:
        char c = cbuf[start];
        if (!WstxInputData.is11NameStartChar(c) && c != ':') {
            return reportInvalidChar(v, c, "not valid as the first ID character");
        }
        int hash = (int) c;
        for (int i = start+1; i <= end; ++i) {
            c = cbuf[i];
            if (!WstxInputData.is11NameChar(c)) {
                return reportInvalidChar(v, c, "not valid as an ID character");
            }
            hash = (hash * 31) + (int) c;
        }

        // Either way, we do need to validate characters, and calculate hash
        ElementIdMap m = v.getIdMap();
        NameKey elemName = v.getElemName();
        Location loc = v.getLocation();
        ElementId id = m.addDefined(cbuf, start, (end - start + 1), hash,
                                    loc, elemName, mName);

        // We can detect dups by checking if Location is the one we passed:
        if (id.getLocation() != loc) {
            return reportValidationProblem(v, "Duplicate id '"+id.getId()+"', first declared at "
                                           +id.getLocation());
        }

        if (normalize) {
            return id.getId();
        }
        return null;
    }

    /**
     * Method called by the validator
     * to ask attribute to verify that the default it has (if any) is
     * valid for such type.
     */
    public void validateDefault(InputProblemReporter rep, boolean normalize)
        throws XMLValidationException
    {
        // Should never get called
        throw new Error(ErrorConsts.ERR_INTERNAL);
    }
}
