package staxperf.single;

import java.io.Reader;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

//import com.ctc.wstx.api.WstxInputProperties;

public class TestSJSXPPerf
    extends BasePerfTest
{
    protected TestSJSXPPerf() {
        super();
    }

    protected XMLInputFactory getFactory()
    {

        System.setProperty("javax.xml.stream.XMLInputFactory",
                           "com.sun.xml.stream.ZephyrParserFactory");
        XMLInputFactory f =  XMLInputFactory.newInstance();

        // To test performance without lazy parsing, uncomment this:
        //f.setProperty(WstxInputProperties.P_LAZY_PARSING, Boolean.FALSE);
        // And without namespaces:
        //f.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);

        return f;
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
        new TestSJSXPPerf().test(args);
    }
}
