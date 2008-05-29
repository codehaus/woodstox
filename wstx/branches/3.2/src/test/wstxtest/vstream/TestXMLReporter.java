package wstxtest.vstream;

import javax.xml.stream.*;

import org.codehaus.stax2.*;

import wstxtest.stream.BaseStreamTest;

/**
 * Simple testing to ensure that {@link XMLReporter} works as
 * expected with respect to validation errors.
 */
public class TestXMLReporter
    extends BaseStreamTest
{
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

        public MyReporter() { }
        
        public void report(String message,
                           String errorType,
                           Object relatedInformation,
                           Location location)
            throws XMLStreamException
        {
            ++count;
        }

        public int getCount() { return count; }
    }
}


