package com.ctc.wstx.dtd;

import javax.xml.stream.Location;

import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.exc.WstxException;
import com.ctc.wstx.sr.AttributeCollector;
import com.ctc.wstx.sr.InputProblemReporter;
import com.ctc.wstx.util.TextBuilder;
import com.ctc.wstx.util.WordResolver;

import com.ctc.wstx.sr.StreamScanner;

/**
 * Specific attribute class for attributes that contain (unique)
 * identifiers.
 */
public final class DTDIdAttr
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
    public DTDIdAttr(NameKey name, int defValueType, int specIndex)
    {
        super(name, defValueType, null, specIndex);
    }

    public DTDAttribute cloneWith(int specIndex)
    {
        return new DTDIdAttr(mName, mDefValueType, specIndex);
    }

    /*
    ///////////////////////////////////////////////////
    // Public API
    ///////////////////////////////////////////////////
     */

    public int getValueType() {
        return TYPE_ID;
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

        // Let's trim leading white space first...
        while (start <= last && StreamScanner.isSpaceChar(ch[start])) {
            ++start;
        }

        // No id?
        if (start > last) {
            reportParseError(v, "Empty ID value");
        }

        while (last > start && StreamScanner.isSpaceChar(ch[last])) {
            --last;
        }

        // Ok, need to check char validity, and also calc hash code:
        char c = ch[start];
        if (!StreamScanner.isNameStartChar(c) && c != ':') {
            reportInvalidChar(v, c, "not valid as the first ID character");
        }
        int hash = (int) c;
        for (int i = start+1; i <= last; ++i) {
            c = ch[i];
            if (!StreamScanner.isNameChar(c)) {
                reportInvalidChar(v, c, "not valid as an ID character");
            }
            hash = (hash * 31) + (int) c;
        }

        // Either way, we do need to validate characters, and calculate hash
        ElementIdMap m = v.getIdMap();
        NameKey elemName = v.getElemName();
        Location loc = v.getLocation();
        ElementId id = m.addDefined(ch, start, (last - start + 1), hash,
                                    loc, elemName, mName);

        // We can detect dups by checking if Location is the one we passed:
        if (id.getLocation() != loc) {
            reportParseError(v, "Duplicate id '"+id.getId()+"', first declared at "
                             +id.getLocation());
        }

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
        throws WstxException
    {
        // Should never get called
        throw new Error(ErrorConsts.ERR_INTERNAL);
    }

    /*
    ///////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////
     */

}
