package wstxtest.stream;

import java.io.*;
import java.util.Random;
import javax.xml.stream.*;

import org.codehaus.stax2.*;
import com.ctc.wstx.api.ReaderConfig;
import com.ctc.wstx.stax.WstxInputFactory;

/**
 * Unit tests for testing Woodstox-specific features of location
 * tracking.
 */
public class TestLocation
    extends BaseStreamTest
{
    public void testSimpleLocation()
        throws XMLStreamException
    {
        final String XML = "\r\n  <root>\r\n </root>";

        XMLInputFactory f = getWstxInputFactory();
        XMLStreamReader2 sr = (XMLStreamReader2)f.createXMLStreamReader(new StringReader(XML));

        int type = sr.next();
        if (type == XMLStreamConstants.SPACE) {
            type = sr.next();
        }
        assertTokenType(START_ELEMENT, type);

        Location loc = sr.getLocationInfo().getStartLocation();
        assertEquals(2, loc.getLineNumber());
        assertEquals(3, loc.getColumnNumber());
        assertEquals(4, loc.getCharacterOffset());

        assertTokenType(CHARACTERS, sr.next());
        assertTokenType(END_ELEMENT, sr.next());
        loc = sr.getLocationInfo().getStartLocation();
        assertEquals(3, loc.getLineNumber());
        assertEquals(2, loc.getColumnNumber());
        assertEquals(13, loc.getCharacterOffset());
    }

    public void testLineNumbers()
        throws XMLStreamException
    {
        final int SEED = 129;
        final int ROWS = 1000;

        // First, let's create xml doc:
        StringBuffer sb = new StringBuffer();
        sb.append("<a>");
        Random r = new Random(SEED);
        for (int i = 0; i < ROWS; ++i) {
            switch (r.nextInt() % 3) {
            case 0:
                sb.append("\r");
                break;
            case 1:
                sb.append("\r\n");
                break;
            default:
                sb.append("\n");
            }
            int ind = r.nextInt() % 7;
            while (--ind >= 0) {
                sb.append(' ');
            }
            sb.append("<b/>");
        }
        sb.append("</a>");

        // And then we'll parse to ensure line numbers and offsets are ok
        
        WstxInputFactory f = getWstxInputFactory();
        // Need to shrink it to get faster convergence
        f.getConfig().setInputBufferLength(23);
        XMLStreamReader2 sr = (XMLStreamReader2) f.createXMLStreamReader(new StringReader(sb.toString()));

        assertTokenType(START_ELEMENT, sr.next());

        int linenr = 1;
        int col = 4;
        int chars = 3;

        r = new Random(SEED);
        while (true) {
            // END_ELEM signals end...
            int type = sr.next();
            if (type == END_ELEMENT) {
                assertEquals("a", sr.getLocalName());
                break;
            }
            assertTokenType(type, type);

            Location loc = sr.getLocationInfo().getStartLocation();
            assertEquals(linenr, loc.getLineNumber());
            assertEquals(col, loc.getColumnNumber());
            assertEquals(chars, loc.getCharacterOffset());

            sb = new StringBuffer();
            boolean offByOne = false;
            switch (r.nextInt() % 3) {
            case 1:
                offByOne = true; // Since \r\n gets truncated to \n
            }
            sb.append("\n");
            int ind = r.nextInt() % 7;
            while (--ind >= 0) {
                sb.append(' ');
            }
            String ws = sb.toString();
            if (!ws.equals(sr.getText())) {
                fail("Expected "+quotedPrintable(ws)+", got "+quotedPrintable(sr.getText()));
            }

            /* Char offset refers to input chars, and thus includes original
             * linefeed (which is longer than result, for \r\n)
             */
            chars += ws.length();
            if (offByOne) {
                ++chars;
            }
            ++linenr;
            // Column won't, but since it's one-based, it'll still equal ws len
            col = sb.length();

            assertTokenType(START_ELEMENT, sr.next());
            assertEquals("b", sr.getLocalName());
            loc = sr.getLocationInfo().getStartLocation();
            assertEquals("Line number wrong", linenr, loc.getLineNumber());
            assertEquals("Column number wrong (line "+linenr+")", col, loc.getColumnNumber());
            assertEquals("Character offset wrong (line "+linenr+")", chars, loc.getCharacterOffset());
            assertTokenType(END_ELEMENT, sr.next());
            assertEquals("b", sr.getLocalName());

            chars += 4;
            col += 4;
        }

    }
}
