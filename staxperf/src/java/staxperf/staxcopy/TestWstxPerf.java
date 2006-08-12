package staxperf.staxcopy;

import java.io.Reader;

import javax.xml.namespace.QName;
import javax.xml.stream.*;
import org.codehaus.stax2.*;

//import com.ctc.wstx.api.WstxInputProperties;

public class TestWstxPerf
    extends BaseCopyTest
{
    protected TestWstxPerf() {
        super();
    }

    protected XMLInputFactory getInputFactory()
    {
        System.setProperty("javax.xml.stream.XMLInputFactory",
                           "com.ctc.wstx.stax.WstxInputFactory");
        XMLInputFactory f =  XMLInputFactory.newInstance();

        // To test performance without lazy parsing, uncomment this:
        //f.setProperty(WstxInputProperties.P_LAZY_PARSING, Boolean.FALSE);
        // And without namespaces:
        //f.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);

        return (XMLInputFactory2) f;
    }

    protected XMLOutputFactory getOutputFactory()
    {
        System.setProperty("javax.xml.stream.XMLOutputFactory",
                           "com.ctc.wstx.stax.WstxOutputFactory");
        XMLOutputFactory f =  XMLOutputFactory.newInstance();
        return (XMLOutputFactory2) f;
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
        new TestWstxPerf().test(args);
    }
}
