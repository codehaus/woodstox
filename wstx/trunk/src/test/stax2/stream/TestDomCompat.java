package stax2.stream;

import java.io.*;

import javax.xml.parsers.*;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.transform.dom.DOMSource;

import org.xml.sax.InputSource;
import org.w3c.dom.Document;

import org.codehaus.stax2.*;

import stax2.BaseStax2Test;

/**
 * Unit test suite that checks that input-side DOM-compatibility
 * features (DOMSource as input) are implemented as expected.
 *<p>
 * This test is part of stax2test suite because a reference implementation
 * of DOM-wrapping/adapting reader is included, and hence it is
 * reasonable to expect that Stax2 implementations would implement
 * this part of DOM interoperability support.
 */
public class TestDomCompat
    extends BaseStax2Test
{
    public void testSimpleDomInput()
        throws Exception
    {
        final String XML =
            "<?xml version='1.0' ?><!--prolog-->"
            +"<ns:root xmlns:ns='http://foo' attr='value'>"
            +"<leaf ns:attr='value2' />"
            +"<?proc instr?><!--comment-->"
            +"\nAnd some text"
            +"</ns:root><?pi-in epilog?>"
            ;

        XMLStreamReader sr = createDomBasedReader(XML, true);

        assertTokenType(COMMENT, sr.next());
        assertEquals("prolog", getAndVerifyText(sr));

        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        assertEquals("ns", sr.getPrefix());
        assertEquals("http://foo", sr.getNamespaceURI());
        QName n = sr.getName();
        assertNotNull(n);
        assertEquals("root", n.getLocalPart());
        
        assertEquals(1, sr.getAttributeCount());
        assertEquals("attr", sr.getAttributeLocalName(0));
        assertNull(sr.getAttributePrefix(0));
        assertNull(sr.getAttributeNamespace(0));
        n = sr.getAttributeName(0);
        assertEquals("attr", n.getLocalPart());
        assertNotNull(n);
        assertEquals("value", sr.getAttributeValue(0));

        assertEquals(1, sr.getNamespaceCount());
        assertEquals("ns", sr.getNamespacePrefix(0));
        assertEquals("http://foo", sr.getNamespaceURI(0));

        NamespaceContext nsCtxt = sr.getNamespaceContext();
        assertNotNull(nsCtxt);
        /* 28-Apr-2006, TSa: Alas, namespace access is only fully
         *   implemented in DOM Level 3... thus, can't check:
         */
        /*
        assertEquals("ns", nsCtxt.getPrefix("http://foo"));
        assertEquals("http://foo", nsCtxt.getNamespaceURI("ns"));
        assertNull(nsCtxt.getPrefix("http://whatever"));
        assertNull(nsCtxt.getNamespaceURI("nsX"));
        */

        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("leaf", sr.getLocalName());
        assertNull(sr.getPrefix());
        assertNull(sr.getNamespaceURI());
        assertEquals(1, sr.getAttributeCount());
        assertEquals("attr", sr.getAttributeLocalName(0));
        assertEquals("ns", sr.getAttributePrefix(0));
        assertEquals("http://foo", sr.getAttributeNamespace(0));
        assertEquals(0, sr.getNamespaceCount());

        assertTokenType(END_ELEMENT, sr.next());
        assertEquals("leaf", sr.getLocalName());
        assertNull(sr.getPrefix());
        assertNull(sr.getNamespaceURI());
        assertEquals(0, sr.getNamespaceCount());

        assertTokenType(PROCESSING_INSTRUCTION, sr.next());
        assertEquals("proc", sr.getPITarget());
        assertEquals("instr", sr.getPIData());

        assertTokenType(COMMENT, sr.next());
        assertEquals("comment", getAndVerifyText(sr));

        assertTokenType(CHARACTERS, sr.next());
        // yeah yeah, could be split...
        assertEquals("\nAnd some text", getAndVerifyText(sr));

        assertTokenType(END_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        assertEquals("ns", sr.getPrefix());
        assertEquals("http://foo", sr.getNamespaceURI());

        assertEquals(1, sr.getNamespaceCount());
        assertEquals("ns", sr.getNamespacePrefix(0));
        assertEquals("http://foo", sr.getNamespaceURI(0));

        assertTokenType(PROCESSING_INSTRUCTION, sr.next());
        assertEquals("pi-in", sr.getPITarget());
        assertEquals("epilog", sr.getPIData());

        assertTokenType(END_DOCUMENT, sr.next());

        assertFalse(sr.hasNext());
        sr.close();
    }

    /**
     * Test added to verify that [WSTX-134] is fixed properly
     */
    public void testDomWhitespace()
        throws Exception
    {
        final String XML =
            "<?xml version='1.0' ?><root>  \n<leaf>\t</leaf>  x </root>";
        XMLStreamReader sr = createDomBasedReader(XML, true);

        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        assertTokenType(CHARACTERS, sr.next());
        assertTrue(sr.isWhiteSpace());
        assertEquals("  \n", getAndVerifyText(sr));
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("leaf", sr.getLocalName());
        assertTokenType(CHARACTERS, sr.next());
        assertTrue(sr.isWhiteSpace());
        assertEquals("\t", getAndVerifyText(sr));
        assertTokenType(END_ELEMENT, sr.next());
        assertEquals("leaf", sr.getLocalName());
        assertTokenType(CHARACTERS, sr.next());
        assertFalse(sr.isWhiteSpace());
        assertEquals("  x ", getAndVerifyText(sr));
        assertTokenType(END_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        assertTokenType(END_DOCUMENT, sr.next());
        sr.close();
    }

    /**
     * Test to verify that [WSTX-145] is properly fixed
     */
    public void testDomCoalescingText()
        throws Exception
    {
        final String XML =
            "<root>Some <![CDATA[content]]> in cdata</root>";

        Document doc = parseDomDoc(XML, true);
        XMLInputFactory2 ifact = getInputFactory();
        setCoalescing(ifact, true);
        XMLStreamReader sr = ifact.createXMLStreamReader(new DOMSource(doc));
        assertTokenType(START_DOCUMENT, sr.getEventType());
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        assertTokenType(CHARACTERS, sr.next());
        assertEquals("Some content in cdata", getAndVerifyText(sr));

        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(END_DOCUMENT, sr.next());
    }

    public void testDomCoalescingType()
        throws Exception
    {
        final String XML =
            "<root><![CDATA[...]]></root>";
            ;
        Document doc = parseDomDoc(XML, true);
        XMLInputFactory2 ifact = getInputFactory();
        setCoalescing(ifact, true);
        XMLStreamReader sr = ifact.createXMLStreamReader(new DOMSource(doc));
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        // Should always be of type CHARACTERS, even if underlying event is CDATA
        assertTokenType(CHARACTERS, sr.next());
        assertEquals("...", getAndVerifyText(sr));

        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(END_DOCUMENT, sr.next());
    }

    /*
    ///////////////////////////////////////////////////
    // Tests for Stax2 (v3) Typed Access API methods
    ///////////////////////////////////////////////////
    */

    public void testPrimitiveTypesBoolean()
        throws Exception
    {
        final String XML = "<root attr='true'>  false  </root>";
        XMLStreamReader2 sr = createDomBasedReader(XML, true);

        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        assertTrue(sr.getAttributeAsBoolean(0));
        assertFalse(sr.getElementAsBoolean());
        // calling above method advances stream to END_ELEMENT
        assertTokenType(END_ELEMENT, sr.getEventType());
        assertEquals("root", sr.getLocalName());
        assertTokenType(END_DOCUMENT, sr.next());
        sr.close();
    }

    public void testPrimitiveTypesInt()
        throws Exception
    {
        final String XML = "<root attr='13'>\n\t-123456</root>";
        XMLStreamReader2 sr = createDomBasedReader(XML, true);

        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        assertEquals(13, sr.getAttributeAsInt(0));
        assertEquals(-123456, sr.getElementAsInt());
        // calling above method advances stream to END_ELEMENT
        assertTokenType(END_ELEMENT, sr.getEventType());
        assertEquals("root", sr.getLocalName());
        assertTokenType(END_DOCUMENT, sr.next());
        sr.close();
    }

    /*
    ///////////////////////////////////////////////////
    // Helper methods
    ///////////////////////////////////////////////////
    */

    private XMLStreamReader2 createDomBasedReader(String content, boolean nsAware)
        throws Exception
    {
        XMLInputFactory2 ifact = getInputFactory();
        XMLStreamReader2 sr = (XMLStreamReader2) ifact.createXMLStreamReader(new DOMSource(parseDomDoc(content, nsAware)));
        // Let's also check it's properly initialized
        assertTokenType(START_DOCUMENT, sr.getEventType());
        return sr;
    }

    private Document parseDomDoc(String content, boolean nsAware)
        throws Exception
    {
        // First, need to parse using JAXP DOM:
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(nsAware);
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(new InputSource(new StringReader(content)));
    }
}
