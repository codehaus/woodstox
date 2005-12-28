/* Woodstox XML processor
 *
 * Copyright (c) 2004- Tatu Saloranta, tatu.saloranta@iki.fi
 *
 * Licensed under the License specified in the file LICENSE which is
 * included with the source code.
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

import org.codehaus.stax2.validation.XMLValidator;

import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.exc.WstxException;
import com.ctc.wstx.sr.InputProblemReporter;
import com.ctc.wstx.util.WordResolver;

/**
 * Class that contains element definitions from DTD.
 *<p>
 * Notes about thread-safety: this class is not thread-safe, since it does
 * not have to be, in general case. That is, the only instances that can
 * be shared are external subset instances, and those are used in read-only
 * manner (with the exception of temporary arrays constructed on-demand).
 */
public final class DTDElement
{

    /*
    ///////////////////////////////////////////////////
    // Information about the element itself
    ///////////////////////////////////////////////////
     */

    final NameKey mName;

    /**
     * Location of the (real) definition of the element; may be null for
     * placeholder elements created to hold ATTLIST definitions
     */
    final Location mLocation;

    /**
     * Base validator object for validating content model of this element;
     * may be null for some simple content models (ANY, EMPTY).
     */
    StructValidator mValidator;

    int mAllowedContent;

    /*
    ///////////////////////////////////////////////////
    // Attribute info
    ///////////////////////////////////////////////////
     */

    HashMap mAttrMap = null;

    /**
     * Ordered list of attributes that have 'special' properties (attribute
     * is required or has a default value); these attributes have to be
     * specifically checked after actual values have been resolved.
     */
    ArrayList mSpecAttrList = null;

    boolean mAnyFixed = false;

    /**
     * Flag that is set to true if there is at least one attribute that
     * has type that requires normalization and/or validation; that is,
     * is of some other type than CDATA.
     */
    boolean mValidateAttrs = false;

    /**
     * Id attribute instance, if one already declared for this element;
     * can only have up to one such attribute per element.
     */
    DTDAttribute mIdAttr;

    /**
     * Notation attribute instance, if one already declared for this element;
     * can only have up to one such attribute per element.
     */
    DTDAttribute mNotationAttr;

    // // // !! If you add new attributes, make sure they get copied
    // // // in #define() method !!

    /*
    ///////////////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////////////
     */

    private DTDElement(Location loc, NameKey name,
                       StructValidator val, int allowedContent)
    {
        mName = name;
        mLocation = loc;
        mValidator = val;
        mAllowedContent = allowedContent;
    }

    /**
     * Method called to create an actual element definition, matching
     * an ELEMENT directive in a DTD subset.
     */
    public static DTDElement createDefined(Location loc, NameKey name,
                                           StructValidator val, int allowedContent)
    {
        if (allowedContent == XMLValidator.CONTENT_ALLOW_UNDEFINED) { // sanity check
            throw new Error("Internal error: trying to use XMLValidator.CONTENT_ALLOW_UNDEFINED via createDefined()");
        }
        return new DTDElement(loc, name, val, allowedContent);
    }

    /**
     * Method called to create a "placeholder" element definition, needed to
     * contain attribute definitions.
     */
    public static DTDElement createPlaceholder(Location loc, NameKey name)
    {
        return new DTDElement(loc, name, null, XMLValidator.CONTENT_ALLOW_UNDEFINED);
    }
        
    /**
     * Method called on placeholder element, to create a real instance that
     * has all attribute definitions placeholder had (it'll always have at
     * least one -- otherwise no placeholder was needed).
     */
    public DTDElement define(Location loc, StructValidator val,
                             int allowedContent)
    {
        verifyUndefined();
        if (allowedContent == XMLValidator.CONTENT_ALLOW_UNDEFINED) { // sanity check
            throw new Error("Internal error: trying to use CONTENT_ALLOW_UNDEFINED via define()");
        }

        DTDElement elem = new DTDElement(loc, mName, val, allowedContent);

        // Ok, need to copy state collected so far:
        elem.mAttrMap = mAttrMap;
        elem.mSpecAttrList = mSpecAttrList;
        elem.mAnyFixed = mAnyFixed;
        elem.mValidateAttrs = mValidateAttrs;
        elem.mIdAttr = mIdAttr;
        elem.mNotationAttr = mNotationAttr;

        return elem;
    }

    /**
     * Method called to "upgrade" a placeholder using a defined element,
     * including adding attributes.
     */
    public void defineFrom(InputProblemReporter rep, DTDElement definedElem)
        throws WstxException
    {
        verifyUndefined();
        mValidator = definedElem.mValidator;
        mAllowedContent = definedElem.mAllowedContent;
        mergeMissingAttributesFrom(rep, definedElem);
    }

    private void verifyUndefined()
    {
        if (mAllowedContent != XMLValidator.CONTENT_ALLOW_UNDEFINED) { // sanity check
            throw new Error("Internal error: redefining defined element spec");
        }
    }

    /**
     * Method called by DTD parser when it has read information about
     * an attribute that belong to this element
     *
     * @return Newly created attribute Object if the attribute definition was
     *   added (hadn't been declared yet); null if it's a duplicate, in which
     *   case original definition sticks.
     */
    public DTDAttribute addAttribute(InputProblemReporter rep,
                                     NameKey attrName, int valueType, int defValueType,
                                     String defValue, WordResolver enumValues)
        throws WstxException
    {
        HashMap m = mAttrMap;
        if (m == null) {
            mAttrMap = m = new HashMap();
        }

        List specList = DTDAttribute.isSpecial(defValueType) ?
            getSpecialList() : null;

        DTDAttribute attr;
        int specIndex = (specList == null) ? -1 : specList.size();

        switch (valueType) {
        case DTDAttribute.TYPE_CDATA:
            attr = new DTDAttribute(attrName, defValueType, defValue, specIndex);
            break;

        case DTDAttribute.TYPE_ENUMERATED:
            attr = new DTDEnumAttr(attrName, defValueType, defValue,
                                   specIndex, enumValues);
            break;

        case DTDAttribute.TYPE_ID:
            attr = new DTDIdAttr(attrName, defValueType, specIndex);
            break;

        case DTDAttribute.TYPE_IDREF:
            attr = new DTDIdRefAttr(attrName, defValueType, defValue,
                                    specIndex);
            break;

        case DTDAttribute.TYPE_IDREFS:
            attr = new DTDIdRefsAttr(attrName, defValueType, defValue,
                                     specIndex);
            break;

        case DTDAttribute.TYPE_ENTITY:
            attr = new DTDEntityAttr(attrName, defValueType, defValue, specIndex);
            break;

        case DTDAttribute.TYPE_ENTITIES:
            attr = new DTDEntitiesAttr(attrName, defValueType, defValue, specIndex);
            break;

        case DTDAttribute.TYPE_NOTATION:
            attr = new DTDNotationAttr(attrName, defValueType, defValue,
                                       specIndex, enumValues);
            break;
        
        case DTDAttribute.TYPE_NMTOKEN:
            attr = new DTDNmTokenAttr(attrName, defValueType, defValue,
                                      specIndex);
            break;

        case DTDAttribute.TYPE_NMTOKENS:
            attr = new DTDNmTokensAttr(attrName, defValueType, defValue,
                                       specIndex);
            break;

        default:
            attr = new DTDTypedAttr(attrName, defValueType, defValue, specIndex,
                                    valueType);
        }

        doAddAttribute(m, rep, attr, specList);
        return attr;
    }

    public void mergeMissingAttributesFrom(InputProblemReporter rep, DTDElement other)
        throws WstxException
    {
        Map otherMap = other.getAttributes();
        HashMap m = mAttrMap;
        if (m == null) {
            mAttrMap = m = new HashMap();
        }

        boolean anyAdded = false;
        
        if (otherMap != null && otherMap.size() > 0) {
            Iterator it = otherMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry me = (Map.Entry) it.next();
                Object key = me.getKey();
                // Should only add if no such attribute exists...
                if (!m.containsKey(key)) {
                    // can only use as is, if it's not a special attr
                    DTDAttribute newAttr = (DTDAttribute) me.getValue();
                    List specList;
                    // otherwise need to clone
                    if (newAttr.isSpecial()) {
                        specList = getSpecialList();
                        newAttr = newAttr.cloneWith(specList.size());
                    } else {
                        specList = null;
                    }
                    doAddAttribute(m, rep, newAttr, specList);
                }
            }
        }
    }

    private void doAddAttribute(Map attrMap, InputProblemReporter rep,
                                DTDAttribute attr, List specList)
        throws WstxException
    {
        NameKey attrName = attr.getName();

        // Maybe we already have it? If so, need to ignore
        if (attrMap.containsKey(attrName)) {
            rep.reportProblem(ErrorConsts.WT_ATTR_DECL, ErrorConsts.W_DTD_DUP_ATTR,
                              attrName, mName);
            return;
        }

        switch (attr.getValueType()) {
        case DTDAttribute.TYPE_ID:
            // Only one such attribute per element (Specs, 1.0#3.3.1)
            if (mIdAttr != null) {
                rep.throwParseError("Invalid id attribute '"+attrName+"' for element <"+mName+">: already had id attribute '"+mIdAttr.getName()+"'");
            }
            mIdAttr = attr;
            break;

        case DTDAttribute.TYPE_NOTATION:
            // Only one such attribute per element (Specs, 1.0#3.3.1)
            if (mNotationAttr != null) {
                rep.throwParseError("Invalid notation attribute '"+attrName+"' for element <"+mName+">: already had notation attribute '"+mNotationAttr.getName()+"'");
            }
            mNotationAttr = attr;
            break;
        }

        attrMap.put(attrName, attr);
        if (specList != null) {
            specList.add(attr);
        }
        if (!mAnyFixed) {
            mAnyFixed = attr.isFixed();
        }
        if (!mValidateAttrs) {
            mValidateAttrs = attr.needsValidation();
        }
    }

    /*
    ///////////////////////////////////////////////////
    // Public API, accessors:
    ///////////////////////////////////////////////////
     */

    public NameKey getName() { return mName; }

    public String toString() {
        return mName.toString();
    }

    public String getDisplayName() {
        return mName.toString();
    }

    public Location getLocation() { return mLocation; }

    public boolean isDefined() {
        return (mAllowedContent != XMLValidator.CONTENT_ALLOW_UNDEFINED);
    }

    /**
     * @return Constant that identifies what kind of nodes are in general
     *    allowed inside this element.
     */
    public int getAllowedContent() {
        return mAllowedContent;
    }

    public HashMap getAttributes() {
        return mAttrMap;
    }

    public int getSpecialCount() {
        return (mSpecAttrList == null) ? 0 : mSpecAttrList.size();
    }

    public List getSpecialAttrs() {
        return mSpecAttrList;
    }

    /**
     * @return True if at least one of the attributes has type other than
     *   CDATA; false if not
     */
    public boolean attrsNeedValidation() {
        return mValidateAttrs;
    }

    public boolean hasFixedAttrs() {
        return mAnyFixed;
    }

    public DTDAttribute getIdAttribute() {
        return mIdAttr;
    }

    public DTDAttribute getNotationAttribute() {
        return mNotationAttr;
    }

    /*
    ///////////////////////////////////////////////////
    // Public API, factory methods:
    ///////////////////////////////////////////////////
     */

    public StructValidator getValidator()
    {
        return (mValidator == null) ? null : mValidator.newInstance();
    }

    /*
    ///////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////
     */

    private List getSpecialList()
    {
        ArrayList l = mSpecAttrList;
        if (l == null) {
            mSpecAttrList = l = new ArrayList();
        }
        return l;
    }
}
