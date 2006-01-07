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
            +"<![CDATA[and <> there you have it!]]>"
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

    /**
     * Unit test for verifyin that writeRaw() works as expected.
     */
    public void testRaw()
        throws XMLStreamException
    {
        String RAW2 = "<elem>foo&amp;bar</elem>";

        for (int i = 0; i < 3; ++i) {
            boolean ns = (i > 0);
            StringWriter strw = new StringWriter();
            XMLStreamWriter2 w = (i == 2) ? getRepairingWriter(strw)
                : getNonRepairingWriter(strw, ns);
            w.writeStartDocument();
            w.writeStartElement("test");
            w.writeAttribute("attr", "value");
            w.writeRaw("this or &apos;that&apos;");
            char[] cbuf = new char[RAW2.length() + 10];
            RAW2.getChars(0, RAW2.length(), cbuf, 3);
            w.writeRaw(cbuf, 3, RAW2.length());
            w.writeEndElement();
            w.writeEndDocument();
            w.close();
            
            // And then let's parse and verify it all:
            XMLStreamReader sr = constructNsStreamReader(strw.toString(), true);
            assertTokenType(START_DOCUMENT, sr.getEventType());
            assertTokenType(START_ELEMENT, sr.next());
            assertEquals("test", sr.getLocalName());
            assertEquals(1, sr.getAttributeCount());
            assertEquals("attr", sr.getAttributeLocalName(0));
            assertEquals("value", sr.getAttributeValue(0));
            assertTokenType(CHARACTERS, sr.next());
            assertEquals("this or 'that'", getAndVerifyText(sr));
            assertTokenType(START_ELEMENT, sr.next());
            assertEquals("elem", sr.getLocalName());
            assertTokenType(CHARACTERS, sr.next());
            assertEquals("foo&bar", getAndVerifyText(sr));
            assertTokenType(END_ELEMENT, sr.next());
            assertEquals("elem", sr.getLocalName());
            assertTokenType(END_ELEMENT, sr.next());
            assertEquals("test", sr.getLocalName());
            assertTokenType(END_DOCUMENT, sr.next());
        }
    }

    /**
     * First a simplish testing of how exotic characters are escaped
     * in attribute values.
     */
    public void testAttrValueWriterSimple()
        throws IOException, XMLStreamException
    {
        // Let's just ensure escaping is done for chars that need it
        //String IN = "Ok, lessee \u00A0; -- \t and this: \u0531.";
        String IN = "Ok, nbsp: \u00A0; and 'quotes' and \"doubles\" too; and multi-bytes too: [\u0531]";
        doTestAttrValueWriter("ISO-8859-1", IN);
        doTestAttrValueWriter("UTF-8", IN);
        doTestAttrValueWriter("US-ASCII", IN);
    }

    /**
     * And then bit more advanced test for things that need special
     * support for round-tripping
     */
    public void testAttrValueWriterTabsEtc()
        throws IOException, XMLStreamException
    {
        String IN = "How about tabs: [\t] or cr+lf [\r\n]";
        doTestAttrValueWriter("ISO-8859-1", IN);
        doTestAttrValueWriter("UTF-8", IN);
        doTestAttrValueWriter("US-ASCII", IN);
    }

    /*
    //////////////////////////////////////////////////////////
    // Non-test methods:
    //////////////////////////////////////////////////////////
     */

    public XMLOutputFactory2 getFactory(boolean nsAware, boolean repairing)
        throws XMLStreamException
    {
        XMLOutputFactory2 f = getOutputFactory();
        f.setProperty(XMLOutputFactory2.P_NAMESPACE_AWARE,
                      Boolean.valueOf(nsAware));
        f.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES,
                      Boolean.valueOf(repairing));
        return f;
    }

    private void doTestAttrValueWriter(String enc, String IN)
        throws IOException, XMLStreamException
    {
        // First, let's explicitly pass the encoding...
        XMLOutputFactory of = getFactory(false, false);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Writer w = new OutputStreamWriter(out, enc);
        XMLStreamWriter sw = of.createXMLStreamWriter(w);
        
        sw.writeStartDocument(enc, "1.0");
        sw.writeStartElement("elem");
        sw.writeAttribute("attr", IN);
        sw.writeEndElement();
        sw.writeEndDocument();
        sw.close();
        w.close();
        
        // Can we parse it ok?
        XMLInputFactory ifact = getInputFactory();
        XMLStreamReader sr = ifact.createXMLStreamReader(new ByteArrayInputStream(out.toByteArray()), enc);

        // First, let's ensure we see the encoding:
        assertTokenType(START_DOCUMENT, sr.getEventType());
        assertEquals(enc, sr.getCharacterEncodingScheme());

        assertTokenType(START_ELEMENT, sr.next());
        assertEquals(1, sr.getAttributeCount());
        String attrValue = sr.getAttributeValue(0);
        if (!IN.equals(attrValue)) {
            failStrings("Incorrect writing/reading of attribute value",
                        IN, attrValue);
        }
        assertTokenType(END_ELEMENT, sr.next());
        assertTokenType(END_DOCUMENT, sr.next());
        sr.close();
    }
}
