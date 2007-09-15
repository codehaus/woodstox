package wstxtest.wstream;

import java.io.*;

import javax.xml.parsers.*;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.transform.dom.DOMResult;
import org.w3c.dom.*;


/**
 * Unit test suite that checks that output-side DOM-compatibility
 * features (DOMResult for output) are implemented as expected.
 */
public class TestDomWrite
    extends BaseWriterTest
{
    public void testNonNsOutput()
        throws Exception
    {
        Document doc = createDomDoc(false);
        XMLOutputFactory of = getFactory(0);
        XMLStreamWriter sw = of.createXMLStreamWriter(new DOMResult(doc));

        sw.writeStartDocument();
        sw.writeStartElement("root");
        sw.writeAttribute("attr", "value");
        sw.writeAttribute("ns:attr2", "value2");
        sw.writeEmptyElement("leaf");
        sw.writeCharacters("text?<ok>");
        sw.writeEndElement();
        sw.writeEndDocument();
        sw.close();

        Element root = doc.getDocumentElement();
        assertNotNull(root);
        assertEquals("root", root.getTagName());
        NamedNodeMap attrs = root.getAttributes();
        assertEquals(2, attrs.getLength());
        assertEquals("value", root.getAttribute("attr"));
        assertEquals("value2", root.getAttribute("ns:attr2"));

        Node child = root.getFirstChild();
        assertNotNull(child);
        assertEquals(Node.ELEMENT_NODE, child.getNodeType());
        Element elem = (Element) child;
        assertEquals("leaf", elem.getTagName());
        attrs = elem.getAttributes();
        assertEquals(0, attrs.getLength());

        child = child.getNextSibling();
        assertEquals(Node.TEXT_NODE, child.getNodeType());
        // Alas, getTextContent() is DOM 3 (Jdk 1.5+)
        //assertEquals("text?<ok>", child.getTextContent());
        // ... so we'll use less refined method
        assertEquals("text?<ok>", child.getNodeValue());
    }

    /*
    ///////////////////////////////////////////////////////
    // Helper methods
    ///////////////////////////////////////////////////////
     */

    private Document createDomDoc(boolean nsAware)
        throws Exception
    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(nsAware);
        return dbf.newDocumentBuilder().newDocument();
    }

    private XMLOutputFactory getFactory(int type)
        throws Exception
    {
        XMLOutputFactory f = getOutputFactory();
        // type 0 -> non-ns, 1 -> ns, non-repairing, 2 -> ns, repairing
        setNamespaceAware(f, type > 0); 
        setRepairing(f, type > 1); 
        return f;
    }
}
