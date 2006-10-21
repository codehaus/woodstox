package staxmate.in;

import java.io.StringReader;

import javax.xml.stream.*;

import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.*;

/**
 * Unit tests for verifying that bundled SMFilter implementations
 * work as expected
 */
public class TestFilter
    extends BaseReaderTest
{
    public void testElementFilter()
        throws Exception
    {
        String XML = "<!-- foo --><root>text<branch /><!-- comment -->  </root>";
        XMLStreamReader sr = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(XML));
        SMInputCursor rootc = SMInputFactory.rootElementCursor(sr);
        assertEquals(SMEvent.START_ELEMENT, rootc.getNext());
        assertEquals("root", rootc.getLocalName());
        SMInputCursor leafc = rootc.childElementCursor();
        assertEquals(SMEvent.START_ELEMENT, leafc.getNext());
        assertEquals("branch", leafc.getLocalName());
        assertNull(leafc.getNext());

        assertNull(rootc.getNext());
    }

    public void testTextFilter()
        throws Exception
    {
        String XML = "<!-- foo --><root>text<branch /><!-- comment --> x </root>";
        XMLInputFactory inf = XMLInputFactory.newInstance();
        inf.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        XMLStreamReader sr = inf.createXMLStreamReader(new StringReader(XML));
        SMInputCursor rootc = SMInputFactory.rootElementCursor(sr);
        rootc.getNext();
        SMInputCursor leafc = rootc.childCursor(SMFilterFactory.getTextOnlyFilter());
        assertEquals(SMEvent.TEXT, leafc.getNext());
        assertEquals("text", leafc.getText());
        /* Can't coalesce over underlying other types: but will skip
         * child elem, and comment
         */
        assertEquals(SMEvent.TEXT, leafc.getNext());
        assertEquals(" x ", leafc.getText());
        assertNull(leafc.getNext());

        assertNull(rootc.getNext());
    }
}

