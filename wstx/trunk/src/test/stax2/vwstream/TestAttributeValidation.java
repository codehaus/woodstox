package stax2.vwstream;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.XMLStreamWriter2;
import org.codehaus.stax2.validation.*;

/**
 * Unit tests for testing handling of attribute value validation, mostly
 * focusing on default value modifiers (#FIXED, #REQUIRED).
 * Validation for specific types are in type-specific additional tests.
 */
public class TestAttributeValidation
    extends BaseOutputTest
{
    final String FIXED_DTD_STR = "<!ELEMENT root EMPTY>\n"
        +"<!ATTLIST root fixAttr CDATA #FIXED 'fixedValue'>\n";
    final String REQUIRED_DTD_STR = "<!ELEMENT root EMPTY>\n"
        +"<!ATTLIST root reqAttr CDATA #REQUIRED>\n";

    public void testValidFixedAttr()
        throws XMLStreamException
    {
        for (int i = 0; i < 3; ++i) {
            boolean nsAware = (i >= 1);
            boolean repairing = (i == 2);
            StringWriter strw = new StringWriter();

            // Ok either without being added:
            XMLStreamWriter2 sw = getDTDValidatingWriter(strw, FIXED_DTD_STR, nsAware, repairing);
            sw.writeStartElement("root");
            sw.writeEndElement();
            sw.writeEndDocument();
            sw.close();

            sw = getDTDValidatingWriter(strw, FIXED_DTD_STR, nsAware, repairing);
            sw.writeEmptyElement("root");
            sw.writeEndDocument();
            sw.close();

            // or by using the exact same value
            sw = getDTDValidatingWriter(strw, FIXED_DTD_STR, nsAware, repairing);
            sw.writeStartElement("root");
            sw.writeAttribute("fixAttr", "fixedValue");
            sw.writeEndElement();
            sw.writeEndDocument();
            sw.close();
        }
    }

    public void testInvalidFixedAttr()
        throws XMLStreamException
    {
        for (int i = 0; i < 3; ++i) {
            boolean nsAware, repairing;
            String modeDesc;

            switch (i) {
            case 0:
                modeDesc = "[non-namespace-aware]";
                nsAware = repairing = false;
                break;
            case 1:
                modeDesc = "[namespace-aware, non-repairing]";
                nsAware = true;
                repairing = false;
                break;
            default:
                modeDesc = "[namespace-aware, repairing]";
                nsAware = repairing = true;
                break;
            }

            // Invalid case, trying to add some other value:

            // non-empty but not same
            StringWriter strw = new StringWriter();
            XMLStreamWriter2 sw = getDTDValidatingWriter(strw, FIXED_DTD_STR, nsAware, repairing);
            sw.writeStartElement("root");
            try {
                sw.writeAttribute("fixAttr", "otherValue");
                fail(modeDesc+" Expected a validation exception when trying to add a #FIXED attribute with 'wrong' value");
            } catch (XMLValidationException vex) {
                // expected...
            }
            // Should not close, since stream is invalid now...

            // empty is not the same as leaving it out:
            strw = new StringWriter();
            sw = getDTDValidatingWriter(strw, FIXED_DTD_STR, nsAware, repairing);
            sw.writeStartElement("root");
            try {
                sw.writeAttribute("fixAttr", "");
                fail(modeDesc+" Expected a validation exception when trying to add a #FIXED attribute with an empty value");
            } catch (XMLValidationException vex) {
                // expected...
            }

            // And finally, same for empty elem in case impl. is different
            strw = new StringWriter();
            sw = getDTDValidatingWriter(strw, FIXED_DTD_STR, nsAware, repairing);
            sw.writeEmptyElement("root");
            try {
                sw.writeAttribute("fixAttr", "foobar");
                fail(modeDesc+" Expected a validation exception when trying to add a #FIXED attribute with an empty value");
            } catch (XMLValidationException vex) {
                // expected...
            }
        }
    }

    public void testValidRequiredAttr()
        throws XMLStreamException
    {
        for (int i = 0; i < 3; ++i) {
            boolean nsAware = (i >= 1);
            boolean repairing = (i == 2);
            StringWriter strw = new StringWriter();

            // Ok if value is added:
            XMLStreamWriter2 sw = getDTDValidatingWriter(strw, REQUIRED_DTD_STR, nsAware, repairing);
            sw.writeStartElement("root");
            sw.writeAttribute("reqAttr", "value");
            sw.writeEndElement();
            sw.writeEndDocument();
            sw.close();

            // ... even if with empty value (for CDATA type, at least)
            sw = getDTDValidatingWriter(strw, REQUIRED_DTD_STR, nsAware, repairing);
            sw.writeStartElement("root");
            sw.writeAttribute("reqAttr", "");
            sw.writeEndElement();
            sw.writeEndDocument();
            sw.close();

            // and ditto for empty element:
            sw = getDTDValidatingWriter(strw, REQUIRED_DTD_STR, nsAware, repairing);
            sw.writeEmptyElement("root");
            sw.writeAttribute("reqAttr", "hii & haa");
            sw.writeEndDocument();
            sw.close();
        }
    }

    public void testInvalidRequiredAttr()
        throws XMLStreamException
    {
        for (int i = 0; i < 3; ++i) {
            boolean nsAware, repairing;
            String modeDesc;

            switch (i) {
            case 0:
                modeDesc = "[non-namespace-aware]";
                nsAware = repairing = false;
                break;
            case 1:
                modeDesc = "[namespace-aware, non-repairing]";
                nsAware = true;
                repairing = false;
                break;
            default:
                modeDesc = "[namespace-aware, repairing]";
                nsAware = repairing = true;
                break;
            }

            // Invalid case: leaving the required attr out:
            StringWriter strw = new StringWriter();
            XMLStreamWriter2 sw = getDTDValidatingWriter(strw, REQUIRED_DTD_STR, nsAware, repairing);
            sw.writeStartElement("root");
            try {
                sw.writeEndElement();
                fail(modeDesc+" Expected a validation exception when omitting a #REQUIRED attribute");
            } catch (XMLValidationException vex) {
                // expected...
            }
            // Should not close, since stream is invalid now...
        }
    }
}

