package test;

import java.io.*;

import javax.xml.XMLConstants;
import javax.xml.stream.*;

import com.ctc.wstx.api.WstxOutputProperties;

/**
 * Simple non-automated unit test for outputting namespace-aware XML
 * documents.
 */
public class TestNsStreamWriter2
{
    private TestNsStreamWriter2() {
    }

    protected XMLOutputFactory getFactory()
    {
        System.setProperty("javax.xml.stream.XMLOutputFactory",
                           "com.ctc.wstx.stax.WstxOutputFactory");
        return XMLOutputFactory.newInstance();
    }

    private String namespace = "http://www.w3.org/2003/05/soap-envelope";

    protected void test()
        throws Exception
    {
        XMLOutputFactory f = getFactory();
        f.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES,
                      //Boolean.FALSE);
                      Boolean.TRUE);
        f.setProperty(WstxOutputProperties.P_OUTPUT_ENABLE_NS,
                      Boolean.TRUE);
        f.setProperty(WstxOutputProperties.P_OUTPUT_EMPTY_ELEMENTS,
                      Boolean.TRUE);
        Writer w = new PrintWriter(System.out);
        XMLStreamWriter sw = f.createXMLStreamWriter(w);

        sw.writeStartDocument();
        sw.setPrefix("env", namespace);
        //sw.setPrefix("test", "http://someTestUri");
        sw.writeStartElement("env", "Envelope", namespace);
        // or: 
        //sw.writeStartElement(namespace "Envelope");

        sw.writeNamespace("env", namespace);
        //sw.writeNamespace("test", "http://someTestUri");

        sw.writeEmptyElement("xml", "stdTag", XMLConstants.XML_NS_URI);
        sw.writeAttribute("xml", XMLConstants.XML_NS_URI, "lang", "fi-FI");
        sw.writeCharacters("\n");
        sw.writeEndElement();

        sw.writeEndDocument();

        sw.flush();
        sw.close();

        w.close();
    }

    public static void main(String[] args)
        throws Exception
    {
        new TestNsStreamWriter2().test();
    }
}
