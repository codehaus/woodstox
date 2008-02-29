package staxperf.staxcopy;

import java.io.Reader;

import javax.xml.namespace.QName;
import javax.xml.stream.*;
import org.codehaus.stax2.*;

//import com.ctc.wstx.api.WstxInputProperties;

public class TestAaltoPerf
    extends BaseCopyTest
{
    protected TestAaltoPerf() { }

    protected XMLInputFactory getInputFactory()
    {
        System.setProperty("javax.xml.stream.XMLInputFactory",
                           "org.codehaus.wool.stax.InputFactoryImpl");
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
                           "org.codehaus.wool.stax.OutputFactoryImpl");
        XMLOutputFactory f =  XMLOutputFactory.newInstance();
        return (XMLOutputFactory2) f;
    }

    public static void main(String[] args) throws Exception
    {
        new TestAaltoPerf().test(args);
    }
}
