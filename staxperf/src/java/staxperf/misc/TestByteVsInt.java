package staxperf.misc;

import java.io.*;
import java.util.*;

/**
 * Simple micro benchmark to see potential benefits of comparing int32s
 * at a time, as opposed to byte-by-byte: especially when tokenizing/parsing
 * UTF-8/Ascii/ISO-Latin1 encoded names.
 *<p>
 * On Athlon, speed difference seems to be 2-to-1 for int32s... so it
 * does seem feasible.
 */
public class TestByteVsInt
{
    private TestByteVsInt() { }

    public void test(String[] args)
        throws IOException
    {
        if (args.length < 1) {
            System.err.println("Usage: java ... "+getClass().getName()+" [file]");
            System.exit(1);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int count;
        byte[] buf = new byte[4000];
        FileInputStream fis = new FileInputStream(new File(args[0]));

        while ((count = fis.read(buf)) > 0) {
            bos.write(buf, 0, count);
        }
        bos.close();
        fis.close();
        test2(bos.toByteArray());
    }

    void test2(byte[] data)
        throws IOException
    {
        final byte[] byteData = "<el1234>".getBytes("UTF-8");
        final int[] intData = calcInts(byteData);
        int i = 0;
        int sum = 0;

        while (true) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            System.gc();

            long curr = System.currentTimeMillis();
            String msg;

            if ((++i & 1) == 0) {
                msg = "Compare, byte";
                sum += testCompByte(data, byteData);
            } else {
                msg = "Compare, int";
                sum += testCompInt(data, intData);
            }

            curr = System.currentTimeMillis() - curr;
            System.out.println("Test '"+msg+"' -> "+curr+" msecs ("
                               +(sum & 0xFF)+").");
        }
    }

    private int[] calcInts(byte[] data)
    {
        int rlen = (data.length + 3) / 4;
        int[] result = new int[rlen];

        int j = 0;
        for (int i = 0; i < rlen; ++i) {
            result[i] =
                data[j] << 24
                | ((data[j+1] & 0xFF) << 16)
                | ((data[j+2] & 0xFF) << 8)
                | ((data[j+3] & 0xFF));
            j += 4;
        }
        return result;
    }

    private int testCompByte(byte[] data, byte[] bytes)
    {
        int wordLen = bytes.length;
        int last = data.length - wordLen;
        int ptr = 0;
        int total = 0;

        while (ptr < last) {
            int hash = 0;
            int last2 = ptr + wordLen;

            for (int i = ptr; i < last2; ++i) {
                hash = (hash * 31) + data[i];
            }
            total += hash;

            for (int i = 0; i < wordLen; ++i) {
                if (bytes[i] == data[ptr+i]) {
                    ++total;
                }
            }
            ptr = last2;
        }
        return total;
    }

    private int testCompInt(byte[] data, int[] words)
    {
        int[] wordBuf = new int[words.length];
        int wordLen = words.length; 
        int last = data.length - (wordLen * 4);
        int ptr = 0;
        int total = 0;

        while (ptr < last) {
            int hash = 0;
            for (int i = 0; i < wordLen; ++i) {
                int word =
                    data[ptr] << 24
                | ((data[ptr+1] & 0xFF) << 16)
                | ((data[ptr+2] & 0xFF) << 8)
                | ((data[ptr+3] & 0xFF));
                wordBuf[i] = word;
                ptr += 4;
                hash = (hash * 31) + word;
            }
            total += hash;
            for (int i = 0; i < wordLen; ++i) {
                if (words[i] == wordBuf[i]) {
                    ++total;
                }
            }
        }
        return total;
    }

    public static void main(String[] args)
        throws IOException
    {
        new TestByteVsInt().test(args);
    }
}
