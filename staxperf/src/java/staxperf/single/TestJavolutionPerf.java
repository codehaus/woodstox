package staxperf.single;

import javolution.text.CharArray;
import javolution.xml.stream.*;

import java.io.*;

public class TestJavolutionPerf
    extends BasePerfTest
{
    javolution.xml.stream.XMLInputFactory mJIF;

    private TestJavolutionPerf()
    {
        mJIF = javolution.xml.stream.XMLInputFactory.newInstance();
    }

    protected javax.xml.stream.XMLInputFactory getFactory()
    {
        return null;
    }

    protected int testExec2(InputStream in, String path) throws Exception
    {
        XMLStreamReader sr = mJIF.createXMLStreamReader(in);

        int total = 0;
        while (sr.hasNext()) {
            int type = sr.next();
            total += type; // so it won't be optimized out...

            //if (sr.hasText()) {
            if (type == CHARACTERS || type == CDATA || type == COMMENT) {
                // Test (a): just check length (no buffer copy)

                int textLen = sr.getTextLength();
                total += textLen;

                // Test (b): access internal read buffer
                /*
                char[] text = sr.getTextCharacters();
                int start = sr.getTextStart();
                int len = sr.getTextLength();
                if (text != null) { // Ref. impl. returns nulls sometimes
                    total += text.length; // to prevent dead code elimination
                }
                */

                // Test (c): construct string (slowest)
                /*
                CharArray text = sr.getText();
                total += text.length();
                */
            }
        }
        sr.close();
        return total;
    }


    public static void main(String[] args) throws Exception
    {
        new TestJavolutionPerf().test(args);
    }
}
