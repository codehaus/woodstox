package com.ctc.wstx.stax.dtd;

/**
 * Class that implements intricacies of the default value handling;
 * the value may not be just a straight-forward constant String -- it
 * may have general entities that have to be expanded.
 */
public final class AttrDefaultValue
{
    final String mCoreValue;

    final int[] mEntityOffsets;

    final String[] mEntityIds;

    /*
    ///////////////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////////////
     */

    public AttrDefaultValue(String coreValue,
                            int[] entityOffsets, String[] entityIds)
    {
        mCoreValue = coreValue;
        mEntityOffsets = entityOffsets;
        mEntityIds = entityIds;
    }

    /*
    ///////////////////////////////////////////////////
    // Public API
    ///////////////////////////////////////////////////
     */

    public String getValue() {
        if (mEntityOffsets == null) {
            return mCoreValue;
        }

        /* !!! TBI: Need to actually expand entities and merge the values
         *    in.
         */
        return mCoreValue;
    }

    public String toString() {
        return getValue();
    }

    /*
    ///////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////
     */

}
