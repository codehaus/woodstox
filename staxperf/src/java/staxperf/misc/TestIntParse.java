package staxperf.misc;

import java.io.*;
import java.util.*;

/**
 * Simple micro benchmark to see whether parsing using three-digits-at-time
 * might be faster than typical digit-per-pass loop.
 */
public class TestIntParse
{
    final static int REPS = 495000;

    private TestIntParse() { }

    public void test(String[] args)
        throws IOException
    {
        int i = 0;
        int sum = 0;
        char[] numberStr = "123456789".toCharArray();

        while (true) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            System.gc();

            long curr = System.currentTimeMillis();
            String msg;

            if ((++i & 1) == 0) {
                msg = "Triplets";
                sum += testTriplet(numberStr);
            } else {
                msg = "Loop";
                sum += testLoop(numberStr);
            }

            curr = System.currentTimeMillis() - curr;
            System.out.println("Test '"+msg+"' -> "+curr+" msecs ("
                               +(sum & 0xFF)+").");
        }
    }

    final static int[] TRIPLETS = new int[10 * 16 * 16];
    static {
        // Let's fill with dummy data, actually; won't change timings
        for (int i = 0; i < TRIPLETS.length; ++i) {
            TRIPLETS[i] = i % 10;
        }
    }

    private int testTriplet(char[] numberStr)
    {
        int loop = REPS;
        int result = 0;

        while (--loop >= 0) {
            int num = 0;

            for (int i = 0; i < 9; i += 3) {
                int ix = ((numberStr[i] - '0') << 8)
                    + ((numberStr[i+1] - '0') << 4)
                    + ((numberStr[i+2] - '0'));
                result = (result * 10) + TRIPLETS[ix];
            }

            result += num;
        }

        return result;
    }

    private int testLoop(char[] numberStr)
    {
        int loop = REPS;
        int result = 0;

        while (--loop >= 0) {
            int num = 0;
            
            for (int i = 0; i < 9; ++i) {
                result = (result * 10) + (numberStr[i] - '0');
            }

            result += num;
        }

        return result;
    }

    public static void main(String[] args)
        throws IOException
    {
        new TestIntParse().test(args);
    }
}
