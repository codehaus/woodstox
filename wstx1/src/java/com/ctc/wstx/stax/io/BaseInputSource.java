package com.ctc.wstx.stax.io;

import java.io.IOException;
import java.net.URL;

import javax.xml.stream.Location;

import com.ctc.wstx.util.TextBuffer;

public abstract class BaseInputSource
    extends WstxInputSource
{
    final String mPublicId;
    
    final String mSystemId;
    
    /**
     * URL that points to original source of input, if known; null if not
     * known (source constructed with just a stream or reader). Used for
     * resolving references from the input that's read from this source.
     */
    final URL mSource;
    
    /**
     * Input buffer this input source uses, if any.
     */
    protected char[] mBuffer;

    protected int mInputLen;

    /*
    ////////////////////////////////////////////////////////////////
    // Saved location information; active information is directly
    // handled by Reader, and then saved to input source when switching
    // to a nested input source.
    ////////////////////////////////////////////////////////////////
    */
    
    int mSavedInputProcessed = 0;
    int mSavedInputRow = 1;
    int mSavedInputRowStart = 0;

    int mSavedInputPtr = 0;

    protected BaseInputSource(WstxInputSource parent, String fromEntity,
                              String publicId, String systemId, URL src)
    {
        super(parent, fromEntity);
        mSystemId = systemId;
        mPublicId = publicId;
        mSource = src;
    }

    /**
     * @return Length of suggested input buffer (if source needs one); used
     *   for passing default buffer size down the input source line.
     */
    public abstract int getInputBufferLength();

    public URL getSource() {
        return mSource;
    }

    public String getPublicId() {
      return mPublicId;
    }

    public String getSystemId() {
      return mSystemId;
    }

    public abstract void initInputLocation(WstxInputData reader);

    public abstract int readInto(WstxInputData reader)
        throws IOException;
    
    public abstract boolean readMore(WstxInputData reader, int minAmount)
        throws IOException;

    public void saveContext(WstxInputData reader)
    {
        // First actual input data
        mSavedInputPtr = reader.mInputPtr;

        // then location
        mSavedInputProcessed = reader.mCurrInputProcessed;
        mSavedInputRow = reader.mCurrInputRow;
        mSavedInputRowStart = reader.mCurrInputRowStart;

        //System.err.println("Saving "+this+"; ptr = "+mSavedInputPtr+", len = "+mInputLen);
    }

    public void restoreContext(WstxInputData reader)
    {
        reader.mInputBuffer = mBuffer;
        reader.mInputLen = mInputLen;
        reader.mInputPtr = mSavedInputPtr;

        //System.err.println("Restoring "+this+"; ptr = "+mSavedInputPtr+", len = "+mInputLen);

        // then location
        reader.mCurrInputProcessed = mSavedInputProcessed;
        reader.mCurrInputRow = mSavedInputRow;
        reader.mCurrInputRowStart = mSavedInputRowStart;
    }

    public abstract void close() throws IOException;

    /*
    //////////////////////////////////////////////////////////
    // Methods for accessing location information
    //////////////////////////////////////////////////////////
     */

    public final WstxInputLocation getLocation()
    {
        return getLocation(mSavedInputProcessed + mSavedInputPtr - 1,
                           mSavedInputRow, mSavedInputPtr - mSavedInputRowStart);
    }

    public final WstxInputLocation getLocation(int total, int row, int col)
    {
        WstxInputLocation pl = (mParent == null) ? null : mParent.getLocation();
        return new WstxInputLocation(pl, getPublicId(), getSystemId(),
                                     total, row, col);
    }
}
