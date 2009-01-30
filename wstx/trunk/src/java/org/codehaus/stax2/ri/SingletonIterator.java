package org.codehaus.stax2.ri;

import java.util.Iterator;

/**
 * Simple read-only iterator that iterators over one specific item, passed
 * in as constructor argument.
 */
public class SingletonIterator<T>
    implements Iterator<T>
{
    private final T mValue;
    
    private boolean mDone = false;
    
    public SingletonIterator(T value) {
        mValue = value;
    }
    
    public boolean hasNext() {
        return !mDone;
    }
    
    public T next() {
        if (mDone) {
            throw new java.util.NoSuchElementException();
        }
        mDone = true;
        return mValue;
    }
    
    public void remove()
    {
        throw new UnsupportedOperationException("Can not remove item from SingletonIterator.");
    }
}
