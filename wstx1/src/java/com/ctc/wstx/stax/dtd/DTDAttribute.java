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

/**
 * Class that contains attribute type definitions from DTD; these are
 * generallyelement definitions from DTD.
 */
public final class DTDAttribute
{
    /*
    ///////////////////////////////////////////////////
    // Type constants
    ///////////////////////////////////////////////////
     */

    // // // Value types

    public final static int TYPE_CDATA = 1;
    public final static int TYPE_ENUMERATED = 2;

    public final static int TYPE_ID = 3;
    public final static int TYPE_IDREF = 4;
    public final static int TYPE_IDREFS = 5;

    public final static int TYPE_ENTITY = 6;
    public final static int TYPE_ENTITIES = 7;

    public final static int TYPE_NOTATION = 8;
    public final static int TYPE_NMTOKEN = 9;
    public final static int TYPE_NMTOKENS = 10;

    // // // Default value types

    public final static int DEF_DEFAULT = 1;
    public final static int DEF_IMPLIED = 2;
    public final static int DEF_REQUIRED = 3;
    public final static int DEF_FIXED = 4;

    /*
    ///////////////////////////////////////////////////
    // Information about the attribute itself
    ///////////////////////////////////////////////////
     */

    final NameKey mName;

    /**
     * Sequential index number that is unique for each attribute of
     * given element.
     */
    final int mIndex;

    final int mValueType, mDefaultValueType;

    final AttrDefaultValue mDefaultValue;

    /*
    ///////////////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////////////
     */

    public DTDAttribute(NameKey name, int index,
                        int valueType, int defValueType,
                        AttrDefaultValue defValue)
    {
        mName = name;
        mIndex = index;
        mValueType = valueType;
        mDefaultValueType = defValueType;
        mDefaultValue = defValue;
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

    public int getDefaultType() {
        return mDefaultValueType;
    }

    public int getValueType() {
        return mValueType;
    }

    public boolean hasDefaultValue() {
        return (mDefaultValue != null);
    }

    /*
    ///////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////
     */

}
