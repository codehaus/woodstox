package staxperf.xsl;

import java.io.*;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import net.sf.saxon.TransformerFactoryImpl;

public final class SaxonFastTest
    extends TestBase
{
    private SaxonFastTest() { }

    protected TransformerFactory getFactory() {
        return new TransformerFactoryImpl();
    }

    protected StreamResult getResult(ByteArrayOutputStream bos)
        throws IOException
    {
        return new StreamResult(new UTF8Writer(bos));
    }

    public static void main(String[] args) throws Exception {
        new SaxonFastTest().test(args);
    }
}
