package com.ctc.wstx.sr;

import java.util.*;

import javax.xml.stream.XMLStreamException;

import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.dtd.DTDElement;
import com.ctc.wstx.dtd.ElementValidator;
import com.ctc.wstx.dtd.NameKey;
import com.ctc.wstx.exc.WstxException;
import com.ctc.wstx.util.SymbolTable;

/**
 * Sub-class of {@link NonNsInputElementStack} used when operating in
 * non-namespace-aware mode but validating contents against DTD
 */
public final class VNonNsInputElementStack
    extends NonNsInputElementStack
{
    /**
     * Map that contains element specifications from DTD; null for
     * non-validating parsers.
     */
    protected Map mElemSpecs;

    ElementValidator mValidator = null;

    /**
     * DTD definition for the current element
     */
    DTDElement mCurrElem;

    private final transient NameKey mTmpKey = new NameKey(null, null);

    /*
    //////////////////////////////////////////////////
    // Life-cycle (create, update state)
    //////////////////////////////////////////////////
     */

    public VNonNsInputElementStack(int initialSize, boolean normAttrs)
    {
        super(initialSize, normAttrs);
    }

    /**
     * Method called by the validating stream reader if and when it has
     * read internal and/or external DTD subsets, and has thus parsed
     * element specifications.
     */
    public void setElementSpecs(Map elemSpecs, SymbolTable symbols,
                                boolean normAttrs, Map generalEntities)
    {
        mElemSpecs = elemSpecs;
        mValidator = new ElementValidator(mReporter, symbols, false,
                                          generalEntities,
                                          mAttrCollector, normAttrs);
    }

    /**
     * @return Validation state that should be effective for the parent
     *   element state
     */
    public int pop()
        throws WstxException
    {
        super.pop();
        return (mValidator == null) ?
            CONTENT_ALLOW_MIXED : mValidator.pop(mReporter);
    }

    /**
     * Method called to update information about top of the stack, with
     * attribute information passed in. Will resolve namespace references,
     * and update namespace stack with information.
     *
     * @return Validation state that should be effective for the fully
     *   resolved element context
     */
    public int resolveElem(boolean internNsURIs)
        throws WstxException
    {
        super.resolveElem(internNsURIs);
        
        /* Ok, need to find the element definition; if not found (or
         * only implicitly defined), need to throw the exception.
         */
        mTmpKey.reset(null, mElements[mSize-1]);

        /* It's ok not to have elements... but not when trying to validate;
         * and we are always validating if we end up here.
         */
        DTDElement elem;
        if (mElemSpecs == null) {
            elem = null; // will trigger an error later on
        } else {
            elem = (DTDElement) mElemSpecs.get(mTmpKey);
        }
        mCurrElem = elem;
        if (elem == null || !elem.isDefined()) {
            mReporter.throwParseError(ErrorConsts.ERR_VLD_UNKNOWN_ELEM, mTmpKey);
        }
        return mValidator.resolveElem(mReporter, elem, null);
    }

    /*
    //////////////////////////////////////////////////
    // Overridden public methods
    //////////////////////////////////////////////////
     */

    /**
     * Input element stack has to ask validator about this data; validator
     * keeps track of attribute declarations for the current element
     */
    public String getAttributeType(int index)
    {
         return mValidator.getAttributeType(index);
    }

    public int getIdAttributeIndex() {
        return mValidator.getIdAttrIndex();
    }

    public int getNotationAttributeIndex() {
        return mValidator.getNotationAttrIndex();
    }
}
