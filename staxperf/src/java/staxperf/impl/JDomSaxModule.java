package staxperf.impl;

import java.io.*;

import javax.xml.stream.*;

import org.xml.sax.InputSource;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;

import staxperf.PerfModule;

public final class JDomSaxModule
    extends PerfModule
{
    final SAXBuilder mBuilder;

    public int mDummyCount;

    public JDomSaxModule(String name, String clsName, boolean validating)
    {
        super(name + (validating ? " [vld]" : " [non-vld]"));
        mBuilder = new SAXBuilder(clsName, validating);
    }

    public int runFor(int seconds, String systemId, byte[] data) throws Exception
    {
        long end = System.currentTimeMillis() + (seconds * 1000);
        int count = 0;
        int dummyCount = 0;
        SAXBuilder builder = mBuilder;

        do {
            ++count;

            InputSource in = new InputSource(new ByteArrayInputStream(data));
            Document domDoc = builder.build(in);
            dummyCount += 1;
        } while (System.currentTimeMillis() < end);

        mDummyCount = dummyCount;

        return count;
    }
}
