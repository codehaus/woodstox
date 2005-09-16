package wstxtest.stream;

import javax.xml.stream.*;

import org.codehaus.stax2.*;

import com.ctc.wstx.sr.WstxStreamReader;
import com.ctc.wstx.ent.EntityDecl;

/**
 * This unit test suite checks to see that Woodstox implementation dependant
 * functionality works the way it's planned to. In some cases future StAX
 * revisions may dictate exact behaviour expected, but for now expected
 * behaviour is based on
 * a combination of educated guessing and intuitive behaviour. 
 */
public class TestEntityRead
    extends BaseStreamTest
{
    public TestEntityRead(String name) {
        super(name);
    }

    /**
     * This unit test checks that the information received as part of the
     * event, in non-expanding mode, is as expected.
     */
    public void testDeclaredInNonExpandingMode()
        throws XMLStreamException
    {
        String XML = "<!DOCTYPE root [\n"
             +" <!ENTITY myent 'value'>\n"
             +"]><root>text:&myent;more</root>"
            ;

        // Non-expanding, coalescing:
        WstxStreamReader sr = getReader(XML, false, true);
        assertTokenType(DTD, sr.next());
        DTDInfo dtd = sr.getDTDInfo();
        assertNotNull(dtd);

        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());

        assertTokenType(CHARACTERS, sr.next());
        assertTokenType(ENTITY_REFERENCE, sr.next());
        assertEquals("myent", sr.getLocalName());
        EntityDecl ed = sr.getCurrentEntityDecl();
        assertNotNull(ed);
        assertEquals("myent", ed.getName());
        assertEquals("value", ed.getReplacementText());

        // The pure stax way:
        assertEquals("value", sr.getText());

        // Finally, let's see that location info is about right?
        Location loc = ed.getLocation();
        assertNotNull(loc);
        assertEquals(2, loc.getLineNumber());

        /* Hmmh. Not 100% if this location makes sense, but... it's the
         * current behaviour, so we can regression test it.
         */
        assertEquals(3, loc.getColumnNumber());
        // don't care about offsets here... location tests catch them

        assertTokenType(CHARACTERS, sr.next());
        assertTokenType(END_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());

        assertTokenType(END_DOCUMENT, sr.next());
        sr.close();
    }

    /**
     * This unit test checks that in non-expanding mode it is acceptable
     * to refer to undeclared entities.
     */
    public void testUndeclaredInNonExpandingMode()
        throws Exception
    {
        String XML = "<!DOCTYPE root [\n"
             +"]><root>text:&myent;more</root>"
            ;

        // Non-expanding, coalescing:
        WstxStreamReader sr = getReader(XML, false, true);
        assertTokenType(DTD, sr.next());
        DTDInfo dtd = sr.getDTDInfo();
        assertNotNull(dtd);

        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());

        assertTokenType(CHARACTERS, sr.next());

        /* Exception would be a real possibility, so let's catch one (if
         * any) for debugging purposes:
         */

        try {
            assertTokenType(ENTITY_REFERENCE, sr.next());
        } catch (XMLStreamException sex) {
            fail("Did not except a stream exception on undeclared entity in non-entity-expanding mode; got: "+sex);
        }

        assertEquals("myent", sr.getLocalName());
        EntityDecl ed = sr.getCurrentEntityDecl();
        assertNull(ed);

        assertTokenType(CHARACTERS, sr.next());
        assertTokenType(END_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());

        assertTokenType(END_DOCUMENT, sr.next());
        sr.close();
    }

    /*
    ////////////////////////////////////////
    // Private methods, other
    ////////////////////////////////////////
     */

    /**
     * Note: all readers for this set of unit tests enable DTD handling;
     * otherwise entity definitions wouldn't be read. Validation shouldn't
     * need to be enabled just for that purpose.
     */
    private WstxStreamReader getReader(String contents, boolean replEntities,
                                       boolean coalescing)
        throws XMLStreamException
    {
        XMLInputFactory f = getInputFactory();
        setNamespaceAware(f, true);
        setSupportDTD(f, true);
        setValidating(f, false);
        setReplaceEntities(f, replEntities);
        setCoalescing(f, coalescing);
        return (WstxStreamReader) constructStreamReader(f, contents);
    }
}
