package com.ctc.wstx.dtd;

import javax.xml.stream.Location;

import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.exc.WstxValidationException;
import com.ctc.wstx.io.WstxInputData;
import com.ctc.wstx.sr.AttributeCollector;
import com.ctc.wstx.sr.InputProblemReporter;
import com.ctc.wstx.util.TextBuilder;
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
    public DTDIdRefAttr(NameKey name, int defValueType, String defValue,
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
     * Method called by the {@link ElementValidator}
     * to let the attribute do necessary normalization and/or validation
     * for the value.
     * 
     */
    public void validate(ElementValidator v, boolean normalize, AttributeCollector ac,
                         int index)
        throws WstxValidationException
    {
        TextBuilder tb = ac.getAttrBuilder();
        char[] ch = tb.getCharBuffer();
        int start = tb.getOffset(index);
        int last = tb.getOffset(index+1) - 1;

        /* Let's skip leading/trailing white space, even if we are not
         * to normalize visible attribute value. This allows for better
         * round-trip handling, but still allow validation.
         */
        while (start <= last && WstxInputData.isSpaceChar(ch[start])) {
            ++start;
        }

        if (start > last) { // empty (all white space) value?
            reportValidationProblem(v, "Empty IDREF value");
        }

        while (last > start && WstxInputData.isSpaceChar(ch[last])) {
            --last;
        }

        // Ok, need to check char validity, and also calc hash code:
        char c = ch[start];
        if (!WstxInputData.is11NameStartChar(c) && c != ':') {
            reportInvalidChar(v, c, "not valid as the first IDREF character");
        }
        int hash = (int) c;
        for (int i = start+1; i <= last; ++i) {
            c = ch[i];
            if (!WstxInputData.is11NameChar(c)) {
                reportInvalidChar(v, c, "not valid as an IDREF character");
            }
            hash = (hash * 31) + (int) c;
        }

        // Ok, let's check and update id ref list...
        ElementIdMap m = v.getIdMap();
        Location loc = v.getLocation();
        ElementId id = m.addReferenced(ch, start, (last - start + 1), hash,
                                       loc, v.getElemName(), mName);
        // and that's all; no checks needed here

        if (normalize) {
            ac.setNormalizedValue(index, id.getId());
        }
    }

    /**
     * Method called by the {@link ElementValidator}
     * to ask attribute to verify that the default it has (if any) is
     * valid for such type.
     */
    public void validateDefault(InputProblemReporter rep, boolean normalize)
        throws WstxValidationException
    {
        mDefValue = validateDefaultName(rep, normalize);
    }

    /*
    ///////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////
     */

}
