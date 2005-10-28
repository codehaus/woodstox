package com.ctc.wstx.sr;

import java.util.*;

import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.validation.XMLValidator;

import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.dtd.DTDElement;
import com.ctc.wstx.dtd.ElementValidator;
import com.ctc.wstx.dtd.NameKey;
import com.ctc.wstx.exc.WstxException;
import com.ctc.wstx.util.SymbolTable;

/**
 * Sub-class of {@link NsInputElementStack} that adds basic support for
 * (DTD-based) validation of XML documents.
 */
public class VNsInputElementStack
    extends NsInputElementStack
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

    public VNsInputElementStack(int initialSize,
                                String prefixXml, String prefixXmlns,
                                boolean normAttrs)
    {
        super(initialSize, prefixXml, prefixXmlns, normAttrs);
    }

    public void beforeRoot() {
        if (mElemSpecs == null) { // No DOCTYPE
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
            mElemSpecs = Collections.EMPTY_MAP;
        } else {
            mElemSpecs = elemSpecs;
        }
        mValidator = new ElementValidator(mReporter, symbols, true,
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
            XMLValidator.CONTENT_ALLOW_ANY_TEXT : mValidator.pop(mReporter);
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
        mTmpKey.reset(mElements[mSize-(ENTRY_SIZE - IX_PREFIX)],
                      mElements[mSize-(ENTRY_SIZE - IX_LOCALNAME)]);

        /* It's ok not to have elements... but not when trying to validate;
         * and we are always validating if we end up here.
         */
        /* 30-Sep-2005, TSa: Actually, if there was no DTD, let's consider
         *   this ok. We should log a warning though
         */
        if (mValidator == null) { // no DTD in use
            return XMLValidator.CONTENT_ALLOW_ANY_TEXT;
        }

        DTDElement elem = (DTDElement) mElemSpecs.get(mTmpKey);
        mCurrElem = elem;
        if (elem == null || !elem.isDefined()) {
            mReporter.throwParseError(ErrorConsts.ERR_VLD_UNKNOWN_ELEM, mTmpKey.toString());
        }
        return mValidator.resolveElem(mReporter, elem, mNamespaces);
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
    public String getAttributeType(int index) {
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
