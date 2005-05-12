package stax2.stream;

import javax.xml.stream.*;

import org.codehaus.stax2.*;
 
import stax2.BaseStax2Test;

/**
 * Set of unit tests that checks that the {@link LocationInfo} implementation
 * works as expected, provides proper values or -1 to indicate "don't know".
 */
public class TestLocationInfo
    extends BaseStax2Test
{
    final static String TEST_DOC =
        "<?xml version='1.0'?>"
        +"<!DOCTYPE root [\n" // first char: 21; row 1
        +"<!ENTITY ent 'simple\ntext'>\n" // fc: 38; row 2
        +"<!ENTITY ent2 '<tag>foo</tag>'>\n" // fc: 66; row 4
        +"]>\n" // fc: 98; row 5
        +"<root>Entity: " // fc: 101; row 6
        +"&ent; " // fc: 115; row 6
        +"<leaf />\r\n" // fc: 121; row 6
        +"&ent2;" // fc: 137; row 7
        +"</root>"; // fc: 144; row 7
    // EOF, fc: 150; row 7

    final static String TEST_SHORT_DOC = "<root />";

    public TestLocationInfo(String name) {
        super(name);
    }

    public void testLocations()
        throws XMLStreamException
    {
        XMLStreamReader2 sr = getReader(TEST_DOC);
        LocationInfo loc;
        loc = sr.getLocationInfo();
        assertLocation(sr, loc.getStartLocation(), 1, 1,
                       0, loc.getStartingByteOffset(),
                       0, loc.getStartingCharOffset());
        assertLocation(sr, loc.getEndLocation(), 22, 1,
                       21, loc.getEndingByteOffset(),
                       21, loc.getEndingCharOffset());

        assertTokenType(DTD, sr.next());
        loc = sr.getLocationInfo();
        assertLocation(sr, loc.getStartLocation(), 22, 1,
                       21, loc.getStartingByteOffset(),
                       21, loc.getStartingCharOffset());
        assertLocation(sr, loc.getEndLocation(), 3, 5,
                       100, loc.getEndingByteOffset(),
                       100, loc.getEndingCharOffset());

        // Let's ignore text/space, if there is one:
        int type;

        while ((type = sr.next()) != START_ELEMENT) {
            ;
        }

        loc = sr.getLocationInfo();
        assertLocation(sr, loc.getStartLocation(), 1, 6,
                       101, loc.getStartingByteOffset(),
                       101, loc.getStartingCharOffset());
        assertLocation(sr, loc.getEndLocation(), 7, 6,
                       107, loc.getEndingByteOffset(),
                       107, loc.getEndingCharOffset());

        // !!! TBI
    }

    public void testInitialLocation()
        throws XMLStreamException
    {
        // First, let's test 'missing' start doc:
        XMLStreamReader2 sr = getReader("<root />");
        LocationInfo loc;
        loc = sr.getLocationInfo();
        assertLocation(sr, loc.getStartLocation(), 1, 1,
                       0, loc.getStartingByteOffset(),
                       0, loc.getStartingCharOffset());
        assertLocation(sr, loc.getEndLocation(), 1, 1,
                       0, loc.getEndingByteOffset(),
                       0, loc.getEndingCharOffset());
        sr.close();

        // and then a real one
        sr = getReader("<?xml version='1.0'\r\n?>");
        loc = sr.getLocationInfo();
        assertLocation(sr, loc.getStartLocation(), 1, 1,
                       0, loc.getStartingByteOffset(),
                       0, loc.getStartingCharOffset());
        assertLocation(sr, loc.getEndLocation(), 3, 2,
                       23, loc.getEndingByteOffset(),
                       23, loc.getEndingCharOffset());
        sr.close();
    }

    private void assertLocation(XMLStreamReader sr, XMLStreamLocation2 loc,
                                int expCol, int expRow,
                                int expByteOffset, long actByteOffset,
                                int expCharOffset, long actCharOffset)
    {
        assertEquals("Incorrect column for "+tokenTypeDesc(sr.getEventType()),
                     expCol, loc.getColumnNumber());
        assertEquals("Incorrect row for "+tokenTypeDesc(sr.getEventType()),
                     expRow, loc.getLineNumber());

        if (actByteOffset == -1) { // no info, that's fine
            ;
        } else {
            assertEquals(expByteOffset, actByteOffset);
        }
        if (actCharOffset == -1) { // no info, that's fine
            ;
        } else {
            assertEquals(expCharOffset, actCharOffset);
        }
    }

    /*
    ////////////////////////////////////////
    // Private methods
    ////////////////////////////////////////
     */

    private XMLStreamReader2 getReader(String contents)
        throws XMLStreamException
    {
        XMLInputFactory f = getInputFactory();
        setCoalescing(f, false); // shouldn't really matter
        setNamespaceAware(f, true);
        setSupportDTD(f, true);
        // No need to validate, just need entities
        setValidating(f, false);
        return (XMLStreamReader2) constructStreamReader(f, contents);
    }
}
