package staxperf.staxcopy;

import java.io.Reader;

import javax.xml.namespace.QName;
import javax.xml.stream.*;
import org.codehaus.stax2.*;

//import com.ctc.wstx.api.WstxInputProperties;

public class TestRiPerf
    extends BaseCopyTest
{
    protected TestRiPerf() {
        super();
    }

    protected XMLInputFactory getInputFactory()
    {
        System.setProperty("javax.xml.stream.XMLInputFactory",
                           "com.bea.xml.stream.MXParserFactory");
        XMLInputFactory f =  XMLInputFactory.newInstance();
        return f;
    }

    protected XMLOutputFactory getOutputFactory()
    {
        System.setProperty("javax.xml.stream.XMLOutputFactory",
                           "com.bea.xml.stream.XMLOutputFactoryBase");
        XMLOutputFactory f =  XMLOutputFactory.newInstance();
        return f;
    }

    public static void main(String[] args) throws Exception
    {
        new TestRiPerf().test(args);
    }
}
