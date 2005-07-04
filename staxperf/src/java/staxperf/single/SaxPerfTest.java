package staxperf.single;

import java.io.*;

import javax.xml.parsers.*;
import org.xml.sax.helpers.DefaultHandler;

public final class SaxPerfTest
{
    SAXParserFactory mFactory;

    protected void init() {
        mFactory = SAXParserFactory.newInstance();
        System.out.println("Factory instance: "+mFactory.getClass());
        mFactory.setNamespaceAware(true);
        mFactory.setValidating(false);
    }

    protected int testExec(InputStream in, DefaultHandler dh)
        throws Exception
    {
        SAXParser p = mFactory.newSAXParser();

        p.parse(in, dh);
        return 1;
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
        final DefaultHandler dh = new DefaultHandler();

        // First, warm up:
        final int WARMUP_ROUNDS = 30;

        System.out.println("Warming up; doing  "+WARMUP_ROUNDS+" iterations.");

        int total = 0; // to prevent any dead code optimizations
        for (int i = 0; i < WARMUP_ROUNDS; ++i) {
            //InputStream fin = new java.io.FileInputStream(filename);
            InputStream fin = new BufferedInputStream(new java.io.FileInputStream(filename));
            try {
                total = testExec(fin, dh);
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
            //InputStream fin = new java.io.FileInputStream(filename);
            InputStream fin = new BufferedInputStream(new java.io.FileInputStream(filename));
            try {
                total += testExec(fin, dh);
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
                System.out.print((subtotal > 9) ? '+' :
                                 (char) ('0' + subtotal));
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

    public static void main(String[] args)
        throws Exception
    {
        new SaxPerfTest().test(args);
    }
}
