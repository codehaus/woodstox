package stax2.vwstream;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.XMLStreamWriter2;
import org.codehaus.stax2.validation.*;

public class TestOutputValidation
    extends BaseOutputTest
{
    public void testValidMixedContent()
        throws XMLStreamException
    {
        final String dtdStr =
            "<!ELEMENT root (#PCDATA | branch)*>\n"
            +"<!ELEMENT branch (branch)*>\n"
        ;

        for (int i = 0; i < 2; ++i) {
            boolean repairing = (i > 0);
            StringWriter strw = new StringWriter();
            XMLStreamWriter2 sw = getDTDValidatingWriter(strw, repairing, dtdStr);
            sw.writeStartElement("root");
            // Should be fine now
            sw.writeCharacters("Text that should be ok");
            sw.writeStartElement("branch");
            // Also, all-whitespace is ok in non-mixed too
            sw.writeCharacters("\t \t   \r   \n");
            sw.writeEndElement();
            sw.writeEndElement();
            sw.writeEndDocument();
        }
    }

    public void testInvalidMixedContent()
        throws XMLStreamException
    {
        final String dtdStr =
            "<!ELEMENT root (branch)>\n"
            +"<!ELEMENT branch ANY>\n"
        ;

        for (int i = 0; i < 2; ++i) {
            boolean repairing = (i > 0);
            StringWriter strw = new StringWriter();
            XMLStreamWriter sw = getDTDValidatingWriter(strw, repairing, dtdStr);
            
            sw.writeStartElement("root");
            // Should get validation exception here:
            try {
                sw.writeCharacters("Illegal text!");
                fail("Expected a validation exception for non-whitespace text output on non-mixed element content");
            } catch (XMLValidationException vex) {
                // expected...
            }
        }
    }
}
