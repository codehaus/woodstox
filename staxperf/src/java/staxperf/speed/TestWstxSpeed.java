package staxperf.speed;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.Reader;
import java.util.Arrays;

import javax.xml.namespace.QName;
import javax.xml.stream.*;

import com.ctc.wstx.stax.*;
import org.codehaus.stax2.*;
import org.codehaus.stax2.io.Stax2ByteArraySource;

/**
 * Manually run speed test to use for comparing speeds of alterate
 * input sources for Woodstox.
 */
public class TestWstxSpeed
    extends staxperf.TestUtil
            implements XMLStreamConstants
{
    final XMLInputFactory2 mFactory;

    final static int TEST_TYPES = 2;
    final static String[] TEST_DESCS = new String[] {
        "Wstx/stream", "Wstx/ByteArray"
    };

    public TestWstxSpeed()
    {
        mFactory = new com.ctc.wstx.stax.WstxInputFactory();
        mFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
    }

    public void test(String[] args)
        throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java ... "+getClass().getName()+" [file]");
            System.exit(1);
        }
        File f = new File(args[0]).getAbsoluteFile();
        String path = f.getAbsoluteFile().toURL().toExternalForm();

        // First, warm up:

        byte[] data = readData(f);
        System.out.println("Path: '"+path+"'");
        final int WARMUP_ROUNDS = 30;
        System.out.println("Warming up; doing "+WARMUP_ROUNDS+" iterations: ");
        int batchSize = 0;

        int total = 0; // to prevent any dead code optimizations
        for (int i = 0; i < WARMUP_ROUNDS; ) {
            // Let's estimate speed from the last warmup...
            int type = (i % TEST_TYPES);
            if (++i == WARMUP_ROUNDS) {
                long now = System.currentTimeMillis();
                total = testExec(0, data, path);
                batchSize = calcBatchSize(System.currentTimeMillis() - now);
            } else {
                total = testExec(0, data, path);
            }
            System.out.print(".");
            // Let's allow some slack time between iterations
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
        }
        System.out.println(" (batch size "+batchSize+")");

        /* Let's try to ensure GC is done so that real test can start from
         * a clean state.
         */
        try {  Thread.sleep(200L); } catch (InterruptedException ie) { }
        System.gc();
        try {  Thread.sleep(100L); } catch (InterruptedException ie) { }

        System.out.println("Ok, warmup done. Now doing real testing.");

        test2(data, path, batchSize);
    }

    private void test2(byte[] data, String systemId, int batchSize)
        throws Exception
    {
        final int[] times = new int[TEST_TYPES];
        final int[] diffs = new int[10];
        final int[] sortedDiffs = new int[diffs.length];

        while (true) {
            for (int type = 0; type < TEST_TYPES; ++type) {
                int total = 0;
                long start = System.currentTimeMillis();
                for (int i = batchSize; --i >= 0; ) {
                    total += testExec(type, data, systemId);
                }
                long msecs = System.currentTimeMillis() - start;
                times[type] = (int) msecs;
                System.out.println("Test '"+TEST_DESCS[type]+"': "+msecs+" ms ("+(total & 0xFFF)+")");
                Thread.sleep(200L);
            }
            System.arraycopy(diffs, 0, diffs, 1, diffs.length-1);
            int diff = times[times.length-1] - times[0];
            diffs[0] = diff;
            int totalDiff = 0;
            for (int i = 0; i < diffs.length; ++i) {
                totalDiff += diffs[i];
            }
            System.arraycopy(diffs, 0, sortedDiffs, 0, diffs.length);
            Arrays.sort(sortedDiffs);
            int median = sortedDiffs[sortedDiffs.length/2];
            System.out.println("Past 10 diffs: total "+totalDiff+" ms, median "+median);
            System.out.println();
        }
    }

    private int testExec(int testType, byte[] data, String systemId)
        throws XMLStreamException
    {
        XMLStreamReader sr;

        switch (testType) {
        case 0:
            sr = mFactory.createXMLStreamReader(new ByteArrayInputStream(data));
            break;
        default:
            sr = mFactory.createXMLStreamReader(new Stax2ByteArraySource(data, 0, data.length));
            break;
        }

        int total = 0;

        while (sr.hasNext()) {
            int type = sr.next();

            total += type; // so it won't be optimized out...

            //if (sr.hasText()) {
            if (type == CHARACTERS || type == CDATA || type == COMMENT) {
                // Test (a): just check length (no buffer copy)
                int textLen = sr.getTextLength();
                total += textLen;

                // Test (b): access internal read buffer
                /*
                char[] text = sr.getTextCharacters();
                int start = sr.getTextStart();
                int len = sr.getTextLength();
                if (text != null) { // Ref. impl. returns nulls sometimes
                    total += text.length; // to prevent dead code elimination
                }

                // Test (c): construct string (slowest)
                /*
                String text = sr.getText();
                total += text.length();
                */
            }
        }
        sr.close();
        return total;
    }

    public static void main(String[] args) throws Exception
    {
        new TestWstxSpeed().test(args);
    }
}
