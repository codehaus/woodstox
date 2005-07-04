package staxperf.impl;

import java.io.*;
import javax.xml.parsers.*; // TRAX, for creating parsers

import org.w3c.dom.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import staxperf.PerfModule;

public final class DomModule
    extends PerfModule
{
    final DocumentBuilderFactory mFactory;

    public int mDummyCount;

    public DomModule(String clsName) {
        super(clsName);
        System.setProperty("javax.xml.parsers.DocumentBuilderFactory", clsName);
        mFactory = DocumentBuilderFactory.newInstance();
        mFactory.setCoalescing(false);
        mFactory.setNamespaceAware(true);
        mFactory.setValidating(false);
    }

    public int runFor(int seconds, String systemId, byte[] data) throws Exception
    {
        long end = System.currentTimeMillis() + (seconds * 1000);
        int count = 0;
        int dummyCount = 0;

        do {
            ++count;
            DocumentBuilder dp = mFactory.newDocumentBuilder();
            Document doc = dp.parse(new InputSource(new ByteArrayInputStream(data)));
            dummyCount ^= doc.hashCode();
        } while (System.currentTimeMillis() < end);

        mDummyCount = dummyCount;

        return count;
    }
}
