package wstxtest.vstream;

import java.io.*;

import javax.xml.stream.*;

import wstxtest.stream.BaseStreamTest;

/**
 * This test suite should really be part of wstx-tools package, but since
 * there is some supporting code within core Woodstox, it was added here.
 * That way it is easier to check that no DTDFlatten functionality is
 * broken by low-level changes.
 */
public class TestDTD
    extends BaseStreamTest
{
    final static class MyReporter implements XMLReporter
    {
        public int count = 0;
        
        public void report(String message, String errorType, Object relatedInformation, Location location)
        {
            ++count;
        }
    }

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

    /*
    //////////////////////////////////////////////////////
    // Helper methods
    //////////////////////////////////////////////////////
     */

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
