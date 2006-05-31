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

    protected abstract XMLInputFactory getInputFactory();
    protected abstract XMLOutputFactory getOutputFactory();

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

    protected int testExec(CharArrayReader in, ByteArrayOutputStream out) throws Exception
    {
        XMLStreamReader2 mStreamReader = (XMLStreamReader2)mInFactory.createXMLStreamReader(in);
        XMLStreamWriter2 mStreamWriter = (XMLStreamWriter2)mOutFactory.createXMLStreamWriter(out, "UTF-8");

        int total = 0;
        while (mStreamReader.hasNext()) {
            int type = mStreamReader.next();
            total += type; // so it won't be optimized out...

            // Let's do a full copy, simplest:
            mStreamWriter.copyEventFromReader(mStreamReader, false);
        }
        mStreamReader.close();
        mStreamWriter.close();
        total += out.size();
        return total;
    }

    protected void printInitial(char[] data)
        throws IOException
    {
        int len = data.length;
        String doc = new String(data);
        if (len > 500) {
            doc = doc.substring(0, 248) + "]...["+doc.substring(len-248);
        }
        System.out.println("Output document: ("+len+"; condensed if above 500 chars)["+doc+"]");
    }

    private final int readData(File f, CharArrayWriter w)
        throws IOException
    {
        Reader r = new InputStreamReader(new FileInputStream(f), "UTF-8");
        char[] buf = new char[8000];
        int count;

        while ((count = r.read(buf)) > 0) {
            w.write(buf, 0, count);
        }
        w.flush();
        w.close();
        r.close();
        return (int) f.length();
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

        CharArrayWriter cw = new CharArrayWriter();
        int len = readData(f, cw);
        char[] data = cw.toCharArray();
        System.out.println("Read in data; "+len+" bytes, "+data.length+" chars.");
        ByteArrayOutputStream out = new ByteArrayOutputStream(len);

        int total = 0; // to prevent any dead code optimizations
        for (int i = 0; i < WARMUP_ROUNDS; ++i) {
            CharArrayReader in = new CharArrayReader(data);
            total = testExec(in, out);
            in.close();
            out.reset();
            if (i == 0) {
                printInitial(data);
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
            CharArrayReader in = new CharArrayReader(data);
            total = testExec(in, out);
            in.close();
            out.reset();
            in = new CharArrayReader(data);
            total = testExec(in, out);
            in.close();
            out.reset();
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
                int ch = (subtotal >> 1) - 1;
                if (ch > 35) {
                    c = '+';
                } else if (ch > 9) {
                    c = (char) ('a' + (ch-10));
                } else {
                    c = (char) ('0' + ch);
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
