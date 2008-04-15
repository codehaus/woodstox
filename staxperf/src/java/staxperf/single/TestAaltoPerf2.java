package staxperf.single;

import java.io.*;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.codehaus.wool.in.*;
import org.codehaus.stax2.io.Stax2ByteArraySource;

public class TestAaltoPerf2
    extends BasePerfTest
{
    private byte[] mMutableData;

    private TestAaltoPerf2() { super(); }

    protected XMLInputFactory getFactory()
    {
        System.setProperty("javax.xml.stream.XMLInputFactory",
                           "org.codehaus.wool.stax.InputFactoryImpl");
        XMLInputFactory f =  XMLInputFactory.newInstance();
        return f;
    }

    protected int testExec(byte[] data, String path) throws Exception
    {
        if (mMutableData == null) {
            mMutableData = new byte[data.length + 1];
            System.arraycopy(data, 0, mMutableData, 0, data.length);
        }

        InputStream in = new ByteArrayInputStream(data);
        Stax2ByteArraySource src = new Stax2ByteArraySource(mMutableData, 0, data.length);
        XMLStreamReader sr = mFactory.createXMLStreamReader(src);
        int ret = testExec2(sr);
        in.close();
        return ret;
    }

    public static void main(String[] args) throws Exception
    {
        new TestAaltoPerf2().test(args);
    }
}
