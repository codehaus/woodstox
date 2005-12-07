package com.ctc.wstx.dtd;

import org.codehaus.stax2.validation.XMLValidationException;

import com.ctc.wstx.sr.InputProblemReporter;
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
     * Method called by the {@link DTDValidator}
     * to let the attribute do necessary normalization and/or validation
     * for the value.
     *<p>
     * Note: identical to the implementation in {@link DTDEnumAttr}
     */
   public String validate(DTDValidator v, char[] cbuf, int start, int end, boolean normalize)
        throws XMLValidationException
    {
        String ok = validateEnumValue(cbuf, start, end, normalize, mEnumValues);
        if (ok == null) {
            String val = new String(cbuf, start, (end-start));
            return reportValidationProblem(v, "Invalid notation value '"+val+"': has to be one of ("
                                    +mEnumValues+")");
        }
        return ok;
    }

    /**
     * Method called by the {@link DTDValidator}
     * to ask attribute to verify that the default it has (if any) is
     * valid for such type.
     */
    public void validateDefault(InputProblemReporter rep, boolean normalize)
        throws XMLValidationException
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
}
