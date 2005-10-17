package test;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.XMLOutputFactory2;

import com.ctc.wstx.api.WstxOutputProperties;

/**
 * Simple non-automated unit test for outputting namespace-aware XML
 * documents.
 */
public class TestNsStreamWriter3
{
    private TestNsStreamWriter3() {
    }

    protected XMLOutputFactory getFactory()
    {
        System.setProperty("javax.xml.stream.XMLOutputFactory",
                           "com.ctc.wstx.stax.WstxOutputFactory");
        return XMLOutputFactory.newInstance();
    }

    protected void test()
        throws Exception
    {
        XMLOutputFactory f = getFactory();
        f.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES,
                      Boolean.TRUE);
        //Boolean.FALSE);
        f.setProperty(XMLOutputFactory2.P_NAMESPACE_AWARE, Boolean.TRUE);
        Writer w = new PrintWriter(System.out);
        XMLStreamWriter sw = f.createXMLStreamWriter(w);

        sw.writeStartDocument();
        sw.writeStartElement("", "root", "");
        sw.writeAttribute("attr", "value");
        sw.writeAttribute("", "", "attr2", "value2");
        sw.writeStartElement("", "branch", "uri:some");
        sw.writeAttribute("", "", "foop", "value2");
        sw.writeStartElement("", "leaf", "");
        sw.writeEndElement();
        sw.writeEndElement();
        /*
        sw.writeAttribute("atpr", "http://attr-prefix", "attr", "value");
        sw.writeAttribute("http://attr-prefix", "attr3", "value!");
        sw.writeAttribute("another", "this & that");
        */
        sw.writeCharacters("Sub-text\n");
        sw.writeEndElement();
        w.flush();
        sw.writeEndDocument();

        sw.flush();
        sw.close();

        w.close();
    }

    public static void main(String[] args)
        throws Exception
    {
        new TestNsStreamWriter3().test();
    }
}
