package stax2.vwstream;

import java.io.*;
import javax.xml.stream.*;

import org.codehaus.stax2.*;
import org.codehaus.stax2.validation.*;

import stax2.BaseStax2Test;

import com.ctc.wstx.sw.BaseStreamWriter;

public class BaseOutputTest
    extends BaseStax2Test
{
    public BaseOutputTest() { super(); }

    public BaseOutputTest(String name) { super(name); }

    public XMLStreamWriter getDTDValidatingWriter(Writer w, boolean nsAware, String dtdSrc)
        throws XMLStreamException
    {
        XMLOutputFactory2 outf = getOutputFactory();
        XMLStreamWriter strw = outf.createXMLStreamWriter(w);
        
        XMLValidatorFactory vd = XMLValidatorFactory.newInstance(XMLValidatorFactory.SCHEMA_ID_DTD);

        XMLValidationSchema schema = vd.createSchema(new StringReader(dtdSrc));

        ((BaseStreamWriter) strw).setValidator(schema);
        return strw;
    }
}
