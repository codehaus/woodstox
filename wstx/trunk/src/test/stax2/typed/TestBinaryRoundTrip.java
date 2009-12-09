package stax2.typed;

import java.io.*;
import java.util.Arrays;

import javax.xml.namespace.QName;
import javax.xml.stream.*;

import org.codehaus.stax2.*;
import org.codehaus.stax2.typed.*;

import stax2.BaseStax2Test;

public class TestBinaryRoundTrip
    extends BaseStax2Test
{
    public void testWstx224() throws Exception
    {
        Base64Variant bv = Base64Variants.MIME;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        XMLStreamWriter2 writer = getTypedWriter(bos);
        byte[] data = new byte[2990];
        // contents shouldn't matter a lot, but:
        Arrays.fill(data, (byte) 33);

        writer.writeStartDocument();
        writer.writeStartElement("doc");
        writer.writeStartElement("data");
        writer.writeBinary(bv, data, 0, data.length);
	
        writer.writeEndElement();
        writer.writeEndElement();
	
        writer.writeEndDocument();
        writer.close();

        byte[] xml = bos.toByteArray();
        XMLStreamReader2 reader = getReader(xml);
        byte[] result = null;
        while (reader.hasNext()) {
            if (reader.next() == XMLStreamConstants.START_ELEMENT && "data".equals(reader.getLocalName())) {
                result = reader.getElementAsBinary(bv);
                break;
            }
        }
        assertNotNull(result);
        assertEquals(data.length, result.length);
        for (int i = 0; i < data.length; ++i) {
            if (data[i] != result[i]) {
                fail("Data differs at offset #"+i+"; expected "+data[i]+", got "+result[i]);
            }
        }
    }

    private XMLStreamReader2 getReader(byte[] data)
        throws XMLStreamException
    {
        XMLInputFactory f = getInputFactory();
        setCoalescing(f, true);
        setNamespaceAware(f, true);
        return (XMLStreamReader2) f.createXMLStreamReader(new ByteArrayInputStream(data));
    }

    protected XMLStreamWriter2 getTypedWriter(ByteArrayOutputStream out)
        throws XMLStreamException
    {
        XMLOutputFactory outf = getOutputFactory();
        return (XMLStreamWriter2) outf.createXMLStreamWriter(out, "UTF-8");
    }
}

