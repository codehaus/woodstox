package stax2.stream;

import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.*;

import stax2.BaseStax2Test;

public class TestXMLStreamReader2
    extends BaseStax2Test
{
    public TestXMLStreamReader2(String name) {
        super(name);
    }

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
        setNamespaceAware(f, nsAware);
        setValidating(f, false);
        return constructStreamReader(f, contents);
    }
}

