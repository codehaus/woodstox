package test;

import java.io.*;

import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.XMLEvent;

import com.ctc.wstx.stax.*;

public class TestEventReader
{
    final XMLInputFactory mFactory;

    public TestEventReader() {
        super();
        System.setProperty("javax.xml.stream.XMLInputFactory",
                           "com.ctc.wstx.stax.WstxInputFactory");
        XMLInputFactory f = XMLInputFactory.newInstance();
        mFactory = f;
        f.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
        //f.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        f.setProperty(XMLInputFactory.REPORTER, new TestReporter());

        // Uncomment for boundary-condition stress tests:
        if (f.isPropertySupported(WstxInputProperties.P_INPUT_BUFFER_LENGTH)) {
            f.setProperty(WstxInputProperties.P_INPUT_BUFFER_LENGTH,
                                 new Integer(16));
        }
        // And let's try to preserve structure as much as possible:
        if (f.isPropertySupported(WstxInputProperties.P_REPORT_PROLOG_WHITESPACE)) {
            f.setProperty(WstxInputProperties.P_REPORT_PROLOG_WHITESPACE, Boolean.TRUE);
        }

        System.out.println("Factory instance: "+f.getClass());
        System.out.println("  coalescing: "+f.getProperty(XMLInputFactory.IS_COALESCING));
    }

    public void test(String[] args)
        throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java ... "+getClass().getName()+" [file]");
            System.exit(1);
        }
        String filename = args[0];
        File file = new File(filename);
        Reader fin = new java.io.FileReader(file);

        // Let's pass generated system id:
        XMLEventReader er = mFactory.createXMLEventReader(file.toURL().toString(), fin);

        Writer out = new PrintWriter(System.out);
        //out.write("[START]\n");
        while (er.hasNext()) {
            XMLEvent evt = er.nextEvent();
// Uncomment for debugging:            
//System.err.println("["+evt.getEventType()+"]: '");
            evt.writeAsEncodedUnicode(out);
            //out.write("'\n");
            out.flush();
        }
        //out.write("[END]\n");
        out.flush();
    }

    public static void main(String[] args) throws Exception
    {
        // Uncomment for infinite looping (stress test)

        /*
        int count = 0;
        while (true) {
            ++count;
            System.err.println("#"+count);
        */
            new TestEventReader().test(args);
            /*
        }
            */
    }
}
