package staxperf.single;

import java.io.*;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import staxperf.TestUtil;

/**
 * Base class for testing sustainable performance of various StAX
 * implementations. Basic operation is as follows:
 *<ul>
 * <li>First implementation is set up, and then <b>warmed up</b>, by doing
 *   couple (30) of repetitions over test document. This ensures JIT has
 *   had a chance to use hot spot compilation of performance bottlenecks
 *  </li>
 * <li>Main testing loop iterates over parsing as many times as possible,
 *    during test period (30 seconds), and reports number of iterations
 *    succesfully done.
 *  </li>
 *</ul>
 */
abstract class BasePerfTest
    extends TestUtil
    implements XMLStreamConstants
{
    private final static int DEFAULT_TEST_SECS = 30;
    private final static int WARMUP_ROUNDS = 50;

    XMLInputFactory mFactory;

    protected int mBatchSize;

    protected abstract XMLInputFactory getFactory();

    protected void init()
    {
        mFactory = getFactory();
        if (mFactory != null) {
            System.out.println("Factory instance: "+mFactory.getClass());
            mFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
            //mFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);

            // Default is ns-aware, no need to re-set:
            //mFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
            //mFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
            mFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
            System.out.print("  coalescing: "+mFactory.getProperty(XMLInputFactory.IS_COALESCING));
            System.out.println(";  ns-aware: "+mFactory.getProperty(XMLInputFactory.IS_NAMESPACE_AWARE));
            System.out.println("  validating: "+mFactory.getProperty(XMLInputFactory.IS_VALIDATING));
        } else {
            System.out.println("Factory instance: <null>");
        }
    }

    private final byte[] readData(File f)
        throws IOException
    {
        int len = (int) f.length();
        byte[] data = new byte[len];
        int offset = 0;
        FileInputStream fis = new FileInputStream(f);
        
        while (len > 0) {
            int count = fis.read(data, offset, len-offset);
            offset += count;
            len -= count;
        }

        return data;
    }

    protected int testExec(byte[] data, String path) throws Exception
    {
        InputStream in = new ByteArrayInputStream(data);

        // !!! TEST: whether to reuse factory or not:
        XMLStreamReader sr = mFactory.createXMLStreamReader(path, in);
        //XMLStreamReader sr = getFactory().createXMLStreamReader(path, in);

        int ret = testExec2(sr);
        in.close();
        return ret;
    }

    protected int testExec2(XMLStreamReader sr) throws Exception
    {
        int total = 0;

        while (sr.hasNext()) {

            int type = sr.next();

            total += type; // so it won't be optimized out...

            //if (sr.hasText()) {
            if (type == CHARACTERS || type == CDATA || type == COMMENT) {
                // Test (a): just check length (no buffer copy)

                /*
                int textLen = sr.getTextLength();
                total += textLen;
                */

                // Test (b): access internal read buffer
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
        File f = new File(fn).getAbsoluteFile();
        /* 14-Apr-2007, TSa: Some parsers (like Sjsxp/Zephyr) have
         *   (performance) problems with non-URL system ids. While this
         *   could be viewed as problem with those impls, let's try not
         *   to skew results, but instead make system ids more palatable.
         */
        //String path = f.getAbsolutePath();
        String path = f.getAbsoluteFile().toURL().toExternalForm();

        // First, warm up:

        byte[] data = readData(f);

        System.out.println("Path: '"+path+"'");
        System.out.println("Warming up; doing  "+WARMUP_ROUNDS+" iterations (real test will run for "+SECS+" seconds): ");

        int total = 0; // to prevent any dead code optimizations
        for (int i = 0; i < WARMUP_ROUNDS; ) {
            // Let's estimate speed from the last warmup...
            if (++i == WARMUP_ROUNDS) {
                long now = System.currentTimeMillis();
                total = testExec(data, path);
                mBatchSize = calcBatchSize(System.currentTimeMillis() - now);
            } else {
                total = testExec(data, path);
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

        System.out.println("Total iterations done: "+count+" [done "+total+"]");
    }
}
