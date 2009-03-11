package staxperf.misc;

import java.math.BigDecimal;
import java.util.*;

/**
 * Simple micro benchmark to see whether parsing using three-digits-at-time
 * might be faster than typical digit-per-pass loop.
 */
public class TestDoubleParse
{
    final static int REPS = 390;

    private TestDoubleParse() { }

    final static double[] NUMBERS = new double[] {
        
        // "Simple" cases (for both Saxon and JDK)
        /*
        0.1, 0.2, 0.3, 0.4, 0.5,
        912450.24, 129400.259204, 111222.99999942,
        0.101, 0.213, 0.383, 0.445, 0.592054,
        10.157, 78.56, 100.50, 12.48, 0.75,
        */

        // "Medium hard" (for Saxon)
        0.011, 0.001266, 0.009102, 0.00007004, 0.00100001,
        0.00291, 0.000005, 0.002499, 0.0025, 0.000001111,
        0.009999, 0.000424, 0.0012345, 0.0029, 0.00881,

        0.01, 10.125, 0.9284751532, 1.0e-05, 12947682.124592,
        0.1, 0.2, 0.3, 0.4, 0.5,
        0.123, 0.0123, 0.00123, 0.000123, 0.0000123,
        2.2307, 9.99998092, 0.120000001, 6.66, 1.45e16,

    };

    final static String[] NUMBER_STRINGS = new String[NUMBERS.length];
    static {
        for (int i = 0; i < NUMBERS.length; ++i) {
            NUMBER_STRINGS[i] = String.valueOf(NUMBERS[i]);
        }
    }

    public void test(String[] args)
    {
        int i = 0;
        int sum = 0;

        while (true) {
            try {  Thread.sleep(300L); } catch (InterruptedException ie) { }

            long curr = System.currentTimeMillis();
            String msg;
            boolean lf = false;

            switch (i++ % 3) {
            case 0:
                lf = true;
                msg = "Double";
                sum += testDouble(NUMBER_STRINGS, REPS);
                break;
            case 1:
                msg = "BD-parse";
                sum += testBdParse(NUMBER_STRINGS, REPS);
                break;
            case 4:
            default:
                msg = "BD-parse+conv";
                sum += testBdFull(NUMBER_STRINGS, REPS);
                break;
            }

            curr = System.currentTimeMillis() - curr;
            if (lf) {
                System.out.println();
            }
            System.out.println("Test '"+msg+"' -> "+curr+" msecs ("
                               +(sum & 0xFF)+").");
        }
    }
        

    private int testDouble(String[] input, int reps)
    {
        int result = 0;
        double d = 0;
        while (--reps >= 0) {
            int tmp = 0;

            for (int i = 0, len = input.length; i < len; ++i) {
                d = Double.parseDouble(input[i]);
            }
            tmp += (int) d + reps;
        }
        return result;
    }

    private int testBdParse(String[] input, int reps)
    {
        int result = 0;
        BigDecimal bd = null;
        while (--reps >= 0) {
            int tmp = 0;

            for (int i = 0, len = input.length; i < len; ++i) {
                bd = new BigDecimal(input[i]);
            }
            tmp += bd.intValue() + reps;
        }
        return result;
    }

    private int testBdFull(String[] input, int reps)
    {
        int result = 0;
        double d = 0;

        while (--reps >= 0) {
            int tmp = 0;

            for (int i = 0, len = input.length; i < len; ++i) {
                BigDecimal bd = new BigDecimal(input[i]);
                d = bd.doubleValue();
            }
            tmp += (int) d + reps;
        }
        return result;
    }

    public static void main(String[] args)
        throws Exception
    {
        new TestDoubleParse().test(args);
    }

}
