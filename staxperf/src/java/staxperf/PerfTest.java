package staxperf;

import java.io.*;
import java.util.*;

import staxperf.impl.*;

public class PerfTest
{
    final static int TEST_SECONDS = 60;
    //final static int TEST_SECONDS = 20;

    /**
     * Let's do warmup for 15 seconds
     */
    final static int WARMUP_SECS = 15;
    //final static int WARMUP_SECS = 2;

    /**
     * And allow 15 seconds before and after GC, between repetitions
     */
    final static int GC_MSECS = 15000;
    //final static int GC_MSECS = 5000;

    final static int ROUNDS = 3;

    final static int REPS = 3;
    //final static int REPS = 1;

    final PerfModule[] mModules;

    final byte[] mData;

    final String mSystemId;

    protected PerfTest(String filename)
        throws IOException
    {
        mModules = new PerfModule[] {

            // First, StAX in stream reading mode:
            new StaxModule("com.ctc.wstx.stax.WstxInputFactory", false),
            new StaxModule("com.bea.xml.stream.MXParserFactory", false),
            //new StaxModule("com.ctc.wstx.stax.WstxInputFactory", true),

            // Then, StAX in event reading mode:
            /*
            new StaxEventModule("com.ctc.wstx.stax.WstxInputFactory", false),
            new StaxEventModule("com.bea.xml.stream.MXParserFactory", false),
            //new StaxEventModule("com.ctc.wstx.stax.WstxInputFactory", true),
            */

            // Then SAX
            new SaxModule("com.bluecast.xml.JAXPSAXParserFactory", false),

            new SaxModule("org.apache.xerces.jaxp.SAXParserFactoryImpl", false),
            new SaxModule("org.apache.crimson.jaxp.SAXParserFactoryImpl", false),

            // Too bad: this seems to break for some reason?!?
            //new SaxModule("org.apache.xerces.jaxp.SAXParserFactoryImpl", true),

            // And finally DOM
            new DomModule("org.apache.xerces.jaxp.DocumentBuilderFactoryImpl"),
            new DomModule("org.apache.crimson.jaxp.DocumentBuilderFactoryImpl"),
            // and JDOM
            new JDomModule("JDom/Wstx", "com.ctc.wstx.stax.WstxInputFactory", false),
            new JDomModule("JDom/Stax-RI", "com.bea.xml.stream.MXParserFactory", false),
            new JDomSaxModule("JDom/Sax/Xerces", "org.apache.xerces.parsers.SAXParser", false),

        };

        File f = new File(filename);
        mSystemId = f.getAbsolutePath();
        FileInputStream fin = new FileInputStream(f);
        try {
            byte[] buf = new byte[16000];
            ByteArrayOutputStream bos = new ByteArrayOutputStream((int) f.length() + 16);
            int count;

            while ((count = fin.read(buf)) > 0) {
                bos.write(buf, 0, count);
            }
            mData = bos.toByteArray();
        } finally {
            fin.close();
        }
    }

    protected void test(int rounds, int reps, int seconds)
        throws Exception
    {
        // First, let's initialize tests:
        int modCount = mModules.length;
        for (int i = 0; i < modCount; ++i) {
            mModules[i].init(rounds);
        }

        for (int round = 0; round < rounds; ++round) {
            System.out.println();
            System.out.println("ROUND "+(round+1)+" / "+rounds);
            System.out.println();
            for (int i = 0; i < modCount; ++i) {
                PerfModule mod = mModules[i];
                System.out.println("Module '"+mod.getImplName()+"': warmup...");

                // First, warming up for N seconds:
                mod.runFor(WARMUP_SECS, mSystemId, mData);

                try {
                    Thread.sleep(GC_MSECS);
                    System.gc();
                    Thread.sleep(GC_MSECS);
                } catch (InterruptedException ie) { }

                int[] results = new int[reps];

                System.out.print("  ");
                for (int rep = 0; rep < reps; ++rep) {
                    if (rep > 0) {
                        System.out.print(", ");
                    }
                    System.out.print("rep #"+(rep+1));
                    int count = mod.runFor(seconds, mSystemId, mData);
                    results[rep] = count;
                    System.out.print(": "+count);
                    try {
                        Thread.sleep(GC_MSECS);
                        System.gc();
                        Thread.sleep(GC_MSECS);
                    } catch (InterruptedException ie) { }
                }
                System.out.println();

                // Ok, done, let's sort them!
                Arrays.sort(results);
                int med = results[reps / 2];
                int high = results[results.length-1];
                mod.addResult(round, med, high);
                System.out.println("  -> "+med+" - "+high);
            }
        }

        // All done!
        
        System.out.println();
        System.out.println("DONE:");
        System.out.println();

        // Let's order them in natural (asceding) order by best count, first:
        TreeMap m = new TreeMap();
        for (int i = 0; i < modCount; ++i) {
            PerfModule mod = mModules[i];
            mod.finalizeResults();
            Integer topCount = new Integer(mod.getTopResult());
            m.put(topCount, mod);
        }

        // Then reverse ordering:
        Iterator it = m.values().iterator();
        PerfModule[] mods = new PerfModule[modCount];
        for (int i = 1; i <= modCount; ++i) {
            PerfModule mod = (PerfModule) it.next();
            mods[mods.length - i] = mod;
        }

        // and display:
        for (int i = 0; i < modCount; ++i) {
            PerfModule mod = mods[i];
            System.out.print("#"+(i+1)+": ");
            System.out.print(mod.getTopResult());
            System.out.print(" (max) ");
            System.out.print(mod.getMedianResult());
            System.out.print(" (med) ");
            System.out.println("'"+mod.getImplName()+"'");
        }
    }

    public static void main(String[] args)
        throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: "+PerfTest.class+" <xml file>");
            System.exit(1);
        }

        /* Let's have 3 (major) rounds; 3 repetitions each; each set
         * running for 60 seconds
         */
        try {
            new PerfTest(args[0]).test(ROUNDS, REPS, TEST_SECONDS);
        } catch (Throwable t) {
            System.err.println("Problem: "+t);
            t.printStackTrace();
        }
    }
}
