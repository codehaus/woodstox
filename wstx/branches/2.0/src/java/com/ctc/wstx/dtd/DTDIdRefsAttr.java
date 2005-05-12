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
 * Attribute class for attributes that contain multiple references
 * to elements that have matching identifier specified.
 */
public final class DTDIdRefsAttr
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
    public DTDIdRefsAttr(NameKey name, int defValueType, String defValue,
                        int specIndex)
    {
        super(name, defValueType, defValue, specIndex);
    }

    public DTDAttribute cloneWith(int specIndex)
    {
        return new DTDIdRefsAttr(mName, mDefValueType, mDefValue, specIndex);
    }

    /*
    ///////////////////////////////////////////////////
    // Public API
    ///////////////////////////////////////////////////
     */

    public int getValueType() {
        return TYPE_IDREFS;
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
        throws WstxException
    {
        TextBuilder tb = ac.getAttrBuilder();
        char[] ch = tb.getCharBuffer();
        int start = tb.getOffset(index);
        int last = tb.getOffset(index+1) - 1;

        /* Let's skip leading/trailing white space, even if we are not
         * to normalize visible attribute value. This allows for better
         * round-trip handling (no changes for physical value caller
         * gets), but still allows succesful validation.
         */
        while (start <= last && WstxInputData.isSpaceChar(ch[start])) {
            ++start;
        }

        // No id?
        if (start > last) {
            reportParseError(v, "Empty IDREFS value");
        }

        while (last > start && WstxInputData.isSpaceChar(ch[last])) {
            --last;
        }

        // Ok; now start points to first, last to last char (both inclusive)
        ElementIdMap m = v.getIdMap();
        Location loc = v.getLocation();

        String idStr = null;
        StringBuffer sb = null;
        while (start <= last) {
            // Ok, need to check char validity, and also calc hash code:
            char c = ch[start];
            if (!WstxInputData.is11NameStartChar(c) && c != ':') {
                reportInvalidChar(v, c, "not valid as the first IDREFS character");
            }
            int hash = (int) c;
            int i = start+1;
            for (; i <= last; ++i) {
                c = ch[i];
                if (WstxInputData.isSpaceChar(c)) {
                    break;
                }
                if (!WstxInputData.is11NameChar(c)) {
                    reportInvalidChar(v, c, "not valid as an IDREFS character");
                }
                hash = (hash * 31) + (int) c;
            }

            // Ok, got the next id ref...
            ElementId id = m.addReferenced(ch, start, i - start, hash,
                                           loc, v.getElemName(), mName);
            
            // Can skip the trailing space char (if there was one)
            start = i+1;

            /* When normalizing, we can possibly share id String, or
             * alternatively, compose normalized String if multiple
             */
            if (normalize) {
                if (idStr == null) { // first idref
                    idStr = id.getId();
                } else {
                    if (sb == null) {
                        sb = new StringBuffer(idStr);
                    }
                    idStr = id.getId();
                    sb.append(' ');
                    sb.append(idStr);
                }
            }

            // Ok, any white space to skip?
            while (start <= last && WstxInputData.isSpaceChar(ch[start])) {
                ++start;
            }
        }

        if (normalize) {
            if (sb != null) {
                idStr = sb.toString();
            }
            ac.setNormalizedValue(index, idStr);
        }
    }

    /**
     * Method called by the {@link ElementValidator}
     * to ask attribute to verify that the default it has (if any) is
     * valid for such type.
     *<p>
     * It's unlikely there will be default values... but just in case,
     * let's implement it properly.
     */
    public void validateDefault(InputProblemReporter rep, boolean normalize)
        throws WstxException
    {
        mDefValue = validateDefaultNames(rep, normalize);
    }

    /*
    ///////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////
     */

}
