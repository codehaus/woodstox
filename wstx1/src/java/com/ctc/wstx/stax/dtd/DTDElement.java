/* Woodstox XML processor.
 *<p>
 * Copyright (c) 2004 Tatu Saloranta, tatu.saloranta@iki.fi
 *<p>
 * You can redistribute this work and/or modify it under the terms of
 * LGPL (Lesser Gnu Public License), as published by
 * Free Software Foundation (http://www.fsf.org). No warranty is
 * implied. See LICENSE for details about licensing.
 */

package com.ctc.wstx.stax.dtd;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.stream.Location;

import com.ctc.wstx.stax.cfg.InputConfigFlags;

/**
 * Class that contains element definitions from DTD.
 */
public final class DTDElement
    implements InputConfigFlags
{
    /*
    ///////////////////////////////////////////////////
    // Type constants
    ///////////////////////////////////////////////////
     */

    /*
    ///////////////////////////////////////////////////
    // Information about the element itself
    ///////////////////////////////////////////////////
     */

    NameKey mName;

    /**
     * Location of the (real) definition of the element; may be null for
     * placeholder elements created to hold ATTLIST definitions
     */
    Location mLocation;

    ContentSpec mContentSpec;

    final int mAllowedContent;

    /*
    ///////////////////////////////////////////////////
    // Attribute info
    ///////////////////////////////////////////////////
     */

    HashMap mAttrs = null;

    /*
    ///////////////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////////////
     */

    public DTDElement(Location loc, NameKey name, ContentSpec spec)
    {
        mLocation = loc;
        mName = name;
        mContentSpec = spec;

        /* Plus then let's figure out what kind of node types this
         * element can contain...
         */
        if (spec == null) {
            // doesn't really greatly matter...
            mAllowedContent = CONTENT_ALLOW_MIXED;
        } else {
            // Mixed content?
            if (spec.hasMixed()) {
                // DTD-based any, or 'true' mixed?
                if (spec.getArity() == ContentSpec.ARITY_ZERO) { // any
                    mAllowedContent = CONTENT_ALLOW_DTD_ANY;
                } else { // mixed
                    mAllowedContent = CONTENT_ALLOW_MIXED;
                }
            } else {
                // completely empty?
                if (spec.getArity() == ContentSpec.ARITY_ZERO) {
                    mAllowedContent = CONTENT_ALLOW_NONE;
                } else {
                    // nope, just non-mixed
                    mAllowedContent = CONTENT_ALLOW_NON_MIXED;
                }
            }
        }
    }

    public void addAttribute(DTDAttribute attr) {
        HashMap m = mAttrs;
        if (m == null) {
            mAttrs = m = new HashMap();
        }
        m.put(attr.getName(), attr);
    }

    public void mergeMissingAttributesFrom(DTDElement other)
    {
        Map otherMap = other.getAttributes();
        HashMap m = mAttrs;
        if (m == null) {
            mAttrs = m = new HashMap();
        }

        if (otherMap != null && otherMap.size() > 0) {
            Iterator it = otherMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry me = (Map.Entry) it.next();
                Object key = me.getKey();
                if (!m.containsKey(key)) {
                    m.put(key, me.getValue());
                }
            }
        }
    }

    /*
    ///////////////////////////////////////////////////
    // Public API
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

    public void setLocation(Location loc) { mLocation = loc; }

    public ContentSpec getContentSpec() {
        return mContentSpec;
    }

    public void setContentSpec(ContentSpec spec) {
        mContentSpec = spec;
    }

    public boolean isDefined() {
        return mContentSpec != null;
    }

    public boolean canContainMixed() {
        /* For undefined elements we can only speculate whether it can;
         * for now let's say it can:
         */
        return (mContentSpec == null) || mContentSpec.hasMixed();
    }

    /**
     * @return Constant that identifies what kind of nodes are in general
     *    allowed inside this element.
     */
    public int getAllowedContent() {
        return mAllowedContent;
    }

    public Map getAttributes() {
        return mAttrs;
    }

    /*
    ///////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////
     */

}
