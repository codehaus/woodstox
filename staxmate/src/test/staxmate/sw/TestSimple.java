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
        SMNamespace ns2 = doc.getNamespace("urn://hihhei", "myns");

        /*
        doc.addComment("Comment!");
        */

        SMOutputElement elem = doc.addElement("root", ns);
        elem.addCharacters("Rock & Roll");
        SMBufferedFragment frag = elem.createBufferedFragment();
        elem.addBuffered(frag);
        frag.addCharacters("[FRAG");
        frag.addComment("!!!");
        frag.addElement("tag", ns);
        SMOutputElement elem2 = elem.addElement("branch", null);
        elem2.addElement("leaf", ns2);
        frag.addCharacters("ment!]");
        frag.release();
        elem.addElement("leaf2", ns2);
        //doc.closeRoot();

        System.out.println("Result:");
        System.out.println(sw.toString());
    }
}
