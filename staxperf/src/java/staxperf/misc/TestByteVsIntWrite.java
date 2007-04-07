package staxperf.misc;

import java.io.*;
import java.util.*;

/**
 * Simple micro benchmark to test how much overhead there is to writing
 * byte-based tokens using various methods.
 */
public class TestByteVsIntWrite
{
    final static int REPS = 500;

    private TestByteVsIntWrite() { }

    public void test(String[] args)
        throws IOException
    {
        if (args.length > 0) {
            System.err.println("Usage: java ... "+getClass().getName()+"");
            System.exit(1);
        }
        test2();
    }

    void test2()
        throws IOException
    {
        byte[] outBuf = new byte[64000];

        final byte[] byteData = new byte[] {
            (byte) 'a',
            (byte) 'b',
            (byte) 'c',
            (byte) 'd'
        };
        final int intData = 0x10203040;
        int sum = 0;

        for (int i = 0; true; ++i) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            System.gc();

            long curr = System.currentTimeMillis();
            String msg;

            switch (i % 5) {
            case 0:
                System.out.println();
                msg = "Write, byte array-copy";
                for (int j = 0; j < REPS; ++j) {
                    sum += testWriteByteArray(outBuf, byteData);
                }
                break;
            case 1:
                msg = "Write, byte set";
                for (int j = 0; j < REPS; ++j) {
                    sum += testWriteByte(outBuf, intData);
                }
                break;
            case 2:
                msg = "Write, int";
                for (int j = 0; j < REPS; ++j) {
                    sum += testWriteInt(outBuf, intData);
                }
                break;
            case 3:
                msg = "Write, char";
                for (int j = 0; j < REPS; ++j) {
                    sum += testWriteChar(outBuf, "Abcd");
                }
                break;
            case 4:
            default:
                msg = "Write, byte manual copy";
                for (int j = 0; j < REPS; ++j) {
                    sum += testWriteByteLoop(outBuf, byteData);
                }
                break;
            }

            curr = System.currentTimeMillis() - curr;
            System.out.println("Test '"+msg+"' -> "+curr+" msecs ("
                               +(sum & 0xFF)+").");
        }
    }

    private int testWriteByteArray(byte[] outBuffer, byte[] data)
    {
        int last = outBuffer.length - data.length;
        int total = 0;

        for (int ptr = 0; ptr < last; ) {
            System.arraycopy(data, 0, outBuffer, ptr, data.length);
            ++total;
            ptr += data.length;
        }
        return total;
    }

    private int testWriteByte(byte[] outBuffer, int data)
    {
        int last = outBuffer.length - 4;
        int total = 0;

        byte b1 = (byte) (data >> 24);
        byte b2 = (byte) (data >> 16);
        byte b3 = (byte) (data >> 8);
        byte b4 = (byte) (data);

        for (int ptr = 0; ptr < last; ) {
            outBuffer[ptr++] = b1;
            outBuffer[ptr++] = b2;
            outBuffer[ptr++] = b3;
            outBuffer[ptr++] = b4;
            ++total;
        }
        return total;
    }

    private int testWriteInt(byte[] outBuffer, int data)
    {
        int last = outBuffer.length - 4;
        int total = 0;

        for (int ptr = 0; ptr < last; ) {
            outBuffer[ptr++] = (byte)(data >> 24);
            outBuffer[ptr++] = (byte)(data >> 16);
            outBuffer[ptr++] = (byte)(data >> 8);
            outBuffer[ptr++] = (byte)(data);
            ++total;
        }
        return total;
    }

    private int testWriteChar(byte[] outBuffer, String word)
    {
        int len = word.length();
        int last = outBuffer.length - len;
        int total = 0;

        for (int ptr = 0; ptr < last; ) {
            for (int i = 0; i < len; ++i) {
                outBuffer[ptr++] = (byte) word.charAt(i);
            }
            ++total;
        }
        return total;
    }

    private int testWriteByteLoop(byte[] outBuffer, byte[] data)
    {
        int len = data.length;
        int last = outBuffer.length - len;
        int total = 0;

        for (int ptr = 0; ptr < last; ) {
            outBuffer[ptr++] = data[0];
            outBuffer[ptr++] = data[1];
            outBuffer[ptr++] = data[2];
            outBuffer[ptr++] = data[3];
            /*
            for (int i = 0; i < len; ++i) {
                outBuffer[ptr++] = data[i];
            }
            */
            ++total;
        }
        return total;
    }

    public static void main(String[] args)
        throws IOException
    {
        new TestByteVsIntWrite().test(args);
    }
}
