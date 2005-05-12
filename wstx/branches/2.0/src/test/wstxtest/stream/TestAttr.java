package wstxtest.stream;

import java.io.*;
import java.util.Random;

import javax.xml.stream.*;

import com.ctc.wstx.stax.WstxInputFactory;

import wstxtest.cfg.*;

public class TestAttr
    extends BaseStreamTest
{
    final static String XML_11_ATTRS =
        "<tag method='a' activeShell='x' source='y' data='z' "
        +"widget='a' length='1' start='2' styledTextNewValue='t' "
        +"replacedText='' styledTextFunction='f' raw='b' />";

    public TestAttr(String name) {
        super(name);
    }

    /**
     * This test case was added after encountering a specific problem, which
     * only occurs when many attributes were spilled from main hash area....
     * and that's why exact attribute names do matter.
     */
    public void testManyAttrs()
        throws Exception
    {
        // First non-NS
        XMLStreamReader sr = getReader(XML_11_ATTRS, false);
        streamThrough(sr);
        // Then NS
        sr = getReader(XML_11_ATTRS, true);
        streamThrough(sr);
    }

    /**
     * Tests that the attributes are returned in the document order;
     * an invariant Woodstox honors (even though not mandated by StAX 1.0).
     */
    public void testAttributeOrder()
        throws XMLStreamException
    {
        String XML =  "<?xml version='1.0'?>"
        +"<!DOCTYPE root [\n"
        +"<!ELEMENT root EMPTY>\n"
        +"<!ATTLIST root realDef CDATA 'def'>\n"
        +"<!ATTLIST root attr2 CDATA #IMPLIED>\n"
        +"<!ATTLIST root attr3 CDATA #IMPLIED>\n"
        +"<!ATTLIST root attr4 CDATA #IMPLIED>\n"
        +"<!ATTLIST root defAttr CDATA 'deftoo'>\n"
        +"]>"
        +"<root attr4='4' attr3='3' defAttr='foobar' attr2='2' />"
            ;

        final String[] EXP = {
            "attr4", "4",
            "attr3", "3",
            "defAttr", "foobar",
            "attr2", "2",
            "realDef", "def",
        };

        for (int i = 0; i < 2; ++i) {
            boolean ns = (i > 0);
            // Need validating, to get default attribute values
            XMLStreamReader sr = getValidatingReader(XML, ns);

            assertTokenType(START_DOCUMENT, sr.getEventType());
            assertTokenType(DTD, sr.next());
            assertTokenType(START_ELEMENT, sr.next());

            assertEquals(5, sr.getAttributeCount());

            for (int ix = 0, len = sr.getAttributeCount(); ix < len; ++ix) {
                String lname = sr.getAttributeLocalName(ix);
                assertEquals("Attr #"+ix+" name", EXP[ix+ix], lname);
                assertEquals("Attr #"+ix+" value",
                             EXP[ix+ix+1], sr.getAttributeValue(ix));

                assertEquals("Attribute '"+lname+"' wrongly identified WRT isSpecified: ",
                             !lname.equals("realDef"),
                             sr.isAttributeSpecified(ix));
            }

            assertTokenType(END_ELEMENT, sr.next());
        }
    }

    /*
    //////////////////////////////////////////////////////
    // Internal methods
    //////////////////////////////////////////////////////
     */

    private XMLStreamReader getReader(String contents, boolean nsAware)
        throws XMLStreamException
    {
        WstxInputFactory f = getWstxInputFactory();
        f.getConfig().doSupportNamespaces(nsAware);
        return constructStreamReader(f, contents);
    }

    private XMLStreamReader getValidatingReader(String contents, boolean nsAware)
        throws XMLStreamException
    {
        WstxInputFactory f = getWstxInputFactory();
        f.getConfig().doSupportNamespaces(nsAware);
        f.getConfig().doSupportDTDs(true);
        f.getConfig().doValidateWithDTD(true);
        return constructStreamReader(f, contents);
    }
}

