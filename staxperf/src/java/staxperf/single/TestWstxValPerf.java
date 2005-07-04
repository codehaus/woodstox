package staxperf.single;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

public class TestWstxValPerf
    extends TestWstxPerf
{
    private TestWstxValPerf() {
        super();
    }

    protected XMLInputFactory getFactory()
    {
        XMLInputFactory f = super.getFactory();
        f.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.TRUE);
        return f;
    }

    public static void main(String[] args) throws Exception
    {
        new TestWstxValPerf().test(args);
    }
}
