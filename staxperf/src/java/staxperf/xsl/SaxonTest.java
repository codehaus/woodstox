package staxperf.xsl;

import java.io.*;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import net.sf.saxon.TransformerFactoryImpl;

public final class SaxonTest
    extends TestBase
{
    protected TransformerFactory getFactory() {
        return new TransformerFactoryImpl();
    }

    protected StreamResult getResult(ByteArrayOutputStream bos) {
        return new StreamResult(bos);
    }
}

