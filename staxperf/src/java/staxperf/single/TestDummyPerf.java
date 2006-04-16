package staxperf.single;

import javax.xml.stream.XMLInputFactory;

import java.io.*;

public class TestDummyPerf
    extends BasePerfTest
{
    final char[] inc = new char[2000];
    final char[] outc = new char[2000];

    private TestDummyPerf() {
        super();
    }

    protected XMLInputFactory getFactory()
    {
        return null;
    }

    protected int testExec(File f, String path) throws Exception
    {
        InputStream in = null;

        try {
            in = createStream(f);
            //Reader r = new InputStreamReader(in, "ISO-8859-1");
            Reader r = new InputStreamReader(in, "UTF-8");
            return testExec(r, inc, outc);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    protected int testExec(Reader r, char[] in, char[] out) throws Exception
    {
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
