package com.ctc.wstx.dtd;

import com.ctc.wstx.exc.WstxException;
import com.ctc.wstx.sr.AttributeCollector;
import com.ctc.wstx.sr.InputProblemReporter;
import com.ctc.wstx.sr.StreamScanner;
import com.ctc.wstx.util.WordResolver;

/**
 * Specific attribute class for attributes that have enumerated values.
 */
public final class DTDEnumAttr
    extends DTDAttribute
{
    final WordResolver mEnumValues;

    /*
    ///////////////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////////////
     */

    public DTDEnumAttr(NameKey name, int defValueType, String defValue,
                       int specIndex, WordResolver enumValues)
    {
        super(name, defValueType, defValue, specIndex);
        mEnumValues = enumValues;
    }

    public DTDAttribute cloneWith(int specIndex)
    {
        return new DTDEnumAttr(mName, mDefValueType, mDefValue,
                               specIndex, mEnumValues);
    }

    /*
    ///////////////////////////////////////////////////
    // Public API
    ///////////////////////////////////////////////////
     */

    public int getValueType() {
        return TYPE_ENUMERATED;
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
        String ok = ac.checkEnumValue(index, mEnumValues);
        if (ok == null) {
            String val = ac.getValue(index);
            reportValidationError(v, "Invalid value '"+val+"': has to be one of ("
                              +mEnumValues+")");
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
        String def = validateDefaultNmToken(rep, normalize);

        // And then that it's one of listed values:
        String shared = mEnumValues.find(def);
        if (shared == null) {
            reportValidationError(rep, "Invalid default value '"+def+"': has to be one of ("
                                  +mEnumValues+")");
        }

        // Ok, cool it's ok...
        if (normalize) {
            mDefValue = shared;
        }
    }

    /*
    ///////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////
     */

}
