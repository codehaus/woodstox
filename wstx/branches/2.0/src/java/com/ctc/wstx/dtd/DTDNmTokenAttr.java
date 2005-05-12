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
public final class DTDNmTokenAttr
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
    public DTDNmTokenAttr(NameKey name, int defValueType, String defValue,
                          int specIndex)
    {
        super(name, defValueType, defValue, specIndex);
    }

    public DTDAttribute cloneWith(int specIndex)
    {
        return new DTDNmTokenAttr(mName, mDefValueType, mDefValue, specIndex);
    }

    /*
    ///////////////////////////////////////////////////
    // Public API
    ///////////////////////////////////////////////////
     */

    public int getValueType() {
        return TYPE_NMTOKEN;
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
        int last = tb.getOffset(index+1) - 1;
        int origCount = last - start;

        // Let's trim leading white space first...
        while (start <= last && WstxInputData.isSpaceChar(ch[start])) {
            ++start;
        }

        // Empty value?
        if (start > last) {
            reportParseError(v, "Empty NMTOKEN value");
        }

        while (last > start && WstxInputData.isSpaceChar(ch[last])) {
            --last;
        }

        // Ok, need to check char validity
        for (int i = start+1; i <= last; ++i) {
            char c = ch[i];
            if (!WstxInputData.is11NameChar(c)) {
                reportInvalidChar(v, c, "not valid NMTOKEN character");
            }
        }

        if (normalize) {
            // Let's only create the String if we trimmed something
            int count = (last - start);
            if (count != origCount) {
                ac.setNormalizedValue(index, new String(ch, start, count+1));
            }
        }
    }

    /**
     * Method called by the {@link ElementValidator}
     * to ask attribute to verify that the default it has (if any) is
     * valid for such type.
     */
    public void validateDefault(InputProblemReporter rep, boolean normalize)
        throws WstxException
    {
        mDefValue = validateDefaultNmToken(rep, normalize);
    }

    /*
    ///////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////
     */

}
