package com.ctc.wstx.dtd;

import java.util.Map;

import javax.xml.stream.Location;

import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.ent.EntityDecl;
import com.ctc.wstx.exc.WstxException;
import com.ctc.wstx.sr.AttributeCollector;
import com.ctc.wstx.sr.InputProblemReporter;
import com.ctc.wstx.util.SymbolTable;
import com.ctc.wstx.util.TextBuilder;
import com.ctc.wstx.util.WordResolver;

import com.ctc.wstx.sr.StreamScanner;

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
        while (start <= last && StreamScanner.isSpaceChar(ch[start])) {
            ++start;
        }

        // Empty value?
        if (last < start) {
            reportParseError(v, "Empty ENTITIES value");
        }
        while (last > start && StreamScanner.isSpaceChar(ch[last])) {
            --last;
        }

        // Ok; now start points to first, last to last char (both inclusive)
        SymbolTable st = v.getSymbolTable();
        Map entMap = v.getEntityMap();
        String idStr = null;
        StringBuffer sb = null;

        while (start <= last) {
            // Ok, need to check char validity, and also calc hash code:
            char c = ch[start];
            if (!StreamScanner.isNameStartChar(c) && c != ':') {
                reportInvalidChar(v, c, "not valid as the first ENTITIES character");
            }
            int hash = (int) c;
            int i = start+1;
            for (; i <= last; ++i) {
                c = ch[i];
                if (StreamScanner.isSpaceChar(c)) {
                    break;
                }
                if (!StreamScanner.isNameChar(c)) {
                    reportInvalidChar(v, c, "not valid as an ENTITIES character");
                }
                hash = (hash * 31) + (int) c;
            }

            EntityDecl ent = findEntityDecl(v, ch, start, (i - start), hash);
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
            while (start <= last && StreamScanner.isSpaceChar(ch[start])) {
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
