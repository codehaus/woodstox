package com.ctc.wstx.stax.ns;

import java.util.Iterator;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import com.ctc.wstx.stax.cfg.ErrorConsts;
import com.ctc.wstx.stax.dtd.*;
import com.ctc.wstx.stax.stream.StreamScanner;
import com.ctc.wstx.util.DataUtil;

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

    final transient NameKey mTmpKey = new NameKey(null, null);

    protected DTDElement[] mSpecStack = null;
    protected int mSpecCount = 0;

    /*
    //////////////////////////////////////////////////
    // Life-cycle (create, update state)
    //////////////////////////////////////////////////
     */

    public VNonNsInputElementStack(int initialSize)
    {
        super(initialSize);
    }

    /**
     * Method called by the validating stream reader if and when it has
     * read internal and/or external DTD subsets, and has thus parsed
     * element specifications.
     */
    public void setElementSpecs(Map elemSpecs) {
        mElemSpecs = elemSpecs;
        mSpecStack = new DTDElement[32];
    }

    /**
     * @return Validation state that should be effective for the parent
     *   element state
     */
    public int pop()
    {
        super.pop();

        if (mSpecStack == null) {
            return CONTENT_ALLOW_MIXED;
        }

        // First, let's remove the top:
        int ix = --mSpecCount;
        mSpecStack[ix] = null;
        // Then let's get info from parent, if any
        return (ix > 0) ?
            mSpecStack[ix-1].getAllowedContent() : CONTENT_ALLOW_NON_MIXED;
    }

    /**
     * Method called to update information about top of the stack, with
     * attribute information passed in. Will resolve namespace references,
     * and update namespace stack with information.
     *
     * @return Validation state that should be effective for the fully
     *   resolved element context
     */
    public int resolveElem(StreamScanner sc, boolean internNsURIs)
        throws XMLStreamException
    {
        super.resolveElem(sc, internNsURIs);

        // Might not have a DTD?
        if (mElemSpecs == null) {
            return CONTENT_ALLOW_MIXED;
        }

        /* Ok, need to find the element definition; if not found (or
         * only implicitly defined), need to throw the exception.
         */
        String elemName = mElements[mSize-1];
        mTmpKey.reset(null, elemName);
        DTDElement elem = (DTDElement) mElemSpecs.get(mTmpKey);
        if (elem == null || !elem.isDefined()) {
            sc.throwParseError(ErrorConsts.ERR_VLD_UNKNOWN_ELEM, elemName);
        }
        // At this point, mSize has been increased...
        if (mSpecCount >= mSpecStack.length) {
            mSpecStack = (DTDElement[]) DataUtil.growArrayBy50Pct(mSpecStack);
        }
        mSpecStack[mSpecCount++] = elem;
        return elem.getAllowedContent();
    }
}
