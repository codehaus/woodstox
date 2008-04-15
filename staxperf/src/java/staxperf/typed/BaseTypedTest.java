package staxperf.typed;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.*;
import org.codehaus.stax2.io.Stax2ByteArraySource;

import staxperf.TestUtil;

/**
 * Base class for testing sustainable performance of typed access via
 * Stax typed extensions.
 */
abstract class BaseTypedTest
    extends TestUtil
    implements XMLStreamConstants
{
    private final static int DEFAULT_TEST_SECS = 30;
    private final static int WARMUP_ROUNDS = 50;

    XMLInputFactory2 mFactory;

    protected int mBatchSize;

    protected abstract XMLInputFactory2 getFactory();

    protected void init()
    {
        mFactory = getFactory();
        System.out.println("Factory instance: "+mFactory.getClass());
        mFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
        // Default is ns-aware, no need to re-set:
        //mFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
        // Shouldn't be validating, but let's ensure:
        mFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
        System.out.print("  coalescing: "+mFactory.getProperty(XMLInputFactory.IS_COALESCING));
        System.out.println(";  ns-aware: "+mFactory.getProperty(XMLInputFactory.IS_NAMESPACE_AWARE));
        System.out.println("  validating: "+mFactory.getProperty(XMLInputFactory.IS_VALIDATING));
    }

    protected int testExec(byte[] data, String path) throws Exception
    {
        Stax2ByteArraySource src = new Stax2ByteArraySource(data, 0, data.length);
        src.setSystemId(path);
        XMLStreamReader2 sr = (XMLStreamReader2) mFactory.createXMLStreamReader(src);
        // Let's also skip root element, for convenience
        while (sr.next() != START_ELEMENT) { }

        int result = testExec2(sr);

        sr.close();

        return result;
    }

    /**
     * @return Count of matched elements (whatever criteria of matching is).
     *   Checked by base test, so that input file should have at least
     *   couple of matches (used as a sanity check)
     */
    protected abstract int testExec2(XMLStreamReader2 sr) throws XMLStreamException;

    public void test(String[] args)
        throws Exception
    {
        if (args.length < 1) {
            System.err.println("Usage: java ... "+getClass().getName()+" [file] <test-period-secs>");
            System.exit(1);
        }

        init();

        String fn = args[0];
        int SECS = DEFAULT_TEST_SECS;
        if (args.length >= 2) {
            SECS = Integer.parseInt(args[1]);
        }
        File f = new File(fn).getCanonicalFile();
        /* Some stax impls are picky, let's try to ensure system id is
         * indeed a valid URL
         */
        String path = f.getAbsoluteFile().toURL().toExternalForm();

        // First, warm up:

        byte[] data = readData(f);

        System.out.println("Path: '"+path+"'");
        System.out.println("Warming up; doing  "+WARMUP_ROUNDS+" iterations (real test will run for "+SECS+" seconds): ");

        int total = 0; // to prevent any dead code optimizations
        for (int i = 0; i < WARMUP_ROUNDS; ) {
            // Let's estimate speed from the last warmup...
            int result;
            if (++i == WARMUP_ROUNDS) {
                long now = System.currentTimeMillis();
                result = testExec(data, path);
                mBatchSize = calcBatchSize(System.currentTimeMillis() - now);
            } else {
                result = testExec(data, path);
                if (i == 1) {
                    System.out.print("[first result: "+result+"]");
                }
            }
            // Sanity check:
            if (result < 10) {
                throw new IllegalStateException("Result < 10 ("+result+"), should get at least 10");
            }

            //testFinish();
            System.out.print(".");
            // Let's allow some slack time between iterations
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
        }
        System.out.println(" (batch size "+mBatchSize+")");

        /* Let's try to ensure GC is done so that real test can start from
         * a clean state.
         */
        try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
        System.gc();
        try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
        System.gc();
        try {  Thread.sleep(100L); } catch (InterruptedException ie) { }

        System.out.println("Ok, warmup done. Now doing real testing, running for "+SECS+" seconds.");

        long nextTime = System.currentTimeMillis();
        long endTime = nextTime + (SECS * 1000);
        int count = 0;
        total = 0;
        int subtotal = 0;
        final long SUB_PERIOD = 1000L; // print once a second
        nextTime += SUB_PERIOD;
        final int REPS = mBatchSize;

        /* Let's try to reduce overhead of System.currentTimeMillis()
         * by calling test method twice each round. May be a problem for
         * big docs/slow readers... but otherwise not.
         */
        while (true) {
            for (int i = 0; i < REPS; ++i) {
                total += testExec(data, path);
            }
            count += REPS;
            long now = System.currentTimeMillis();
            if (now > endTime) {
                break;
            }
            /* let's only print once a second... limits console overhead,
             * but still informs about progress.
             */
            ++subtotal;
            if (now > nextTime) {
                char c;
                subtotal -= 1;
                if (subtotal > 35) {
                    c = '+';
                } else if (subtotal > 9) {
                    c = (char) ('a' + (subtotal-10));
                } else {
                    c = (char) ('0' + subtotal);
                }
                System.out.print(c);
                nextTime += SUB_PERIOD;
                if (nextTime < now) {
                    nextTime = now;
                }
                subtotal = 0;
            }
        }

        System.out.println();
        System.out.println("Total iterations done: "+count+" [done "+total+"]");
    }
}
