package staxperf.impl;

import java.io.*;

import javax.xml.stream.*;
import javax.xml.stream.events.XMLEvent;

import staxperf.PerfModule;

public final class StaxEventModule
    extends PerfModule
    implements XMLStreamConstants
{
    final XMLInputFactory mFactory;

    /**
     * Variable to store dummy counter; trying to ensure JIT won't eliminate
     * dead code...
     */
    public int mDummyCount;

    public StaxEventModule(String clsName, boolean validating)
    {
        super(clsName +"/EVT"+ (validating ? " [vld]" : " [non-vld]"));
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
            XMLEventReader sr = mFactory.createXMLEventReader
                (systemId, new ByteArrayInputStream(data));
            while (sr.hasNext()) {
                XMLEvent evt = sr.nextEvent();
                int type = evt.getEventType();
                dummyCount += type;

                /* No need to access data; assuming event Object must
                 * have already pretty much collected it all.
                 */

                if (type == CHARACTERS || type == CDATA) {
                    ;
                } else if (type == COMMENT) {
                    ;
                } else if (type == PROCESSING_INSTRUCTION) {
                    ;
                } else {
                    continue;
                }
            }

        } while (System.currentTimeMillis() < end);

        mDummyCount = dummyCount;

        return count;
    }
}

