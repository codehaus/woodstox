package staxperf.single;

import java.io.*;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.codehaus.stax2.io.*;

public class TestAaltoBytePerf
    extends BasePerfTest
{
    private TestAaltoBytePerf() { }

    protected XMLInputFactory getFactory()
    {
        System.setProperty("javax.xml.stream.XMLInputFactory",
                           "org.codehaus.wool.stax.InputFactoryImpl");
        XMLInputFactory f =  XMLInputFactory.newInstance();
        return f;
    }

    protected int testExec(byte[] data, String path) throws Exception
    {
        Stax2ByteArraySource src = new Stax2ByteArraySource(data, 0, data.length);
        XMLStreamReader sr = mFactory.createXMLStreamReader(src);
        int ret = testExec2(sr);
        return ret;
    }

    public static void main(String[] args) throws Exception
    {
        new TestAaltoBytePerf().test(args);
    }
}
