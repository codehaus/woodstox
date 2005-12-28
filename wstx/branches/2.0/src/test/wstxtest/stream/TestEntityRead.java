package wstxtest.stream;

import java.util.*;
import javax.xml.stream.*;

import org.codehaus.stax2.*;

import com.ctc.wstx.api.WstxInputProperties;
import com.ctc.wstx.ent.EntityDecl;
import com.ctc.wstx.exc.WstxLazyException;
import com.ctc.wstx.sr.WstxStreamReader;

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
        XMLStreamReader2 sr = getReader(XML, false, true);
        assertTokenType(DTD, sr.next());
        DTDInfo dtd = sr.getDTDInfo();
        assertNotNull(dtd);

        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());

        assertTokenType(CHARACTERS, sr.next());
        assertTokenType(ENTITY_REFERENCE, sr.next());
        assertEquals("myent", sr.getLocalName());
        EntityDecl ed = ((WstxStreamReader) sr).getCurrentEntityDecl();
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
     * This unit test verifies that it's possible to add a Map of
     * expansions from Entity names to 
     */
    public void testUndeclaredUsingCustomMap()
        throws XMLStreamException
    {
        // First, let's check actual usage:

        String XML = "<root>ok: &myent;&myent2;</root>";
        String EXP_TEXT = "ok: (simple)expand to ([text])";
        XMLInputFactory fact = getConfiguredFactory(true, true);
        Map m = new HashMap();
        m.put("myent", "(simple)");
        m.put("myent3", "[text]");
        m.put("myent2", "expand to (&myent3;)");
        fact.setProperty(WstxInputProperties.P_CUSTOM_INTERNAL_ENTITIES, m);
        XMLStreamReader sr = constructStreamReader(fact, XML);
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        assertTokenType(CHARACTERS, sr.next());
        assertEquals(EXP_TEXT, getAndVerifyText(sr));
        assertTokenType(END_ELEMENT, sr.next());
        sr.close();

        /* And then see if we can query configured value and get expected
         * types of results
         */
        m = (Map) fact.getProperty(WstxInputProperties.P_CUSTOM_INTERNAL_ENTITIES);
        assertNotNull(m);
        assertEquals(3, m.size());
        Iterator it = m.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String name = entry.getKey().toString();
            if (name.equals("myent") || name.equals("myent2")
                || name.equals("myent3")) {
                // fine, let's just verify the type
                EntityDecl ed = (EntityDecl) entry.getValue();
                assertNotNull(ed);
            } else {
                fail("Unexpected entity '"+name+"' in the custom entity map");
            }
        }
    }

    /*
    ////////////////////////////////////////
    // Private methods
    ////////////////////////////////////////
     */

    /**
     * Note: all readers for this set of unit tests enable DTD handling;
     * otherwise entity definitions wouldn't be read. Validation shouldn't
     * need to be enabled just for that purpose.
     */
    private XMLStreamReader2 getReader(String contents, boolean replEntities,
                                       boolean coalescing)
        throws XMLStreamException
    {
        XMLInputFactory f = getConfiguredFactory(replEntities, coalescing);
        return (XMLStreamReader2) constructStreamReader(f, contents);
    }

    private XMLInputFactory getConfiguredFactory(boolean replEntities, boolean coalescing)
        throws XMLStreamException
    {
        XMLInputFactory f = getInputFactory();
        setNamespaceAware(f, true);
        setSupportDTD(f, true);
        setValidating(f, false);
        setReplaceEntities(f, replEntities);
        setCoalescing(f, coalescing);
        return f;
    }
}
