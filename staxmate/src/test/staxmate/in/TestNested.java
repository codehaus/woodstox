package staxmate.in;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.*;

public class TestNested
    extends BaseReaderTest
{
    public void testSimpleTwoLevel()
        throws Exception
    {
        String XML = "<?xml version='1.0'?>"
            +"<root>\n"
            +"<leaf />"
            +"<leaf attr='xyz'>text</leaf>"
            +"</root>\n";
        XMLStreamReader sr = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(XML));
        SMInputCursor rootc = SMInputFactory.rootElementCursor(sr);
        assertEquals(SMEvent.START_ELEMENT, rootc.getNext()); // should always have root
        assertEquals("root", rootc.getLocalName());
        SMInputCursor leafc = rootc.childElementCursor();

        assertEquals(SMEvent.START_ELEMENT, leafc.getNext());
        assertEquals("leaf", leafc.getLocalName());

        assertEquals(SMEvent.START_ELEMENT, leafc.getNext());
        assertEquals("leaf", leafc.getLocalName());
        assertEquals(1, leafc.getAttrCount());
        assertEquals("attr", leafc.getAttrLocalName(0));
        assertEquals("xyz", leafc.getAttrValue(0));

        assertEquals("text", leafc.collectDescendantText(true));

        assertNull(leafc.getNext());

        assertNull(rootc.getNext());
        
        sr.close();
    }

    public void testSimpleThreeLevel()
        throws Exception
    {
        String XML =
            /*
            "<root name='root'>"
            +"<branch name='br1'>"
            +"<leaf name='leaf1'>text</leaf>"
            +"<leaf name='leaf2'>text2</leaf>"
            +"</branch>"
            +"</root>"
            */
"<?xml version='1.0' encoding='UTF-8'?>\n"
+"<ptProperties markeplaceId='1' ttl='3600' version='200606201646500918'>\n"
+"<pt name='1234567890123456789012345678901234567890'><prop name='glProductGroup'>Authority Non Buyable</prop><prop name='websiteDisplayGroup'>Authority Non Buyable</prop><prop name='isMarketplaceEnabled'>false</prop></pt>\n"
+"<pt name='ABIS_APPAREL'><prop name='glProductGroup'>Apparel</prop><prop name='websiteDisplayGroup'>Apparel</prop></pt>"
            ;

        XMLStreamReader sr = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(XML));
        SMInputCursor rootc = SMInputFactory.rootElementCursor(sr);

        assertEquals(SMEvent.START_ELEMENT, rootc.getNext()); // should always have root
        assertEquals("ptProperties", rootc.getLocalName());
        assertEquals(3, rootc.getAttrCount());

        SMInputCursor brc = rootc.childElementCursor();
        assertEquals(SMEvent.START_ELEMENT, brc.getNext());
        assertEquals("pt", brc.getLocalName());
        assertEquals(1, brc.getAttrCount());

        SMInputCursor leafc = brc.childElementCursor();
        assertEquals(SMEvent.START_ELEMENT, leafc.getNext());
        assertEquals("prop", leafc.getLocalName());
        assertEquals(1, leafc.getAttrCount());
        assertEquals("Authority Non Buyable", leafc.collectDescendantText(false));

        assertEquals(SMEvent.START_ELEMENT, leafc.getNext());
        assertEquals("prop", leafc.getLocalName());
        // ... and more properties

        // Enough, let's move to the next at branch level:
        assertEquals(SMEvent.START_ELEMENT, brc.getNext());
        assertEquals("pt", brc.getLocalName());

        // And then check that root is done:

        assertNull(rootc.getNext());
        
        sr.close();
    }
}
