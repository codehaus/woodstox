package com.ctc.wstx.dtd;

import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.stream.Location;

import org.codehaus.stax2.validation.XMLValidationException;

import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.ent.EntityDecl;
import com.ctc.wstx.io.WstxInputData;
import com.ctc.wstx.sr.InputProblemReporter;
import com.ctc.wstx.util.WordResolver;

/**
 * Specific attribute class for attributes that contain (unique)
 * identifiers.
 */
public final class DTDEntitiesAttr
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
    public DTDEntitiesAttr(NameKey name, int defValueType, String defValue,
                           int specIndex)
    {
        super(name, defValueType, defValue, specIndex);
    }

    public DTDAttribute cloneWith(int specIndex)
    {
        return new DTDEntitiesAttr(mName, mDefValueType, mDefValue, specIndex);
    }

    /*
    ///////////////////////////////////////////////////
    // Public API
    ///////////////////////////////////////////////////
     */

    public int getValueType() {
        return TYPE_ENTITIES;
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
     * 
     */
    public String validate(DTDValidator v, char[] cbuf, int start, int end, boolean normalize)
        throws XMLValidationException
    {
        /* Let's skip leading/trailing white space, even if we are not
         * to normalize visible attribute value. This allows for better
         * round-trip handling (no changes for physical value caller
         * gets), but still allows succesful validation.
         */
        while (start < end && WstxInputData.isSpaceChar(cbuf[start])) {
            ++start;
        }

        // Empty value?
        if (start >= end) {
            return reportValidationProblem(v, "Empty ENTITIES value");
        }
        --end; // so that it now points to the last char
        while (end > start && WstxInputData.isSpaceChar(cbuf[end])) {
            --end;
        }

        // Ok; now start points to first, last to last char (both inclusive)
        String idStr = null;
        StringBuffer sb = null;

        while (start <= end) {
            // Ok, need to check char validity, and also calc hash code:
            char c = cbuf[start];
            if (!WstxInputData.is11NameStartChar(c) && c != ':') {
                return reportInvalidChar(v, c, "not valid as the first ENTITIES character");
            }
            int hash = (int) c;
            int i = start+1;
            for (; i <= end; ++i) {
                c = cbuf[i];
                if (WstxInputData.isSpaceChar(c)) {
                    break;
                }
                if (!WstxInputData.is11NameChar(c)) {
                    return reportInvalidChar(v, c, "not valid as an ENTITIES character");
                }
                hash = (hash * 31) + (int) c;
            }

            EntityDecl ent = findEntityDecl(v, cbuf, start, (i - start), hash);
            // only returns if entity was found...
            
            // Can skip the trailing space char (if there was one)
            start = i+1;

            /* When normalizing, we can possibly share id String, or
             * alternatively, compose normalized String if multiple
             */
            if (normalize) {
                if (idStr == null) { // first idref
                    idStr = ent.getName();
                } else {
                    if (sb == null) {
                        sb = new StringBuffer(idStr);
                    }
                    idStr = ent.getName();
                    sb.append(' ');
                    sb.append(idStr);
                }
            }

            // Ok, any white space to skip?
            while (start <= end && WstxInputData.isSpaceChar(cbuf[start])) {
                ++start;
            }
        }

        if (normalize) {
            if (sb != null) {
                idStr = sb.toString();
            }
            return idStr;
        }

        return null;
    }

    /**
     * Method called by the {@link DTDValidator}
     * to ask attribute to verify that the default it has (if any) is
     * valid for such type.
     */
    public void validateDefault(InputProblemReporter rep, boolean normalize)
        throws XMLValidationException
    {
        String normStr = validateDefaultNames(rep, true);
        if (normalize) {
            mDefValue = normStr;
        }

        // Ok, but were they declared?

        /* Performance really shouldn't be critical here (only called when
         * parsing DTDs, which get cached) -- let's just
         * tokenize using standard StringTokenizer
         */
        StringTokenizer st = new StringTokenizer(normStr);
        /* !!! 03-Dec-2004, TSa: This is rather ugly -- need to know we
         *   actually really get a DTD reader, and DTD reader needs
         *   to expose a special method... but it gets things done.
         */
        MinimalDTDReader dtdr = (MinimalDTDReader) rep;
        while (st.hasMoreTokens()) {
            String str = st.nextToken();
            EntityDecl ent = dtdr.findEntity(str);
            // Needs to exists, and be an unparsed entity...
            checkEntity(rep, normStr, ent);
        }
    }

    /*
    ///////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////
     */

}
