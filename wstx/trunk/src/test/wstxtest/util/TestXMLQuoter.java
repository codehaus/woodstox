package wstxtest.util;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

import junit.framework.TestCase;

import com.ctc.wstx.util.XMLQuoter;

/**
 * Simple unit tests for testing {@link XMLQuoter}
 */
public class TestXMLQuoter
    extends TestCase
{
    public TestXMLQuoter(String name) {
        super(name);
    }

    public void testOutputXMLText()
        throws IOException // never gets thrown, actually
    {
        StringWriter sw = new StringWriter();
        XMLQuoter.outputXMLText(sw, "<>");
        assertEquals("&lt;>", sw.toString());
        sw = new StringWriter();
        XMLQuoter.outputXMLText(sw, "<>".toCharArray(), 0, 2);
        assertEquals("&lt;>", sw.toString());

        sw = new StringWriter();
        XMLQuoter.outputXMLText(sw, "<!CDATA[]]>");
        assertEquals("&lt;!CDATA[]]&gt;", sw.toString());

        sw = new StringWriter();
        XMLQuoter.outputXMLText(sw, "&");
        assertEquals("&amp;", sw.toString());
        sw = new StringWriter();
        XMLQuoter.outputXMLText(sw, ">xx");
        assertEquals(">xx", sw.toString());

        sw = new StringWriter();
        XMLQuoter.outputXMLText(sw, "rock & roll>");
        assertEquals("rock &amp; roll>", sw.toString());

        sw = new StringWriter();
        XMLQuoter.outputXMLText(sw, "ab&cd".toCharArray(), 1, 3);
        assertEquals("b&amp;c", sw.toString());
    }

    public void testOutputDTDText()
    {
        // !!! TBI
    }

    public void testOutputDoubleQuotedAttr()
    {
        // !!! TBI
    }

    public void testIsAllWhitespace()
    {
        assertTrue(XMLQuoter.isAllWhitespace("  \r   \r\n    \t"));
        assertTrue(XMLQuoter.isAllWhitespace(" "));
        assertTrue(XMLQuoter.isAllWhitespace(" ".toCharArray(), 0, 1));
        assertTrue(XMLQuoter.isAllWhitespace("\r\n\t"));
        assertTrue(XMLQuoter.isAllWhitespace("\r\n\t".toCharArray(), 0, 3));
        assertTrue(XMLQuoter.isAllWhitespace("x \t".toCharArray(), 1, 2));

        assertFalse(XMLQuoter.isAllWhitespace("x"));
        assertFalse(XMLQuoter.isAllWhitespace("                      !"));
    }
}
