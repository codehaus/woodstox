package staxperf.misc;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import staxperf.TestUtil;

/**
 * Micro-benchmark to test gzip decompression speed for given data.
 */
public class TestGunzipSpeed
    extends BaseCompressTest
{
    private final Inflater mInflater;
    private final Deflater mDeflater;

    private byte[] mCompBuffer;
    private byte[] mDecompBuffer;

    private TestGunzipSpeed() {
        super("GZIP/deflate");
        // use default compress ratio for testing (5?)
        mInflater = new Inflater(false);
        mDeflater = new Deflater();
    }

    @Override
    protected byte[] doCompress(byte[] inputData) throws Exception
    {
        // false -> no nowrap; i.e. use standard deflate header fields
        mDeflater.setInput(inputData);
        mDeflater.finish();
        if (mCompBuffer == null) {
            mCompBuffer = new byte[inputData.length + (inputData.length >> 4)];
        }
        int compLen = mDeflater.deflate(mCompBuffer);
        mDeflater.reset();
        return Arrays.copyOf(mCompBuffer, compLen);
    }

    @Override
    protected byte[] doDecompress(byte[] compData, int uncompLen) throws Exception
    {
        mInflater.setInput(compData);
        if (mDecompBuffer == null || mDecompBuffer.length < uncompLen) {
            mDecompBuffer = new byte[uncompLen];
        }
        int len = mInflater.inflate(mDecompBuffer);
        mInflater.reset();
        return Arrays.copyOf(mDecompBuffer, len);
    }

    public static void main(String[] args) throws Exception
    {
        new TestGunzipSpeed().test(args);
    }

}
