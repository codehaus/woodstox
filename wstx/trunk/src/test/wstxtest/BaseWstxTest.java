package wstxtest;

import java.io.*;
import java.util.HashMap;

import junit.framework.TestCase;

import javax.xml.stream.*;

import com.ctc.wstx.stax.*;

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

    WstxInputFactory mInputFactory = null;

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
    
    protected WstxInputFactory getInputFactory()
    {
        if (mInputFactory == null) {
            mInputFactory = getNewInputFactory();
        }
        return mInputFactory;
    }

    protected static WstxInputFactory getNewInputFactory()
    {
        return (WstxInputFactory) XMLInputFactory.newInstance();
    }

    protected static XMLStreamReader constructStreamReader(XMLInputFactory f, String content)
        throws XMLStreamException
    {
        return f.createXMLStreamReader(new StringReader(content));
    }

    protected static XMLStreamReader constructStreamReaderForFile(XMLInputFactory f, String filename)
        throws IOException, XMLStreamException
    {
        File inf = new File(filename);
        XMLStreamReader sr = f.createXMLStreamReader(inf.toURL().toString(),
                                                     new FileReader(inf));
        assertEquals(sr.getEventType(), START_DOCUMENT);
        return sr;
    }

    /*
    //////////////////////////////////////////////////
    // Configuring input factory
    //////////////////////////////////////////////////
     */

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
