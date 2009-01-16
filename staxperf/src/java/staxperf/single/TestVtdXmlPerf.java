package staxperf.single;

import java.io.*;

import com.ximpleware.*;
import com.ximpleware.parser.*;

public class TestVtdXmlPerf
    extends BasePerfTest
{
    private TestVtdXmlPerf() { }

    protected javax.xml.stream.XMLInputFactory getFactory()
    {
        return null;
    }

    @Override
    protected int testExec(byte[] data, String path) throws Exception
    {
        VTDGen vg = new VTDGen();

        //vg.setDoc_BR(data);
        vg.setDoc(data);
        vg.parse(true);

        return vg.hashCode();
    }

    public static void main(String[] args) throws Exception
    {
        new TestVtdXmlPerf().test(args);
    }
}
