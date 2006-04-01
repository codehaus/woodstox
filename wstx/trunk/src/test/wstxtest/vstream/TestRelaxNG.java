package wstxtest.stream;

import java.io.*;

import javax.xml.stream.*;

import com.ctc.wstx.stax.WstxInputFactory;

import org.codehaus.stax2.*;
import org.codehaus.stax2.validation.*;

import wstxtest.cfg.*;

/**
 * This is a simple base-line "smoke test" that checks that RelaxNG
 * validation works at least minimally.
 */
public class TestRelaxNG
    extends BaseStreamTest
{
    final static String SIMPLE_RNG_SCHEMA =
        "<element name='dict' xmlns='http://relaxng.org/ns/structure/1.0'>\n"
        +" <oneOrMore>\n"
        +"  <element name='term'>\n"
        +"   <attribute name='type' />\n"
        +"   <optional>\n"
        +"     <attribute name='extra' />\n"
        +"   </optional>\n"
        +"   <element name='word'><text />\n"
        +"   </element>\n"
        +"   <element name='description'> <text />\n"
        +"   </element>\n"
        +"  </element>\n"
        +" </oneOrMore>\n"
        +"</element>"
        ;

    /**
     * Similar schema, but one that uses namespaces
     */
    final static String SIMPLE_RNG_NS_SCHEMA =
        "<element xmlns='http://relaxng.org/ns/structure/1.0' name='root'>\n"
        +" <zeroOrMore>\n"
        +"  <element name='ns:leaf' xmlns:ns='http://test'>\n"
        +"   <optional>\n"
        +"     <attribute name='attr1' />\n"
        +"   </optional>\n"
        +"   <optional>\n"
        +"     <attribute name='ns:attr2' />\n"
        +"   </optional>\n"
        +"   <text />\n"
        +"  </element>\n"
        +" </zeroOrMore>\n"
        +"</element>"
        ;

    /**
     * Test validation against a simple document valid according
     * to a simple RNG schema.
     */
    public void testSimpleNonNs()
        throws XMLStreamException
    {
        String XML =
            "<?xml version='1.0'?>"
            +"<dict>\n"
            +" <term type='name'>\n"
            +"  <word>foobar</word>\n"
            +"  <description>Foo Bar</description>\n"
            +" </term>"
            +" <term type='word' extra='123'>\n"
            +"  <word>fuzzy</word>\n"
            +"  <description>adjective</description>\n"
            +" </term>"
            +"</dict>"
            ;

        XMLValidationSchema schema = parseRngSchema(SIMPLE_RNG_SCHEMA);
        XMLStreamReader2 sr = getReader(XML);
        sr.validateAgainst(schema);

        try {
            assertTokenType(START_ELEMENT, sr.next());
            assertEquals("dict", sr.getLocalName());
            
            while (sr.hasNext()) {
                int type = sr.next();
            }
        } catch (XMLValidationException vex) {
            fail("Did not expect validation exception, got: "+vex);
        }

        assertTokenType(END_DOCUMENT, sr.getEventType());
    }

    /**
     * This unit test checks for couple of simple validity problems
     * against the simple rng schema. It does not use namespaces
     * (a separate test is needed for ns handling).
     */
    public void testInvalidNonNs()
        throws XMLStreamException
    {
        XMLValidationSchema schema = parseRngSchema(SIMPLE_RNG_SCHEMA);

        // First, wrong root element:
        String XML = "<term type='x'>\n"
            +"  <word>foobar</word>\n"
            +"  <description>Foo Bar</description>\n"
            +"</term>";
        verifyRngFailure(XML, schema, "wrong root element");

        // Then, wrong child ordering:
        XML = "<dict>\n"
            +"<term type='x'>\n"
            +"  <description>Foo Bar</description>\n"
            +"  <word>foobar</word>\n"
            +"</term></dict>";
        verifyRngFailure(XML, schema, "illegal child element ordering");

        // Then, missing children:
        XML = "<dict>\n"
            +"<term type='x'>\n"
            +"</term></dict>";
        verifyRngFailure(XML, schema, "missing children");
        XML = "<dict>\n"
            +"<term type='x'>\n"
            +"<word>word</word>"
            +"</term></dict>";
        verifyRngFailure(XML, schema, "incomplete children");

        // Then illegal text in non-mixed element
        XML = "<dict>\n"
            +"<term type='x'>No text allowed here"
            +"  <word>foobar</word>\n"
            +"  <description>Foo Bar</description>\n"
            +"</term></dict>";
        verifyRngFailure(XML, schema, "invalid non-whitespace text");

        // missing attribute
        XML = "<dict>\n"
            +"<term>"
            +"  <word>foobar</word>\n"
            +"  <description>Foo Bar</description>\n"
            +"</term></dict>";
        // Then undeclared attributes
        XML = "<dict>\n"
            +"<term attr='value' type='x'>"
            +"  <word>foobar</word>\n"
            +"  <description>Foo Bar</description>\n"
            +"</term></dict>";
        verifyRngFailure(XML, schema, "undeclared attribute");
        XML = "<dict>\n"
            +"<term type='x'>"
            +"  <word type='noun'>foobar</word>\n"
            +"  <description>Foo Bar</description>\n"
            +"</term></dict>";
        verifyRngFailure(XML, schema, "undeclared attribute");
    }

    public void testSimpleNs()
        throws XMLStreamException
    {
        String XML = "<root>\n"
            +" <myns:leaf xmlns:myns='http://test' attr1='123' />\n"
            +" <ns2:leaf xmlns:ns2='http://test' ns2:attr2='123' />\n"
            +"</root>"
            ;

        XMLValidationSchema schema = parseRngSchema(SIMPLE_RNG_NS_SCHEMA);
        XMLStreamReader2 sr = getReader(XML);
        sr.validateAgainst(schema);

        try {
            assertTokenType(START_ELEMENT, sr.next());
            assertEquals("root", sr.getLocalName());
            
            while (sr.hasNext()) {
                int type = sr.next();
            }
        } catch (XMLValidationException vex) {
            fail("Did not expect validation exception, got: "+vex);
        }

        assertTokenType(END_DOCUMENT, sr.getEventType());
    }

    /**
     * Unit test checks that the namespace matching works as
     * expected.
     */
    public void testInvalidNs()
        throws XMLStreamException
    {
        XMLValidationSchema schema = parseRngSchema(SIMPLE_RNG_NS_SCHEMA);

        // First, wrong root element:
        String XML = "<root xmlns='http://test'>\n"
            +"<leaf />\n"
            +"</root>";
        verifyRngFailure(XML, schema, "wrong root element");

        // Wrong child namespace
        XML = "<root>\n"
            +"<leaf xmlns='http://other' />\n"
            +"</root>";
        verifyRngFailure(XML, schema, "wrong child element namespace");

        // Wrong attribute namespace
        XML = "<root>\n"
            +"<ns:leaf xmlns:ns='http://test' ns:attr='123' />\n"
            +"</root>";
        verifyRngFailure(XML, schema, "wrong attribute namespace");
    }

    /*
    //////////////////////////////////////////////////////////////
    // Helper methods
    //////////////////////////////////////////////////////////////
     */

    XMLValidationSchema parseRngSchema(String contents)
        throws XMLStreamException
    {
        XMLValidationSchemaFactory schF = XMLValidationSchemaFactory.newInstance(XMLValidationSchema.SCHEMA_ID_RELAXNG);
        return schF.createSchema(new StringReader(contents));
    }

    XMLStreamReader2 getReader(String contents)
        throws XMLStreamException
    {
        XMLInputFactory2 f = getInputFactory();
        setValidating(f, false);
        return constructStreamReader(f, contents);
    }

    void verifyRngFailure(String xml, XMLValidationSchema schema, String failMsg)
        throws XMLStreamException
    {
        XMLStreamReader2 sr = getReader(xml);
        sr.validateAgainst(schema);
        try {
            while (sr.hasNext()) {
                int type = sr.next();
            }
            fail("Expected validity exception for "+failMsg);
        } catch (XMLValidationException vex) {
            // Uncomment for trouble shooting
            //System.err.println("DEBUG: expected '"+failMsg+"', got '"+vex.getMessage()+"'");
            // should get this specific type; not basic stream exception
        } catch (XMLStreamException sex) {
            fail("Expected XMLValidationException for "+failMsg+", got: "+sex);
        }
    }
}
