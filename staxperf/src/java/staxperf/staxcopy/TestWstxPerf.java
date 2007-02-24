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
        return (XMLInputFactory2) f;
    }

    protected XMLOutputFactory getOutputFactory()
    {
        System.setProperty("javax.xml.stream.XMLOutputFactory",
                           "com.ctc.wstx.stax.WstxOutputFactory");
        XMLOutputFactory f =  XMLOutputFactory.newInstance();
        return (XMLOutputFactory2) f;
    }

    public static void main(String[] args) throws Exception
    {
        new TestWstxPerf().test(args);
    }
}
