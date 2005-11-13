package com.ctc.wstx.sr;

import javax.xml.stream.XMLStreamException;

import java.util.*;

import org.codehaus.stax2.validation.XMLValidator;

import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.dtd.ElementValidator;
import com.ctc.wstx.exc.WstxException;
import com.ctc.wstx.util.SymbolTable;

/**
 * Sub-class of {@link NonNsInputElementStack} used when operating in
 * non-namespace-aware mode but validating contents against DTD
 */
public final class VNonNsInputElementStack
    extends NonNsInputElementStack
{
    ElementValidator mValidator = null;

    /*
    //////////////////////////////////////////////////
    // Life-cycle (create, update state)
    //////////////////////////////////////////////////
     */

    public VNonNsInputElementStack(int initialSize,
                                   boolean normAttrs, boolean internNsURIs)
    {
        super(initialSize, normAttrs, internNsURIs);
    }

    public void beforeRoot()
    {
        if (mValidator == null) { // No DOCTYPE
            /* It's ok to miss it, but it may not be what caller wants. Either
             * way, let's pass the info and continue
             */
            mReporter.reportProblem(ErrorConsts.WT_DT_DECL, ErrorConsts.W_MISSING_DTD);
        }
    }

    /**
     * Method called by the validating stream reader if and when it has
     * read internal and/or external DTD subsets, and has thus parsed
     * element specifications.
     */
    public void setElementSpecs(Map elemSpecs, SymbolTable symbols,
                                boolean normAttrs, Map generalEntities)
    {
        /* 30-Sep-2005, TSa: This gets called if there was a DOCTYPE
         *   declaration..
         */
        if (elemSpecs == null) { // no DTD
            elemSpecs = Collections.EMPTY_MAP;
        }
        mValidator = new ElementValidator(mReporter, symbols, elemSpecs, false,
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
            XMLValidator.CONTENT_ALLOW_ANY_TEXT : mValidator.pop();
    }

    /**
     * Method called to update information about top of the stack, with
     * attribute information passed in. Will resolve namespace references,
     * and update namespace stack with information.
     *
     * @return Validation state that should be effective for the fully
     *   resolved element context
     */
    public int resolveElem()
        throws WstxException
    {
        super.resolveElem();

        /* 30-Sep-2005, TSa: Actually, if there was no DTD, let's consider
         *   this ok. We should log a warning though
         */
        if (mValidator == null) { // no DTD in use
            return XMLValidator.CONTENT_ALLOW_ANY_TEXT;
        }
        return mValidator.validateElem(null, mElements[mSize-1], null);
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
        return (mValidator == null) ? null : 
            mValidator.getAttributeType(index);
    }

    public int getIdAttributeIndex() {
        return (mValidator == null) ? -1 : 
            mValidator.getIdAttrIndex();
    }

    public int getNotationAttributeIndex() {
        return (mValidator == null) ? -1 :
            mValidator.getNotationAttrIndex();
    }
}
