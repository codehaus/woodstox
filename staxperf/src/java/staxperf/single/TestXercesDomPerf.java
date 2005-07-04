package staxperf.single;

import java.io.*;
import javax.xml.parsers.*; // TRAX, for creating parsers

import org.w3c.dom.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.stream.XMLInputFactory;

//import com.ctc.wstx.stax.*;

public class TestXercesDomPerf
    extends BasePerfTest
{
    final DocumentBuilderFactory mFactory;

    private TestXercesDomPerf() {
        super();
        System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
                           "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
        mFactory = DocumentBuilderFactory.newInstance();
        mFactory.setCoalescing(false);
        mFactory.setNamespaceAware(true);
        mFactory.setValidating(false);
        System.out.println("DOM factory: "+mFactory.getClass());
    }

    protected XMLInputFactory getFactory()
    {
        return null;
    }

    protected int testExec2(InputStream in, String path) throws Exception
    {
        DocumentBuilder dp = mFactory.newDocumentBuilder();
        Document doc = dp.parse(new InputSource(in));
        return doc.hashCode();
    }

    /*
    public void testFinish()
        throws Exception
    {
        com.ctc.wstx.util.SymbolTable symt = msc.getSymbolTable();
        double seek = symt.calcAvgSeek();
        seek = ((int) (100.0  * seek)) / 100.0;
        System.out.println("Symbol count: "+symt.size()+", avg len: "+seek+".");
    }
    */

    public static void main(String[] args) throws Exception
    {
        new TestXercesDomPerf().test(args);
    }
}
