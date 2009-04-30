package wstxtest.vstream;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.validation.*;

import wstxtest.stream.BaseStreamTest;

/**
 * This test suite should really be part of wstx-tools package, but since
 * there is some supporting code within core Woodstox, it was added here.
 * That way it is easier to check that no DTDFlatten functionality is
 * broken by low-level changes.
 */
public class TestDTD
    extends BaseValidationTest
{
    final static class MyReporter implements XMLReporter
    {
        public int count = 0;
        
        public void report(String message, String errorType, Object relatedInformation, Location location)
        {
            ++count;
        }
    }

    final static String SIMPLE_DTD =
        "<!ELEMENT root (leaf+)>\n"
        +"<!ATTLIST root attr CDATA #REQUIRED>\n"
        +"<!ELEMENT leaf EMPTY>\n"
        ;

    /**
     * Test to show how [WSTX-190] occurs.
     */
    public void testMissingAttrWithReporter()
        throws XMLStreamException
    {
        String XML = "<!DOCTYPE root [\n"
            +"<!ELEMENT root EMPTY>\n"
            +"]><root attr='123' />";
        MyReporter rep = new MyReporter();
        XMLStreamReader sr = getValidatingReader(XML, rep);
        assertTokenType(DTD, sr.next());
        // and now should get a validation problem
        assertTokenType(START_ELEMENT, sr.next());
        assertTokenType(END_ELEMENT, sr.next());
        sr.close();
        assertEquals(1, rep.count);
    }

    public void testFullValidationOk()
        throws XMLStreamException
    {
        String XML = "<root attr='123'><leaf /></root>";
        XMLValidationSchema schema = parseDTDSchema(SIMPLE_DTD);
        XMLStreamReader2 sr = getReader(XML);
        sr.validateAgainst(schema);
        while (sr.next() != END_DOCUMENT) { }
        sr.close();
    }

    /**
     * And then a test for validating starting when stream points
     * to START_ELEMENT
     */
    public void testPartialValidationOk()
        throws XMLStreamException
    {
        String XML = "<root attr='123'><leaf /></root>";
        XMLValidationSchema schema = parseDTDSchema(SIMPLE_DTD);
        XMLStreamReader2 sr = getReader(XML);
        assertTokenType(START_ELEMENT, sr.next());
        sr.validateAgainst(schema);
        while (sr.next() != END_DOCUMENT) { }
        sr.close();
    }

    /**
     * Another test for checking that validation does end when
     * sub-tree ends...
     */
    public void testPartialValidationFollowedBy()
        throws XMLStreamException
    {
        String XML = "<x><root><leaf /></root><foobar /></x>";
        XMLValidationSchema schema = parseDTDSchema(SIMPLE_DTD);
        XMLStreamReader2 sr = getReader(XML);
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("x", sr.getLocalName());
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("root", sr.getLocalName());
        sr.validateAgainst(schema);
        while (sr.next() != END_DOCUMENT) { }
        sr.close();
    }

    /*
    //////////////////////////////////////////////////////
    // Helper methods
    //////////////////////////////////////////////////////
     */

    private XMLStreamReader2 getReader(String contents)
        throws XMLStreamException
    {
        XMLInputFactory f = getInputFactory();
        setValidating(f, false);
        return constructStreamReader(f, contents);
    }

    private XMLStreamReader getValidatingReader(String contents, XMLReporter rep)
        throws XMLStreamException
    {
        XMLInputFactory f = getInputFactory();
        if (rep != null) {
            f.setProperty(XMLInputFactory.REPORTER, rep);
        }
        setSupportDTD(f, true);
        setValidating(f, true);
        return constructStreamReader(f, contents);
    }
}
