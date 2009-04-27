package test;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.XMLOutputFactory2;
import org.codehaus.stax2.XMLStreamProperties;

import com.ctc.wstx.api.InvalidCharHandler;
import com.ctc.wstx.api.WstxOutputProperties;

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

    //final String ENCODING = "ISO-8859-1";
    final String ENCODING = "UTF-8";

    protected void test()
        throws Exception
    {
        XMLOutputFactory f = getFactory();
        f.setProperty(XMLStreamProperties.XSP_NAMESPACE_AWARE,
                      Boolean.TRUE);
        //Boolean.FALSE);
        f.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES,
                      //Boolean.TRUE);
                      Boolean.FALSE);
        f.setProperty(XMLOutputFactory2.P_AUTOMATIC_EMPTY_ELEMENTS,
                      //Boolean.TRUE);
                      Boolean.FALSE);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        XMLStreamWriter w = f.createXMLStreamWriter(bos, ENCODING);

        final String URL_P1 = "http://p1.org";
        final String URL_P2 = "http://ns.p2.net/yeehaw.html";
        final String URL_DEF = "urn:default";

        final String TEXT = "  some text\n";

        w.writeStartDocument();
 
        w.setPrefix("p1", URL_P1);
        w.writeStartElement("test");
        w.writeNamespace("p1", URL_P1);

        w.setDefaultNamespace(URL_DEF);
        w.setPrefix("p2", URL_P2);
        w.writeStartElement("", "branch", URL_DEF);
        w.writeDefaultNamespace(URL_DEF);
        w.writeNamespace("p2", URL_P2);

        // Ok, let's see that we can also clear out the def ns:
        w.setDefaultNamespace("");
        w.writeStartElement("", "leaf", "");
        w.writeDefaultNamespace("");

        w.writeCharacters(TEXT);

        w.writeEndElement(); // first leaf

        w.writeEmptyElement(URL_P1, "leaf"); // second leaf

        w.writeEndElement(); // branch
        w.writeEndElement(); // root elem
        w.writeEndDocument();

        w.flush();
        w.close();

        System.err.println("DOC -> '"+new String(bos.toByteArray(), ENCODING)+"'");
    }

    public static void main(String[] args)
        throws Throwable
    {
        try {
            new TestNsStreamWriter().test();
        } catch (XMLStreamException ex) {
            if (ex.getCause() != null) {
                throw ex.getCause();
            }
            throw ex;
        }
    }
}
