package wstxtest.stream;

import java.io.*;
import java.util.*;
import javax.xml.stream.*;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.codehaus.stax2.*;

/**
 * This unit test suite verifies that the auto-closing feature works
 * as expected (both explicitly, and via Source object being passed).
 */
public class TestAutoClosing
    extends BaseStreamTest
{
    /**
     * This unit test checks the default behaviour; with no auto-close, no
     * automatic closing should occur, nor explicit one unless specific
     * forcing method is used.
     */
    public void testNoAutoClose()
        throws XMLStreamException
    {
        final String XML = "<root>...</root>";

        XMLInputFactory2 f = getFactory(false);
        MyReader input = new MyReader(XML);
        XMLStreamReader2 sr = (XMLStreamReader2) f.createXMLStreamReader(input);
        // shouldn't be closed to begin with...
        assertFalse(input.isClosed());
        assertTokenType(START_ELEMENT, sr.next());
        assertFalse(input.isClosed());

        // nor closed half-way through with basic close()
        sr.close();
        assertFalse(input.isClosed());

        // ok, let's finish it up:
        streamThrough(sr);
        // still not closed
        assertFalse(input.isClosed());

        // except when forced to:
        sr.closeCompletely();
        assertTrue(input.isClosed());

        // ... and should be ok to call it multiple times:
        sr.closeCompletely();
        sr.closeCompletely();
        assertTrue(input.isClosed());
    }

    /**
     * This unit test checks that when auto-closing option is set, the
     * passed in input stream does get properly closed both when EOF
     * is hit, and when we call close() prior to EOF.
     */
    public void testExplicitAutoClose()
        throws XMLStreamException
    {
        final String XML = "<root>...</root>";

        // First, explicit close:
        XMLInputFactory2 f = getFactory(true);
        MyReader input = new MyReader(XML);
        XMLStreamReader2 sr = (XMLStreamReader2) f.createXMLStreamReader(input);
        assertFalse(input.isClosed());
        assertTokenType(START_ELEMENT, sr.next());
        assertFalse(input.isClosed());
        sr.close();
        assertTrue(input.isClosed());
        // also, let's verify we can call more than once:
        sr.close();
        sr.close();
        assertTrue(input.isClosed());

        // Then implicit close:
        input = new MyReader(XML);
        sr = (XMLStreamReader2) f.createXMLStreamReader(input);
        assertFalse(input.isClosed());
        streamThrough(sr);
        assertTrue(input.isClosed());
    }

    /**
     * This unit test checks that even when auto-closing option is NOT set,
     * some types of input sources (namely, ones that calling application
     * can not directly or reliably close: when application passes
     * a File, URL, Source object), stream reader deals it as if auto-closing
     * option was actually set. Because of this,
     * underlying input stream does get properly closed both when EOF
     * is hit, and when we call close() prior to EOF.
     */
    public void testImplicitAutoClose()
        throws XMLStreamException
    {
        final String XML = "<root>...</root>";

        // First, explicit close:
        XMLInputFactory2 f = getFactory(true);
        MySource input = MySource.createFor(XML);
        XMLStreamReader2 sr = (XMLStreamReader2) f.createXMLStreamReader(input);
        assertFalse(input.isClosed());
        assertTokenType(START_ELEMENT, sr.next());
        assertFalse(input.isClosed());
        sr.close();
        assertTrue(input.isClosed());
        // also, let's verify we can call more than once:
        sr.close();
        sr.close();
        assertTrue(input.isClosed());

        // Then implicit close:
        input = MySource.createFor(XML);
        sr = (XMLStreamReader2) f.createXMLStreamReader(input);
        assertFalse(input.isClosed());
        streamThrough(sr);
        assertTrue(input.isClosed());
    }

    /*
    ////////////////////////////////////////
    // Non-test methods
    ////////////////////////////////////////
     */

    XMLInputFactory2 getFactory(boolean autoClose)
    {
        XMLInputFactory2 f = getInputFactory();
        f.setProperty(XMLInputFactory2.P_AUTO_CLOSE_INPUT,
                      Boolean.valueOf(autoClose));
        return f;
    }

    /*
    ////////////////////////////////////////
    // Helper mock classes
    ////////////////////////////////////////
     */

    final static class MyReader
        extends StringReader
    {
        boolean mIsClosed = false;

        public MyReader(String contents) {
            super(contents);
        }

        public void close() {
            mIsClosed = true;
            super.close();
        }

        public boolean isClosed() { return mIsClosed; }
    }

    final static class MySource
        extends StreamSource
    {
        final MyReader mReader;

        private MySource(MyReader reader) {
            super(reader);
            mReader = reader;
        }

        public static MySource createFor(String content) {
            MyReader r = new MyReader(content);
            return new MySource(r);
        }

        public boolean isClosed() {
            return mReader.isClosed();
        }

        public Reader getReader() {
            return mReader;
        }
    }
}
