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

import java.util.*;

import javax.xml.stream.Location;

import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.cfg.InputConfigFlags;
import com.ctc.wstx.compat.JdkFeatures;
import com.ctc.wstx.exc.WstxException;
import com.ctc.wstx.sr.AttributeCollector;
import com.ctc.wstx.sr.InputProblemReporter;
import com.ctc.wstx.util.DataUtil;
import com.ctc.wstx.util.StringVector;
import com.ctc.wstx.util.SymbolTable;

/**
 * Class that will be instantiated by specialized instances of
 * {@link com.ctc.wstx.sr.InputElementStack}, specifically the
 * validating ones (whether
 * namespace-aware or not).
 */
public class ElementValidator
    implements
    InputConfigFlags
{
    final static int DEFAULT_STACK_SIZE = 32;

    /**
     * Let's actually just reuse a local Map...
     */
    final static HashMap EMPTY_MAP = new HashMap();

    /*
    ///////////////////////////////////////
    // Configuration
    ///////////////////////////////////////
    */

    final InputProblemReporter mReporter;

    /**
     * Symbol table used for resolving entity names
     */
    final SymbolTable mSymbols;

    /**
     * General entities defined in DTD subsets; needed for validating
     * ENTITY/ENTITIES attributes.
     */
    final Map mGeneralEntities;

    final boolean mNsAware;

    /**
     * We need to work with the attribute collector instance,
     * when validating attributes, adding default values and
     * so on.
     */
    final AttributeCollector mAttrCollector;

    /**
     * Flag that indicates whether parser wants the attribute values
     * to be normalized (according to XML specs) or not (which may be
     * more efficient, although not strictly legal according to specs)
     */
    final boolean mNormAttrs;

    /*
    ///////////////////////////////////////
    // Element definition/spec stack
    ///////////////////////////////////////
    */

    /**
     * Stack of element definitions read from DTD.
     */
    protected DTDElement[] mElemStack = null;

    protected int mElemCount = 0;

    /**
     * Stack of content structure validators, on matching indexes
     * with element specification objects.
     */
    protected StructValidator[] mValidators = null;

    /*
    ///////////////////////////////////////
    // Collected other state information
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
    BitSet mTmpSpecs;

    /*
    ///////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////
    */

    public ElementValidator(InputProblemReporter rep, SymbolTable symbols,
                            boolean nsAware, Map genEntities,
                            AttributeCollector ac, boolean normAttrs)
    {
        mReporter = rep;
        mSymbols = symbols;
        mNsAware = nsAware;
        mGeneralEntities = genEntities;
        mAttrCollector = ac;
        mNormAttrs = normAttrs;
        mElemStack = new DTDElement[DEFAULT_STACK_SIZE];
        mValidators = new StructValidator[DEFAULT_STACK_SIZE];
    }

    /*
    ///////////////////////////////////////
    // Public API
    ///////////////////////////////////////
    */

    /**
     * @return Validation state that should be effective for the parent
     *   element state
     */
    public int pop(InputProblemReporter rep)
        throws WstxException
    {
        // First, let's remove the top:
        int ix = --mElemCount;
        DTDElement closingElem = mElemStack[ix];
        mElemStack[ix] = null;
        StructValidator v = mValidators[ix];
        mValidators[ix] = null;

        // Validation?
        if (v != null) {
            String msg = v.fullyValid();
            if (msg != null) {
                rep.throwParseError("Validation error, element </"
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
                    throwParseError(ref.getLocation(), "Undefined id '"+ref.getId()
                                    +"': referenced from element <"
                                    +ref.getElemName()+">, attribute '"
                                    +ref.getAttrName()+"'");
                }
            }

            // doesn't really matter; epilog/prolog differently handled:
            return CONTENT_ALLOW_NON_MIXED;
        }
        return mElemStack[ix-1].getAllowedContent();
    }

    /**
     * Method called to update information about the newly encountered (start)
     * element. At this point namespace information has been resolved, but
     * no DTD validation has been done. Validator is to do these validations,
     * including checking for attribute value (and existence) compatibility.
     *
     * @param rep Reporter instance that can be used to report back problems
     *   (via exceptions)
     * @param elem Element to resolve
     * @param ns (optional) Data structure that contains all currently
     *   active namespace declarations; may be needed for resolving namespaced
     *   default attributes' namespace URIs.
     *
     * @return Validation state that should be effective for the fully
     *   resolved element context
     */
    public int resolveElem(InputProblemReporter rep, DTDElement elem, StringVector ns)
        throws WstxException
    {
        int elemCount = mElemCount++;
        if (elemCount >= mElemStack.length) {
            mElemStack = (DTDElement[]) DataUtil.growArrayBy50Pct(mElemStack);
            mValidators = (StructValidator[]) DataUtil.growArrayBy50Pct(mValidators);
        }
        mElemStack[elemCount] = elem;

        /* Ok; we have updated element spec stack: then we need to handle
         * attributes:
         */
        StringVector attrNames = mAttrCollector.getNameList();
        HashMap attrMap = elem.getAttributes();
        if (attrMap == null) {
            attrMap = EMPTY_MAP;
        }
        
        BitSet specBits;
        int specCount = elem.getSpecialCount();
        
        if (specCount == 0) {
            specBits = null;
        } else {
            specBits = mTmpSpecs;
            if (specBits == null) {
                mTmpSpecs = specBits = new BitSet(specCount);
            } else {
                specBits.clear();
            }
        }

        NameKey tmpKey = mTmpKey;
        boolean validateAttrs = elem.attrsNeedValidation();
        boolean anyFixed = elem.hasFixedAttrs();
        
        for (int i = 0, j = 0, len = attrNames.size(); i < len; ) {
            if (mNsAware) {
                tmpKey.reset(attrNames.getString(i), attrNames.getString(i+1));
                i += 2;
            } else {
                tmpKey.reset(null, attrNames.getString(i));
                ++i;
            }
            DTDAttribute attr = (DTDAttribute) attrMap.get(tmpKey);
            if (attr == null) {
                rep.throwParseError(ErrorConsts.ERR_VLD_UNKNOWN_ATTR,
                                   elem.toString(), tmpKey.toString());
            }
            if (specBits != null) { // Need to mark that we got it
                int specIndex = attr.getSpecialIndex();
                if (specIndex >= 0) {
                    specBits.set(specIndex);
                }
            }
            
            // Need to validate?
            if (validateAttrs) {
                attr.validate(this, mNormAttrs, mAttrCollector, j);
            }
            if (anyFixed && attr.isFixed()) {
                String exp = attr.getDefaultValue();
                String act = mAttrCollector.getValue(j);
                if (!act.equals(exp)) {
                    rep.throwParseError("Value of attribute \""+attr+"\" (element <"+elem+">) not \""+exp+"\" as expected, but \""+act+"\"");
                }
            }
            ++j;
        }
        
        // Any special attributes missing?
        if (specBits != null) {
            int ix = specBits.nextClearBit(0);
            while (ix < specCount) { // something amiss!
                List specAttrs = elem.getSpecialAttrs();
                DTDAttribute attr = (DTDAttribute) specAttrs.get(ix);

                if (attr.isRequired()) {
                    rep.throwParseError("Required attribute '"+attr+"' missing from element <"+elem+">");
                }
                // Ok, if not required, should have default value!
                String def = attr.getDefaultValue();
                if (def == null) {
                    throw new Error("Internal error: null default attribute value");
                }
                NameKey an = attr.getName();
                mAttrCollector.addDefaultAttr(rep, ns, an.getPrefix(),
                                              an.getLocalName(), def);
                ix = specBits.nextClearBit(ix+1);
            }
        }

        // Ok, how about structural validation?
        StructValidator pv = (elemCount > 0) ? mValidators[elemCount-1] : null;

        if (pv != null) {
            String msg = pv.tryToValidate(elem.getName());
            if (msg != null) {
                int ix = msg.indexOf("$END");
                String pname = mElemStack[elemCount-1].toString();
                if (ix >= 0) {
                    msg = msg.substring(0, ix) + "</"+pname+">"
                        +msg.substring(ix+4);
                }
                rep.throwParseError("Validation error, encountered element <"
                                   +elem.getName()+"> as child of <"
                                   +pname+">: "+msg);
            }
        }

        // Ok, need to get the child validator, then:
        mValidators[elemCount] = elem.getValidator();

        return elem.getAllowedContent();
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
        DTDElement elem = mElemStack[mElemCount-1];
        return elem.getName();
    }

    InputProblemReporter getReporter() {
        return mReporter;
    }

    Location getLocation() {
        return mReporter.getLocation();
    }

    ElementIdMap getIdMap() {
        if (mIdMap == null) {
            mIdMap = new ElementIdMap();
        }
        return mIdMap;
    }

    SymbolTable getSymbolTable() {
        return mSymbols;
    }

    Map getEntityMap() {
        return mGeneralEntities;
    }

    /*
    ///////////////////////////////////////
    // Package methods, error handling
    ///////////////////////////////////////
    */

    void throwParseError(String msg) throws WstxException {
        mReporter.throwParseError(msg);
    }

    void throwParseError(Location loc, String msg) throws WstxException {
        mReporter.throwParseError(loc, msg);
    }

    /*
    ///////////////////////////////////////
    // Private methods
    ///////////////////////////////////////
    */
}
