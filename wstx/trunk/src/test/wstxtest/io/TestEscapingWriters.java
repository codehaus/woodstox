package wstxtest.io;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

import junit.framework.TestCase;

import com.ctc.wstx.io.*;

/**
 * Simple unit tests for testing functionality of escaping writers
 * ({@link AttrValueEscapingWriter}, {@link TextEscapingWriter}).
 */
public class TestEscapingWriters
    extends TestCase
{
    public TestEscapingWriters(String name) {
        super(name);
    }

    public void testOutputXMLText()
        throws IOException // never gets thrown, actually
    {
        /*
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
        */
    }

    public void testOutputDoubleQuotedAttr()
    {
        // !!! TBI
    }
}
