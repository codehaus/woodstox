package com.ctc.wstx.io;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;

/**
 * Input source that reads input via a Reader.
 */
public class ReaderSource
    extends BaseInputSource
{
    /**
     * Underlying Reader to read character data from
     */
    Reader mReader;

    /**
     * If true, will close the underlying Reader when this source is closed;
     * if false will leave it open.
     */
    final boolean mDoRealClose;

    int mInputProcessed = 0;
    int mInputRow = 1;
    int mInputRowStart = 0;

    public ReaderSource(WstxInputSource parent, String fromEntity,
                        String pubId, String sysId, URL src,
                        Reader r, boolean realClose, int bufSize)
    {
        super(parent, fromEntity, pubId, sysId, src);
        mReader = r;
        mDoRealClose = realClose;
        mBuffer = new char[bufSize];
    }

    /**
     * Method called to change the default offsets this source has. Generally
     * done when the underlying Reader had been partially read earlier (like
     * reading the xml declaration before starting real parsing).
     */
    public void setInputOffsets(int proc, int row, int rowStart)
    {
        mInputProcessed = proc;
        mInputRow = row;
        mInputRowStart = rowStart;
    }

    /**
     * Input location is easy to set, as we'll start from the beginning
     * of a File.
     */
    public void initInputLocation(WstxInputData reader)
    {
        reader.mCurrInputProcessed = mInputProcessed;
        reader.mCurrInputRow = mInputRow;
        reader.mCurrInputRowStart = mInputRowStart;
    }

    public int getInputBufferLength() {
        return mBuffer.length;
    }

    public int readInto(WstxInputData reader)
        throws IOException
    {
        /* Shouldn't really try to read after closing, but it may be easier
         * for caller not to have to keep track of closure...
         */
        if (mBuffer == null) {
            return -1;
        }
        int count = mReader.read(mBuffer, 0, mBuffer.length);
        if (count < 1) {
            /* Let's prevent caller from accidentally being able to access
             * data, first.
             */
            mInputLen = 0;
            reader.mInputPtr = 0;
            reader.mInputLen = 0;
            if (count == 0) {
                /* Sanity check; should never happen with correctly written
                 * Readers:
                 */
                throw new IOException("Reader returned 0 characters, even when asked to read up to "+mBuffer.length);
            }
            return -1;
        }
        reader.mInputBuffer = mBuffer;
        reader.mInputPtr = 0;
        mInputLen = count;
        reader.mInputLen = count;

        return count;
    }

    public boolean readMore(WstxInputData reader, int minAmount)
        throws IOException
    {
        /* Shouldn't really try to read after closing, but it may be easier
         * for caller not to have to keep track of closure...
         */
        if (mBuffer == null) {
            return false;
        }

        int ptr = reader.mInputPtr;
        int currAmount = mInputLen - ptr;

        // Let's first adjust caller's data appropriately:

        // Existing data to move?
        if (currAmount > 0) {
            System.arraycopy(mBuffer, ptr, mBuffer, 0, currAmount);
            // Let's also offset amount of data that will be remaining
            reader.mCurrInputProcessed -= currAmount;
            reader.mCurrInputRowStart += currAmount;
            minAmount -= currAmount;
        }
        reader.mInputBuffer = mBuffer;
        reader.mInputPtr = 0;
        mInputLen = currAmount;

        while (minAmount > 0) {
            int amount = mBuffer.length - currAmount;
            int actual = mReader.read(mBuffer, currAmount, amount);
            if (actual < 1) {
                if (actual == 0) { // sanity check:
                    throw new IOException("Reader returned 0 characters, even when asked to read up to "+amount);
                }
                reader.mInputLen = mInputLen = currAmount;
                return false;
            }
            currAmount += actual;
            minAmount -= actual;
        }
        reader.mInputLen = mInputLen = currAmount;
        return true;
    }

    public void close()
        throws IOException
    {
        if (mDoRealClose) {
            mReader.close();
        }
        /* Let's help GC a bit, in case there might be back references
         * to this Onject from somewhere...
         */
        mReader = null;
        mBuffer = null;
    }
}

