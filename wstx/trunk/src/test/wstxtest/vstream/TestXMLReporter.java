package wstxtest.vstream;

import javax.xml.stream.*;

import org.codehaus.stax2.validation.XMLValidationProblem;

import wstxtest.stream.BaseStreamTest;

/**
 * Simple testing to ensure that {@link XMLReporter} works as
 * expected with respect to validation errors.
 */
public class TestXMLReporter
    extends BaseStreamTest
{
    /**
     * Basic unit test for verifying that XMLReporter gets validation
     * errors reported.
     */
    public void testValidationError()
        throws XMLStreamException
    {
        String XML =
            "<!DOCTYPE root [\n"
            +" <!ELEMENT root (#PCDATA)>\n"
            +"]><root>...</root>";
            ;
        MyReporter rep = new MyReporter();
        XMLStreamReader sr = getReader(XML, rep);

        // First, valid case, shouldn't get any notifications
        streamThrough(sr);
        sr.close();
        assertEquals(0, rep.getCount());

        // Then invalid, with one error
        XML =
            "<!DOCTYPE root [\n"
            +" <!ELEMENT root (leaf+)>\n"
            +"]><root></root>";
        ;
        rep = new MyReporter();
        sr = getReader(XML, rep);
        streamThrough(sr);
        sr.close();
        assertEquals(1, rep.getCount());
    }

    /**
     * Test for specific validation error, mostly to verify
     * fix to [WSTX-155] (and guard against regression)
     */
    public void testMissingAttrError()
        throws XMLStreamException
    {
        String XML =
            "<!DOCTYPE root [\n"
            +" <!ELEMENT root (#PCDATA)>\n"
            +" <!ATTLIST root attr CDATA #REQUIRED>\n"
            +"]><root />";
            ;
        MyReporter rep = new MyReporter();
        XMLStreamReader sr = getReader(XML, rep);

        streamThrough(sr);
        sr.close();
        assertEquals(1, rep.getCount());
    }

    public void testInvalidFixedAttr()
        throws XMLStreamException
    {
        // Not ok to have any other value, either completely different
        String XML = "<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"<!ATTLIST root attr CDATA #FIXED 'fixed'>\n"
            +"]>\n<root attr='wrong'/>";
        MyReporter rep = new MyReporter();
        XMLStreamReader sr = getReader(XML, rep);

        streamThrough(sr);
        sr.close();
        assertEquals(1, rep.getCount());

        // Or one with extra white space (CDATA won't get fully normalized)
        XML = "<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"<!ATTLIST root attr CDATA #FIXED 'fixed'>\n"
            +"]>\n<root attr=' fixed '/>";
        rep = new MyReporter();
        sr = getReader(XML, rep);
        streamThrough(sr);
        sr.close();
        assertEquals(1, rep.getCount());
    }


    public void testInvalidIdAttr()
        throws XMLStreamException
    {
        // Error: undefined id 'someId'
        String XML = "<!DOCTYPE elem [\n"
            +"<!ELEMENT elem (elem*)>\n"
            +"<!ATTLIST elem id ID #IMPLIED>\n"
            +"<!ATTLIST elem ref IDREF #IMPLIED>\n"
            +"]>\n<elem ref='someId'/>";
        MyReporter rep = new MyReporter();
        XMLStreamReader sr = getReader(XML, rep);

        streamThrough(sr);
        sr.close();
        assertEquals(1, rep.getCount());

        // Error: empty idref value
        XML = "<!DOCTYPE elem [\n"
            +"<!ELEMENT elem (elem*)>\n"
            +"<!ATTLIST elem id ID #IMPLIED>\n"
            +"<!ATTLIST elem ref IDREF #IMPLIED>\n"
            +"]>\n<elem ref=''/>";
        rep = new MyReporter();
        sr = getReader(XML, rep);
        streamThrough(sr);
        sr.close();
        assertEquals(1, rep.getCount());
    }

    public void testInvalidSimpleChoiceStructure()
        throws XMLStreamException
    {
        String XML = "<!DOCTYPE root [\n"
            +"<!ELEMENT root (a1 | a2)+>\n"
            +"<!ELEMENT a1 EMPTY>\n"
            +"<!ELEMENT a2 (#PCDATA)>\n"
            +"]>\n"
            +"<root />";
        MyReporter rep = new MyReporter();
        XMLStreamReader sr = getReader(XML, rep);

        streamThrough(sr);
        sr.close();
        assertEquals(1, rep.getCount());
    }
        
    /**
     * This test verifies that exception XMLReporter rethrows gets
     * properly propagated.
     */
    public void testErrorRethrow()
        throws XMLStreamException
    {
        String XML =
            "<!DOCTYPE root [\n"
            +" <!ELEMENT root (leaf+)>\n"
            +"]><root></root>";
        ;
        MyReporter rep = new MyReporter();
        rep.enableThrow();
        XMLStreamReader sr = getReader(XML, rep);
        try {
            streamThrough(sr);
            fail("Expected a re-thrown exception for invalid content");
        } catch (XMLStreamException xse) {
            ;
        }
        sr.close();
        assertEquals(1, rep.getCount());
    }

    /*
    //////////////////////////////////////////////////
    // Helper methods
    //////////////////////////////////////////////////
     */

    private XMLStreamReader getReader(String xml, XMLReporter rep)
        throws XMLStreamException
    {
        XMLInputFactory f = getInputFactory();
        setNamespaceAware(f, true);
        setSupportDTD(f, true);
        setValidating(f, true);
        f.setXMLReporter(rep);
        return constructStreamReader(f, xml);
    }

    final static class MyReporter
        implements XMLReporter
    {
        int count = 0;

        boolean doThrow = false;

        public MyReporter() { }

        public void enableThrow() { doThrow = true; }

        public void report(String message,
                           String errorType,
                           Object relatedInfo,
                           Location location)
            throws XMLStreamException
        {
            ++count;
            if (doThrow) {
                throw new XMLStreamException(message, location);
            }
            /* 30-May-2008, TSa: Need to ensure that extraArg is of
             *   type XMLValidationProblem; new constraint for Woodstox
             */
            if (relatedInfo == null) {
                throw new IllegalArgumentException("relatedInformation null, should be an instance of XMLValidationProblem");
            }
            if (!(relatedInfo instanceof XMLValidationProblem)) {
                throw new IllegalArgumentException("relatedInformation not an instance of XMLValidationProblem (but "+relatedInfo.getClass().getName()+")");
            }
        }

        public int getCount() { return count; }
    }
}


