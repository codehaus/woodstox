package staxperf.single;

import java.io.Reader;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import com.ctc.wstx.stax.*;

public class TestRIPerf
    extends BasePerfTest
{
    private TestRIPerf() {
        super();
    }

    protected XMLInputFactory getFactory() {
        System.setProperty("javax.xml.stream.XMLInputFactory",
                           "com.bea.xml.stream.MXParserFactory");
        return XMLInputFactory.newInstance();
    }

    public static void main(String[] args) throws Exception
    {
        new TestRIPerf().test(args);
    }
}
