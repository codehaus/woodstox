package stax2.stream;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.*;

import stax2.BaseStax2Test;

public class TestXMLStreamReader2
    extends BaseStax2Test
{
    public void testProperties()
        throws XMLStreamException
    {
        doTestProperties(false);
        doTestProperties(true);
    }

    public void testSkipElement()
        throws XMLStreamException
    {
        doTestSkipElement(false);
        doTestSkipElement(true);
    }

    public void testGetPrefixedName()
        throws XMLStreamException
    {
        doTestGetPrefixedName(false);
        doTestGetPrefixedName(true);
    }

    /**
     * Test inspired by [WSTX-211]
     */
    public void testLongerCData() throws Exception
    {
	String SRC_TEXT =
            "\r\n"+
"123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678\r\n"
+"<embededElement>Woodstox 4.0.5 does not like this embedded element.  However, if you take\r\n"
+"out one or more characters from the really long line (so that less than 500 characters come between\r\n"
+"'CDATA[' and the opening of the embeddedElement tag (including LF), then Woodstox will instead\r\n"
	    +"complain that the CDATA section wasn't ended.";
	String DST_TEXT = SRC_TEXT.replace("\r\n", "\n");
	String XML = "<?xml version='1.0' encoding='utf-8'  ?>"
+"<test><![CDATA["+SRC_TEXT+"]]></test>";
	// Hmmh. Seems like we need the BOM...	
	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	bos.write(0xEF);
	bos.write(0xBB);
	bos.write(0xBF);
	bos.write(XML.getBytes("UTF-8"));
	byte[] bytes = bos.toByteArray();
        XMLInputFactory2 f = getInputFactory();
        // important: don't force coalescing, that'll convert CDATA to CHARACTERS
        setCoalescing(f, false);
	
	XMLStreamReader sr = f.createXMLStreamReader(new ByteArrayInputStream(bytes));
        assertTokenType(START_DOCUMENT, sr.getEventType());
        assertTokenType(START_ELEMENT, sr.next());
	assertEquals("test", sr.getLocalName());
	// This should still work, although with linefeed replacements
	assertEquals(DST_TEXT, sr.getElementText());
        assertTokenType(END_ELEMENT, sr.getEventType());
	sr.close();
    }

    /*
    ////////////////////////////////////////
    // Private methods, shared test code
    ////////////////////////////////////////
     */

    public void doTestProperties(boolean ns)
        throws XMLStreamException
    {
        final String XML = "<root><child attr='123' /><child2>xxx</child2></root>";
        XMLStreamReader2 sr = getReader(XML, ns);

        assertTokenType(START_DOCUMENT, sr.getEventType());
        assertEquals(0, sr.getDepth());
        assertFalse(sr.isEmptyElement());

        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        assertEquals(1, sr.getDepth());
        assertFalse(sr.isEmptyElement());

        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("child", sr.getLocalName());
        assertEquals(2, sr.getDepth());
        assertTrue(sr.isEmptyElement());
        assertTokenType(END_ELEMENT, sr.next());
        assertEquals("child", sr.getLocalName());
        assertEquals(2, sr.getDepth());
        assertFalse(sr.isEmptyElement());

        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("child2", sr.getLocalName());
        assertEquals(2, sr.getDepth());
        assertFalse(sr.isEmptyElement());

        assertTokenType(CHARACTERS, sr.next());
        assertEquals("xxx", getAndVerifyText(sr));
        assertEquals(2, sr.getDepth());
        // note: shouldn't cause an exception
        assertFalse(sr.isEmptyElement());

        assertTokenType(END_ELEMENT, sr.next());
        assertEquals("child2", sr.getLocalName());
        assertEquals(2, sr.getDepth());
        assertFalse(sr.isEmptyElement());

        assertTokenType(END_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        assertEquals(1, sr.getDepth());
        assertFalse(sr.isEmptyElement());

        assertTokenType(END_DOCUMENT, sr.next());
        assertEquals(0, sr.getDepth());
        assertFalse(sr.isEmptyElement());
    }


    public void doTestSkipElement(boolean ns)
        throws XMLStreamException
    {
        final String XML = "<root><child attr='123' /><child2>xxx</child2></root>";
        XMLStreamReader2 sr = getReader(XML, ns);
        assertTokenType(START_DOCUMENT, sr.getEventType());

        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        sr.skipElement();
        assertTokenType(END_ELEMENT, sr.getEventType());
        assertEquals("root", sr.getLocalName());
        assertTokenType(END_DOCUMENT, sr.next());
    }

    public void doTestGetPrefixedName(boolean ns)
        throws XMLStreamException
    {
        final String XML =
            "<!DOCTYPE root [\n"
            +"<!ENTITY intEnt '<leaf />'>\n"
            +"]>"
            +"<root>"
            +"<xy:elem xmlns:xy='http://foo' xmlns:another='http://x'>"
            +"<?proc instr?>&intEnt;<another:x /></xy:elem>"
            +"</root>"
            ;
        XMLStreamReader2 sr = getReader(XML, ns);
        assertTokenType(DTD, sr.next());
        assertEquals("root", sr.getPrefixedName());
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("root", sr.getPrefixedName());
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("xy:elem", sr.getPrefixedName());
        assertTokenType(PROCESSING_INSTRUCTION, sr.next());
        assertEquals("proc", sr.getPrefixedName());
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("leaf", sr.getPrefixedName());
        assertTokenType(END_ELEMENT, sr.next());
        assertEquals("leaf", sr.getPrefixedName());
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("another:x", sr.getPrefixedName());
        assertTokenType(END_ELEMENT, sr.next());
        assertEquals("another:x", sr.getPrefixedName());
        assertTokenType(END_ELEMENT, sr.next());
        assertEquals("xy:elem", sr.getPrefixedName());
        assertTokenType(END_ELEMENT, sr.next());
        assertEquals("root", sr.getPrefixedName());
        assertTokenType(END_DOCUMENT, sr.next());
    }

    /*
    ////////////////////////////////////////
    // Private methods, other
    ////////////////////////////////////////
     */

    private XMLStreamReader2 getReader(String contents, boolean nsAware)
        throws XMLStreamException
    {
        XMLInputFactory2 f = getInputFactory();
        setCoalescing(f, true);
        setSupportDTD(f, true);
        setNamespaceAware(f, nsAware);
        setValidating(f, false);
        return constructStreamReader(f, contents);
    }
}

