package stax2.typed;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.*;
import org.codehaus.stax2.typed.*;

import stax2.BaseStax2Test;

/**
 * Set of simple unit tests to verify implementation
 * of {@link TypedXMLStreamWriter}.
 *
 * @author Tatu Saloranta
 */
public class TestTypedWriter
    extends BaseStax2Test
{
    /*
    ////////////////////////////////////////
    // Tests for numeric/enum types
    ////////////////////////////////////////
     */

    public void testSimpleBooleanElem()
        throws Exception
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        XMLStreamWriter2 w = getTypedWriter(bos);
        writeBooleanElem(w, true);
        w.close();
        checkBooleanElem(bos, true);

        w = getTypedWriter(bos);
        writeBooleanElem(w, false);
        w.close();
        checkBooleanElem(bos, false);
    }

    public void testSimpleBooleanAttr()
        throws Exception
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        XMLStreamWriter2 w = getTypedWriter(bos);
        writeBooleanAttr(w, true);
        w.close();
        checkBooleanAttr(bos, true);

        w = getTypedWriter(bos);
        writeBooleanAttr(w, false);
        w.close();
        checkBooleanAttr(bos, false);
    }

    public void testMultipleBooleanAttr()
        throws Exception
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        XMLStreamWriter2 w = getTypedWriter(bos);

        w.writeStartDocument();
        w.writeStartElement("root");

        w.writeBooleanAttribute(null, null, "a1", true);
        w.writeBooleanAttribute(null, null, "xyz", false);
        w.writeBooleanAttribute(null, null, "_attr3", true);

        w.writeEndElement();
        w.writeEndDocument();

        XMLStreamReader2 sr = getRootReader(bos);
        assertEquals(3, sr.getAttributeCount());
        int ix1 = sr.getAttributeIndex("", "a1");
        int ix2 = sr.getAttributeIndex("", "xyz");
        int ix3 = sr.getAttributeIndex("", "_attr3");
        if (ix1 < 0 || ix2 < 0 || ix3 < 0) {
            fail("Couldn't find indexes of attributes: a1="+ix1+", xyz="+ix2+", _attr3="+ix3);
        }
        assertTrue(sr.getAttributeAsBoolean(ix1));
        assertFalse(sr.getAttributeAsBoolean(ix2));
        assertTrue(sr.getAttributeAsBoolean(ix3));

        sr.close();
    }

    /*
    ////////////////////////////////////////
    // Private methods
    ////////////////////////////////////////
     */

    private void checkBooleanElem(ByteArrayOutputStream src, boolean expState)
        throws XMLStreamException
    {
        XMLStreamReader2 sr = getRootReader(src);
        assertEquals(expState, sr.getElementAsBoolean());
        sr.close();
    }

    private void checkBooleanAttr(ByteArrayOutputStream src, boolean expState)
        throws XMLStreamException
    {
        XMLStreamReader2 sr = getRootReader(src);
        assertEquals(expState, sr.getAttributeAsBoolean(0));
        sr.close();
    }

    private void writeBooleanElem(TypedXMLStreamWriter sw, boolean b)
        throws XMLStreamException
    {
        sw.writeStartDocument();
        sw.writeStartElement("root");
        sw.writeBoolean(b);
        sw.writeEndElement();
        sw.writeEndDocument();
    }

    private void writeBooleanAttr(TypedXMLStreamWriter sw, boolean b)
        throws XMLStreamException
    {
        sw.writeStartDocument();
        sw.writeStartElement("root");
        sw.writeBooleanAttribute(null, null, "attr", b);
        sw.writeEndElement();
        sw.writeEndDocument();
    }

    private XMLStreamWriter2 getTypedWriter(ByteArrayOutputStream out)
        throws XMLStreamException
    {
        out.reset();
        XMLOutputFactory outf = getOutputFactory();
        return (XMLStreamWriter2) outf.createXMLStreamWriter(out, "UTF-8");
    }

    // XMLStreamReader2 extends TypedXMLStreamReader
    private XMLStreamReader2 getRootReader(ByteArrayOutputStream src)
        throws XMLStreamException
    {
        XMLStreamReader2 sr = getReader(src.toByteArray());
        assertTokenType(START_DOCUMENT, sr.getEventType());
        while (sr.next() != START_ELEMENT) { }
        assertTokenType(START_ELEMENT, sr.getEventType());
        return sr;
    }

    private XMLStreamReader2 getReader(byte[] data)
        throws XMLStreamException
    {
        XMLInputFactory f = getInputFactory();
        setCoalescing(f, false); // shouldn't really matter
        setNamespaceAware(f, true);
        return (XMLStreamReader2) f.createXMLStreamReader(new ByteArrayInputStream(data));
    }
}
