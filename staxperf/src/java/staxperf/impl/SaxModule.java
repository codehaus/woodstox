package staxperf.impl;

import java.io.*;
import javax.xml.parsers.*; // TRAX, for creating parsers

import org.w3c.dom.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import staxperf.PerfModule;

public final class SaxModule
    extends PerfModule
{
    final SAXParserFactory mFactory;
    final MyHandler mHandler;

    public int mDummyCount;

    public SaxModule(String clsName, boolean validating)
    {
        super(clsName + (validating ? " [vld]" : " [non-vld]"));
        System.setProperty("javax.xml.parsers.SAXParserFactory", clsName);
        mFactory = SAXParserFactory.newInstance();
        mFactory.setNamespaceAware(true);
        mFactory.setValidating(validating);
        //mFactory.setValidating(true);
        mHandler = new MyHandler();
    }

    public int runFor(int seconds, String systemId, byte[] data) throws Exception
    {
        long end = System.currentTimeMillis() + (seconds * 1000);
        int count = 0;
        int dummyCount = 0;

        do {
            ++count;
            SAXParser parser = mFactory.newSAXParser();
            XMLReader xr = parser.getXMLReader();

            mHandler.init();
            xr.setContentHandler((ContentHandler) mHandler);
            xr.parse(new InputSource(new ByteArrayInputStream(data)));
            dummyCount += mHandler.getTotal();
        } while (System.currentTimeMillis() < end);

        mDummyCount = dummyCount;

        return count;
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

        public void init() {
            total = 0;
        }

        public void characters(char[] ch, int start, int length) {
            total += length;
        }

        public void startElement(String name, AttributeList al) {
            total += 1;
        }

        public void endElement(String name, AttributeList al) {
            total += 1;
        }

        public void processingInstruction(String target, String data) {
            total += target.length();
        }

        public int getTotal() {
            return total;
        }
   }
}
