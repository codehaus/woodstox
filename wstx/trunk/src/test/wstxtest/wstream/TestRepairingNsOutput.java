package wstxtest.wstream;

import java.io.*;

import javax.xml.stream.*;

public class TestRepairingNsOutput
    extends BaseWriterTest
{

    /*
    ////////////////////////////////////////////////////
    // Main test methods
    ////////////////////////////////////////////////////
     */

    public void testNoDummyDefaultNs()
        throws XMLStreamException
    {
        XMLOutputFactory f = getFactory();
        StringWriter strw = new StringWriter();
        XMLStreamWriter sw = f.createXMLStreamWriter(strw);

        sw.writeStartDocument();
        sw.writeStartElement("", "root", "");
        sw.writeAttribute("attr", "value");
        sw.writeAttribute("", "", "attr2", "value2");
        sw.writeStartElement("", "leaf", "");
        sw.writeAttribute("", "", "foop", "value2");
        sw.writeCharacters("Sub-text\n");
        sw.writeEndElement();
        sw.writeEndDocument();
        sw.close();

        String result = strw.toString();

        if (result.indexOf("xmlns=\"\"") > 0) {
            fail("Did not expect unnecessary default NS declarations, but found some in result: ["+result+"]");
        }
     }

    /*
    ////////////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////////////
     */

    private XMLOutputFactory getFactory()
        throws XMLStreamException
    {
        XMLOutputFactory f = getOutputFactory();
        setNamespaceAware(f, true);
        setRepairing(f, true);
        return f;
    }

}

