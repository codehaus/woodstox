package stax2.stream;

import java.io.*;
import javax.xml.stream.*;

import org.codehaus.stax2.*;

import stax2.BaseStax2Test;

public class TestStreamReader
    extends BaseStax2Test
{
    /**
     * Unit test to verify fixing of (and guard against regression of)
     * [WSTX-201].
     */
    public void testIsCharacters() throws Exception
    {
        XMLInputFactory2 f = getInputFactory();
        setNamespaceAware(f, true);
        setCoalescing(f, true);
        XMLStreamReader sr = constructStreamReader(f, "<root><![CDATA[abc]]></root>");
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        // should both return CHARACTERS
        assertTokenType(CHARACTERS, sr.next());
        // and be considered of characters...
        assertEquals(CHARACTERS, sr.getEventType());
        assertTrue(sr.isCharacters());
        assertTokenType(END_ELEMENT, sr.next());
    }
}
