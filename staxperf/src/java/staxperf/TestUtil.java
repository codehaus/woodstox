package staxperf;

import java.io.*;

public class TestUtil
{
    protected TestUtil() { }

    protected int calcBatchSize(long time)
    {
        /* Let's aim at 10 of such batches per second (== 100 ms per batch);
         * however, for fastest cases, may need to relax that restriction
         * (should work out ok, since time we got is unlikely to yet
         * be 'final' speed)
         */
        if (time < 1) { // 1 msec or less; at least 5 of such batches doable / sec
            return 100;
        }
        if (time < 2) { // 1 - 2 msecs; at least 10 should be doable
            return 50;
        }
        if (time < 3) { // 2-3 msecs
            return 40;
        }
        if (time < 5) { // 3 - 4
            return 25;
        }
        if (time < 7) { // 6 - 7
            return 20;
        }
        if (time < 13) { // 8 - 12
            return 10;
        }
        if (time < 26) { // 13 - 25
            return 5;
        }
        if (time < 36) { // 26 - 35
            return 4;
        }
        if (time < 71) { 
            return 2;
        }
        return 1;
    }

    public final static byte[] readData(File f)
        throws IOException
    {
        int len = (int) f.length();
        byte[] data = new byte[len];
        int offset = 0;
        FileInputStream fis = new FileInputStream(f);
        
        while (len > 0) {
            int count = fis.read(data, offset, len-offset);
            offset += count;
            len -= count;
        }

        return data;
    }

}
