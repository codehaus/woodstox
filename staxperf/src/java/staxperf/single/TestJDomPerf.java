package staxperf.single;

import java.io.*;
import javax.xml.parsers.*; // TRAX, for creating parsers

import org.jdom.Document;
import org.jdom.input.StAXBuilder;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

public class TestJDomPerf
    extends BasePerfTest
{
    final XMLInputFactory mFactory;

    private TestJDomPerf()
    {
        super();
        System.setProperty("javax.xml.stream.XMLInputFactory",
                           "com.ctc.wstx.stax.WstxInputFactory");
        mFactory =  XMLInputFactory.newInstance();
        mFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
        mFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
        mFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES,
                             Boolean.TRUE);
        mFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.TRUE);
        mFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
        System.out.println("StAX factory (for JDOM): "+mFactory.getClass());
    }

    protected XMLInputFactory getFactory()
    {
        return null;
    }

    protected int testExec2(InputStream in, String path) throws Exception
    {
        XMLStreamReader sr = mFactory.createXMLStreamReader(path, in);
        StAXBuilder builder = new StAXBuilder();
        Document domDoc = builder.build(sr);
        return domDoc.hashCode();
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
        new TestJDomPerf().test(args);
    }
}
