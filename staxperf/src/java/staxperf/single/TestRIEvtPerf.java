package staxperf.single;

import java.io.Reader;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

public class TestRIEvtPerf
    extends BaseEvtPerfTest
{
    private TestRIEvtPerf() {
        super();
    }

    protected XMLInputFactory getFactory() {
        return XMLInputFactory.newInstance();
    }

    public static void main(String[] args) throws Exception
    {
        new TestRIEvtPerf().test(args);
    }
}
