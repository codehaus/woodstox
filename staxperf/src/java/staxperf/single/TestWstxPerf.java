package staxperf.single;

import java.io.Reader;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

//import com.ctc.wstx.api.WstxInputProperties;

public class TestWstxPerf
    extends BasePerfTest
{
    protected TestWstxPerf() {
        super();
    }

    protected XMLInputFactory getFactory()
    {
        System.setProperty("javax.xml.stream.XMLInputFactory",
                           "com.ctc.wstx.stax.WstxInputFactory");
        XMLInputFactory f =  XMLInputFactory.newInstance();

        // To test performance without lazy parsing, uncomment this:
        //f.setProperty(WstxInputProperties.P_LAZY_PARSING, Boolean.FALSE);
        // And without namespaces:
        //f.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);

        return f;
    }

    public static void main(String[] args) throws Exception
    {
        new TestWstxPerf().test(args);
    }
}
