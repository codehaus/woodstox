package com.ctc.wstx.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Simple read-only iterator that iterators over one specific item, passed
 * in as constructor argument.
 */
public final class SingletonIterator
    implements Iterator
{
    private final Object mValue;

    private boolean mDone = false;

    public SingletonIterator(Object value) {
        mValue = value;
    }

    public boolean hasNext() {
        return !mDone;
    }

    public Object next() {
        if (mDone) {
            throw new NoSuchElementException();
        }
        mDone = true;
        return mValue;
    }

    public void remove() {
        throw new UnsupportedOperationException("Can not remove item from SingletonIterator.");
    }
}

