package staxperf.misc;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import staxperf.TestUtil;

/**
 * Micro-benchmark to test gzip decompression speed for given data.
 */
public class TestGunzipSpeed
{
    private Inflater mInflater;

    private byte[] mDecompBuffer;

    private TestGunzipSpeed() { }

    public void test(String[] args) throws Exception
    {
        if (args.length != 1) {
            throw new IllegalArgumentException("Need exactly one argument, input file name");
        }
        byte[] inputData = TestUtil.readData(new File(args[0]));
        System.out.println("Input data length: "+inputData.length);
        byte[] compData = compress(inputData);
        System.out.println("Compressed data length: "+compData.length);
        double ratio = (double) compData.length / (double) inputData.length;
        System.out.println("(compressed to "+((int)(100.0 * ratio))+"% of original)"); 

        // Let's do enough rounds to handle, say, 10 megs of data:
        int reps = Math.max(1, (1 << 10 << 10) / inputData.length);
        System.out.println(" -> "+reps+" decomps per round");

        mInflater = new Inflater(false);
        mDecompBuffer = new byte[inputData.length];

        while (true) {
            long time = System.currentTimeMillis();
            testDecomp(inputData, compData, reps);
            double msecs = System.currentTimeMillis() - time;
            double inputSpeed = (reps * inputData.length) / msecs;
            double compSpeed = (reps * compData.length) / msecs;

            System.out.println("Throughput: "+((int) inputSpeed)+" b/msec ("+((int) compSpeed)+" compressed) ["+msecs+" msecs for "+reps+"]");

            Thread.sleep(200L);
        }
    }

    private void testDecomp(byte[] inputData, byte[] compData, int reps)
        throws Exception
    {
        while (--reps >= 0) {
            mInflater.setInput(compData);
            int len = mInflater.inflate(mDecompBuffer);
            if (len != inputData.length || !mInflater.finished()) {
                throw new IllegalStateException("Broken decompression; expected "+inputData.length+", got "+len+"; finished: "+mInflater.finished());
            }
            mInflater.reset();
        }
    }

    byte[] compress(byte[] inputData)
    {
        // Let's test multiple lengths; return last
        int[] levels = { 0, 1, 5, 9 };
        int compLen = 0;
        // Assume at most 1/16 expansion...
        byte[] buffer = new byte[inputData.length + (inputData.length >> 4)];
        for (int level : levels) {
            // false -> no nowrap; i.e. use standard deflate header fields
            Deflater d = new Deflater(level, false);
            d.setInput(inputData);
            d.finish();
            compLen = d.deflate(buffer);
            // is it guaranteed to compress all?
            System.out.println("Size with compression level "+level+": "+compLen+" bytes.");
        }
        return Arrays.copyOf(buffer, compLen);
    }

    public static void main(String[] args) throws Exception
    {
        new TestGunzipSpeed().test(args);
    }

}
