package org.codehaus.stax2.ri;

import java.io.*;

import javax.xml.stream.*;

import stax2.BaseStax2Test;

/**
 * @author tsaloranta
 *
 * @since 4.1
 */
public class TestStax2ReaderAdapter extends BaseStax2Test
{
    public void testSimple() throws Exception
    {
        final String XML = "<root><a>xyz</a><b>abc</b></root>";
        XMLInputFactory f = getInputFactory();
        XMLStreamReader reader1 = f.createXMLStreamReader(new StringReader(XML));
        Stax2ReaderAdapter adapter = new Stax2ReaderAdapter(reader1);
        assertTokenType(START_DOCUMENT, adapter.getEventType());

        assertTokenType(START_ELEMENT, adapter.next());
        assertEquals("root", adapter.getLocalName());

        assertTokenType(START_ELEMENT, adapter.next());
        assertEquals("a", adapter.getLocalName());
        assertTokenType(CHARACTERS, adapter.next());
        assertEquals("xyz", adapter.getText());
        assertTokenType(END_ELEMENT, adapter.next());
        assertEquals("a", adapter.getLocalName());

        assertTokenType(START_ELEMENT, adapter.next());
        assertEquals("b", adapter.getLocalName());
        assertTokenType(CHARACTERS, adapter.next());
        assertEquals("abc", adapter.getText());
        assertTokenType(END_ELEMENT, adapter.next());
        assertEquals("b", adapter.getLocalName());

        assertTokenType(END_ELEMENT, adapter.next());
        assertEquals("root", adapter.getLocalName());

        assertTokenType(END_DOCUMENT, adapter.next());
    }

    public void testSimpleWithTypedText() throws Exception
    {
        final String XML = "<root><a>xyz</a><b>abc</b></root>";
        XMLInputFactory f = getInputFactory();
        XMLStreamReader reader1 = f.createXMLStreamReader(new StringReader(XML));
        Stax2ReaderAdapter adapter = new Stax2ReaderAdapter(reader1);
        assertTokenType(START_DOCUMENT, adapter.getEventType());

        assertTokenType(START_ELEMENT, adapter.next());
        assertEquals("root", adapter.getLocalName());

        assertTokenType(START_ELEMENT, adapter.next());
        assertEquals("a", adapter.getLocalName());
        assertEquals("xyz", adapter.getElementText());
        assertTokenType(END_ELEMENT, adapter.getEventType());
        assertEquals("a", adapter.getLocalName());

        assertTokenType(START_ELEMENT, adapter.next());
        assertEquals("b", adapter.getLocalName());
        assertEquals("abc", adapter.getElementText());
        assertTokenType(END_ELEMENT, adapter.getEventType());
        assertEquals("b", adapter.getLocalName());

        assertTokenType(END_ELEMENT, adapter.next());
        assertEquals("root", adapter.getLocalName());

        assertTokenType(END_DOCUMENT, adapter.next());
    }

}
