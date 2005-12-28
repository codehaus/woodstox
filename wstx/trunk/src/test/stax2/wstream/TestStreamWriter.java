package stax2.wstream;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.*;

/**
 * Unit test suite that focuses on testing additional methods that
 * StAX2 has for stream writers.
 */
public class TestStreamWriter
    extends BaseWriterTest
{
    public void testCData()
        throws XMLStreamException
    {
        final String CDATA_TEXT = "Let's test it with some ] ]> data; <tag>s and && chars and all!";

        for (int i = 0; i < 2; ++i) {
            boolean ns = (i > 0);
            StringWriter strw = new StringWriter();
            XMLStreamWriter2 w = getNonRepairingWriter(strw, ns);
            
            w.writeStartDocument();
            w.writeStartElement("test");

            char[] cbuf = new char[CDATA_TEXT.length() + 10];
            CDATA_TEXT.getChars(0, CDATA_TEXT.length(), cbuf, 3);
            w.writeCData(cbuf, 3, CDATA_TEXT.length());
            w.writeEndElement();
            w.writeEndDocument();
            w.close();
            
            // And then let's parse and verify it all:
            
            XMLStreamReader sr = constructNsStreamReader(strw.toString(), true);
            assertTokenType(START_DOCUMENT, sr.getEventType());
            assertTokenType(START_ELEMENT, sr.next());
            
            // Now, parsers are allowed to report CHARACTERS or CDATA
            int tt = sr.next();
            if (tt != CHARACTERS && tt != CDATA) {
                assertTokenType(CDATA, tt); // to cause failure
            }
            assertFalse(sr.isWhiteSpace());
            assertEquals(CDATA_TEXT, getAndVerifyText(sr));
            assertTokenType(END_ELEMENT, sr.next());
            assertTokenType(END_DOCUMENT, sr.next());
        }
    }

    public void testCopy()
        throws XMLStreamException
    {
        final String XML =
            "<?xml version='1.0'?>\n"
            +"<!DOCTYPE root [  <!ENTITY foo 'value'> ]>\n"
            +"<root>\n"
            +"<!-- comment! --><?proc instr?>"
            +"Text: &amp; <leaf attr='xyz' xmlns:a='url:foo' a:xyz='1' />"
            +"</root>"
            ;

        for (int i = 0; i < 2; ++i) {
            boolean ns = (i > 0);
            XMLStreamReader2 sr = constructNsStreamReader(XML, ns);
            StringWriter strw = new StringWriter();
            XMLStreamWriter2 w = getNonRepairingWriter(strw, ns);
            while (sr.hasNext()) {
                sr.next();
                w.copyEventFromReader(sr, false);
            }
            sr.close();
            String xmlOut = strw.toString();

            // And let's parse it to verify it's still valid...
            sr = constructNsStreamReader(xmlOut, ns);
            streamThrough(sr);
        }
    }
}
