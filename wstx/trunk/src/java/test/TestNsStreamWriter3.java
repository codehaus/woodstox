package test;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.XMLOutputFactory2;
import org.codehaus.stax2.XMLStreamWriter2;
import org.codehaus.stax2.XMLStreamProperties;
import org.codehaus.stax2.validation.*;

import com.ctc.wstx.api.WstxOutputProperties;
import com.ctc.wstx.sw.BaseStreamWriter;

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
        f.setProperty(XMLStreamProperties.XSP_NAMESPACE_AWARE, Boolean.TRUE);
        f.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES,
                      Boolean.TRUE);
        //Boolean.FALSE);

        Writer w = new PrintWriter(System.out);
        XMLStreamWriter2 sw = (XMLStreamWriter2)f.createXMLStreamWriter(w);

        final String URI = "http://foo";

        sw.writeStartDocument();
        sw.writeStartElement(null, "root");
	sw.writeNamespace("foo", URI);
        sw.writeStartElement(URI, "leaf");
        sw.writeAttribute(URI, "ns-attr", "1");
        sw.writeAttribute(null, "ns-attr", "2");
        sw.writeEndElement();
        sw.writeEndElement();
        sw.writeCharacters("\n"); // to add lf for terminal output
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
