package stax2.stream;

import java.io.*;
import java.util.*;

import javax.xml.stream.*;

import stax2.BaseStax2Test;

/**
 * Unit test(s) that verify correct functioning of
 * {@link XMLStreamReader#getElementText}. This might actually
 * belong more to the core StaxTest, but as bug was found from
 * Woodstox, let's start by just adding them here first.
 *
 * @author Tatu Saloranta
 *
 * @since 3.0
 */
public class TestGetElement
    extends BaseStax2Test
{
    /**
     * This unit test checks the default behaviour; with no auto-close, no
     * automatic closing should occur, nor explicit one unless specific
     * forcing method is used.
     */
    public void testLarge()
        throws Exception
    {
        final int LEN = 258000;
        final long SEED = 72;

        ByteArrayOutputStream bos = new ByteArrayOutputStream(LEN+2000);
        XMLStreamWriter sw = getOutputFactory().createXMLStreamWriter(bos, "UTF-8");
        Random r = new Random(SEED);

        sw.writeStartDocument();
        sw.writeStartElement("data");

        int rowCount = 0;

        while (bos.size() < LEN) {
            sw.writeStartElement("row");

            sw.writeStartElement("a");
            sw.writeCharacters(String.valueOf(r.nextInt()));
            sw.writeEndElement();
            sw.writeStartElement("b");
            sw.writeCharacters(String.valueOf(r.nextLong()));
            sw.writeEndElement();
            sw.writeStartElement("c");
            sw.writeCharacters(String.valueOf(r.nextBoolean()));
            sw.writeEndElement();
            sw.writeCharacters("\n"); // to make debugging easier

            sw.writeEndElement();
            sw.flush();
            ++rowCount;
        }
        sw.writeEndElement();
        sw.writeEndDocument();
        sw.close();

        XMLStreamReader sr = getInputFactory().createXMLStreamReader(new ByteArrayInputStream(bos.toByteArray()));
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("data", sr.getLocalName());
        int actRowCount = 0;
        r = new Random(SEED);

        while (sr.nextTag() == START_ELEMENT) { // <row>
            ++actRowCount;
            assertEquals("row", sr.getLocalName());
            expectElemText(sr, "a", String.valueOf(r.nextInt()));
            expectElemText(sr, "b", String.valueOf(r.nextLong()));
            expectElemText(sr, "c", String.valueOf(r.nextBoolean()));
            assertTokenType(END_ELEMENT, sr.nextTag()); // match </row>
        }
        assertEquals(rowCount, actRowCount);
    }

    /*
    ///////////////////////////////////////////////
    // Helper methods
    ///////////////////////////////////////////////
     */

    private void expectElemText(XMLStreamReader sr, String elem, String value)
        throws XMLStreamException
    {
        assertTokenType(START_ELEMENT, sr.nextTag());
        assertEquals(elem, sr.getLocalName());
        String actValue = sr.getElementText();
        if (!value.equals(actValue)) {
            fail("Expected value '"+value+"' (for element '"+elem+"'), got '"+actValue+"' (len "+actValue.length()+"): location "+sr.getLocation());
        }
        assertTokenType(END_ELEMENT, sr.getEventType());
    }
}
