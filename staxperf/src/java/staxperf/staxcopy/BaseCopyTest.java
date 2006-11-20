package staxperf.staxcopy;

import java.io.*;
import java.util.*;

import javax.xml.stream.*;
import org.codehaus.stax2.*;
import org.codehaus.stax2.evt.XMLEvent2;

import staxperf.TestUtil;

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
    extends TestUtil
    implements XMLStreamConstants
{
    final static int DEFAULT_TEST_SECS = 30;
    final static int WARMUP_ROUNDS = 50;

    XMLInputFactory mInFactory;
    XMLOutputFactory mOutFactory;

    XMLEvent2[] mEvents;

    char[] mRawData;

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
            mOutFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.FALSE);
            //mOutFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);
            System.out.println("  repairing: "+mOutFactory.getProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES));
        } else {
            System.out.println("Out factory instance: <null>");
        }
    }

    // Slow read/write, using stream reader + writer combo
    /*
    protected final int testExec(CharArrayReader in, ByteArrayOutputStream out) throws Exception
    {
        XMLStreamReader2 mStreamReader = (XMLStreamReader2)mInFactory.createXMLStreamReader(in);
        XMLStreamWriter2 mStreamWriter = (XMLStreamWriter2)mOutFactory.createXMLStreamWriter(out, "UTF-8");

        while (mStreamReader.hasNext()) {
            int type = mStreamReader.next();
            // Let's do a full copy, simplest:
            mStreamWriter.copyEventFromReader(mStreamReader, false);
        }
        mStreamReader.close();
        mStreamWriter.close();
        return out.size();
    }
    */

    // Faster output-events variation:
    protected final int testExec(CharArrayReader in, ByteArrayOutputStream out) throws Exception
    {
        XMLStreamWriter2 sw = (XMLStreamWriter2)mOutFactory.createXMLStreamWriter(out, "UTF-8");
        //XMLStreamWriter2 sw = (XMLStreamWriter2)mOutFactory.createXMLStreamWriter(out, "ISO-8859-1");
        XMLEvent2[] events = mEvents;

        for (XMLEvent2 evt : events) {
            evt.writeUsing(sw);
        }
        sw.close();
        return out.size();
    }

    // Real output composition, but no real writer
    /*
    protected final int testExec(CharArrayReader in, ByteArrayOutputStream out) throws Exception
    {
        Writer w = new DummyWriter();
        XMLStreamWriter2 sw = (XMLStreamWriter2)((XMLOutputFactory2)mOutFactory).createXMLStreamWriter(w, "UTF-8");
        XMLEvent2[] events = mEvents;
        for (XMLEvent2 evt : events) {
            evt.writeUsing(sw);
        }
        sw.close();
        return out.size();
    }
    */

    // UTF-8 backed pseudo-test; compared as baseline
    /*
    protected final int testExec(CharArrayReader in, ByteArrayOutputStream out) throws Exception
    {
        char[] data = mRawData;
        final int CHUNK = 4000;
        int offset = 0;
        OutputStreamWriter w = new OutputStreamWriter(out, "UTF-8");
        int amt;
        while ((amt = data.length - offset) > 0) {
            if (amt > CHUNK) {
                amt = CHUNK;
            }
            w.write(data, offset, amt);
            offset += amt;
        }
        w.close();
        return out.size();
    }
    */

    protected void printInitial(char[] data)
    {
        int len = data.length;
        String doc = new String(data);
        if (len > 200) {
            doc = doc.substring(0, 98) + "]...["+doc.substring(len-98);
        }
        System.out.println("Output document: ("+len+" chars; condensed if above 200 chars)["+doc+"]");
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
        CharArrayWriter cw = new CharArrayWriter();
        int len = readData(f, cw);
        mRawData = cw.toCharArray();
        System.out.println("Read in data; "+len+" bytes, "+mRawData.length+" chars.");
        testReadWrite(SECS, new ByteArrayOutputStream(len));
    }

    private void testReadWrite(int SECS, ByteArrayOutputStream out)
        throws Exception
    {
        mEvents = readEvents(mRawData);

        final int batchSize = findBatchSize(out, WARMUP_ROUNDS, true);
        // And then let's do bit more warmup, with 5x batch size
        findBatchSize(out, batchSize*5, false);

        System.out.println(" (warmup done, let's GC and sleep a bit)");
        /* Let's try to ensure GC is done so that real test can start from
         * a clean state.
         */
        try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
        System.gc();
        try {  Thread.sleep(200L); } catch (InterruptedException ie) { }
        System.gc();
        try {  Thread.sleep(100L); } catch (InterruptedException ie) { }

        System.out.println("Ok. Now doing real testing, running for "+SECS+" seconds.");

        long nextTime = System.currentTimeMillis();
        long endTime = nextTime + (SECS * 1000);
        int count = 0;
        int total = 0; // to prevent any dead code optimizations
        int subtotal = 0;
        final long SUB_PERIOD = 1000L; // print once a second
        nextTime += SUB_PERIOD;

        while (true) {
            for (int i = 0; i < batchSize; ++i) {
                CharArrayReader in = new CharArrayReader(mRawData);
                total += testExec(in, out);
                in.close();
                out.reset();
            }
            count += batchSize;
            long now = System.currentTimeMillis();
            if (now > endTime) {
                break;
            }
            // let's only print once a second... limits console overhead
            ++subtotal;
            if (now > nextTime) {
                printProgress(subtotal);
                subtotal = 0;
                nextTime += SUB_PERIOD;
                if (nextTime < now) {
                    nextTime = now;
                }
            }
        }

        System.out.println("Total iterations done: "+count+" [done "+total+"]");
    }

    private XMLEvent2[] readEvents(char[] data)
        throws XMLStreamException
    {
        Reader cr = new CharArrayReader(data);
        XMLEventReader er = mInFactory.createXMLEventReader(cr);
        ArrayList al = new ArrayList();
        while (er.hasNext()) {
            al.add(er.nextEvent());
        }
        er.close();
        XMLEvent2[] result = new XMLEvent2[al.size()];
        return (XMLEvent2[]) al.toArray(result);
    }

    private final void printProgress(int subtotal)
    {
        char c;
        int ch = subtotal - 1;
        if (ch > 35) {
            c = '+';
        } else if (ch > 9) {
            c = (char) ('a' + (ch-10));
        } else {
            c = (char) ('0' + ch);
        }
        System.out.print(c);
    }

    private int findBatchSize(ByteArrayOutputStream out, int reps, boolean initial)
        throws Exception
    {
        // First, warm up:
        if (initial) {
            System.out.println("Warming up; doing  "+reps+" iterations, to determine batch size");
        }
        int total = 0;
        long now = 0L;

        for (int i = 0; i < reps; ) {
            CharArrayReader in = new CharArrayReader(mRawData);
            now = System.currentTimeMillis();
            total = testExec(in, out);
            now = System.currentTimeMillis() - now;
            in.close();
            out.reset();
            ++i;
            if (initial) {
                if (i == 1) {
                    System.err.println("[output size: "+total+"]");
                    printInitial(mRawData);
                } else {
                    System.out.print(".");
                }
                /* Let's allow some slack time between iterations (before
                 * the full batch at least, that is)
                 */
                try {  Thread.sleep(50L); } catch (InterruptedException ie) { }
            }
        }

        if (initial) {
            int batch = calcBatchSize(now);
            System.err.println("[batch size: last one took "+now+" msecs -> "+batch+"]");
            return batch;
        }
        return total;
    }

    final static class DummyWriter
        extends Writer
    {
        public Writer append(char c)
        {
            return this;
        }

        public Writer 	append(CharSequence csq)
        {
            return this;
        }

        public Writer 	append(CharSequence csq, int start, int end)
        {
            return this;
        }

        public void 	close()
        {
        }

        public void 	flush()
        {
        }

        public void 	write(char[] cbuf)
        {
        }

        public void 	write(char[] cbuf, int off, int len)
        {
        }

        public void 	write(int c)
        {
        }

        public void 	write(String str)
        {
        }

        public void 	write(String str, int off, int len) 
        {
        }
    }
}
