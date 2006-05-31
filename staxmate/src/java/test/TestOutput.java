package test;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.staxmate.SMOutputFactory;
import org.codehaus.staxmate.out.*;

public class TestOutput
{
    private TestOutput() { }

    public void test()
        throws XMLStreamException
    {
        XMLOutputFactory fact = SMOutputFactory.getGlobalXMLOutputFactory();
        StringWriter strw = new StringWriter();
        XMLStreamWriter sw = fact.createXMLStreamWriter(strw);
        SMOutputDocument doc = SMOutputFactory.createOutputDocument(sw, "1.0", "UTF-8", true);

        doc.setIndentation("\n                              ", 1, 3);

        SMOutputElement root = doc.addElement(null, "root");
        root.addAttribute(root.getNamespace("uri:foo"), "attr", "1");
        root.addAttribute(root.getNamespace("uri:foo2", "funky"), "attr2", "r&b");

        SMOutputElement branch = root.addElement(root.getNamespace("uri:foo3"), "branch");
        SMOutputElement leaf = branch.addElement(null, "leaf");

        leaf.addAttribute(leaf.getNamespace("uri:foo3"), "leafAttr", "!<2");
        leaf.addCharacters("Text...");

        doc.closeRoot();

        System.out.println("DOC ["+strw.toString()+"]");
    }

    public static void main(String[] args)
        throws XMLStreamException
    {
        new TestOutput().test();
    }
}
