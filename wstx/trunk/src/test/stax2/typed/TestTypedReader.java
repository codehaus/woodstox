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
