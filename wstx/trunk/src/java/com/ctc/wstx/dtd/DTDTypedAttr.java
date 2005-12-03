package com.ctc.wstx.dtd;

import org.codehaus.stax2.validation.XMLValidationException;

import com.ctc.wstx.sr.InputProblemReporter;

/**
 * Specific attribute class for attributes that are neither basic CDATA
 * attributes nor enumerated attributes. This generally means that they
 * do need to be normalized, and have some additional value restrictions.
 */
public final class DTDTypedAttr
    extends DTDAttribute
{
    /* ??? 27-Nov-2005, TSa: Hmmh. When will this type actually be used?!?!
     */

    final int mValueType;

    final boolean mSingleValued;
    final boolean mNameValued;

    /*
    ///////////////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////////////
     */

    public DTDTypedAttr(NameKey name, int defValueType, String defValue,
                        int specIndex, int valueType)
    {
        super(name, defValueType, defValue, specIndex);
        mValueType = valueType;

        if (valueType == TYPE_ENTITIES
            || valueType == TYPE_IDREFS
            || valueType == TYPE_NMTOKENS) {
            mSingleValued = false;
            mNameValued = (valueType != TYPE_NMTOKENS);
        } else {
            mSingleValued = true;
            mNameValued = (valueType != TYPE_NMTOKEN);
        }
    }

    public DTDAttribute cloneWith(int specIndex)
    {
        return new DTDTypedAttr(mName, mDefValueType, mDefValue, specIndex,
                                mValueType);
    }

    /*
    ///////////////////////////////////////////////////
    // Public API
    ///////////////////////////////////////////////////
     */

    public int getValueType() {
        return mValueType;
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
   public String validate(ElementValidator v, char[] cbuf, int start, int end, boolean normalize)
        throws XMLValidationException
    {
        // !!! TBI
        return null;
    }

    /**
     * Method called by the {@link ElementValidator}
     * to ask attribute to verify that the default it has (if any) is
     * valid for such type.
     */
    public void validateDefault(InputProblemReporter rep, boolean normalize)
        throws XMLValidationException
    {
        // !!! TBI
    }

    /*
    ///////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////
     */

}
