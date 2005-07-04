package staxperf.impl;

import java.io.*;

import javax.xml.stream.*;

import staxperf.PerfModule;

public final class StaxModule
    extends PerfModule
    implements XMLStreamConstants
{
    final XMLInputFactory mFactory;

    /**
     * Variable to store dummy counter; trying to ensure JIT won't eliminate
     * dead code...
     */
    public int mDummyCount;

    public StaxModule(String clsName, boolean validating)
    {
        super(clsName + (validating ? " [vld]" : " [non-vld]"));
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

        do {
            ++count;
            XMLStreamReader sr = mFactory.createXMLStreamReader
                (systemId, new ByteArrayInputStream(data));
            while (sr.hasNext()) {
                int type = sr.next();
                dummyCount += type;

                if (type == CHARACTERS || type == CDATA) {
                    //str = sr.getText();
                    char[] ch = sr.getTextCharacters();
                    dummyCount+= ch.length + sr.getTextStart() + sr.getTextLength();
                } else if (type == COMMENT) {
                    //str = sr.getText();
                    char[] ch = sr.getTextCharacters();
                    dummyCount+= ch.length + sr.getTextStart() + sr.getTextLength();
                } else if (type == PROCESSING_INSTRUCTION) {
                    String str = sr.getPIData();
                    dummyCount += str.length();
                } else {
                    continue;
                }
            }

        } while (System.currentTimeMillis() < end);

        mDummyCount = dummyCount;

        return count;
    }
}

