package staxperf.staxcopy;

import java.io.*;

import javax.xml.stream.*;
import org.codehaus.stax2.*;

/**
 * Base class for testing sustainable copy (read-plus-write) performance of
 * StAX implementations. Basic operation is as follows:
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
abstract class BaseCopyTest
    implements XMLStreamConstants
{
    private final int DEFAULT_TEST_SECS = 30;

    XMLInputFactory mInFactory;
    XMLOutputFactory mOutFactory;
    XMLStreamReader2 mStreamReader;
    XMLStreamWriter2 mStreamWriter;

    protected abstract XMLInputFactory2 getInputFactory();
    protected abstract XMLOutputFactory2 getOutputFactory();

    byte[] mOutputBytes = null;

    protected void init()
    {
        mInFactory = getInputFactory();
        if (mInFactory != null) {
            System.out.println("In factory instance: "+mInFactory.getClass());
            mInFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);

            // Default is ns-aware, no need to re-set:
            //mInFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
            //mInFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
            mInFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
            System.out.print("  coalescing: "+mInFactory.getProperty(XMLInputFactory.IS_COALESCING));
            System.out.println(";  ns-aware: "+mInFactory.getProperty(XMLInputFactory.IS_NAMESPACE_AWARE));
            System.out.println("  validating: "+mInFactory.getProperty(XMLInputFactory.IS_VALIDATING));
        } else {
            System.out.println("In factory instance: <null>");
        }

        mOutFactory = getOutputFactory();
        if (mOutFactory != null) {
            System.out.println("Out factory instance: "+mOutFactory.getClass());
            //mOutFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.FALSE);
            mOutFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);
            System.out.println("  repairing: "+mOutFactory.getProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES));
        } else {
            System.out.println("Out factory instance: <null>");
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
        mStreamReader = (XMLStreamReader2)mInFactory.createXMLStreamReader(path, in);
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1000);
        mStreamWriter = (XMLStreamWriter2)mOutFactory.createXMLStreamWriter(bos, "UTF-8");

        int total = 0;
        while (mStreamReader.hasNext()) {
            int type = mStreamReader.next();
            total += type; // so it won't be optimized out...

            // Let's do a full copy, simplest:
            mStreamWriter.copyEventFromReader(mStreamReader, false);
        }
        mStreamWriter.close();
        byte[] outb = bos.toByteArray();
        total += outb.length;
        mOutputBytes = outb;
        return total;
    }

    public InputStream createStream(File f)
        throws IOException
    {
        return new FileInputStream(f);
    }

    protected void printInitial()
        throws IOException
    {
        int len = mOutputBytes.length;
        System.out.println("Output document: ("+len+")["+new String(mOutputBytes, "UTF-8")+"]");
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
            if (i == 0) {
                printInitial();
            }
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

        /* Let's try to reduce overhead of System.currentTimeMillis()
         * by calling test method twice each round. May be a problem for
         * big docs/slow readers... but otherwise not.
         */
        while (true) {
            total += testExec(f, path);
            total += testExec(f, path);
            //testFinish();
            long now = System.currentTimeMillis();
            if (now > endTime) {
                break;
            }
            count += 2;
            /* let's only print once a second... limits console overhead,
             * but still informs about progress.
             */
            subtotal += 2;
            if (now > nextTime) {
                char c;
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
