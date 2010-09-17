package wstxtest.msv;

import javax.xml.stream.*;

import org.codehaus.stax2.*;
import org.codehaus.stax2.validation.*;

import wstxtest.vstream.BaseValidationTest;

/**
 * Test for XML Schema value constraints (default, required) for
 * elements and attributes.
 */
public class TestW3CDefaultValues
    extends BaseValidationTest
{
    final static String SCHEMA_WITH_DEFAULTS = "";

    final static String SCHEMA_WITH_REQUIRED = "";
    
    public void testAttributeDefault() throws Exception
    {
        XMLValidationSchema schema = parseW3CSchema(SCHEMA_WITH_DEFAULTS);
        XMLStreamReader2 sr = getReader("<price>129</price>");
        sr.validateAgainst(schema);
        streamThrough(sr);
    }

    /*
    ///////////////////////////////////////////////////////////////////////
    // Helper methods
    ///////////////////////////////////////////////////////////////////////
    */

    XMLStreamReader2 getReader(String contents) throws XMLStreamException
    {
        XMLInputFactory2 f = getInputFactory();
        setValidating(f, false);
        return constructStreamReader(f, contents);
    }
    
}
