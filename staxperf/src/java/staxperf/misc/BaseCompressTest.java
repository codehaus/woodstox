package staxperf.misc;

import java.io.*;
import java.util.*;

import staxperf.TestUtil;

/**
 * Base class for compression Micro-benchmarks.
 */
public abstract class BaseCompressTest
{
    final String mCompressMethod;

    protected BaseCompressTest(String method) {
        mCompressMethod = method;
    }

    protected final void test(String[] args) throws Exception
    {
        if (args.length != 1) {
            throw new IllegalArgumentException("Need exactly one argument, input file name");
        }
        System.out.println("Testing '"+mCompressMethod+"'");
        byte[] inputData = TestUtil.readData(new File(args[0]));
        System.out.println("Input data length: "+inputData.length);
        byte[] compData = doCompress(inputData);
        System.out.println("Compressed data length: "+compData.length);
        // Sanity check for input data
        {
            byte[] test  = doDecompress(compData, inputData.length);
            if (test.length != inputData.length) {
                throw new IOException("Internal error: broken codec ("+mCompressMethod+"); input length "+inputData.length+", decoded to "+test.length);
            }
        }
 
       double ratio = (double) compData.length / (double) inputData.length;
        System.out.println("(compressed to "+((int)(100.0 * ratio))+"% of original)"); 
        // Let's do enough rounds to handle, say, 10 megs of data:
        int reps = Math.max(1, (1 << 10 << 10) / inputData.length);
        System.out.println(" -> test with "+reps+" comp/decomp per round");
        test(inputData, compData, reps);
    }

    protected final void test(byte[] inputData, byte[] compData, int reps)
        throws Exception
    {
        while (true) {
            // First, compress
            long time = System.nanoTime();
            int value = testCompress(inputData, compData, reps);
            time = System.nanoTime() - time;
            double msecs = (double) time / 1000000.0;
            System.out.println("Compress: "+TestUtil.calcSpeed(msecs, reps * inputData.length)
                               +" ("+TestUtil.calcSpeed(msecs, reps * compData.length)+" compressed) ["+((int) msecs)+" msecs for "+reps);

            Thread.sleep(200L);

            // Then decompress
            time = System.nanoTime();
            value = testDecompress(inputData, compData, reps);
            time = System.nanoTime() - time;
            msecs = (double) time / 1000000.0;
            System.out.println(" De-compress: "+TestUtil.calcSpeed(msecs, reps * inputData.length)
                               +" ("+TestUtil.calcSpeed(msecs, reps * compData.length)+" compressed) ["
                               +((int) msecs)+" msecs for "+reps);

            Thread.sleep(500L);
        }
    }

    protected int testCompress(byte[] inputData, byte[] compData, int reps)
        throws Exception
    {
        byte[] result = null;
        while (--reps >= 0) {
            result = doCompress(inputData);
        }
        return (int) inputData[0];
    }

    protected abstract byte[] doCompress(byte[] inputData) throws Exception;

    protected int testDecompress(byte[] inputData, byte[] compData, int reps)
        throws Exception
    {
        byte[] result = null;
        while (--reps >= 0) {
            result = doDecompress(compData, inputData.length);
        }
        if (inputData.length != result.length) {
            throw new IllegalStateException("Broken decompression; expected "+inputData.length+", got "+result.length);
        }
        return (int) inputData[0];
    }

    protected abstract byte[] doDecompress(byte[] compData, int uncompLen) throws Exception;
}

