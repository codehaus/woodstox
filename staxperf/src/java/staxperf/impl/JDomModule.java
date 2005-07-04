package staxperf.impl;

import java.io.*;

import javax.xml.stream.*;

import org.jdom.Document;
import org.jdom.input.StAXBuilder;

import staxperf.PerfModule;

public final class JDomModule
    extends PerfModule
{
    final XMLInputFactory mFactory;

    public int mDummyCount;

    public JDomModule(String name, String clsName, boolean validating)
    {
        super(name + (validating ? " [vld]" : " [non-vld]"));
        System.setProperty("javax.xml.stream.XMLInputFactory", clsName);
        mFactory =  XMLInputFactory.newInstance();
        mFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
        mFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
        mFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES,
                             Boolean.TRUE);
        mFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.TRUE);
        mFactory.setProperty(XMLInputFactory.IS_VALIDATING,
                             validating ? Boolean.TRUE : Boolean.FALSE);
    }

    public int runFor(int seconds, String systemId, byte[] data) throws Exception
    {
        long end = System.currentTimeMillis() + (seconds * 1000);
        int count = 0;
        int dummyCount = 0;

        StAXBuilder builder = new StAXBuilder();

        do {
            ++count;

            InputStream in = new ByteArrayInputStream(data);
            XMLStreamReader sr = mFactory.createXMLStreamReader(systemId, in);
            Document domDoc = builder.build(sr);
            dummyCount += 1;
        } while (System.currentTimeMillis() < end);

        mDummyCount = dummyCount;

        return count;
    }
}
