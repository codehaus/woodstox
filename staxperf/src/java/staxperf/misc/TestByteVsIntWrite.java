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
        final int LEN = 3;

        for (int i = 0; true; ++i) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            System.gc();

            long curr = System.currentTimeMillis();
            String msg;

            switch (i % 6) {
            case 0:
                System.out.println();
                msg = "Write, byte array-copy";
                for (int j = 0; j < REPS; ++j) {
                    sum += testWriteByteArray(outBuf, byteData, LEN);
                }
                break;
            case 1:
                msg = "Write, byte set";
                for (int j = 0; j < REPS; ++j) {
                    sum += testWriteByte2(outBuf, intData, LEN);
                }
                break;
            case 2:
                msg = "Write, int (exact)";
                for (int j = 0; j < REPS; ++j) {
                    sum += testWriteInt1(outBuf, intData, LEN);
                }
                break;
            case 3:
                msg = "Write, int (always 4)";
                for (int j = 0; j < REPS; ++j) {
                    sum += testWriteInt2(outBuf, intData, LEN);
                }
                break;
            case 4:
                msg = "Write, char";
                for (int j = 0; j < REPS; ++j) {
                    sum += testWriteChar(outBuf, "Abcd", LEN);
                }
                break;
            case 5:
            default:
                msg = "Write, byte manual copy";
                for (int j = 0; j < REPS; ++j) {
                    sum += testWriteByteLoop(outBuf, byteData, LEN);
                }
                break;
            }

            curr = System.currentTimeMillis() - curr;
            System.out.println("Test '"+msg+"' -> "+curr+" msecs ("
                               +(sum & 0xFF)+").");
        }
    }

    private int testWriteByteArray(byte[] outBuffer, byte[] data, int len)
    {
        int last = outBuffer.length - len;
        int total = 0;

        for (int ptr = 0; ptr < last; ) {
            System.arraycopy(data, 0, outBuffer, ptr, len);
            ++total;
            ptr += len;
        }
        return total;
    }

    private int testWriteByte1(byte[] outBuffer, int data, int len)
    {
        int last = outBuffer.length - 4;
        int total = 0;

        byte b1 = (byte) (data >> 24);
        byte b2 = (byte) (data >> 16);
        byte b3 = (byte) (data >> 8);
        byte b4 = (byte) (data);

        for (int ptr = 0; ptr < last; ) {
            outBuffer[ptr] = b1;
            outBuffer[ptr+1] = b2;
            outBuffer[ptr+2] = b3;
            outBuffer[ptr+3] = b4;
            ptr += len;
            ++total;
        }
        return total;
    }

    private int testWriteByte2(byte[] outBuffer, int data, int len)
    {
        int last = outBuffer.length - 4;
        int total = 0;

        byte b1 = (byte) (data >> 24);
        byte b2 = (byte) (data >> 16);
        byte b3 = (byte) (data >> 8);
        byte b4 = (byte) (data);

        for (int ptr = 0; ptr < last; ) {
            switch (len) {
            case 4:
                outBuffer[ptr+3] = b4;
            case 3:
                outBuffer[ptr+2] = b3;
            case 2:
                outBuffer[ptr+1] = b2;
            default:
                outBuffer[ptr] = b1;
            }
            ptr += len;
            ++total;
        }
        return total;
    }

    private int testWriteInt1(byte[] outBuffer, int data, int len)
    {
        int last = outBuffer.length - 4;
        int total = 0;

        for (int ptr = 0; ptr < last; ) {
            switch (len) {
            case 4:
                outBuffer[ptr+3] = (byte)(data);
            case 3:
                outBuffer[ptr+2] = (byte)(data >> 8);
            case 2:
                outBuffer[ptr+1] = (byte)(data >> 16);
            default:
                outBuffer[ptr] = (byte)(data >> 24);
            }
            ptr += len;
            ++total;
        }
        return total;
    }

    private int testWriteInt2(byte[] outBuffer, int data, int len)
    {
        int last = outBuffer.length - 4;
        int total = 0;

        for (int ptr = 0; ptr < last; ) {
            outBuffer[ptr] = (byte)(data >> 24);
            outBuffer[ptr+1] = (byte)(data >> 16);
            outBuffer[ptr+2] = (byte)(data >> 8);
            outBuffer[ptr+3] = (byte)(data);
            ptr += len;
            ++total;
        }
        return total;
    }

    private int testWriteChar(byte[] outBuffer, String word, int len)
    {
        int last = outBuffer.length - len;
        int total = 0;

        for (int ptr = 0; ptr < last; ) {
            /*
            for (int i = 0; i < len; ++i) {
                outBuffer[ptr++] = (byte) word.charAt(i);
            }
            */
            outBuffer[ptr++] = (byte)word.charAt(0);
            if (len > 2) {
                outBuffer[ptr++] = (byte)word.charAt(1);
                outBuffer[ptr++] = (byte)word.charAt(2);
                if (len > 3) {
                    outBuffer[ptr++] = (byte)word.charAt(3);
                }
            } else {
                if (len > 1) {
                    outBuffer[ptr++] = (byte)word.charAt(1);
                }
            }

            ++total;
        }
        return total;
    }

    private int testWriteByteLoop(byte[] outBuffer, byte[] data, int len)
    {
        int last = outBuffer.length - len;
        int total = 0;

        for (int ptr = 0; ptr < last; ) {
            /*
            outBuffer[ptr++] = data[0];
            outBuffer[ptr++] = data[1];
            outBuffer[ptr++] = data[2];
            outBuffer[ptr++] = data[3];
            */

            outBuffer[ptr++] = data[0];
            if (len > 2) {
                outBuffer[ptr++] = data[1];
                outBuffer[ptr++] = data[2];
                if (len > 3) {
                    outBuffer[ptr++] = data[3];
                }
            } else {
                if (len > 1) {
                    outBuffer[ptr++] = data[1];
                }
            }

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
