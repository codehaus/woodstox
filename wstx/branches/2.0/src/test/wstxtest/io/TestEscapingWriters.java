package wstxtest.io;

import java.io.*;
import java.util.*;

import junit.framework.TestCase;

import com.ctc.wstx.io.WriterBase;

/**
 * Simple unit tests for testing functionality of escaping writers
 * ({@link AttrValueEscapingWriter}, {@link TextEscapingWriter}).
 */
public class TestEscapingWriters
    extends TestCase
{
    /**
     * Need to create a dummy class to test WriterBase's functionality
     */
    private final class TestableWriter
        extends WriterBase
    {
        TestableWriter(Writer out) {
            super(out);
        }

        // Ugh, need this method to call the protected method...
        public void doWriteAsEntity(int c)
            throws IOException
        {
            writeAsEntity(c);
        }
    }

    public TestEscapingWriters(String name) {
        super(name);
    }

    /**
     * This unit test was added to catch a regression bug in
     * {@link WriterBase#writeAsEntity}.
     */
    public void testWriteAsEntity()
        throws IOException // never gets thrown, actually
    {
        int[] in_c = new int[] {
            13, 127, 0x00a0, 0x00ff, 4097
        };
        String[] out_str = new String[] {
            "&#x000d;",
            "&#x007f;",
            "&#x00a0;",
            "&#x00ff;",
            "&#x1001;",
        };

        for (int i = 0; i < in_c.length; ++i) {
            StringWriter sw = new StringWriter();
            TestableWriter wb = new TestableWriter(sw);
            wb.doWriteAsEntity((char) in_c[i]);
            sw.close();
            assertEquals(out_str[i], sw.toString());
        }
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
