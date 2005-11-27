package com.ctc.wstx.dtd;

import com.ctc.wstx.exc.WstxValidationException;
import com.ctc.wstx.sr.AttributeCollector;
import com.ctc.wstx.sr.InputProblemReporter;
import com.ctc.wstx.sr.StreamScanner;
import com.ctc.wstx.util.WordResolver;

/**
 * Specific attribute class for attributes that are of NOTATION type,
 * and also contain enumerated set of legal values.
 */
public final class DTDNotationAttr
    extends DTDAttribute
{
    final WordResolver mEnumValues;

    /*
    ///////////////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////////////
     */

    public DTDNotationAttr(NameKey name, int defValueType, String defValue,
                           int specIndex, WordResolver enumValues)
    {
        super(name, defValueType, defValue, specIndex);
        mEnumValues = enumValues;
    }

    public DTDAttribute cloneWith(int specIndex)
    {
        return new DTDNotationAttr(mName, mDefValueType, mDefValue,
                                   specIndex, mEnumValues);
    }

    /*
    ///////////////////////////////////////////////////
    // Public API
    ///////////////////////////////////////////////////
     */

    public int getValueType() {
        return TYPE_NOTATION;
    }

    public boolean typeIsNotation() {
        return true;
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
     *<p>
     * Note: identical 
     */
    public void validate(ElementValidator v, boolean normalize, AttributeCollector ac,
                         int index)
        throws WstxValidationException
    {
        String ok = ac.checkEnumValue(index, mEnumValues);
        if (ok == null) {
            String val = ac.getValue(index);
            reportValidationProblem(v, "Invalid value '"+val+"': has to be one of ("
                                    +mEnumValues+")");
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
        // First, basic checks that it's a valid non-empty name:
        String def = validateDefaultName(rep, normalize);

        // And then that it's one of listed values:
        String shared = mEnumValues.find(def);
        if (shared == null) {
            reportValidationProblem(rep, "Invalid default value '"+def+"': has to be one of ("
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
