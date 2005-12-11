package test;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.XMLOutputFactory2;
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

    final static String dtdStr =
        "<!ELEMENT root (branch)>\n"
        +"<!ELEMENT branch ANY>\n"
        ;

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

        XMLValidatorFactory vd = XMLValidatorFactory.newInstance(XMLValidatorFactory.SCHEMA_ID_DTD);

        XMLValidationSchema schema = vd.createSchema(new StringReader(dtdStr));

        ((BaseStreamWriter) sw).setValidator(schema);

        sw.writeStartDocument();
        sw.writeStartElement("", "root", "");
        sw.writeAttribute("attr", "value");
        sw.writeAttribute("", "", "attr2", "value2");
        sw.writeCharacters("Illegal text!");
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
