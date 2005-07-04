package staxperf.single;

import java.io.*;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

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
    implements XMLStreamConstants
{
    private final int DEFAULT_TEST_SECS = 30;

    XMLInputFactory mFactory;
    XMLStreamReader mStreamReader;

    protected abstract XMLInputFactory getFactory();

    protected void init()
    {
        mFactory = getFactory();
        if (mFactory != null) {
            System.out.println("Factory instance: "+mFactory.getClass());
            mFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
            //mFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
            mFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
            //mFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
            mFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
            System.out.print("  coalescing: "+mFactory.getProperty(XMLInputFactory.IS_COALESCING));
            System.out.println(";  ns-aware: "+mFactory.getProperty(XMLInputFactory.IS_NAMESPACE_AWARE));
            System.out.println("  validating: "+mFactory.getProperty(XMLInputFactory.IS_VALIDATING));
        } else {
            System.out.println("Factory instance: <null>");
        }
    }

    protected int testExec(File f, String path) throws Exception
    {
        InputStream in = null;

        try {
            in = createStream(f);
            return testExec2(in, path);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    protected int testExec2(InputStream in, String path) throws Exception
    {
        //mStreamReader = mFactory.createXMLStreamReader(r);
        mStreamReader = mFactory.createXMLStreamReader(path, in);

        int total = 0;
        while (mStreamReader.hasNext()) {
            int type = mStreamReader.next();
            total += type; // so it won't be optimized out...

            //if (mStreamReader.hasText()) {
            if (type == CHARACTERS || type == CDATA) {
                // Test (a): just check length (no buffer copy)

                /*
                int textLen = mStreamReader.getTextLength();
                total += textLen;
                */

                // Test (b): access internal read buffer

                char[] text = mStreamReader.getTextCharacters();
                int start = mStreamReader.getTextStart();
                int len = mStreamReader.getTextLength();
                if (text != null) { // Ref. impl. returns nulls sometimes
                    total += text.length; // to prevent dead code elimination
                }

                // Test (c): Access internal buffer (medium)

                /*
                if (type == CHARACTERS || type == CDATA) {
                    System.out.println("Text (ws = "+mStreamReader.isWhiteSpace()+") = '"+text+"'.");
                } else {
                    System.out.println("Text = '"+text+"'.");
                }
                */
            }
        }
        return total;
    }

    public Reader createReader(File f)
        throws IOException
    {
        FileInputStream fin = new FileInputStream(f);
        //InputStreamReader inr = new InputStreamReader(fin, "UTF-8");
        InputStreamReader inr = new InputStreamReader(fin, "ISO-8859-1");
        //InputStreamReader inr = new InputStreamReader(fin);

        return inr;
    }

    public InputStream createStream(File f)
        throws IOException
    {
        return new FileInputStream(f);
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
        String path = f.getAbsolutePath();

        // First, warm up:
        final int WARMUP_ROUNDS = 30;

        System.out.println("Warming up; doing  "+WARMUP_ROUNDS+" iterations (real test will run for "+SECS+" seconds): ");

        int total = 0; // to prevent any dead code optimizations
        for (int i = 0; i < WARMUP_ROUNDS; ++i) {
            total = testExec(f, path);
            //testFinish();
            System.out.print(".");
            // Let's allow some slack time between iterations
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
        }

        //System.out.println(" [total: "+total+"]");

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

        while (true) {
            total += testExec(f, path);
            //testFinish();
            long now = System.currentTimeMillis();
            if (now > endTime) {
                break;
            }
            ++count;
            /* let's only print once a second... limits console overhead,
             * but still informs about progress.
             */
            ++subtotal;
            if (now > nextTime) {
                System.out.print((subtotal > 9) ? '+' :
                                 (char) ('0' + subtotal));
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
