package wstxtest.stream;

import java.io.*;

import javax.xml.stream.*;

/**
 * This set on unit tests checks that woodstox-specific invariants
 * regarding automatic input encoding detection are maintained. Some
 * of these might be required by stax specification too, but it is not
 * quite certain, thus tests are included in woodstox-specific packages.
 */
public class TestEncodingDetection
    extends BaseStreamTest
{
    public void testUtf8()
        throws IOException, XMLStreamException
    {
        /* Default is, in absence of any other indications, UTF-8...
         * let's check the shortest legal doc:
         */
        String XML = "<a/>";
        byte[] b = XML.getBytes("UTF-8");
        XMLStreamReader sr = getReader(b);
        assertTokenType(START_DOCUMENT, sr.getEventType());
        assertNull(sr.getCharacterEncodingScheme());
        assertEquals("UTF-8", sr.getEncoding());
        // let's iterate just for fun though
        assertTokenType(START_ELEMENT, sr.next());
        sr.close();
    }

    public void testUtf16()
        throws XMLStreamException
    {
        // Should be able to figure out encoding...
        String XML = ".<?xml version='1.0'?><root/>";

        /* Let's first check a somewhat common case; figuring out UTF-16
         * encoded doc (which has to have BOM, thus); first, big-endian
         */
        StringBuffer sb = new StringBuffer(XML);
        sb.setCharAt(0, (char) 0xFEFF);

        byte[] b = getUtf16Bytes(sb.toString(), true);
        XMLStreamReader sr = getReader(b);
        assertTokenType(START_DOCUMENT, sr.getEventType());
        assertNull(sr.getCharacterEncodingScheme());
        assertEquals("UTF-16BE", sr.getEncoding());
        // let's iterate just for fun though
        assertTokenType(START_ELEMENT, sr.next());
        sr.close();

        // and then little-endian
        b = getUtf16Bytes(sb.toString(), false);
        sr = getReader(b);
        assertTokenType(START_DOCUMENT, sr.getEventType());
        assertNull(sr.getCharacterEncodingScheme());
        assertEquals("UTF-16LE", sr.getEncoding());
        assertTokenType(START_ELEMENT, sr.next());
        sr.close();
    }

    /*
    /////////////////////////////////////////
    // Non-test methods
    /////////////////////////////////////////
     */

    private byte[] getUtf16Bytes(String input, boolean bigEndian)
    {
        int len = input.length();
        byte[] b = new byte[len+len];
        int offset = bigEndian ? 1 : 0; // offset for LSB
        for (int i = 0; i < len; ++i) {
            int c = input.charAt(i);
            // BOM is 2-byte, others 1 byte...
            b[i+i+offset] = (byte) (c & 0xFF);
            b[i+i+(1 - offset)] = (byte) (c >> 8);
        }
        return b;
    }

    private XMLStreamReader getReader(byte[] b)
        throws XMLStreamException
    {
        XMLInputFactory f = getInputFactory();
        return f.createXMLStreamReader(new ByteArrayInputStream(b));
    }
}
