package stax2.stream;

import java.io.*;
import javax.xml.stream.*;

import org.codehaus.stax2.*;

import stax2.BaseStax2Test;

public class TestXMLStreamReader2
    extends BaseStax2Test
{
    public void testPropertiesNative()
        throws XMLStreamException
    {
        doTestProperties(false, false);
        doTestProperties(true, false);
    }

    public void testPropertiesWrapped()
        throws XMLStreamException
    {
        doTestProperties(false, true);
        doTestProperties(true, true);
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

    public void testReportCData() throws XMLStreamException
    {
        _testCData(false, false);
        _testCData(false, true);
        _testCData(true, false);
        _testCData(true, true);
    }

    public void _testCData(boolean wrapped, boolean report) throws XMLStreamException
    {
        final String XML = "<root><![CDATA[test]]></root>";

        XMLInputFactory2 f = getInputFactory();
        // important: don't force coalescing, that'll convert CDATA to CHARACTERS
        setCoalescing(f, false);
        f.setProperty(XMLInputFactory2.P_REPORT_CDATA, new Boolean(report));
        XMLStreamReader sr = f.createXMLStreamReader(new StringReader(XML));
        if (wrapped) {
            sr = wrapWithAdapter(sr);
        }
        assertTokenType(START_ELEMENT, sr.next());
        int t = sr.next();
        assertEquals("test", getAndVerifyText(sr));
        if (report) {
            assertTokenType(CDATA, t);
        } else {
            assertTokenType(CHARACTERS, t);
        }
        assertTokenType(END_ELEMENT, sr.next());
        sr.close();
    }


    /*
    ////////////////////////////////////////
    // Private methods, shared test code
    ////////////////////////////////////////
     */

    /**
     * @param wrapped If true, will use Stax2ReaderAdapter to
     *   wrap the stream reader implementation
     */
    public void doTestProperties(boolean ns, boolean wrapped)
        throws XMLStreamException
    {
        final String XML = "<root><child attr='123' /><child2>xxx</child2></root>";
        XMLStreamReader2 sr = getReader(XML, ns);
        if (wrapped) {
            sr = wrapWithAdapter(sr);
        }

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

        /* Can only test this for native readers; adapter has no way
         * of implementing it reliably for Stax1 impls:
         */
        if (!wrapped) {
            assertTrue(sr.isEmptyElement());
        }
        assertTokenType(END_ELEMENT, sr.next());
        assertEquals("child", sr.getLocalName());
        assertEquals(2, sr.getDepth());
        if (!wrapped) { // as above, only for non-wrapped
            assertFalse(sr.isEmptyElement());
        }

        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("child2", sr.getLocalName());
        assertEquals(2, sr.getDepth());
        if (!wrapped) { // as above, only for non-wrapped
            assertFalse(sr.isEmptyElement());
        }

        assertTokenType(CHARACTERS, sr.next());
        assertEquals("xxx", getAndVerifyText(sr));
        assertEquals(2, sr.getDepth());
        // note: shouldn't cause an exception
        if (!wrapped) {
            assertFalse(sr.isEmptyElement());
        }

        assertTokenType(END_ELEMENT, sr.next());
        assertEquals("child2", sr.getLocalName());
        assertEquals(2, sr.getDepth());
        if (!wrapped) {
            assertFalse(sr.isEmptyElement());
        }

        assertTokenType(END_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        assertEquals(1, sr.getDepth());
        if (!wrapped) {
            assertFalse(sr.isEmptyElement());
        }

        assertTokenType(END_DOCUMENT, sr.next());
        assertEquals(0, sr.getDepth());
        if (!wrapped) {
            assertFalse(sr.isEmptyElement());
        }
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
        try {
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
        } catch (XMLStreamException xse) {
            fail("Did not expect any problems during parsing, but got: "+xse);
        }
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

