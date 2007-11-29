package staxperf.misc;

import java.math.BigInteger;
import java.util.*;

import net.sf.saxon.value.FloatingPointConverter;
import net.sf.saxon.om.FastStringBuffer;

/**
 * Simple micro benchmark to see whether specialized int printing
 * is measurably faster than the default JDK provided alternative.
 */
public class TestIntPrint
{
    final static int REPS = 1970;

    int BUFLEN = -1;

    private TestIntPrint() { }

    final static int[] NUMBERS = new int[] {
        0
        
        // Small numbers
        ,1, 5, 12, -3, 0, 56, 9, 3, -2, -11, -143, 13, -39, 7, 98, -69, 123

        // Medium numbers
        ,102, 5214, 123252, -92412, 146, -999, -1204, 13503, 17, -99, 123000, -34002, 0xFFFF, -32784, 92003

        // Big numbers
        ,0x7fffFFFF, 0x10203040, 0xF8001234, -2000100200, -1999888777, 1888999777, 0x3abc452
    };

    public void test(String[] args)
    {
        /* First, let's determine buffer size we need (to minimize
         * allocation overhead)
         */
        BUFLEN = 8000;
        BUFLEN = testJdk(NUMBERS, 1);

        // And verify size is stable:
        {
            int len2 = testJdk(NUMBERS, 1);
            if (BUFLEN != len2) {
                throw new Error("Internal error: unstable int length, "+BUFLEN+" vs "+len2);
            }
        }

        System.out.println("NOTE: buffer length: "+BUFLEN);
        try {  Thread.sleep(300L); } catch (InterruptedException ie) { }

        int i = 0;
        int sum = 0;

        while (true) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }

            long curr = System.currentTimeMillis();
            String msg;
            boolean lf = false;

            switch (i++ % 3) {
            case 0:
                msg = "JDK";
                sum = testJdk(NUMBERS, REPS);
                break;
            case 1:
                msg = "Custom/fast";
                sum = testCustom(NUMBERS, REPS);
                break;
            case 2:
            default:
                msg = "Custom/copy";
                sum = testCustom2(NUMBERS, REPS);
                break;
            }

            curr = System.currentTimeMillis() - curr;
            if (lf) {
                System.out.println();
            }
            System.out.println("Test '"+msg+"' -> "+curr+" msecs ("
                               +(sum / REPS)+").");
        }
    }
        
    private int testJdk(int[] input, int reps)
    {
        int result = 0;
        while (--reps >= 0) {
            StringBuilder sb = new StringBuilder(BUFLEN);
            int ix = 0;
            for (int i = 0; i < 800; ++i) {
                if (--ix < 0) {
                    ix = input.length-1;
                }
                sb.append(input[ix]);
            }
            result += sb.length();

            if (sb.length() > BUFLEN) throw new Error("Internal error buflen = "+sb.length()+" > "+BUFLEN);
        }

        return result;
    }

    private int testCustom(int[] input, int reps)
    {
        int result = 0;
        while (--reps >= 0) {
            char[] buf = new char[BUFLEN];
            int ix = 0;
            int offset = 0;
            for (int i = 0; i < 800; ++i) {
                if (--ix < 0) {
                    ix = input.length-1;
                }
                offset = outputInt(input[ix], buf, offset);
            }
            result += offset;

            if (offset > BUFLEN) throw new Error("Internal error buflen = "+offset+" > "+BUFLEN);
        }

        return result;
    }

    private int testCustom2(int[] input, int reps)
    {
        int result = 0;
        final char[] buf = new char[20];
        while (--reps >= 0) {
            int ix = 0;
            StringBuilder sb = new StringBuilder(BUFLEN);
            for (int i = 0; i < 800; ++i) {
                if (--ix < 0) {
                    ix = input.length-1;
                }
                int len = outputInt(input[ix], buf, 0);
                sb.append(buf, 0, len);
            }
            result += sb.length();
        }

        return result;
    }

    public static void main(String[] args)
        throws Exception
    {
        new TestIntPrint().test(args);
    }


    // // // Test code cut'n pasted from elsewhere

    private final static char NULL_CHAR = (char) 0;

    private static int MILLION = 1000000;
    private static int BILLION = 1000000000;

    final static String SMALLEST_INT = String.valueOf(Integer.MIN_VALUE);

    final static char[] LEADING_TRIPLETS = new char[4000];
    final static char[] FULL_TRIPLETS = new char[4000];
    static {
        /* Let's fill it with NULLs for ignorable leading digits,
         * and digit chars for others
         */
        int ix = 0;
        for (int i1 = 0; i1 < 10; ++i1) {
            char f1 = (char) ('0' + i1);
            char l1 = (i1 == 0) ? NULL_CHAR : f1;
            for (int i2 = 0; i2 < 10; ++i2) {
                char f2 = (char) ('0' + i2);
                char l2 = (i1 == 0 && i2 == 0) ? NULL_CHAR : f2;
                for (int i3 = 0; i3 < 10; ++i3) {
                    // Last is never to be empty
                    char f3 = (char) ('0' + i3);
                    LEADING_TRIPLETS[ix] = l1;
                    LEADING_TRIPLETS[ix+1] = l2;
                    LEADING_TRIPLETS[ix+2] = f3;
                    FULL_TRIPLETS[ix] = f1;
                    FULL_TRIPLETS[ix+1] = f2;
                    FULL_TRIPLETS[ix+2] = f3;
                    ix += 4;
                }
            }
        }
    }

    /**
     * @return Offset within buffer after outputting int
     */
    public static int outputInt(int value, char[] buffer, int offset)
    {
        if (value < 0) {
            if (value == Integer.MIN_VALUE) {
                // Special case: no matching positive value within range
                int len = SMALLEST_INT.length();
                SMALLEST_INT.getChars(0, len, buffer, offset);
                return (offset + len);
            }
            buffer[offset++] = '-';
            value = -value;
        }

        if (value < MILLION) { // at most 2 triplets...
            if (value < 1000) {
                if (value < 10) {
                    buffer[offset++] = (char) ('0' + value);
                } else {
                    offset = outputLeadingTriplet(value, buffer, offset);
                }
            } else {
                int thousands = value / 1000;
                value -= (thousands * 1000); // == value % 1000
                offset = outputLeadingTriplet(thousands, buffer, offset);
                offset = outputFullTriplet(value, buffer, offset);
            }
            return offset;
        }

        // ok, all 3 triplets included
        /* Let's first hand possible billions separately before
         * handling 3 triplets. This is possible since we know we
         * can have at most '2' as billion count.
         */
        boolean hasBillions = (value >= BILLION);
        if (hasBillions) {
            value -= BILLION;
            if (value >= BILLION) {
                value -= BILLION;
                buffer[offset++] = '2';
            } else {
                buffer[offset++] = '1';
            }
        }
        int newValue = value / 1000;
        int ones = (value - (newValue * 1000)); // == value % 1000
        value = newValue;
        newValue /= 1000;
        int thousands = (value - (newValue * 1000));
        
        // value now has millions, which have 1, 2 or 3 digits
        if (hasBillions) {
            offset = outputFullTriplet(newValue, buffer, offset);
        } else {
            offset = outputLeadingTriplet(newValue, buffer, offset);
        }
        offset = outputFullTriplet(thousands, buffer, offset);
        offset = outputFullTriplet(ones, buffer, offset);
        return offset;
    }

    private static int outputLeadingTriplet(int triplet, char[] buffer, int offset)
    {
        int digitOffset = (triplet << 2);
        char c = LEADING_TRIPLETS[digitOffset++];
        if (c != NULL_CHAR) {
            buffer[offset++] = c;
        }
        c = LEADING_TRIPLETS[digitOffset++];
        if (c != NULL_CHAR) {
            buffer[offset++] = c;
        }
        // Last is required to be non-empty
        buffer[offset++] = LEADING_TRIPLETS[digitOffset];
        return offset;
    }

    private static int outputFullTriplet(int triplet, char[] buffer, int offset)
    {
        int digitOffset = (triplet << 2);
        buffer[offset++] = FULL_TRIPLETS[digitOffset++];
        buffer[offset++] = FULL_TRIPLETS[digitOffset++];
        buffer[offset++] = FULL_TRIPLETS[digitOffset];
        return offset;
    }
}
