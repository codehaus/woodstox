package staxmate.sw;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.staxmate.SMOutputFactory;
import org.codehaus.staxmate.sw.*;

public class TestSimple
    extends BaseWriterTest
{
    public void testSimple()
        throws Exception
    {
        StringWriter sw = new StringWriter();
        XMLStreamWriter xw = XMLOutputFactory.newInstance().createXMLStreamWriter(sw);
        SMOutputDocument doc = SMOutputFactory.createOutputDocument(xw);

        SMNamespace ns = doc.getNamespace("http://foo");
        doc.addComment("Comment!");
        SMOutputElement elem = doc.addElement("root", ns);
System.err.println("[3]");

        doc.closeRoot();
System.err.println("[4]");

        System.out.println("Result:");
        System.out.println(sw.toString());
    }
}
