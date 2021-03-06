package test;

import java.io.*;

import javax.xml.stream.*;

import com.ctc.wstx.stax.WstxOutputProperties;

/**
 * Simple non-automated unit test for outputting namespace-aware XML
 * documents.
 */
public class TestNsStreamWriter
{
    private TestNsStreamWriter() {
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
        f.setProperty(WstxOutputProperties.P_OUTPUT_ENABLE_NS,
                      Boolean.TRUE);
        f.setProperty(WstxOutputProperties.P_OUTPUT_EMPTY_ELEMENTS,
                      Boolean.TRUE);
        Writer w = new PrintWriter(System.out);
        XMLStreamWriter sw = f.createXMLStreamWriter(w);

        sw.writeStartDocument();
        sw.writeComment("Comment!");
        sw.writeCharacters("\n");
        sw.writeStartElement("root");
        sw.writeAttribute("attr", "value");
        sw.writeAttribute("atpr", "http://attr-prefix", "attr", "value");
        sw.writeAttribute("http://attr-prefix", "attr3", "value!");
        sw.writeAttribute("another", "this & that");
        //sw.writeAttribute("attr", "whatever"); // error!
        sw.setDefaultNamespace("http://default"); // error if not output
        sw.setPrefix("myprefix", "http://mydotcom"); // - "" -
        sw.writeStartElement(null, "elem");
        //sw.writeNamespace("myprefix", "http://mydotcom");
        //sw.writeDefaultNamespace("http://default");
        sw.writeCharacters("Sub-text\n");
        sw.writeEmptyElement("http://mydotcom", "ns-elem");
        sw.writeEndElement();
        sw.writeCharacters("\n");
        //sw.writeStartElement("elem3:foo"); // error, colon inside local name
        sw.writeStartElement("elem3");
        sw.writeEndElement();
        sw.writeCharacters("Root text <> ]]>\n");
        sw.writeEndElement();
        //sw.writeEmptyElement("secondRoot"); // error!
        sw.writeCharacters("\n"); // white space in epilog
        sw.writeProcessingInstruction("target", "some data");
        sw.writeCharacters("\n"); // white space in epilog
        sw.writeEndDocument();

        sw.flush();
        sw.close();

        w.close();
    }

    public static void main(String[] args)
        throws Exception
    {
        new TestNsStreamWriter().test();
    }
}
