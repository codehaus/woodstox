package staxperf.single;

import java.io.Reader;
import javax.xml.parsers.*; // TRAX, for creating parsers

import org.w3c.dom.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.stream.XMLInputFactory;

//import com.ctc.wstx.stax.*;

public class TestCrimsonSaxPerf
    extends BasePerfTest
{
    final SAXParserFactory mFactory;
    final MyHandler mHandler;

    private TestCrimsonSaxPerf() {
        super();
        System.setProperty("javax.xml.parsers.SAXParserFactory",
                           "org.apache.crimson.jaxp.SAXParserFactoryImpl");
        mFactory = SAXParserFactory.newInstance();
        mFactory.setNamespaceAware(true);
        mFactory.setValidating(false);
        //mFactory.setValidating(true); // to use param. entities in int subset
        System.out.println("SAX factory: "+mFactory.getClass());
        mHandler = new MyHandler();
    }

    protected XMLInputFactory getFactory()
    {
        return null;
    }

    protected int testExec(Reader r) throws Exception
    {
        SAXParser parser = mFactory.newSAXParser();
        XMLReader xr = parser.getXMLReader();

        xr.setContentHandler((ContentHandler) mHandler);
        InputSource in = new InputSource(r);
        // Let's set system id to get relative references ok
        in.setSystemId(new java.io.File(".").getAbsoluteFile().toURL().toString());
        xr.parse(in);
        return mHandler.getTotal();
    }

    /*
    public void testFinish()
        throws Exception
    {
        com.ctc.wstx.util.SymbolTable symt = msc.getSymbolTable();
        double seek = symt.calcAvgSeek();
        seek = ((int) (100.0  * seek)) / 100.0;
        System.out.println("Symbol count: "+symt.size()+", avg len: "+seek+".");
    }
    */

    public static void main(String[] args) throws Exception
    {
        new TestCrimsonSaxPerf().test(args);
    }

    /**
     * Dummy handler; used to make sure all events are properly sent, and
     * JIT can not eliminate any dead code.
     */
    final static class MyHandler
        extends DefaultHandler
    {
        int total = 0;

        public MyHandler() {
        }

        public void characters(char[] ch, int start, int length) {
            total += length;
        }

        public void startElement(String name, AttributeList al) {
            total += 1;
        }

        public int getTotal() {
            return total;
        }
   }
}
