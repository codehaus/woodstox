package wstxtest.stream;

import java.io.*;
import java.util.Random;

import javax.xml.stream.*;

import wstxtest.cfg.*;

import com.ctc.wstx.stax.WstxInputFactory;

public class TestAttr
    extends BaseStreamTest
{
    final static String XML_11_ATTRS =
        "<tag method='a' activeShell='x' source='y' data='z' "
        +"widget='a' length='1' start='2' styledTextNewValue='t' "
        +"replacedText='' styledTextFunction='f' raw='b' />";

    public TestAttr(String name) {
        super(name);
    }

    /**
     * This test case was added after encountering a specific problem, which
     * only occurs when many attributes were spilled from main hash area....
     * and that's why exact attribute names do matter.
     */
    public void testManyAttrs()
        throws Exception
    {
        // First non-NS
        XMLStreamReader sr = getReader(XML_11_ATTRS, false);
        streamThrough(sr);
        // Then NS
        sr = getReader(XML_11_ATTRS, true);
        streamThrough(sr);
    }

    /*
    //////////////////////////////////////////////////////
    // Internal methods
    //////////////////////////////////////////////////////
     */

    private XMLStreamReader getReader(String contents, boolean nsAware)
        throws XMLStreamException
    {
        WstxInputFactory f = getInputFactory();
        f.doSupportNamespaces(nsAware);
        return constructStreamReader(f, contents);
    }
}

