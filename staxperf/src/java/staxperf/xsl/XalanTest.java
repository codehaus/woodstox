package staxperf.xsl;

import java.io.*;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.apache.xalan.xsltc.trax.TransformerFactoryImpl;

public final class XalanTest
    extends TestBase
{
    private XalanTest() { }

    protected TransformerFactory getFactory() {
        return new TransformerFactoryImpl();
    }

    protected StreamResult getResult(ByteArrayOutputStream bos) {
        return new StreamResult(bos);
    }

    public static void main(String[] args) throws Exception {
        new XalanTest().test(args);
    }
}

