package com.ctc.wstx.dtd;

import java.util.Map;

import javax.xml.stream.Location;

import org.codehaus.stax2.validation.XMLValidationException;

import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.ent.EntityDecl;
import com.ctc.wstx.io.WstxInputData;
import com.ctc.wstx.sr.InputProblemReporter;
import com.ctc.wstx.util.SymbolTable;
import com.ctc.wstx.util.WordResolver;

/**
 * Specific attribute class for attributes that contain (unique)
 * identifiers.
 */
public final class DTDEntityAttr
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
     */
    public DTDEntityAttr(NameKey name, int defValueType, DefaultAttrValue defValue,
                         int specIndex)
    {
        super(name, defValueType, defValue, specIndex);
    }

    public DTDAttribute cloneWith(int specIndex)
    {
        return new DTDEntityAttr(mName, mDefValueType, mDefValue, specIndex);
    }

    /*
    ///////////////////////////////////////////////////
    // Public API
    ///////////////////////////////////////////////////
     */

    public int getValueType() {
        return TYPE_ENTITY;
    }

    /*
    ///////////////////////////////////////////////////
    // Public API, validation
    ///////////////////////////////////////////////////
     */

    /**
     * Method called by the {@link DTDValidatorBase}
     * to let the attribute do necessary normalization and/or validation
     * for the value.
     */
    public String validate(DTDValidatorBase v, char[] cbuf, int start, int end, boolean normalize)
        throws XMLValidationException
    {
        while (start < end && WstxInputData.isSpaceChar(cbuf[start])) {
            ++start;
        }

        // Empty value?
        if (start >= end) {
            return reportValidationProblem(v, "Empty ENTITY value");
        }
        --end; // so that it now points to the last char
        while (end > start  && WstxInputData.isSpaceChar(cbuf[end])) {
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

        EntityDecl ent = findEntityDecl(v, cbuf, start, (end - start + 1), hash);
        // only returns if it succeeded...

        return normalize ? ent.getName() : null;
    }

    /**
     * Method called by the validator object
     * to ask attribute to verify that the default it has (if any) is
     * valid for such type.
     */
    public void validateDefault(InputProblemReporter rep, boolean normalize)
        throws XMLValidationException
    {
        String normStr = validateDefaultName(rep, normalize);
        if (normalize) {
            mDefValue.setValue(normStr);
        }

        // Ok, but was it declared?

        /* 03-Dec-2004, TSa: This is rather ugly -- need to know we
         *   actually really get a DTD reader, and DTD reader needs
         *   to expose a special method... but it gets things done.
         */
        EntityDecl ent = ((MinimalDTDReader) rep).findEntity(normStr);
        checkEntity(rep, normStr, ent);
    }
}
