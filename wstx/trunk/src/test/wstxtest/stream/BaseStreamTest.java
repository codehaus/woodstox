package wstxtest.stream;

import java.io.*;
import java.util.HashMap;

import javax.xml.stream.*;

import wstxtest.BaseWstxTest;
import wstxtest.cfg.*;

public class BaseStreamTest
    extends BaseWstxTest
{
    protected BaseStreamTest(String name) {
        super(name);
    }

    /*
    //////////////////////////////////////////////////
    // "Special" accessors
    //////////////////////////////////////////////////
     */

    /**
     * Method that not only gets currently available text from the 
     * reader, but also checks that its consistenly accessible using
     * different StAX methods.
     */
    protected static String getAndVerifyText(XMLStreamReader sr)
        throws XMLStreamException
    {
        int expLen = sr.getTextLength();
        // Hmmh. It's only ok to return empty text for DTD event
        if (sr.getEventType() != DTD) {
            assertTrue("Stream reader should never return empty Strings.",  (expLen > 0));
        }
        String text = sr.getText();
        assertNotNull("getText() should never return null.", text);
        assertEquals(expLen, text.length());
        char[] textChars = sr.getTextCharacters();
        int start = sr.getTextStart();
        String text2 = new String(textChars, start, expLen);
        assertEquals(text, text2);
        return text;
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
                result += sr.getText().hashCode();
            }
            if (sr.hasName()) {
                result += sr.getName().hashCode();
            }
        }

        return result;
    }

    protected int streamAndCheck(XMLInputFactory f, InputConfigIterator it,
                                 String input, String expOutput)
        throws XMLStreamException, UnsupportedEncodingException
    {
        int count = 0;

        // Let's loop couple of input methods
        for (int m = 0; m < 3; ++m) {
            XMLStreamReader sr;
            
            /* Contents shouldn't really contain anything
             * outside ISO-Latin; however, detection may
             * be tricky.. so let's just test with UTF-8,
             * for now?
             */

            switch (m) {
            case 0: // simple StringReader:
                sr = constructStreamReader(f, input);
                break;
            case 1: // via InputStream and auto-detection
                {
                    ByteArrayInputStream bin = new ByteArrayInputStream
                        (input.getBytes("UTF-8"));
                    sr = f.createXMLStreamReader(bin);
                }
                break;
            case 2: // explicit UTF-8 stream
                {
                    ByteArrayInputStream bin = new ByteArrayInputStream
                        (input.getBytes("UTF-8"));
                    Reader br = new InputStreamReader(bin, "UTF-8");
                    sr = f.createXMLStreamReader(br);
                }
                break;
            default: throw new Error("Internal error");
            }

            count += streamAndCheck(sr, it, input, expOutput);
        }
        return count;
    }

    protected int streamAndSkip(XMLInputFactory f, InputConfigIterator it,
                                String input)
        throws XMLStreamException, UnsupportedEncodingException
    {
        int count = 0;

        // Let's loop couple of input methods
        for (int m = 0; m < 3; ++m) {
            XMLStreamReader sr;

            switch (m) {
            case 0: // simple StringReader:
                sr = constructStreamReader(f, input);
                break;
            case 1: // via InputStream and auto-detection
                {
                    ByteArrayInputStream bin = new ByteArrayInputStream
                        (input.getBytes("UTF-8"));
                    sr = f.createXMLStreamReader(bin);
                }
                break;
            case 2: // explicit UTF-8 stream
                {
                    ByteArrayInputStream bin = new ByteArrayInputStream
                        (input.getBytes("UTF-8"));
                    Reader br = new InputStreamReader(bin, "UTF-8");
                    sr = f.createXMLStreamReader(br);
                }
                break;
            default: throw new Error("Internal error");
            }

            count += streamAndSkip(sr, it, input);
        }
        return count;
    }

    protected int streamAndCheck(XMLStreamReader sr, InputConfigIterator it,
                                  String input, String expOutput)
        throws XMLStreamException
    {
        int type;

        /* Let's ignore leading white space and DTD; and stop on encountering
         * something else
         */
        do {
            type = sr.next();
        } while ((type == SPACE) || (type == DTD));
        
        StringBuffer act = new StringBuffer(1000);
        int count = 0;

        do {
            count += type;
            if (type == START_ELEMENT || type == END_ELEMENT) {
                act.append('<');
                if (type == END_ELEMENT) {
                    act.append('/');
                }
                String prefix = sr.getPrefix();
                if (prefix != null && prefix.length() > 0) {
                    act.append(prefix);
                    act.append(':');
                }
                act.append(sr.getLocalName());
                act.append('>');
            } else if (type == CHARACTERS || type == SPACE || type == CDATA) {
                // No quoting, doesn't have to result in legal XML
                act.append(sr.getText());
            } else if (type == COMMENT) {
                act.append("<!--");
                act.append(sr.getText());
                act.append("-->");
            } else if (type == PROCESSING_INSTRUCTION) {
                act.append("<!?");
                act.append(sr.getPITarget());
                String data = sr.getPIData();
                if (data != null) {
                    act.append(data.trim());
                }
                act.append("?>");
            } else if (type == ENTITY_REFERENCE) {
                act.append(sr.getText());
            } else {
                fail("Unexpected event type "+type);
            }
        } while ((type = sr.next()) != END_DOCUMENT);
        
        String result = act.toString();
        if (!result.equals(expOutput)) {
            String desc = it.toString();
            int round = it.getIndex();

        // uncomment for debugging:

            /*
        System.err.println("FAIL: round "+round+" ["+desc+"]");
        System.err.println("Input:  '"+input.toString()+"'");
        System.err.println("Exp:    '"+expOutput.toString()+"'");
        System.err.println("Actual: '"+act.toString()+"'");
            */

            fail("Failure with '"+desc+"' (round #"+round+"):\n<br />"
                 +"Input : {"+input+"}\n<br />"
                 +"Output: {"+result+"}\n<br />"
                 +"Exp.  : {"+expOutput+"}\n<br />");
        }

        return count;
    }

    protected int streamAndSkip(XMLStreamReader sr, InputConfigIterator it,
                                String input)
        throws XMLStreamException
    {
        int count = 0;

        while (sr.hasNext()) {
            count += sr.next();
        }
        return count;
    }
}
