/* Woodstox XML processor
 *
 * Copyright (c) 2004 Tatu Saloranta, tatu.saloranta@iki.fi
 *
 * Licensed under the License specified in file LICENSE, included with
 * the source code.
 * You may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ctc.wstx.dtd;

import java.text.MessageFormat;
import java.util.*;

import javax.xml.stream.Location;

import org.codehaus.stax2.validation.*;

import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.compat.JdkFeatures;
import com.ctc.wstx.exc.WstxException;
import com.ctc.wstx.exc.WstxValidationException;
import com.ctc.wstx.sr.AttributeCollector;
import com.ctc.wstx.util.DataUtil;
import com.ctc.wstx.util.StringUtil;
import com.ctc.wstx.util.StringVector;

/**
 * Woodstox implementation of {@link XMLValidator}; the class that
 * handles DTD-based validation.
 */
public class DTDValidator
    extends XMLValidator
{
    /*
    /////////////////////////////////////////////////////
    // Constants
    /////////////////////////////////////////////////////
     */

    /**
     * Estimated maximum depth of typical documents; used to allocate
     * the array for element stack
     */
    final static int DEFAULT_STACK_SIZE = 32;

    /**
     * Estimated maximum number of attributes for a single element
     */
    final static int EXP_MAX_ATTRS = 32;

    /**
     * Let's actually just reuse a local Map...
     */
    final static HashMap EMPTY_MAP = new HashMap();

    /*
    ///////////////////////////////////////
    // Configuration
    ///////////////////////////////////////
    */

    /**
     * DTD schema ({@link DTDSubsetImpl}) object that created this validator
     * instance.
     */
    final DTDSubset mSchema;

    /**
     * Validation context (owner) for this validator. Needed for adding
     * default attribute values, for example.
     */
    final ValidationContext mContext;

    /**
     * Map that contains element specifications from DTD; null if no
     * DOCTYPE declaration found.
     */
    final Map mElemSpecs;

    /**
     * General entities defined in DTD subsets; needed for validating
     * ENTITY/ENTITIES attributes.
     */
    final Map mGeneralEntities;

    /**
     * Flag that indicates whether parser wants the attribute values
     * to be normalized (according to XML specs) or not (which may be
     * more efficient, although not compliant with the specs)
     */
    boolean mNormAttrs;

    /**
     * Determines if identical problems (definition of the same element,
     * for example) should cause multiple error notifications or not:
     * if true, will get one error per instance, if false, only the first
     * one will get reported.
     */
    protected boolean mReportDuplicateErrors = false;

    /*
    ///////////////////////////////////////
    // Element definition/spec stack
    ///////////////////////////////////////
    */

    /**
     * This is the element that is currently being validated; valid
     * during
     * <code>validateElementStart</code>,
     * <code>validateAttribute</code>,
     * <code>validateElementAndAttributes</code> calls.
     */
    protected DTDElement mCurrElem = null;

    /**
     * Stack of element definitions matching the current active element stack.
     * Instances are elements definitions read from DTD.
     */
    protected DTDElement[] mElems = null;

    /**
     * Attribute definitions for attributes the current element may have
     */
    protected HashMap mCurrAttrDefs = null;

    /**
     * Bitset used for keeping track of required and defaulted attributes
     * for which values have been found.
     */
    protected BitSet mCurrSpecialAttrs = null;

    boolean mCurrHasAnyFixed = false;

    /**
     * Number of elements in {@link #mElems}.
     */
    protected int mElemCount = 0;

    protected StructValidator[] mValidators = null;

    /**
     * List of attribute declarations/specifications, one for each
     * attribute of the current element, for which there is a matching
     * value (either explicitly defined, or assigned via defaulting).
     */
    protected DTDAttribute[] mAttrSpecs = null;

    /**
     * Number of attribute specification Objects in
     * {@link #mAttrSpecs}; needed to store in case type information
     * is requested later on.
     */
    protected int mAttrCount = 1;

    /**
     * Index of the attribute of type ID, within current element's
     * attribute list. Track of this is kept separate from other
     * attribute since id attributes often need to be used for resolving
     * cross-references.
     */
    protected int mIdAttrIndex = -1;

    /*
    ///////////////////////////////////////
    // Id/idref state
    ///////////////////////////////////////
    */

    /**
     * Information about declared and referenced element ids (unique
     * ids that attributes may defined, as defined by DTD)
     */
    protected ElementIdMap mIdMap = null;

    /*
    ///////////////////////////////////////
    // Temporary helper objects
    ///////////////////////////////////////
    */

    final transient NameKey mTmpKey = new NameKey(null, null);

    /**
     * Reusable lazily instantiated BitSet; needed to keep track of
     * missing 'special' attributes (required ones, ones with default
     * values)
     */
    BitSet mTmpSpecialAttrs;

    /**
     * Temporary buffer attribute instances can share for validation
     * purposes
     */
    char[] mTmpAttrValueBuffer = null;

    /*
    ///////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////
    */

    public DTDValidator(DTDSubset schema, ValidationContext ctxt,
                        Map elemSpecs, Map genEntities)
    {
        mSchema = schema;
        mContext = ctxt;
        mElemSpecs = (elemSpecs == null || elemSpecs.size() == 0) ?
            Collections.EMPTY_MAP : elemSpecs;
        mGeneralEntities = genEntities;
        // By default, let's assume they are to be normalized (fully xml compliant)
        mNormAttrs = true;
        mElems = new DTDElement[DEFAULT_STACK_SIZE];
        mValidators = new StructValidator[DEFAULT_STACK_SIZE];
        mAttrSpecs = new DTDAttribute[EXP_MAX_ATTRS];
    }

    /*
    ///////////////////////////////////////
    // Configuration
    ///////////////////////////////////////
    */

    /**
     * Method that allows enabling/disabling attribute value normalization.
     * In general, readers by default enable normalization (to be fully xml
     * compliant),
     * whereas writers do not (since there is usually little to gain, if
     * anything -- it is even possible value may be written before validation
     * is called in some cases)
     */
    public void setAttrValueNormalization(boolean state) {
        mNormAttrs = state;
    }

    /*
    ///////////////////////////////////////
    // XMLValidator implementation
    ///////////////////////////////////////
    */

    public XMLValidationSchema getSchema() {
        return mSchema;
    }

    /**
     * Method called to update information about the newly encountered (start)
     * element. At this point namespace information has been resolved, but
     * no DTD validation has been done. Validator is to do these validations,
     * including checking for attribute value (and existence) compatibility.
     */
    public void validateElementStart(String localName, String uri, String prefix)
        throws XMLValidationException
    {
        /* Ok, need to find the element definition; if not found (or
         * only implicitly defined), need to throw the exception.
         */
        mTmpKey.reset(prefix, localName);

        DTDElement elem = (DTDElement) mElemSpecs.get(mTmpKey);

        /* Let's add the entry in (even if it's a null); this is necessary
         * to keep things in-sync if allowing graceful handling of validity
         * errors
         */
        int elemCount = mElemCount++;
        if (elemCount >= mElems.length) {
            mElems = (DTDElement[]) DataUtil.growArrayBy50Pct(mElems);
            mValidators = (StructValidator[]) DataUtil.growArrayBy50Pct(mValidators);
        }
        mElems[elemCount] = mCurrElem = elem;
        if (elem == null || !elem.isDefined()) {
            reportValidationProblem(ErrorConsts.ERR_VLD_UNKNOWN_ELEM, mTmpKey.toString());
        }

        // Is this element legal under the parent element?
        StructValidator pv = (elemCount > 0) ? mValidators[elemCount-1] : null;

        if (pv != null) {
            String msg = pv.tryToValidate(elem.getName());
            if (msg != null) {
                int ix = msg.indexOf("$END");
                String pname = mElems[elemCount-1].toString();
                if (ix >= 0) {
                    msg = msg.substring(0, ix) + "</"+pname+">"
                        +msg.substring(ix+4);
                }
                reportValidationProblem("Validation error, encountered element <"
                                        +elem.getName()+"> as a child of <"
                                        +pname+">: "+msg);
            }
        }

        // Ok, need to get the child validator, then:
        if (elem == null) {
            mValidators[elemCount] = null;
            mCurrAttrDefs = EMPTY_MAP;
            mCurrHasAnyFixed = false;
            mCurrSpecialAttrs = null;
        } else {
            mValidators[elemCount] = elem.getValidator();
            mCurrAttrDefs = elem.getAttributes();
            if (mCurrAttrDefs == null) {
                mCurrAttrDefs = EMPTY_MAP;
            }
            mCurrHasAnyFixed = elem.hasFixedAttrs();
            
            mAttrCount = 0;
            mIdAttrIndex = -2; // -2 as a "don't know yet" marker
            int specCount = elem.getSpecialCount();
            if (specCount == 0) {
                mCurrSpecialAttrs = null;
            } else {
                BitSet bs = mTmpSpecialAttrs;
                if (bs == null) {
                    mTmpSpecialAttrs = bs = new BitSet(specCount);
                } else {
                    bs.clear();
                }
                mCurrSpecialAttrs = bs;
            }
        }
    }

    public String validateAttribute(String localName, String uri,
                                    String prefix, String value)
        throws XMLValidationException
    {
        DTDAttribute attr = (DTDAttribute) mCurrAttrDefs.get(mTmpKey.reset(prefix, localName));
        if (attr == null) {
            // Only report error if not already covering from an error:
            if (mCurrElem == null) {
                return null;
            }
            reportValidationProblem(ErrorConsts.ERR_VLD_UNKNOWN_ATTR,
                                    mCurrElem.toString(), mTmpKey.toString());
        }
        int index = mAttrCount++;
        if (index >= mAttrSpecs.length) {
            mAttrSpecs = (DTDAttribute[]) DataUtil.growArrayBy50Pct(mAttrSpecs);
        }
        mAttrSpecs[index] = attr;
        if (mCurrSpecialAttrs != null) { // Need to mark that we got it
            int specIndex = attr.getSpecialIndex();
            if (specIndex >= 0) {
                mCurrSpecialAttrs.set(specIndex);
            }
        }
        String result = attr.validate(this, value, mNormAttrs);
        if (mCurrHasAnyFixed && attr.isFixed()) {
            String act = (result == null) ? value : result;
            String exp = attr.getDefaultValue();
            if (!act.equals(exp)) {
                reportValidationProblem("Value of attribute \""+attr+"\" (element <"+mCurrElem+">) not \""+exp+"\" as expected, but \""+act+"\"");
            }
        }
        return result;
    }

    public String validateAttribute(String localName, String uri,
                                    String prefix,
                                    char[] valueChars, int valueStart,
                                    int valueEnd)
        throws XMLValidationException
    {
        DTDAttribute attr = (DTDAttribute) mCurrAttrDefs.get(mTmpKey.reset(prefix, localName));
        if (attr == null) {
            // Only report error if not already covering from an error:
            if (mCurrElem == null) {
                return null;
            }
            reportValidationProblem(ErrorConsts.ERR_VLD_UNKNOWN_ATTR,
                                    mCurrElem.toString(), mTmpKey.toString());
        }
        int index = mAttrCount++;
        if (index >= mAttrSpecs.length) {
            mAttrSpecs = (DTDAttribute[]) DataUtil.growArrayBy50Pct(mAttrSpecs);
        }
        mAttrSpecs[index] = attr;
        if (mCurrSpecialAttrs != null) { // Need to mark that we got it
            int specIndex = attr.getSpecialIndex();
            if (specIndex >= 0) {
                mCurrSpecialAttrs.set(specIndex);
            }
        }
        String result = attr.validate(this, valueChars, valueStart, valueEnd, mNormAttrs);
        if (mCurrHasAnyFixed && attr.isFixed()) {
            String exp = attr.getDefaultValue();
            boolean match;
            if (result == null) {
                match = StringUtil.matches(exp, valueChars, valueStart, valueEnd);
            } else {
                match = exp.equals(result);
            }
            if (!match) {
                String act = (result == null) ? 
                    new String(valueChars, valueStart, valueEnd) : result;
                reportValidationProblem("Value of #FIXED attribute \""+attr+"\" (element <"+mCurrElem+">) not \""+exp+"\" as expected, but \""+act+"\"");
            }
        }
        return result;
    }
    
    public int validateElementAndAttributes()
        throws XMLValidationException
    {
        // Ok: are we fine with the attributes?
        DTDElement elem = mCurrElem;
        if (elem == null) { // had an error, most likely no such element defined...
            // need to just return, nothing to do here
            return XMLValidator.CONTENT_ALLOW_ANY_TEXT;
        }
        
        // Any special attributes missing?
        if (mCurrSpecialAttrs != null) {
            BitSet specBits = mCurrSpecialAttrs;
            int specCount = elem.getSpecialCount();
            int ix = specBits.nextClearBit(0);
            while (ix < specCount) { // something amiss!
                List specAttrs = elem.getSpecialAttrs();
                DTDAttribute attr = (DTDAttribute) specAttrs.get(ix);

                if (attr.isRequired()) {
                    reportValidationProblem("Required attribute '"+attr+"' missing from element <"+elem+">");
                }
                // Ok, if not required, should have default value!
                String def = attr.getDefaultValue();
                if (def == null) {
                    throw new Error("Internal error: null default attribute value");
                }
                NameKey an = attr.getName();
                // Ok, do we need to find the URI?
                String prefix = an.getPrefix();
                String uri = "";
                if (prefix != null && prefix.length() > 0) {
                    uri = mContext.getNamespaceURI(prefix);
                    // Can not map to empty NS!
                    if (uri == null || uri.length() == 0) {
                        reportValidationProblem("Unbound namespace prefix '"+prefix+"' for default attribute "+attr);
                        // May continue if we don't throw errors, just collect them to a list
                        uri = "";
                    }
                }
                int defIx = mContext.addDefaultAttribute(an.getLocalName(),
                                                         uri, prefix, def);
                if (defIx < 0) {
                    throw new Error("Internal error: tried to add default attribute "+attr+", but value for it already existed");
                }

                if (defIx >= 0) { // -1 means it was not added...
                    while (defIx >= mAttrSpecs.length) {
                        mAttrSpecs = (DTDAttribute[]) DataUtil.growArrayBy50Pct(mAttrSpecs);
                    }
                    /* Any intervening empty slots? (can happen if other
                     * validators add default attributes...)
                     */
                    while (mAttrCount < defIx) {
                        mAttrSpecs[mAttrCount++] = null;
                    }
                    mAttrSpecs[defIx] = attr;
                    mAttrCount = defIx+1;
                }
                ix = specBits.nextClearBit(ix+1);
            }
        }

        return elem.getAllowedContent();
    }

    /**
     * @return Validation state that should be effective for the parent
     *   element state
     */
    public int validateElementEnd(String localName, String uri, String prefix)
        throws XMLValidationException
    {
        // First, let's remove the top:
        int ix = --mElemCount;
        DTDElement closingElem = mElems[ix];
        mElems[ix] = null;
        StructValidator v = mValidators[ix];
        mValidators[ix] = null;

        // Validation?
        if (v != null) {
            String msg = v.fullyValid();
            if (msg != null) {
                reportValidationProblem("Validation error, element </"
                                        +closingElem+">: "+msg);
            }
        }

        // Then let's get info from parent, if any
        if (ix < 1) { // root element closing..
            /* 02-Oct-2004, TSa: Now we can also check that all id references
             *    pointed to ids that actually are defined
             */
            if (mIdMap != null) {
                ElementId ref = mIdMap.getFirstUndefined();
                if (ref != null) { // problem!
                    reportValidationProblem("Undefined id '"+ref.getId()
                                            +"': referenced from element <"
                                            +ref.getElemName()+">, attribute '"
                                            +ref.getAttrName()+"'",
                                            ref.getLocation());
                }
            }

            // doesn't really matter; epilog/prolog differently handled:
            return XMLValidator.CONTENT_ALLOW_WS;
        }
        return mElems[ix-1].getAllowedContent();
    }

    public void validateText(String text, boolean lastTextSegment)
        throws XMLValidationException
    {
        /* This method is a NOP, since basic DTD has no mechanism for
         * validating textual content.
         */
    }

    public void validateText(char[] cbuf, int textStart, int textEnd,
                             boolean lastTextSegment)
        throws XMLValidationException
    {
        /* This method is a NOP, since basic DTD has no mechanism for
         * validating textual content.
         */
    }

    /*
    ///////////////////////////////////////
    // Attribute info access
    ///////////////////////////////////////
    */

    // // // Access to type info

    public String getAttributeType(int index)
    {
        return mAttrSpecs[index].getValueTypeString();
    }    

    /**
     * Method for finding out the index of the attribute (collected using
     * the attribute collector; having DTD-derived info in same order)
     * that is of type ID. DTD explicitly specifies that at most one
     * attribute can have this type for any element.
     * 
     * @return Index of the attribute with type ID, in the current
     *    element, if one exists: -1 otherwise
     */
    public int getIdAttrIndex()
    {
        // Let's figure out the index only when needed
        int ix = mIdAttrIndex;
        if (ix == -2) {
            ix = -1;
            if (mCurrElem != null) {
                DTDAttribute idAttr = mCurrElem.getIdAttribute();
                if (idAttr != null) {
                    DTDAttribute[] attrs = mAttrSpecs;
                    for (int i = 0, len = attrs.length; i < len; ++i) {
                    if (attrs[i] == idAttr) {
                        ix = i;
                        break;
                    }
                    }
                }
            }
            mIdAttrIndex = ix;
        }
        return ix;
    }

    /**
     * Method for finding out the index of the attribute (collected using
     * the attribute collector; having DTD-derived info in same order)
     * that is of type NOTATION. DTD explicitly specifies that at most one
     * attribute can have this type for any element.
     * 
     * @return Index of the attribute with type NOTATION, in the current
     *    element, if one exists: -1 otherwise
     */
    public int getNotationAttrIndex()
    {
        /* If necessary, we could find this index when resolving the
         * element, could avoid linear search. But who knows how often
         * it's really needed...
         */
        for (int i = 0, len = mAttrCount; i < len; ++i) {
            if (mAttrSpecs[i].typeIsNotation()) {
                return i;
            }
        }
        return -1;
    }

    /*
    ///////////////////////////////////////
    // Package methods, accessors
    ///////////////////////////////////////
    */

    /**
     * Name of current element on the top of the element stack.
     */
    NameKey getElemName() {
        DTDElement elem = mElems[mElemCount-1];
        return elem.getName();
    }

    Location getLocation() {
        return mContext.getValidationLocation();
    }

    ElementIdMap getIdMap() {
        if (mIdMap == null) {
            mIdMap = new ElementIdMap();
        }
        return mIdMap;
    }

    Map getEntityMap() {
        return mGeneralEntities;
    }

    char[] getTempAttrValueBuffer(int neededLength)
    {
        if (mTmpAttrValueBuffer == null
            || mTmpAttrValueBuffer.length < neededLength) {
            int size = (neededLength < 100) ? 100 : neededLength;
            mTmpAttrValueBuffer = new char[size];
        }
        return mTmpAttrValueBuffer;
    }

    /*
    ///////////////////////////////////////
    // Package methods, error handling
    ///////////////////////////////////////
    */

    /**
     * Method called to report validity problems; depending on mode, will
     * either throw an exception, or add a problem notification to the
     * list of problems.
     */
    void reportValidationProblem(String msg)
        throws XMLValidationException
    {
        doReportProblem(msg, null);
    }

    void reportValidationProblem(String msg, Location loc)
        throws XMLValidationException
    {
        doReportProblem(msg, loc);
    }

    void reportValidationProblem(String format, String arg)
        throws XMLValidationException
    {
        doReportProblem(MessageFormat.format(format, new Object[] { arg }),
                        null);
    }

    void reportValidationProblem(String format, String arg1, String arg2)
        throws XMLValidationException
    {
        doReportProblem(MessageFormat.format(format, new Object[] { arg1, arg2 }),
                        null);
    }

    /*
    ///////////////////////////////////////
    // Private methods
    ///////////////////////////////////////
    */

    protected void doReportProblem(String msg, Location loc)
        throws XMLValidationException
    {
        if (loc == null) {
            loc = getLocation();
        }
        throw WstxValidationException.create(msg, loc, XMLValidationProblem.SEVERITY_ERROR);
    }
}
