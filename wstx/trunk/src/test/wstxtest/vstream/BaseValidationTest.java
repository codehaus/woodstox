package wstxtest.vstream;

import java.io.StringReader;
import java.net.URL;

import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.validation.*;

import wstxtest.stream.BaseStreamTest;

abstract class BaseValidationTest
    extends BaseStreamTest
{
    XMLValidationSchema parseSchema(String contents, String schemaType)
        throws XMLStreamException
    {
        XMLValidationSchemaFactory schF = XMLValidationSchemaFactory.newInstance(schemaType);
        return schF.createSchema(new StringReader(contents));
    }

    XMLValidationSchema parseSchema(URL ref, String schemaType)
        throws XMLStreamException
    {
        XMLValidationSchemaFactory schF = XMLValidationSchemaFactory.newInstance(schemaType);
        return schF.createSchema(ref);
    }

    XMLValidationSchema parseRngSchema(String contents)
        throws XMLStreamException
    {
        return parseSchema(contents, XMLValidationSchema.SCHEMA_ID_RELAXNG);
    }

    XMLValidationSchema parseDTDSchema(String contents)
        throws XMLStreamException
    {
        return parseSchema(contents, XMLValidationSchema.SCHEMA_ID_DTD);
    }

    XMLValidationSchema parseW3CSchema(String contents)
        throws XMLStreamException
    {
        return parseSchema(contents, XMLValidationSchema.SCHEMA_ID_W3C_SCHEMA);
    }
}
