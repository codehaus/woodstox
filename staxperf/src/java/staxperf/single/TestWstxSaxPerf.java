package staxperf.single;

import java.io.*;
import javax.xml.parsers.*; // TRAX, for creating parsers

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.stream.XMLInputFactory;

public class TestWstxSaxPerf
    extends BasePerfTest
{
    final SAXParserFactory mFactory;
    final MyHandler mHandler;
    final XMLReader mReader;

    private TestWstxSaxPerf()
        throws Exception
    {
        super();
        System.setProperty("javax.xml.parsers.SAXParserFactory",
                           "com.ctc.wstx.sax.WstxSAXParserFactory");
        mFactory = SAXParserFactory.newInstance();
        mFactory.setNamespaceAware(true);
        mFactory.setValidating(false);
        //mFactory.setValidating(true);
        System.out.println("SAX factory: "+mFactory.getClass());
        mHandler = new MyHandler();
        SAXParser parser = mFactory.newSAXParser();
        mReader = parser.getXMLReader();
    }

    protected XMLInputFactory getFactory()
    {
        return null;
    }

    protected int testExec2(InputStream in, String path) throws Exception
    {
        //XMLReader xr = mParser.getXMLReader();
        XMLReader xr = mReader;
        xr.setContentHandler((ContentHandler) mHandler);
        xr.parse(new InputSource(in));
        return mHandler.getTotal();
    }

    public static void main(String[] args) throws Exception
    {
        new TestWstxSaxPerf().test(args);
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
