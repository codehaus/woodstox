package wstxtest;

import java.io.*;
import java.util.HashMap;

import junit.framework.TestCase;

import javax.xml.stream.*;

import org.codehaus.stax2.*;

import com.ctc.wstx.stax.WstxInputFactory;

public class BaseWstxTest
    extends TestCase
    implements XMLStreamConstants
{
    final static HashMap mTokenTypes = new HashMap();
    static {
        mTokenTypes.put(new Integer(START_ELEMENT), "START_ELEMENT");
        mTokenTypes.put(new Integer(END_ELEMENT), "END_ELEMENT");
        mTokenTypes.put(new Integer(START_DOCUMENT), "START_DOCUMENT");
        mTokenTypes.put(new Integer(END_DOCUMENT), "END_DOCUMENT");
        mTokenTypes.put(new Integer(CHARACTERS), "CHARACTERS");
        mTokenTypes.put(new Integer(CDATA), "CDATA");
        mTokenTypes.put(new Integer(COMMENT), "COMMENT");
        mTokenTypes.put(new Integer(PROCESSING_INSTRUCTION), "PROCESSING_INSTRUCTION");
        mTokenTypes.put(new Integer(DTD), "DTD");
        mTokenTypes.put(new Integer(SPACE), "SPACE");
        mTokenTypes.put(new Integer(ENTITY_REFERENCE), "ENTITY_REFERENCE");
    }

    /*
    ///////////////////////////////////////////////////
    // Consts for expected values
    ///////////////////////////////////////////////////
     */

    /*
    ///////////////////////////////////////////////////
    // Lazy-loaded thingies
    ///////////////////////////////////////////////////
     */

    XMLInputFactory2 mInputFactory = null;

    /*
    ///////////////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////////////
     */

    protected BaseWstxTest(String name) {
        super(name);
    }

    /*
    //////////////////////////////////////////////////
    // Factory methods
    //////////////////////////////////////////////////
     */
    
    protected XMLInputFactory2 getInputFactory()
    {
        if (mInputFactory == null) {
            /* 29-Nov-2004, TSa: Better ensure we get the right
             *   implementation...
             */
            System.setProperty("javax.xml.stream.XMLInputFactory",
                               "com.ctc.wstx.stax.WstxInputFactory");
            mInputFactory = getNewInputFactory();
        }
        return mInputFactory;
    }

    protected WstxInputFactory getWstxInputFactory() {
        return (WstxInputFactory) getInputFactory();
    }

    protected static XMLInputFactory2 getNewInputFactory()
    {
        return (XMLInputFactory2) XMLInputFactory.newInstance();
    }

    protected static XMLStreamReader2 constructStreamReader(XMLInputFactory f, String content)
        throws XMLStreamException
    {
        return (XMLStreamReader2) f.createXMLStreamReader(new StringReader(content));
    }

    protected static XMLStreamReader2 constructStreamReaderForFile(XMLInputFactory f, String filename)
        throws IOException, XMLStreamException
    {
        File inf = new File(filename);
        XMLStreamReader sr = f.createXMLStreamReader(inf.toURL().toString(),
                                                     new FileReader(inf));
        assertEquals(sr.getEventType(), START_DOCUMENT);
        return (XMLStreamReader2) sr;
    }

    /*
    //////////////////////////////////////////////////
    // Configuring input factory
    //////////////////////////////////////////////////
     */

    protected static void setNamespaceAware(XMLInputFactory f, boolean state)
        throws XMLStreamException
    {
        // will always succeed for woodstox factories...
        f.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.valueOf(state));
    }

    protected static void setCoalescing(XMLInputFactory f, boolean state)
        throws XMLStreamException
    {
        f.setProperty(XMLInputFactory.IS_COALESCING, Boolean.valueOf(state));
    }

    protected static void setValidating(XMLInputFactory f, boolean state)
        throws XMLStreamException
    {
        f.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.valueOf(state));
    }

    protected static void setSupportDTD(XMLInputFactory f, boolean state)
        throws XMLStreamException
    {
        f.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.valueOf(state));
    }

    /*
    //////////////////////////////////////////////////
    // Assertions
    //////////////////////////////////////////////////
     */

    protected static void assertTokenType(int expType, int actType)
    {
        if (expType != actType) {
            String expStr = (String) mTokenTypes.get(new Integer(expType));
            String actStr = (String) mTokenTypes.get(new Integer(actType));

            if (expStr == null) {
                expStr = ""+expType;
            }
            if (actStr == null) {
                actStr = ""+actType;
            }
            fail("Expected token "+expStr+"; got "+actStr+".");
        }
    }

    protected static void failStrings(String msg, String exp, String act)
    {
        // !!! TODO: Indicate position where Strings differ
        fail(msg+": expected "+quotedPrintable(exp)+", got "
             +quotedPrintable(act));
    }

    /**
     * Method that not only gets currently available text from the 
     * reader, but also checks that its consistenly accessible using
     * different (basic) StAX methods.
     */
    protected static String getAndVerifyText(XMLStreamReader sr)
        throws XMLStreamException
    {
        int expLen = sr.getTextLength();
        /* Hmmh. It's only ok to return empty text for DTD event... well,
         * maybe also for CDATA, since empty CDATA blocks are legal?
         */
        /* !!! 01-Sep-2004, TSa:
         *  note: theoretically, in coalescing mode, it could be possible
         *  to have empty CDATA section(s) get converted to CHARACTERS,
         *  which would be empty... may need to enhance this to check that
         *  mode is not coalescing? Or something
         */
        if (sr.getEventType() == CHARACTERS) {
            assertTrue("Stream reader should never return empty Strings.",  (expLen > 0));
        }
        String text = sr.getText();
        assertNotNull("getText() should never return null.", text);
        assertEquals("Expected text length of "+expLen+", got "+text.length(),
		     expLen, text.length());
        char[] textChars = sr.getTextCharacters();
        int start = sr.getTextStart();
        String text2 = new String(textChars, start, expLen);
        assertEquals(text, text2);
        return text;
    }

    /*
    //////////////////////////////////////////////////
    // Debug/output helpers
    //////////////////////////////////////////////////
     */

    protected static String printable(char ch)
    {
        if (ch == '\n') {
            return "\\n";
        }
        if (ch == '\r') {
            return "\\r";
        }
        if (ch == '\t') {
            return "\\t";
        }
        if (ch == ' ') {
            return "_";
        }
        if (ch > 127 || ch < 32) {
            StringBuffer sb = new StringBuffer(6);
            sb.append("\\u");
            String hex = Integer.toHexString((int)ch);
            for (int i = 0, len = 4 - hex.length(); i < len; i++) {
                sb.append('0');
            }
            sb.append(hex);
            return sb.toString();
        }
        return null;
    }

    protected static String printable(String str)
    {
        if (str == null || str.length() == 0) {
            return str;
        }

        int len = str.length();
        StringBuffer sb = new StringBuffer(len + 64);
        for (int i = 0; i < len; ++i) {
            char c = str.charAt(i);
            String res = printable(c);
            if (res == null) {
                sb.append(c);
            } else {
                sb.append(res);
            }
        }
        return sb.toString();
    }

    protected static String quotedPrintable(String str)
    {
        if (str == null || str.length() == 0) {
            return "[0]''";
        }
        return "[len: "+str.length()+"] '"+printable(str)+"'";
    }
}
