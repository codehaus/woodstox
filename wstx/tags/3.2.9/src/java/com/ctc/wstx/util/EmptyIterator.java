package com.ctc.wstx.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Simple implementation of "null iterator", iterator that has nothing to
 * iterate over.
 */
public final class EmptyIterator
    implements Iterator
{
    final static String[] sNoStrings = new String[0];
    final static char[] sNoChars = new char[0];
    final static int[] sNoInts = new int[0];

    final static EmptyIterator sInstance = new EmptyIterator();

    private EmptyIterator() { }

    public static EmptyIterator getInstance() {
        return sInstance;
    }

    public boolean hasNext() { return false; }

    public Object next() {
        throw new NoSuchElementException();
    }

    public void remove() {
        // could as well throw IllegalOperationException...
        throw new IllegalStateException();
    }

    public final static String[] getEmptyStringArray() {
        return sNoStrings;
    }

    public final static int[] getEmptyIntArray() {
        return sNoInts;
    }

    public final static char[] getEmptyCharArray() {
        return sNoChars;
    }
}
