package staxperf.single;

import javax.xml.stream.XMLInputFactory;

import java.io.Reader;

public class TestDummyPerf
    extends BasePerfTest
{
    private TestDummyPerf() {
        super();
    }

    protected XMLInputFactory getFactory()
    {
        return null;
    }

    protected int testExec(Reader r) throws Exception
    {
        char[] in = new char[2000];
        char[] out = new char[2000];
        int sum = 0;

        while (true) {
            int count = r.read(in);
            if (count < 0) {
                break;
            }
            for (int i = 0; i < count; ++i) {
                char c = in[i];
                sum += c;
                out[i] = c;
            }
        }
        return sum;
    }

    /*
    public void testFinish()
        throws Exception
    {
        com.ctc.wstx.util.SymbolTable symt = msc.getSymbolTable();
        double seek = symt.calcAvgSeek();
        seek = ((int) (100.0  * seek)) / 100.0;
        System.out.println("Symbol count: "+symt.size()+", avg len: "+seek+".");
    }
    */

    public static void main(String[] args) throws Exception
    {
        new TestDummyPerf().test(args);
    }
}
