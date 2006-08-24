package wstxtest.io;

import java.io.*;
import java.util.*;

import junit.framework.TestCase;

import com.ctc.wstx.io.*;

import wstxtest.BaseWstxTest;

/**
 * Simple unit tests for testing functionality of escaping writers
 * ({@link AttrValueEscapingWriter}, {@link TextEscapingWriter}).
 */
public class TestEscapingWriters
    extends TestCase
{
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
            "&#xd;", // simple optimization
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

    public void testOutputXMLTextSingleByte()
        throws IOException // never gets thrown, actually
    {
        // First, generic
        doTestSBText(true, true, "<>", "&lt;>");
        doTestSBText(true, true, "<!CDATA[]]>", "&lt;!CDATA[]]&gt;");
        doTestSBText(true, true, ">xx", ">xx");
        doTestSBText(true, true, "rock & roll>", "rock &amp; roll>");
        doTestSBText(true, true, "ab&cd", "ab&amp;cd");
        doTestSBText(true, true, "Hah&", "Hah&amp;");
    }

    private void doTestSBText(boolean doAscii, boolean doISOLatin,
                              String input, String output)
        throws IOException
    {
        for (int i = 0; i < 2; ++i) {
            for (int type = 0; type < 4; ++type) {
                boolean ascii = (i == 0);
                StringWriter sw = new StringWriter();
                Writer w;
                
                if (ascii) {
                    if (!doAscii) {
                        continue;
                    }
                    w = new SingleByteTextWriter(sw, "US-ASCII", 128);
                } else {
                    if (!doISOLatin) {
                        continue;
                    }
                    w = new SingleByteTextWriter(sw, "ISO-8859-1", 256);
                }

                switch (type) {
                case 0: // String output
                    w.write(input);
                    break;
                case 1: // char array, 0 off
                    w.write(input.toCharArray());
                    break;
                case 2: // char array, non-0 off
                    {
                        char[] buf = new char[input.length() + 8];
                        input.getChars(0, input.length(), buf, 4);
                        w.write(buf, 4, input.length());
                    }
                    break;
                default: // char by char
                    for (int x = 0; x < input.length(); ++x) {
                        w.write(input.charAt(x));
                    }
                }

                if (type == 2) { continue; } // HACK

                w.close();
                String act = sw.toString();
                if (!output.equals(act)) {
//System.out.println("Got: ["+act+"]");
//System.out.println("Exp: ["+output+"]");

                    fail("Encoding failure (ascii: "+ascii+", type "+type+"): expected '"
                         +BaseWstxTest.printableWithSpaces(output)+"', got '"
                         +BaseWstxTest.printableWithSpaces(act)+"'");
                }
            }
        }
    }

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
}
