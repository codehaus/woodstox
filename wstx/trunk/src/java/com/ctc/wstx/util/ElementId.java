package com.ctc.wstx.util;

import javax.xml.stream.Location;

import com.ctc.wstx.cfg.ErrorConsts;

/**
 * Simple container Object used to store information about id attribute
 * values, and references to such (as of yet undefined) values.
 *<p>
 * Instances can be in one of 2 modes: either in fully defined mode,
 * in which case information refers to location where value was defined
 * (ie. we had id as a value of ID type attribute); or in undefined mode,
 * in which case information refers to the first reference.
 *<p>
 * Note: {@link ElementIdMap} and this class are considered to be closely
 * bound; as a result, {@link ElementIdMap} uses straight field access
 * for {@link ElementId} Objects. This is almost as if ElementId was
 * an inner class of the Map; it also reduces number of accessors and
 * mutators ("monkey code") needed.
 */
public final class ElementId
{
    /**
     * Flag that indicates whether this Object presents a defined id
     * value (value of an ID attribute) or just a reference to one.
     */
    boolean mDefined;

    /*
    /////////////////////////////////////////////////
    // Information about id value or value reference,
    // depending on mDefined flag
    /////////////////////////////////////////////////
    */

    /**
     * Actual id value
     */
    final String mIdValue;

    /**
     * Location of either definition (if {@link #mDefined} is true; or
     * first reference (otherwise). Used when reporting errors; either
     * a referenced id has not been defined, or there are multiple
     * definitions of same id.
     */
    Location mLocation;

    /**
     * Name of element for which this id refers.
     */
    PrefixedName mElemName;

    /**
     * Name of the attribute that contains this id value (often "id", 
     * but need not be)
     */
    PrefixedName mAttrName;

    /*
    ////////////////////////////////////////////////////
    // Linking information, needed by the map to keep
    // track of collided ids, as well as undefined ids
    ////////////////////////////////////////////////////
    */

    ElementId mNextUndefd;

    ElementId mNextColl;

    /*
    /////////////////////////////////////////////////
    // Life cycle
    /////////////////////////////////////////////////
    */

    ElementId(String id, Location loc, boolean defined,
              PrefixedName elemName, PrefixedName attrName)
    {
        mIdValue = id;
        mLocation = loc;
        mDefined = defined;
        mElemName = elemName;
        mAttrName = attrName;
    }

    /*
    /////////////////////////////////////////////////
    // Public API
    /////////////////////////////////////////////////
    */

    public String getId() {
        return mIdValue;
    }

    public Location getLocation() {
        return mLocation;
    }

    public PrefixedName getElemName() {
        return mElemName;
    }

    public PrefixedName getAttrName() {
        return mAttrName;
    }
    
    public boolean idMatches(char[] buf, int start, int len)
    {
        if (mIdValue.length() != len) {
            return false;
        }
        // Assumes it's always at least one char long
        if (buf[start] != mIdValue.charAt(0)) {
            return false;
        }
        int i = 1;
        len += start;
        while (++start < len) {
            if (buf[start] != mIdValue.charAt(i)) {
                return false;
            }
            ++i;
        }
        return true;
    }

    public void markDefined(Location defLoc) {
        if (mDefined) { // sanity check
            throw new IllegalStateException(ErrorConsts.ERR_INTERNAL);
        }
        mDefined = true;
        mLocation = defLoc;
    }

    /*
    /////////////////////////////////////////////////
    // Other methods
    /////////////////////////////////////////////////
    */

    public String toString() {
        return mIdValue;
    }
}

