package wstxtest.stream;

import java.io.*;
import java.util.Random;

import javax.xml.stream.*;

import wstxtest.cfg.*;

/**
 * Set of unit tests that check how Woodstox handles white space in
 * prolog and/or epilog.
 */
import com.ctc.wstx.stax.WstxInputFactory;

public class TestPrologWS
    extends BaseStreamTest
{
    final static String XML1 = "<?xml version='1.0'?>   <root />\n";
    final static String XML2 = "\n \n<root />   ";

    public TestPrologWS(String name) {
        super(name);
    }

    public void testReportPrologWS()
        throws XMLStreamException
    {
        for (int i = 0; i < 4; ++i) {
            boolean lazy = (i & 1) == 0;
            boolean firstDoc = (i & 2) == 0;
            String content = firstDoc ? XML1 : XML2;
            XMLStreamReader sr = getReader(content, true, (i == 1));

            assertTokenType(START_DOCUMENT, sr.getEventType());

            assertTokenType(SPACE, sr.next());
            if (firstDoc) {
                assertEquals("   ", getAndVerifyText(sr));
            } else {
                assertEquals("\n \n", getAndVerifyText(sr));
            }

            assertTokenType(START_ELEMENT, sr.next());
            assertTokenType(END_ELEMENT, sr.next());

            assertTokenType(SPACE, sr.next());

            if (firstDoc) {
                assertEquals("\n", getAndVerifyText(sr));
            } else {
                assertEquals("   ", getAndVerifyText(sr));
            }

            assertTokenType(END_DOCUMENT, sr.next());
        }
    }

    public void testIgnorePrologWS()
        throws XMLStreamException
    {
        for (int i = 0; i < 4; ++i) {
            boolean lazy = (i & 1) == 0;
            String content = ((i & 2) == 0) ? XML1 : XML2;
            XMLStreamReader sr = getReader(content, false, (i == 1));

            assertTokenType(START_DOCUMENT, sr.getEventType());

            assertTokenType(START_ELEMENT, sr.next());
            assertEquals("root", sr.getLocalName());
            assertTokenType(END_ELEMENT, sr.next());

            assertTokenType(END_DOCUMENT, sr.next());
        }
    }

    /*
    //////////////////////////////////////////////////////
    // Internal methods
    //////////////////////////////////////////////////////
     */

    /**
     * Method called via input config iterator, with all possible
     * configurations
     */
    public void runTest(WstxInputFactory f, InputConfigIterator it)
        throws Exception
    {
        String XML = "<root>"
            +"<!-- first comment -->\n"
            +"  <!-- - - - - -->"
            +"<!-- Longer comment that contains quite a bit of content\n"
            +" so that we can check boundary - conditions too... -->"
            +"<!----><!-- and entities: &amp; &#12;&#x1d; -->\n"
            +"</root>";
        XMLStreamReader sr = constructStreamReader(f, XML);

        streamAndCheck(sr, it, XML, XML);
    }

    /*
    ////////////////////////////////////////
    // Private methods, other
    ////////////////////////////////////////
     */

    private XMLStreamReader getReader(String contents, boolean prologWS,
                                      boolean lazyParsing)
        throws XMLStreamException
    {
        WstxInputFactory f = getInputFactory();
        f.doReportPrologWhitespace(prologWS);
        f.doParseLazily(lazyParsing);
        return constructStreamReader(f, contents);
    }
}

