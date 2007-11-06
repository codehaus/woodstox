package wstxtest.vstream;

import java.io.*;

import javax.xml.stream.*;

import com.ctc.wstx.stax.WstxInputFactory;

import org.codehaus.stax2.*;
import org.codehaus.stax2.validation.*;

import wstxtest.cfg.*;
import wstxtest.stream.BaseStreamTest;

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
        verifyRngFailure(XML, schema, "wrong root element",
                         "is not allowed. Possible tag names are");

        // Then, wrong child ordering:
        XML = "<dict>\n"
            +"<term type='x'>\n"
            +"  <description>Foo Bar</description>\n"
            +"  <word>foobar</word>\n"
            +"</term></dict>";
        verifyRngFailure(XML, schema, "illegal child element ordering",
                         "tag name \"description\" is not allowed. Possible tag names are");

        // Then, missing children:
        XML = "<dict>\n"
            +"<term type='x'>\n"
            +"</term></dict>";
        verifyRngFailure(XML, schema, "missing children",
                         "uncompleted content model. expecting: <word>");

        XML = "<dict>\n"
            +"<term type='x'>\n"
            +"<word>word</word>"
            +"</term></dict>";
        verifyRngFailure(XML, schema, "incomplete children",
                         "uncompleted content model. expecting: <description>");

        // Then illegal text in non-mixed element
        XML = "<dict>\n"
            +"<term type='x'>No text allowed here"
            +"  <word>foobar</word>\n"
            +"  <description>Foo Bar</description>\n"
            +"</term></dict>";
        verifyRngFailure(XML, schema, "invalid non-whitespace text",
                         "Element <term> has non-mixed content specification; can not contain non-white space text");

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
        verifyRngFailure(XML, schema, "undeclared attribute",
                         "unexpected attribute \"attr\"");
        XML = "<dict>\n"
            +"<term type='x'>"
            +"  <word type='noun'>foobar</word>\n"
            +"  <description>Foo Bar</description>\n"
            +"</term></dict>";
        verifyRngFailure(XML, schema, "undeclared attribute",
                         "unexpected attribute \"type\"");
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
        verifyRngFailure(XML, schema, "wrong root element",
                         "namespace URI of tag \"root\" is wrong");

        // Wrong child namespace
        XML = "<root>\n"
            +"<leaf xmlns='http://other' />\n"
            +"</root>";
        verifyRngFailure(XML, schema, "wrong child element namespace",
                         "namespace URI of tag \"leaf\" is wrong.");

        // Wrong attribute namespace
        XML = "<root>\n"
            +"<ns:leaf xmlns:ns='http://test' ns:attr1='123' />\n"
            +"</root>";
        verifyRngFailure(XML, schema, "wrong attribute namespace",
                         "unexpected attribute \"attr1\"");
    }

    /**
     * This unit test verifies that the validation can be stopped
     * half-way through processing, so that sub-trees (for example)
     * can be validated. In this case, we will verify this functionality
     * by trying to validate invalid document up to the point where it
     * is (or may) still be valid, stop validation, and then continue
     * reading. This should not result in an exception.
     */
    public void testSimplePartialNonNs()
        throws XMLStreamException
    {
        String XML =
            "<?xml version='1.0'?>"
            +"<dict>"
            +"<term type='name'><invalid />"
            +"</term>"
            +"</dict>"
            ;

        XMLValidationSchema schema = parseRngSchema(SIMPLE_RNG_SCHEMA);

        XMLStreamReader2 sr = getReader(XML);
        XMLValidator vtor = sr.validateAgainst(schema);

        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("dict", sr.getLocalName());
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("term", sr.getLocalName());

        /* So far so good; but here we'd get an exception... so
         * let's stop validating
         */
        assertSame(vtor, sr.stopValidatingAgainst(schema));
        try {
            // And should be good to go
            assertTokenType(START_ELEMENT, sr.next());
            assertEquals("invalid", sr.getLocalName());
            assertTokenType(END_ELEMENT, sr.next());
            assertEquals("invalid", sr.getLocalName());
            assertTokenType(END_ELEMENT, sr.next());
            assertEquals("term", sr.getLocalName());
            assertTokenType(END_ELEMENT, sr.next());
            assertEquals("dict", sr.getLocalName());
            assertTokenType(END_DOCUMENT, sr.next());
        } catch (XMLValidationException vex) {
            fail("Did not expect validation exception after stopping validation, got: "+vex);
        }
        sr.close();

        // And let's do the same, just using the other stopValidatingAgainst method
        sr = getReader(XML);
        vtor = sr.validateAgainst(schema);

        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("dict", sr.getLocalName());
        assertTokenType(START_ELEMENT, sr.next());
        assertEquals("term", sr.getLocalName());

        assertSame(vtor, sr.stopValidatingAgainst(vtor));
        try {
            // And should be good to go
            assertTokenType(START_ELEMENT, sr.next());
            assertEquals("invalid", sr.getLocalName());
            assertTokenType(END_ELEMENT, sr.next());
            assertEquals("invalid", sr.getLocalName());
            assertTokenType(END_ELEMENT, sr.next());
            assertEquals("term", sr.getLocalName());
            assertTokenType(END_ELEMENT, sr.next());
            assertEquals("dict", sr.getLocalName());
            assertTokenType(END_DOCUMENT, sr.next());
        } catch (XMLValidationException vex) {
            fail("Did not expect validation exception after stopping validation, got: "+vex);
        }
        sr.close();
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

    void verifyRngFailure(String xml, XMLValidationSchema schema, String failMsg, String failPhrase)
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
            String origMsg = vex.getMessage();
            String msg = (origMsg == null) ? "" : origMsg.toLowerCase();
            if (msg.indexOf(failPhrase.toLowerCase()) < 0) {
                fail("Expected validation exception for "+failMsg+", containing phrase '"+failPhrase+"': got '"+origMsg+"'");
            }
            // should get this specific type; not basic stream exception
        } catch (XMLStreamException sex) {
            fail("Expected XMLValidationException for "+failMsg);
        }
    }
}
