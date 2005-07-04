package staxperf.single;

import java.io.Reader;
import javax.xml.parsers.*; // TRAX, for creating parsers

import org.w3c.dom.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.stream.XMLInputFactory;

//import com.ctc.wstx.stax.*;

public class TestCrimsonDomPerf
    extends BasePerfTest
{
    final DocumentBuilderFactory mFactory;

    private TestCrimsonDomPerf() {
        super();
        System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
                           "org.apache.crimson.jaxp.DocumentBuilderFactoryImpl");
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

    protected int testExec(Reader r) throws Exception
    {
        DocumentBuilder dp = mFactory.newDocumentBuilder();
        Document doc = dp.parse(new InputSource(r));
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
        new TestCrimsonDomPerf().test(args);
    }
}
