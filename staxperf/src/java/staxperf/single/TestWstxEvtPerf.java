package staxperf.single;

import java.io.Reader;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

//import com.ctc.wstx.stax.*;

public final class TestWstxEvtPerf
    extends BaseEvtPerfTest
{
    private TestWstxEvtPerf() {
        super();
    }

    protected XMLInputFactory getFactory()
    {
        System.setProperty("javax.xml.stream.XMLInputFactory",
                           "com.ctc.wstx.stax.WstxInputFactory");
        return XMLInputFactory.newInstance();
    }

    public static void main(String[] args) throws Exception
    {
        new TestWstxEvtPerf().test(args);
    }
}
