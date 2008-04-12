package wstxtest.stream;

import java.io.*;

import javax.xml.parsers.*;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.transform.dom.DOMSource;

import org.xml.sax.InputSource;
import org.w3c.dom.Document;

import org.codehaus.stax2.*;

/**
 * Unit test suite that checks that input-side DOM-compatibility
 * features (DOMSource as input) are implemented as expected.
 */
public class TestDomCompat
    extends BaseStreamTest
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

        // First, need to parse using JAXP DOM:
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document dom = db.parse(new InputSource(new StringReader(XML)));

        XMLInputFactory2 ifact = getInputFactory();
        XMLStreamReader sr = ifact.createXMLStreamReader(new DOMSource(dom));

        assertTokenType(START_DOCUMENT, sr.getEventType());

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
            "<?xml version='1.0' ?><root>  \n<leaf>\t</leaf>  x </root>"
            ;

        // First, need to parse using JAXP DOM:
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document dom = db.parse(new InputSource(new StringReader(XML)));

        XMLInputFactory2 ifact = getInputFactory();
        XMLStreamReader sr = ifact.createXMLStreamReader(new DOMSource(dom));

        assertTokenType(START_DOCUMENT, sr.getEventType());
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
            ;

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document dom = db.parse(new InputSource(new StringReader(XML)));

        XMLInputFactory2 ifact = getInputFactory();
        setCoalescing(ifact, true);
        XMLStreamReader sr = ifact.createXMLStreamReader(new DOMSource(dom));
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
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document dom = db.parse(new InputSource(new StringReader(XML)));
        XMLInputFactory2 ifact = getInputFactory();
        setCoalescing(ifact, true);
        XMLStreamReader sr = ifact.createXMLStreamReader(new DOMSource(dom));
        assertTokenType(START_DOCUMENT, sr.getEventType());
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        // Should always be of type CHARACTERS, even if underlying event is CDATA
        assertTokenType(CHARACTERS, sr.next());
        assertEquals("...", getAndVerifyText(sr));

        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(END_DOCUMENT, sr.next());
    }
}
