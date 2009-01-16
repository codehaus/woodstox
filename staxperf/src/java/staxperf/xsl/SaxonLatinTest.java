package staxperf.xsl;

import java.io.*;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import net.sf.saxon.TransformerFactoryImpl;

public final class SaxonLatinTest
    extends TestBase
{
    private SaxonLatinTest() { }

    protected TransformerFactory getFactory() {
        return new TransformerFactoryImpl();
    }

    protected StreamResult getResult(ByteArrayOutputStream bos)
        throws IOException
    {
        return new StreamResult(new OutputStreamWriter(bos, "ISO-8859-1"));

        /*
        BufferedOutputStream buf = new BufferedOutputStream(bos);
        Writer w = new OutputStreamWriter(buf, "ISO-8859-1");
        return new StreamResult(w);
        */
        //return new StreamResult(bos);
    }

    public static void main(String[] args) throws Exception {
        new SaxonLatinTest().test(args);
    }
}
