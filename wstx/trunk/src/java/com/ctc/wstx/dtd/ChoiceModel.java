package com.ctc.wstx.dtd;

import java.util.*;

import com.ctc.wstx.cfg.ErrorConsts;

/**
 * Model class that encapsulates set of sub-models, of which one (and only
 * one) needs to be matched.
 */
public class ChoiceModel
    extends ModelNode
{
    final ModelNode[] mSubModels;

    boolean mNullable = false;

    BitSet mFirstPos, mLastPos;

    /*
    ///////////////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////////////
     */

    protected ChoiceModel(ModelNode[] subModels)
    {
        super();
        mSubModels = subModels;
        boolean nullable = false;
        for (int i = 0, len = subModels.length; i < len; ++i) {
            if (subModels[i].isNullable()) {
                nullable = true;
                break;
            }
        }
        mNullable = nullable;
    }

    /*
    ///////////////////////////////////////////////////
    // Public API
    ///////////////////////////////////////////////////
     */

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < mSubModels.length; ++i) {
            if (i > 0) {
                sb.append(" | ");
            }
            sb.append(mSubModels[i].toString());
        }
        sb.append(')');
        return sb.toString();
    }

    /**
     * Method that has to create a deep copy of the model, without
     * sharing any of existing Objects.
     */
    public ModelNode cloneModel()
    {
        int len = mSubModels.length;
        ModelNode[] newModels = new ModelNode[len];
        for (int i = 0; i < len; ++i) {
            newModels[i] = mSubModels[i].cloneModel();
        }
        return new ChoiceModel(newModels);
    }

    public boolean isNullable() {
        return mNullable;
    }

    public void indexTokens(List tokens)
    {
        // First, let's ask sub-models to calc their settings
        for (int i = 0, len = mSubModels.length; i < len; ++i) {
            mSubModels[i].indexTokens(tokens);
        }
    }

    public void addFirstPos(BitSet firstPos) {
        if (mFirstPos == null) {
            mFirstPos = new BitSet();
            for (int i = 0, len = mSubModels.length; i < len; ++i) {
                mSubModels[i].addFirstPos(mFirstPos);
            }
        }
        firstPos.or(mFirstPos);
    }

    public void addLastPos(BitSet lastPos) {
        if (mLastPos == null) {
            mLastPos = new BitSet();
            for (int i = 0, len = mSubModels.length; i < len; ++i) {
                mSubModels[i].addLastPos(mLastPos);
            }
        }
        lastPos.or(mLastPos);
    }

    public void calcFollowPos(BitSet[] followPosSets)
    {
        // need to let child models do their stuff:
        for (int i = 0, len = mSubModels.length; i < len; ++i) {
            mSubModels[i].calcFollowPos(followPosSets);
        }
    }

    /*
    ///////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////
     */


    /*
    ///////////////////////////////////////////////////
    // Validator class that can be used for simple
    // choices (including mixed content)
    ///////////////////////////////////////////////////
     */

    final static class Validator
        extends StructValidator
    {
        final char mArity;
        final NameKeySet mNames;

        int mCount = 0;

        public Validator(char arity, NameKeySet names)
        {
            mArity = arity;
            mNames = names;
        }

        /**
         * Rules for reuse are simple: if we can have any number of
         * repetitions, we can just use a shared root instance. Although
         * its count variable will get updated this doesn't really
         * matter as it won't be used. Otherwise a new instance has to
         * be created always, to keep track of instance counts.
         */
        public StructValidator newInstance() {
            return (mArity == '*') ? this : new Validator(mArity, mNames);
        }

        public String tryToValidate(NameKey elemName)
        {
            if (mNames == null) {
                // should never happen?!?!
                return ErrorConsts.ERR_INTERNAL;
            }
            if (!mNames.contains(elemName)) {
                if (mNames.hasMultiple()) {
                    return "Expected one of ("+mNames.toString(" | ")+")";
                }
                return "Expected <"+mNames.toString("")+">";
            }
            if (++mCount > 1) {
                if (mArity == '?') {
                    if (mNames.hasMultiple()) {
                        return "More than one instance; expected at most one of ("
                            +mNames.toString(" | ")+")";
                    }
                    return "More than one instance; expected at most one";
                }
                if (mArity == ' ') {
                    if (mNames.hasMultiple()) {
                        return "More than one instance; expected exactly one of ("
                            +mNames.toString(" | ")+")";
                    }
                    return "More than one instance; expected exactly one";
                }
            }
            return null;
        }
        
        public String fullyValid()
        {
            switch (mArity) {
            case '*':
            case '?':
                return null;
            case '+': // need at least one (and multiples checked earlier)
            case ' ':
                if (mCount > 0) {
                    return null;
                }
                return "Expected "+(mArity == '+' ? "at least" : "")
                    +" one of elements ("+mNames+")";
            }
            // should never happen:
            throw new Error("Internal error");
        }
    }
}
