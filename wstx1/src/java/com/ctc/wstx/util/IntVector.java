package com.ctc.wstx.util;

public final class IntVector
{
    final static int[] sEmptyArray = new int[0];

    private int[] mInts;

    private final int mInitialSize;

    private int mSize;

    public IntVector(int initialSize) {
        mInitialSize = initialSize;
        // Let's lazily instantiate it, still
        mInts = null;
        mSize = 0;
    }

    public int size() { return mSize; }

    public int getInt(int index) {
        return mInts[index];
    }

    public int popInt() {
        if (mSize < 1) {
            throw new IllegalStateException("Popping from empty stack.");
        }
        return mInts[--mSize];
    }

    public int peekInt() {
        if (mSize < 1) {
            throw new IllegalStateException("Peeking from empty stack.");
        }
        return mInts[mSize - 1];
    }

    public void addInt(int value) {
        if (mInts == null) {
            mInts = new int[mInitialSize];
        } else if (mSize >= mInts.length) {
            int[] old = mInts;
            mInts = new int[mSize + mSize];
            System.arraycopy(old, 0, mInts, 0, old.length);
        }
        mInts[mSize++] = value;
    }

    public int[] getInternalArray() {
        return (mInts == null) ? sEmptyArray : mInts;
    }
}
