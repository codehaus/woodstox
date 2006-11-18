package stax2;

import java.io.*;

import javax.xml.stream.*;

import org.codehaus.stax2.*;

import wstxtest.BaseWstxTest;

/**
 * Base unit test class to be inherited by all unit tests that test
 * StAX2 API compatibility.
 */
public class BaseStax2Test
    extends BaseWstxTest
{
    protected BaseStax2Test() { super(); }
    protected BaseStax2Test(String name) { super(name); }

    /*
    //////////////////////////////////////////////////
    // Factory methods
    //////////////////////////////////////////////////
     */

    protected XMLStreamReader2 constructNsStreamReader(String content, boolean coal)
        throws XMLStreamException
    {
        XMLInputFactory f = getInputFactory();
        setNamespaceAware(f, true);
        setCoalescing(f, coal);
        return (XMLStreamReader2) f.createXMLStreamReader(new StringReader(content));
    }

    protected XMLStreamReader2 constructNsStreamReader(InputStream in, boolean coal)
        throws XMLStreamException
    {
        XMLInputFactory f = getInputFactory();
        setNamespaceAware(f, true);
        setCoalescing(f, coal);
        return (XMLStreamReader2) f.createXMLStreamReader(in);
    }
}

