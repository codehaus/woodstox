package staxperf.single;

import java.io.*;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

//import com.ctc.wstx.api.WstxInputProperties;

import org.codehaus.wool.async.AsyncByteScanner;
import org.codehaus.wool.async.AsyncUtfScanner;
import org.codehaus.wool.in.ReaderConfig;
import org.codehaus.wool.in.StreamReaderImpl;

public class TestAaltoAsyncPerf
    extends BasePerfTest
{
    /**
     * We'll need a temporary in which data is explicitly read
     */
    byte[] mBuffer;

    private TestAaltoAsyncPerf() {
        super();
        mBuffer = new byte[4000];
    }

    protected XMLInputFactory getFactory()
    {
        // This is not really needed yet, will be in future:
        try {
            Class cls = Class.forName("org.codehaus.wool.stax.InputFactoryImpl");
            return (XMLInputFactory) cls.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // To test Char/Reader based parsing, uncomment:
    @Override
    protected int testExec(byte[] data, String path) throws Exception
    {
        ReaderConfig cfg = new ReaderConfig();
        cfg.setActualEncoding("UTF-8");
        AsyncUtfScanner asc = new AsyncUtfScanner(cfg);
        XMLStreamReader sr = new StreamReaderImpl(asc);

        ByteArrayInputStream bin = new ByteArrayInputStream(data);
        
        int ret = testAsyncExec(sr, asc, bin);
        return ret;
    }

    protected int testAsyncExec(XMLStreamReader sr, AsyncUtfScanner asc, InputStream in) throws Exception
    {
        {
            int count = in.read(mBuffer);
            asc.addInput(mBuffer, 0, count);
        }

        int total = 0;
        while (sr.hasNext()) {
            int type = sr.next();

            total += type; // so it won't be optimized out...

            //if (sr.hasText()) {
            if (type == CHARACTERS || type == CDATA || type == COMMENT) {
                // Test (a): just check length (no buffer copy)

                /*
                int textLen = sr.getTextLength();
                total += textLen;
                */

                // Test (b): access internal read buffer
                char[] text = sr.getTextCharacters();
                int start = sr.getTextStart();
                int len = sr.getTextLength();
                if (text != null) { // Ref. impl. returns nulls sometimes
                    total += text.length; // to prevent dead code elimination
                }

                // Test (c): construct string (slowest)
                /*
                String text = sr.getText();
                total += text.length();
                */
            } else if (type == AsyncByteScanner.EVENT_INCOMPLETE) {
                int count = in.read(mBuffer);
                if (count < 0) {
                    break;
                }
                asc.addInput(mBuffer, 0, count);
            }
        }
        sr.close();
        return total;
    }

    public static void main(String[] args) throws Exception
    {
        new TestAaltoAsyncPerf().test(args);
    }
}
