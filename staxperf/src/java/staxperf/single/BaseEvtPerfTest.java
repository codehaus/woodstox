package staxperf.single;

import java.io.*;

import javax.xml.stream.*;
import javax.xml.stream.events.XMLEvent;

/**
 * Base class for testing sustainable performance of event readers of
 * various StAX implementations. Basic operation is as follows:
 *<ul>
 * <li>First implementation is set up, and then <b>warmed up</b>, by doing
 *   couple (20) of repetitions over test document. This ensures JIT has
 *   had a chance to use hot spot compilation of performance bottlenecks
 *  </li>
 * <li>Main testing loop iterates over parsing as many times as possible,
 *    during test period (60 seconds), and reports number of iterations
 *    succesfully done.
 *  </li>
 *</ul>
 */
abstract class BaseEvtPerfTest
    implements XMLStreamConstants
{
    XMLInputFactory mFactory;
    XMLEventReader mEventReader;

    protected abstract XMLInputFactory getFactory();

    protected void init() {
        mFactory = getFactory();
        System.out.println("Factory instance: "+mFactory.getClass());
        mFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
        //mFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        mFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
        //mFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
        mFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
        System.out.println("  coalescing: "+mFactory.getProperty(XMLInputFactory.IS_COALESCING));
    }

    protected int testExec(InputStream in) throws Exception
    {
        mEventReader = mFactory.createXMLEventReader(in);

        int total = 0;
        while (mEventReader.hasNext()) {
            XMLEvent evt = mEventReader.nextEvent();
            if (evt.isStartElement()) {
                ; // nothing to do for now
            }
            total += evt.getEventType(); // to prevent dead code elimination
        }
        return total;
    }

    public void test(String[] args)
        throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java ... "+getClass().getName()+" [file]");
            System.exit(1);
        }

        init();

        String filename = args[0];

        // First, warm up:
        final int WARMUP_ROUNDS = 20;

        System.out.println("Warming up; doing  "+WARMUP_ROUNDS+" iterations.");

        int total = 0; // to prevent any dead code optimizations
        for (int i = 0; i < WARMUP_ROUNDS; ++i) {
            InputStream fin = new FileInputStream(filename);
            try {
                total = testExec(fin);
                //testFinish();
            } finally {
                fin.close();
            }
            System.out.print(".");
            // Let's allow some slack time between iterations
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
        }

        System.out.println(" [total: "+total+"]");

        /* Let's try to ensure GC is done so that real test can start from
         * a clean state.
         */
        try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
        System.gc();
        try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
        System.gc();
        try {  Thread.sleep(100L); } catch (InterruptedException ie) { }

        //final int TEST_PERIOD = 60;
        final int TEST_PERIOD = 30;

        System.out.println("Ok, warmup done. Now doing real testing, running for "+TEST_PERIOD+" seconds.");

        long nextTime = System.currentTimeMillis();
        long endTime = nextTime + (TEST_PERIOD * 1000);
        int count = 0;
        total = 0;
        int subtotal = 0;
        final long SUB_PERIOD = 1000L; // print once a second
        nextTime += SUB_PERIOD;

        while (true) {
            InputStream fin = new java.io.FileInputStream(filename);
            try {
                total += testExec(fin);
                //testFinish();
            } finally {
                fin.close();
            }
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

        System.out.println("Done ["+total+"]!");
        System.out.println("Total iterations done: "+count);
    }
}
