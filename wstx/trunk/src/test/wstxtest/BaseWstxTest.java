package wstxtest;

import java.io.*;
import java.util.HashMap;

import junit.framework.TestCase;

import javax.xml.stream.*;

import org.codehaus.stax2.*;
import org.codehaus.stax2.evt.*;

import com.ctc.wstx.api.WstxInputProperties;
import com.ctc.wstx.api.WstxOutputProperties;
import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;

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

    /**
     * Switch that can be turned on to verify to display ALL exact Exceptions
     * thrown when Exceptions are expected. This is sometimes necessary
     * when debugging, since it's impossible to automatically verify
     * that Exception is exactly the right one, since there is no
     * strict Exception type hierarchy for StAX problems.
     *<p>
     * Note: Not made 'final static', so that compiler won't inline
     * it. Makes possible to do partial re-compilations.
     * Note: Since it's only used as the default value, sub-classes
     *  can separately turn it off as necessary
     */
    //protected static boolean DEF_PRINT_EXP_EXCEPTION = true;
    protected static boolean DEF_PRINT_EXP_EXCEPTION = false;

    protected boolean PRINT_EXP_EXCEPTION = DEF_PRINT_EXP_EXCEPTION;

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
    XMLOutputFactory2 mOutputFactory = null;
    XMLEventFactory2 mEventFactory = null;

    /*
    ///////////////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////////////
     */

    protected BaseWstxTest() { super(); }

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

    protected XMLEventFactory2 getEventFactory()
    {
        if (mEventFactory == null) {
            System.setProperty("javax.xml.stream.XMLEventFactory",
                               "com.ctc.wstx.stax.WstxEventFactory");
            mEventFactory = (XMLEventFactory2) XMLEventFactory.newInstance();
        }
        return mEventFactory;
    }

    protected WstxInputFactory getWstxInputFactory() {
        return (WstxInputFactory) getInputFactory();
    }

    protected static XMLInputFactory2 getNewInputFactory()
    {
        return (XMLInputFactory2) XMLInputFactory.newInstance();
    }

    protected XMLOutputFactory2 getOutputFactory()
    {
        if (mOutputFactory == null) {
            System.setProperty("javax.xml.stream.XMLOutputFactory",
                               "com.ctc.wstx.stax.WstxOutputFactory");
            mOutputFactory = getNewOutputFactory();
        }
        return mOutputFactory;
    }

    protected WstxOutputFactory getWstxOutputFactory() {
        return (WstxOutputFactory) getOutputFactory();
    }

    protected static XMLOutputFactory2 getNewOutputFactory()
    {
        return (XMLOutputFactory2) XMLOutputFactory.newInstance();
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

    protected static XMLEventReader2 constructEventReader(XMLInputFactory f, String content)
        throws XMLStreamException
    {
        return (XMLEventReader2) f.createXMLEventReader(new StringReader(content));
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

    protected static void setReplaceEntities(XMLInputFactory f, boolean state)
        throws XMLStreamException
    {
        f.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES,
                      state ? Boolean.TRUE : Boolean.FALSE);
    }

    protected static void setSupportExternalEntities(XMLInputFactory f, boolean state)
        throws XMLStreamException
    {
        f.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES,
                      state ? Boolean.TRUE : Boolean.FALSE);
    }

    protected static void setLazyParsing(XMLInputFactory f, boolean state)
        throws XMLStreamException
    {
        f.setProperty(WstxInputProperties.P_LAZY_PARSING,
                      state ? Boolean.TRUE : Boolean.FALSE);
    }

    protected static void setMinTextSegment(XMLInputFactory f, int len)
        throws XMLStreamException
    {
        f.setProperty(WstxInputProperties.P_MIN_TEXT_SEGMENT, new Integer(len));
    }

    /*
    //////////////////////////////////////////////////
    // Configuring output factory
    //////////////////////////////////////////////////
     */

    protected static void setRepairing(XMLOutputFactory f, boolean state)
    {
        f.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES,
                      Boolean.valueOf(state));
    }

    protected static void setNamespaceAware(XMLOutputFactory f, boolean state)
    {
        f.setProperty(XMLOutputFactory2.P_NAMESPACE_AWARE, Boolean.valueOf(state));
    }

    protected static void setValidateContent(XMLOutputFactory f, boolean state)
    {
        f.setProperty(WstxOutputProperties.P_OUTPUT_VALIDATE_CONTENT,
                      Boolean.valueOf(state));
    }

    protected static void setValidateNames(XMLOutputFactory f, boolean state)
    {
        f.setProperty(WstxOutputProperties.P_OUTPUT_VALIDATE_NAMES,
                      Boolean.valueOf(state));
    }

    /*
    //////////////////////////////////////////////////
    // Higher-level test methods
    //////////////////////////////////////////////////
     */

    /**
     * Method that will iterate through contents of an XML document
     * using specified stream reader; will also access some of data
     * to make sure reader reads most of lazy-loadable data.
     * Method is usually called to try to get an exception for invalid
     * content.
     *
     * @return Dummy value calculated on contents; used to make sure
     *   no dead code is eliminated
     */
    protected int streamThrough(XMLStreamReader sr)
        throws XMLStreamException
    {
        int result = 0;

        while (sr.hasNext()) {
            int type = sr.next();
            result += type;
            if (sr.hasText()) {
                /* will also do basic verification for text content, to 
                 * see that all text accessor methods return same content
                 */
                result += getAndVerifyText(sr).hashCode();
            }
            if (sr.hasName()) {
                result += sr.getName().hashCode();
            }
        }

        return result;
    }

    protected int streamThroughFailing(XMLInputFactory f, String contents,
                                       String msg)
        throws XMLStreamException
    {
        int result = 0;
        try {
            XMLStreamReader sr = constructStreamReader(f, contents);
            result = streamThrough(sr);
        } catch (XMLStreamException ex) { // good
            if (PRINT_EXP_EXCEPTION) {
                System.out.println("Expected failure: '"+ex.getMessage()+"' "
                                   +"(matching message: '"+msg+"')");
            }
            return 0;
        } catch (RuntimeException ex2) { // ok
            if (PRINT_EXP_EXCEPTION) {
                System.out.println("Expected failure: '"+ex2.getMessage()+"' "
                                   +"(matching message: '"+msg+"')");
            }
            return 0;
        } catch (Throwable t) { // not so good
            fail("Expected an XMLStreamException or RuntimeException for "+msg
                 +", got: "+t);
        }

        fail("Expected an exception for "+msg);
        return result; // never gets here
    }

    protected int streamThroughFailing(XMLStreamReader sr, String msg)
        throws XMLStreamException
    {
        int result = 0;
        try {
            result = streamThrough(sr);
        } catch (XMLStreamException ex) { // good
            if (PRINT_EXP_EXCEPTION) {
                System.out.println("Expected failure: '"+ex.getMessage()+"' "
                                   +"(matching message: '"+msg+"')");
            }
            return 0;
        } catch (RuntimeException ex2) { // ok
            if (PRINT_EXP_EXCEPTION) {
                System.out.println("Expected failure: '"+ex2.getMessage()+"' "
                                   +"(matching message: '"+msg+"')");
            }
            return 0;
        } catch (Throwable t) { // not so good
            fail("Expected an XMLStreamException or RuntimeException for "+msg
                 +", got: "+t);
        }

        fail("Expected an exception for "+msg);
        return result; // never gets here
    }

    /*
    //////////////////////////////////////////////////
    // Assertions
    //////////////////////////////////////////////////
     */

    protected static String tokenTypeDesc(int tt)
    {
	String desc = (String) mTokenTypes.get(new Integer(tt));
	return (desc == null) ? ("["+tt+"]") : desc;
    }

    protected static void assertTokenType(int expType, int actType)
    {
        if (expType != actType) {
            String expStr = tokenTypeDesc(expType);
            String actStr = tokenTypeDesc(actType);

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

    public static String printable(char ch)
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

    public static String printableWithSpaces(char ch)
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

    public static String printable(String str)
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

    public static String printableWithSpaces(String str)
    {
        if (str == null || str.length() == 0) {
            return str;
        }

        int len = str.length();
        StringBuffer sb = new StringBuffer(len + 64);
        for (int i = 0; i < len; ++i) {
            char c = str.charAt(i);
            String res = printableWithSpaces(c);
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
