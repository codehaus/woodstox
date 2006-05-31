package staxperf.single;

import java.io.Reader;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

//import com.ctc.wstx.api.WstxInputProperties;

public class TestWoolPerf
    extends BasePerfTest
{
    protected TestWoolPerf() {
        super();
    }

    protected XMLInputFactory getFactory()
    {
        System.setProperty("javax.xml.stream.XMLInputFactory",
                           "org.codehaus.wool.stax.WoolInputFactory");
        XMLInputFactory f =  XMLInputFactory.newInstance();
        return f;
    }

    public static void main(String[] args) throws Exception
    {
        new TestWoolPerf().test(args);
    }
}
