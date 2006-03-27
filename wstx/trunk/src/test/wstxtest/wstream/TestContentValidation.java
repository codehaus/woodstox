package wstxtest.wstream;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.*;

/**
 * This unit test suite verifies that output-side content validation
 * works as expected, when enabled.
 */
public class TestContentValidation
    extends BaseWriterTest
{
    final String COMMENT_CONTENT_IN = "can not have -- in there";
    final String COMMENT_CONTENT_OUT = "can not have - - in there";

    final String CDATA_CONTENT_IN = "CData looks like this: <![CDATA[text]]>";
    final String CDATA_CONTENT_OUT = "CData looks like this: <![CDATA[text]]>";

    final String PI_CONTENT_IN = "this should end PI: ?> shouldn't it?";
    final String PI_CONTENT_OUT = "this should end PI: ?> shouldn't it?";

    /*
    ////////////////////////////////////////////////////
    // Main test methods
    ////////////////////////////////////////////////////
     */

    public void testCommentChecking()
        throws XMLStreamException
    {
        for (int i = 0; i <= 2; ++i) {
            XMLOutputFactory2 f = getFactory(i, true, false);
            StringWriter strw = new StringWriter();
            XMLStreamWriter2 sw = (XMLStreamWriter2) f.createXMLStreamWriter(strw);
            sw.writeStartDocument();
            sw.writeStartElement("root");
            try {
                sw.writeComment(COMMENT_CONTENT_IN);
                fail("Expected an XMLStreamException for illegal comment content (contains '--') in checking + non-fixing mode");
            } catch (XMLStreamException sex) {
                // good
            } catch (Throwable t) {
                fail("Expected an XMLStreamException for illegal comment content (contains '--') in checking + non-fixing mode; got: "+t);
            }
        }
    }

    public void testCommentFixing()
        throws Exception
    {
        for (int i = 0; i <= 2; ++i) {
            XMLOutputFactory2 f = getFactory(i, true, true);
            StringWriter strw = new StringWriter();
            XMLStreamWriter2 sw = (XMLStreamWriter2) f.createXMLStreamWriter(strw);
            sw.writeStartDocument();
            sw.writeStartElement("root");
            /* now it should be ok, and result in one padded or
             * 2 separate comments...
             */
            sw.writeComment(COMMENT_CONTENT_IN);
            sw.writeEndElement();
            sw.writeEndDocument();
            sw.close();

            String output = strw.toString();

            // so far so good; but let's ensure it also parses:
            XMLStreamReader sr = getReader(output);
            assertTokenType(START_ELEMENT, sr.next());
            assertTokenType(COMMENT, sr.next());
            StringBuffer sb = new StringBuffer();
            sb.append(getAndVerifyText(sr));

            // May get another one too...?
            int type;

            while ((type = sr.next()) == COMMENT) {
                sb.append(getAndVerifyText(sr));
            }

            /* Ok... now, except for additional spaces, we should have
             * about the same content:
             */
            /* For now, since it's wstx-specific, let's just hard-code
             * exactly what we are to get:
             */
            String act = sb.toString();
            if (!COMMENT_CONTENT_OUT.equals(act)) {
                failStrings("Failed to properly quote comment content",
                            COMMENT_CONTENT_OUT, act);
            }
            assertTokenType(END_ELEMENT, type);
        }
    }

    public void testCDataChecking()
        throws Exception
    {
        for (int i = 0; i <= 2; ++i) {
            XMLOutputFactory2 f = getFactory(i, true, false);
            StringWriter strw = new StringWriter();
            XMLStreamWriter2 sw = (XMLStreamWriter2) f.createXMLStreamWriter(strw);
            sw.writeStartDocument();
            sw.writeStartElement("root");
            try {
                sw.writeCData(CDATA_CONTENT_IN);
                fail("Expected an XMLStreamException for illegal CDATA content (contains ']]>') in checking + non-fixing mode");
            } catch (XMLStreamException sex) {
                // good
            } catch (Throwable t) {
                fail("Expected an XMLStreamException for illegal CDATA content (contains ']]>') in checking + non-fixing mode; got: "+t);
            }
        }
    }

    public void testCDataFixing()
        throws Exception
    {
        for (int i = 0; i <= 2; ++i) {
            XMLOutputFactory2 f = getFactory(i, true, true);
            StringWriter strw = new StringWriter();
            XMLStreamWriter2 sw = (XMLStreamWriter2) f.createXMLStreamWriter(strw);
            sw.writeStartDocument();
            sw.writeStartElement("root");
            /* now it should be ok, and result in two separate CDATA
             * segments...
             */
            sw.writeCData(CDATA_CONTENT_IN);
            sw.writeEndElement();
            sw.writeEndDocument();
            sw.close();

            String output = strw.toString();

            // so far so good; but let's ensure it also parses:
            XMLStreamReader sr = getReader(output);
            assertTokenType(START_ELEMENT, sr.next());
            int type = sr.next();

            assertTokenType(CDATA, type);
            StringBuffer sb = new StringBuffer();
            sb.append(getAndVerifyText(sr));

            // Should be getting one or more segments...
            while ((type = sr.next()) == CDATA) {
                sb.append(getAndVerifyText(sr));
            }

            String act = sb.toString();
            if (!CDATA_CONTENT_OUT.equals(act)) {
                failStrings("Failed to properly quote CDATA content",
                            CDATA_CONTENT_OUT, act);
            }
            assertTokenType(END_ELEMENT, type);
        }
    }

    public void testPIChecking()
        throws Exception
    {
        for (int i = 0; i <= 2; ++i) {
            XMLOutputFactory2 f = getFactory(i, true, false);
            StringWriter strw = new StringWriter();
            XMLStreamWriter2 sw = (XMLStreamWriter2) f.createXMLStreamWriter(strw);
            sw.writeStartDocument();
            sw.writeStartElement("root");
            try {
                sw.writeProcessingInstruction("target", PI_CONTENT_IN);
                fail("Expected an XMLStreamException for illegal PI content (contains '?>') in checking + non-fixing mode");
            } catch (XMLStreamException sex) {
                // good
            } catch (Throwable t) {
                fail("Expected an XMLStreamException for illegal PI content (contains '?>') in checking + non-fixing mode; got: "+t);
            }
        }
    }

    // // Note: no way (currently?) to fix PI content; thus, no test:


    /*
    ////////////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////////////
     */

    private XMLOutputFactory2 getFactory(int type, boolean checkAll, boolean fixAll)
        throws XMLStreamException
    {
        XMLOutputFactory2 f = getOutputFactory();
        // type 0 -> non-ns, 1 -> ns, non-repairing, 2 -> ns, repairing
        setNamespaceAware(f, type > 0); 
        setRepairing(f, type > 1); 
        setValidateAll(f, checkAll);
        setFixContent(f, fixAll);
        return f;
    }

    private XMLStreamReader getReader(String content)
        throws XMLStreamException
    {
        XMLInputFactory2 f = getInputFactory();
        setCoalescing(f, false);
        return f.createXMLStreamReader(new StringReader(content));
    }
}

