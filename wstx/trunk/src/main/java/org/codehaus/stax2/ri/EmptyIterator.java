package org.codehaus.stax2.ri;

import java.util.Iterator;

/**
 * Simple implementation of "null iterator", iterator that has nothing to
 * iterate over.
 */
public final class EmptyIterator<T>
    implements Iterator<T>
{
    final static EmptyIterator<Object> sInstance = new EmptyIterator<Object>();
    
    private EmptyIterator() { }
    
    @SuppressWarnings("unchecked")
	public static <T> EmptyIterator<T> getInstance() { return (EmptyIterator<T>) sInstance; }
    
    public boolean hasNext() { return false; }
    
    public T next() {
        throw new java.util.NoSuchElementException();
    }
    
    public void remove()
    {
        /* The reason we do this is that we know for a fact that
         * it can not have been moved
         */
        throw new IllegalStateException();
    }
}
