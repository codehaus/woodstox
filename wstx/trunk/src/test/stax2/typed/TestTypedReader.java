package stax2.typed;

import javax.xml.stream.*;

import org.codehaus.stax2.*;
import org.codehaus.stax2.typed.*;

import stax2.BaseStax2Test;

/**
 * Set of simple unit tests to verify implementation
 * of {@link TypedXMLStreamReader}.
 *
 * @author Tatu Saloranta
 */
public class TestTypedReader
    extends BaseStax2Test
{
    final static long TOO_BIG_FOR_INT = ((long) Integer.MAX_VALUE)+1L;

    final static long TOO_SMALL_FOR_INT = ((long) Integer.MIN_VALUE)-1L;

    /*
    ////////////////////////////////////////
    // Tests for numeric/enum types
    ////////////////////////////////////////
     */

    public void testSimpleBooleanElem()
        throws Exception
    {
        // simple boolean
        checkBooleanElem("<root>true</root>", true);
        // with white space normalization
        checkBooleanElem("<root>\tfalse\n\r</root>", false);
        // Then non-canonical alternatives
        checkBooleanElem("<root>0   \t</root>", false);
        checkBooleanElem("<root>\r1</root>", true);

        // And finally invalid ones
        checkBooleanElemException("<root>yes</root>");
        /* Although "01" would be valid integer equal to "1",
         * it's not a legal boolean nonetheless (as per my reading
         * of W3C Schema specs)
         */
        checkBooleanElemException("<root>01</root>");
    }

    public void testSimpleBooleanAttr()
        throws Exception
    {
        checkBooleanAttr("<root attr='true' />", true);
        checkBooleanAttr("<root attr=\"\tfalse\n\r\" />", false);
        checkBooleanAttr("<root attr='0   \t' />", false);
        checkBooleanAttr("<root attr=\"\r1\" />", true);

        checkBooleanAttrException("<root attr=\"yes\" />");
        checkBooleanAttrException("<root attr='01' />");
    }

    public void testMultipleBooleanAttr()
        throws Exception
    {
        XMLStreamReader2 sr = getRootReader("<root a1='true' b=\"false\" third='0' />");
        assertEquals(3, sr.getAttributeCount());
        int ix1 = sr.getAttributeIndex("", "a1");
        int ix2 = sr.getAttributeIndex("", "b");
        int ix3 = sr.getAttributeIndex("", "third");
        if (ix1 < 0 || ix2 < 0 || ix3 < 0) {
            fail("Couldn't find indexes of attributes: a1="+ix1+", b="+ix2+", third="+ix3);
        }
        assertTrue(sr.getAttributeAsBoolean(ix1));
        assertFalse(sr.getAttributeAsBoolean(ix2));
        assertFalse(sr.getAttributeAsBoolean(ix3));

        sr.close();
    }

    public void testSimpleIntElem()
        throws Exception
    {
        checkIntElem("<root>000000000000000000000000012</root>", 12);


        checkIntElem("<root>0</root>", 0);
        // with white space normalization
        checkIntElem("<root>291\t</root>", 291);
        checkIntElem("<root>   \t1</root>", 1);
        checkIntElem("<root>3 </root>", 3);
        checkIntElem("<root>  -7 </root>", -7);
        // with signs, spacing etc
        checkIntElem("<root>-1234</root>", -1234);
        checkIntElem("<root>+3</root>", 3);
        checkIntElem("<root>-0</root>", 0);
        checkIntElem("<root>-0000</root>", 0);
        checkIntElem("<root>-001</root>", -1);
        checkIntElem("<root>+0</root>", 0);
        checkIntElem("<root>+0  </root>", 0);
        checkIntElem("<root>+00</root>", 0);
        checkIntElem("<root>000000000000000000000000012</root>", 12);
        checkIntElem("<root>-00000000</root>", 0);
        int v = 1200300400;
        checkIntElem("<root>   \r\n+"+v+"</root>", v);
        checkIntElem("<root> "+Integer.MAX_VALUE+"</root>", Integer.MAX_VALUE);
        checkIntElem("<root> "+Integer.MIN_VALUE+"</root>", Integer.MIN_VALUE);

        // And finally invalid ones
        checkIntElemException("<root>12a3</root>");
        checkIntElemException("<root>5000100200</root>"); // overflow
        checkIntElemException("<root>3100200300</root>"); // overflow
        checkIntElemException("<root>-4100200300</root>"); // underflow
        checkIntElemException("<root>"+TOO_BIG_FOR_INT+"</root>"); // overflow as well
        checkIntElemException("<root>"+TOO_SMALL_FOR_INT+"</root>"); // underflow as well
        checkIntElemException("<root>-  </root>");
        checkIntElemException("<root>+</root>");
        checkIntElemException("<root> -</root>");
    }

    public void testSimpleIntAttr()
        throws Exception
    {
        checkIntAttr("<root attr='+0   \t' />", 0);
        checkIntAttr("<root attr='13' />", 13);
        checkIntAttr("<root attr='123' />", 123);
        checkIntAttr("<root attr=\"\t-12\n\r\" />", -12);
        checkIntAttr("<root attr='+0   \t' />", 0);
        checkIntAttr("<root attr=\"\r-00\" />", 0);
        checkIntAttr("<root attr='-000000000000012345' />", -12345);
        checkIntAttr("<root attr='"+Integer.MAX_VALUE+"  ' />", Integer.MAX_VALUE);
        checkIntAttr("<root attr='"+Integer.MIN_VALUE+"'  />", Integer.MIN_VALUE);

        checkIntAttrException("<root attr=\"abc\" />");
        checkIntAttrException("<root attr='1c' />");
        checkIntAttrException("<root attr='\n"+TOO_BIG_FOR_INT+"' />");
        checkIntAttrException("<root attr=\""+TOO_SMALL_FOR_INT+"   \" />");
        checkIntAttrException("<root attr='-' />");
        checkIntAttrException("<root attr='  + ' />");
    }

    public void testMultipleIntAttr()
        throws Exception
    {
        XMLStreamReader2 sr = getRootReader("<root a1='123456789' b=\"-123456789\" third='0' />");
        assertEquals(3, sr.getAttributeCount());
        int ix1 = sr.getAttributeIndex("", "a1");
        int ix2 = sr.getAttributeIndex("", "b");
        int ix3 = sr.getAttributeIndex("", "third");
        if (ix1 < 0 || ix2 < 0 || ix3 < 0) {
            fail("Couldn't find indexes of attributes: a1="+ix1+", b="+ix2+", third="+ix3);
        }
        assertEquals(123456789, sr.getAttributeAsInt(ix1));
        assertEquals(-123456789, sr.getAttributeAsInt(ix2));
        assertEquals(0, sr.getAttributeAsInt(ix3));

        sr.close();
    }

    /*
    ////////////////////////////////////////
    // Private methods, second-level tests
    ////////////////////////////////////////
     */

    private void checkBooleanElem(String doc, boolean expState)
        throws XMLStreamException
    {
        XMLStreamReader2 sr = getRootReader(doc);
        assertEquals(expState, sr.getElementAsBoolean());
        sr.close();
    }

    private void checkBooleanAttr(String doc, boolean expState)
        throws XMLStreamException
    {
        XMLStreamReader2 sr = getRootReader(doc);
        // Assumption is that there's just one attribute...
        boolean actState = sr.getAttributeAsBoolean(0);
        assertEquals(expState, actState);
        sr.close();
    }

    private void checkBooleanElemException(String doc)
        throws XMLStreamException
    {
        XMLStreamReader2 sr = getRootReader(doc);
        try {
            /*boolean b =*/ sr.getElementAsBoolean();
            fail("Expected exception for invalid input ["+doc+"]");
        } catch (TypedXMLStreamException xse) {
            ; // good
        }
    }

    private void checkBooleanAttrException(String doc)
        throws XMLStreamException
    {
        XMLStreamReader2 sr = getRootReader(doc);
        try {
            /*boolean b =*/ sr.getAttributeAsBoolean(0);
            fail("Expected exception for invalid input ["+doc+"]");
        } catch (TypedXMLStreamException xse) {
            ; // good
        }
    }

    private void checkIntElem(String doc, int expState)
        throws XMLStreamException
    {
        XMLStreamReader2 sr = getRootReader(doc);
        assertEquals(expState, sr.getElementAsInt());
        sr.close();
    }

    private void checkIntAttr(String doc, int expState)
        throws XMLStreamException
    {
        XMLStreamReader2 sr = getRootReader(doc);
        // Assumption is that there's just one attribute...
        int actState = sr.getAttributeAsInt(0);
        assertEquals(expState, actState);
        sr.close();
    }

    private void checkIntElemException(String doc)
        throws XMLStreamException
    {
        XMLStreamReader2 sr = getRootReader(doc);
        try {
            /*int b =*/ sr.getElementAsInt();
            fail("Expected exception for invalid input ["+doc+"]");
        } catch (TypedXMLStreamException xse) {
            ; // good
        }
    }

    private void checkIntAttrException(String doc)
        throws XMLStreamException
    {
        XMLStreamReader2 sr = getRootReader(doc);
        try {
            /*int b =*/ sr.getAttributeAsInt(0);
            fail("Expected exception for invalid input ["+doc+"]");
        } catch (TypedXMLStreamException xse) {
            ; // good
        }
    }

    /*
    ////////////////////////////////////////
    // Private methods, reader construction
    ////////////////////////////////////////
     */

    // XMLStreamReader2 extends TypedXMLStreamReader
    private XMLStreamReader2 getRootReader(String str)
        throws XMLStreamException
    {
        XMLStreamReader2 sr = getReader(str);
        assertTokenType(START_DOCUMENT, sr.getEventType());
        while (sr.next() != START_ELEMENT) { }
        assertTokenType(START_ELEMENT, sr.getEventType());
        return sr;
    }

    private XMLStreamReader2 getReader(String contents)
        throws XMLStreamException
    {
        XMLInputFactory f = getInputFactory();
        setCoalescing(f, false); // shouldn't really matter
        setNamespaceAware(f, true);
        return (XMLStreamReader2) constructStreamReader(f, contents);
    }
}
