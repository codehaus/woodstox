package stax2.vwstream;

import java.io.*;
import javax.xml.stream.*;

import org.codehaus.stax2.*;
import org.codehaus.stax2.validation.*;

import stax2.BaseStax2Test;

public class BaseOutputTest
    extends BaseStax2Test
{
    public BaseOutputTest() { super(); }

    public BaseOutputTest(String name) { super(name); }

    public XMLStreamWriter2 getDTDValidatingWriter(Writer w, boolean repairing, String dtdSrc)
        throws XMLStreamException
    {
        XMLOutputFactory2 outf = getOutputFactory();
        XMLStreamWriter2 strw = (XMLStreamWriter2)outf.createXMLStreamWriter(w);
        XMLValidatorFactory vd = XMLValidatorFactory.newInstance(XMLValidatorFactory.SCHEMA_ID_DTD);
        vd.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, new Boolean(repairing));

        XMLValidationSchema schema = vd.createSchema(new StringReader(dtdSrc));

        strw.validateAgainst(schema);
        strw.writeStartDocument();
        return strw;
    }
}
