package test;

import java.io.*;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.xml.stream.*;

import org.codehaus.stax2.LocationInfo;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamReader2;

import com.ctc.wstx.api.WstxInputProperties;

/**
 * Simple stream speed stress test, useful for profiling, as well as for
 * quickly checking high-level performance effects of changes (albeit
 * not very accurately, obviously -- need longer running composite
 * tests for such verifications)
 */
public class TestStreamSpeed
    implements XMLStreamConstants
{
    final XMLInputFactory mInputFactory;

    private TestStreamSpeed() {
        System.setProperty("javax.xml.stream.XMLInputFactory",
                           "com.ctc.wstx.stax.WstxInputFactory");

        XMLInputFactory f = XMLInputFactory.newInstance();
        System.out.println("Factory instance: "+f.getClass());

        //f.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
        f.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        f.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
        //f.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
        f.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES,
                      //Boolean.FALSE
                      Boolean.TRUE
                      );

        f.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.TRUE);
        f.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);

        f.setProperty(XMLInputFactory.REPORTER, new TestReporter());

        f.setProperty(XMLInputFactory.RESOLVER, new TestResolver1());

        if (f.isPropertySupported(XMLInputFactory2.P_REPORT_PROLOG_WHITESPACE)) {
            f.setProperty(XMLInputFactory2.P_REPORT_PROLOG_WHITESPACE,
                          Boolean.FALSE
                          //Boolean.TRUE
            );
        }

        /*
        if (f.isPropertySupported(WstxInputProperties.P_MIN_TEXT_SEGMENT)) {
            f.setProperty(WstxInputProperties.P_MIN_TEXT_SEGMENT,
                          new Integer(23));
        }
        */

        /*
        if (f.isPropertySupported(WstxInputProperties.P_BASE_URL)) {
            f.setProperty(WstxInputProperties.P_BASE_URL, file.toURL());
        }
        */

        /*
        System.out.print("Coalesce: "+f.getProperty(XMLInputFactory.IS_COALESCING));
        System.out.println(", NS-aware: "+f.getProperty(XMLInputFactory.IS_NAMESPACE_AWARE));
        System.out.print("Entity-expanding: "+f.getProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES));
        System.out.println(", validating: "+f.getProperty(XMLInputFactory.IS_VALIDATING));
        */

        mInputFactory = f;
    }

    protected int test(File file)
        throws Exception
    {
        InputStream in = new FileInputStream(file);

        // Let's deal with gzipped files too?
        if (file.getName().endsWith(".gz")) {
            System.out.println("[gzipped input file!]");
            in = new GZIPInputStream(in);
        }

        byte[] data = readData(in);
        System.out.println(" -> "+data.length+" bytes read.");

        return test2(data);
    }

    protected int test2(byte[] data)
        throws Exception
    {
        int total = 0;

        long orig = System.currentTimeMillis();
        int count = 5;

        System.out.print("First, warming up: ");
        while (--count >= 0) {
            total += test3(data);
            System.out.print(".");
        }
        System.out.println();
        System.out.println("Ok, great. Then the actual test:");

        count = 150;
        while (--count >= 0) {
            long now = System.currentTimeMillis();
            total += (test3(data) & 0xFF);
            System.out.println("Took: "+(System.currentTimeMillis() - now)+" msecs.");


            // Let's allow some time for other tasks too:
            try {
                System.gc();
                Thread.sleep(50L); // 50 msecs... is it enough?
            } catch (InterruptedException ioe) { }
        }

        System.out.println();
        System.out.println("Total time: "+(System.currentTimeMillis()-orig)+" msecs.");

        return count;
    }

    protected int test3(byte[] data)
        throws Exception
    {
        XMLInputFactory f = mInputFactory;
        XMLStreamReader2 sr = (XMLStreamReader2) f.createXMLStreamReader
            (new ByteArrayInputStream(data)
             //,"ISO-8859-1"
             //,"UTF-8"
             );
        int result = 0;

        while (sr.hasNext()) {
            int type = sr.next();
            if (type == CHARACTERS) { // let's prevent skipping
                result += sr.getTextLength();
            }
            result += type; // so it won't be optimized out...
        }
        return result;
    }

    public static void main(String[] args)
        throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java ... "+TestStreamSpeed.class+" [file]");
            System.exit(1);
        }

        try {
            int total = new TestStreamSpeed().test(new File(args[0]));
            System.out.println("Total: "+total);
        } catch (Throwable t) {
          System.err.println("Error: "+t);
          t.printStackTrace();
        }
    }

    public static byte[] readData(InputStream fin)
        throws IOException
    {
        try {
            byte[] buf = new byte[16000];
            ByteArrayOutputStream bos = new ByteArrayOutputStream(4000);
            int count;

            while ((count = fin.read(buf)) > 0) {
                bos.write(buf, 0, count);
            }
            return bos.toByteArray();
        } finally {
            fin.close();
        }
    }
}
