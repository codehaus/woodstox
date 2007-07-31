package staxperf.misc;

import java.io.*;
import java.util.*;

/**
 * Simple micro benchmark to see whether parsing using three-digits-at-time
 * might be faster than typical digit-per-pass loop.
 */
public class TestIntParse
{
    final static int REPS = 3999000;

    int DIGITS = 6;

    private TestIntParse() { }

    public void test(String[] args)
        throws IOException
    {
        int i = 0;
        int sum = 0;
        char[] numberStr = "123456789".toCharArray();

        while (true) {
            if ((++i % 4) == 0) {
                System.out.println();
            }
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }

            long curr = System.currentTimeMillis();
            String msg;

            switch (i % 4) {
            case 0:
                msg = "Triplets";
                sum += testTriplet(numberStr, DIGITS);
                break;
            case 1:
                msg = "Loop";
                sum += testLoop(numberStr, DIGITS);
                break;
            case 2:
                msg = "Triplets/2";
                sum += testTriplet2(numberStr, DIGITS);
                break;
            default:
                msg = "Unrolled loop";
                sum += testUnrolled(numberStr, DIGITS);
                break;
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
            TRIPLETS[i] = (i & 511)+1;
        }
    }
        
    private int testTriplet(char[] numberStr, int digits)
    {
        int loop = REPS;
        int result = 0;

        while (--loop >= 0) {
            int num = 0;

            for (int i = 0; i < digits; i += 3) {
                int ix = ((numberStr[i] - '0') << 8)
                    + ((numberStr[i+1] - '0') << 4)
                    + ((numberStr[i+2] - '0'));
                // 1000 = 1024 - 16 - 8
//                if (num != 0)
                num *= 1000;
                num += TRIPLETS[ix];
            }
                    
            result += num;
        }

        return result;
    }

    private int testTriplet2(char[] numberStr, int digits)
    {
        int loop = REPS;
        int result = 0;

        while (--loop >= 0) {
            /*
            int ix = ((numberStr[0] - '0') << 8)
                + ((numberStr[1] - '0') << 4)
                + ((numberStr[2] - '0'));
            int num = TRIPLETS[ix];
            if (digits > 3) {
                ix = ((numberStr[3] - '0') << 8)
                    + ((numberStr[4] - '0') << 4)
                    + ((numberStr[5] - '0'));
                num = (num * 1000) + TRIPLETS[ix];
                if (digits > 6) {
                    ix = ((numberStr[6] - '0') << 8)
                        + ((numberStr[7] - '0') << 4)
                        + ((numberStr[8] - '0'));
                    num = (num * 1000) + TRIPLETS[ix];
                }
            }
            */

            int ix = ((numberStr[0] - '0') * 100)
                + ((numberStr[1] - '0') * 10)
                + ((numberStr[2] - '0'));
            int num = TRIPLETS[ix];
            if (digits > 3) {
                ix = ((numberStr[3] - '0') * 100)
                    + ((numberStr[4] - '0') * 10)
                    + ((numberStr[5] - '0'));
                num = (num * 1000) + TRIPLETS[ix];
                if (digits > 6) {
                    ix = ((numberStr[6] - '0') * 100)
                        + ((numberStr[7] - '0') * 10)
                        + ((numberStr[8] - '0'));
                    num = (num * 1000) + TRIPLETS[ix];
                }
            }
                    
            result += num;
        }

        return result;
    }

    private int testLoop(char[] numberStr, int digits)
    {
        //int loop = REPS >> 3; // 1/8
        int loop = REPS;
        int result = 0;

        while (--loop >= 0) {
            int num = numberStr[0] - '0';
            
            for (int i = 1; i < digits; ++i) {
                num = (num * 10) + (numberStr[i] - '0');
                // int digit = (numberStr[i] - '0');
                // num = (num * 10) + (numberStr[i] - '0');
            }

            result += num;
        }

        return result;
    }

    private int testUnrolled(char[] numberStr, int digits)
    {
        int loop = REPS;
        int result = 0;

        while (--loop >= 0) {
            result += calcUnrolled(numberStr, digits);
        }
        return result;
    }

    private int calcUnrolled(char[] numberStr, int digits)
    {
            /*
            int i = 0;
            int num = 0;
            for (; i < digits; i += 3) {
                int ix = ((numberStr[i] - '0') * 100)
                    + ((numberStr[i+1] - '0') * 10)
                    + ((numberStr[i+2] - '0'));
                num *= 1000;
                num += ix;
            }
            */

            /*
            int num = 0;
            int i = digits - 1;
            switch (digits) {
            case 9:
                num = (numberStr[i-8] - '0') * 10;
            case 8:
                num += (numberStr[i-7] - '0');
                num *= 10;
            case 7:
                num += (numberStr[i-6] - '0');
                num *= 10;
            case 6:
                num += (numberStr[i-5] - '0');
                num *= 10;
            case 5:
                num += (numberStr[i-4] - '0');
                num *= 10;
            case 4:
                num += (numberStr[i-3] - '0');
                num *= 10;
            case 3:
                num += (numberStr[i-2] - '0');
                num *= 10;
            case 2:
                num += (numberStr[i-1] - '0');
                num *= 10;
            case 1:
                num += (numberStr[i] - '0');
            }
            */

        int i = 0;
        int num = (numberStr[i] - '0');
        if (++i < digits) {
         num = (num * 10) + (numberStr[i] - '0');
         if (++i < digits) {
             num = (num * 10) + (numberStr[i] - '0');
             if (++i < digits) {
                 num = (num * 10) + (numberStr[i] - '0');
                 if (++i < digits) {
                     num = (num * 10) + (numberStr[i] - '0');
                     if (++i < digits) {
                         num = (num * 10) + (numberStr[i] - '0');
                         if (++i < digits) {
                             num = (num * 10) + (numberStr[i] - '0');
                             if (++i < digits) {
                                 num = (num * 10) + (numberStr[i] - '0');
                                 if (++i < digits) {
                                     num = (num * 10) + (numberStr[i] - '0');
                                 }
                             }
                         }
                     }
                 }
             }
         }
        }
        return num;
    }

    public static void main(String[] args)
        throws IOException
    {
        new TestIntParse().test(args);
    }
}
