package staxperf.misc;

import java.math.BigInteger;
import java.util.*;

import net.sf.saxon.value.FloatingPointConverter;
import net.sf.saxon.om.FastStringBuffer;

/**
 * Simple micro benchmark to see whether parsing using three-digits-at-time
 * might be faster than typical digit-per-pass loop.
 */
public class TestDoublePrint
{
    final static int BUFLEN = 2000;

    //final static int REPS = 190;
    final static int REPS = 70;

    private TestDoublePrint() { }

    final static double[] NUMBERS = new double[] {
        
        // "Simple" cases (for both Saxon and JDK)
        /*
        0.1, 0.2, 0.3, 0.4, 0.5,
        912450.24, 129400.259204, 111222.99999942,
        0.101, 0.213, 0.383, 0.445, 0.592054,
        10.157, 78.56, 100.50, 12.48, 0.75
        */

        // "Medium hard" (for Saxon)
        0.011, 0.001266, 0.009102, 0.00007004, 0.00100001,
        0.00291, 0.000005, 0.002499, 0.0025, 0.000001111,
        0.009999, 0.000424, 0.0012345, 0.0029, 0.00881

        /*
        0.01, 10.125, 0.9284751532, 1.0e-05, 12947682.124592,
        0.1, 0.2, 0.3, 0.4, 0.5,
        0.123, 0.0123, 0.00123, 0.000123, 0.0000123,
        2.2307, 9.99998092, 0.120000001, 6.66, 1.45e16
        */
    };

    public void test(String[] args)
    {
        int i = 0;
        int sum = 0;

        while (true) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }

            long curr = System.currentTimeMillis();
            String msg;
            boolean lf = false;

            switch (i++ % 5) {
            case 0:
                lf = true;
                msg = "JDK";
                sum += testJdk(NUMBERS, REPS);
                break;
            case 1:
                msg = "Saxon";
                sum += testSaxon(NUMBERS, REPS);
                break;
            case 2:
                msg = "Saxon/local";
                sum += testLocalFpp(NUMBERS, REPS);
                break;
            case 3:
                msg = "Saxon/local2";
                sum += testLocalFpp2(NUMBERS, REPS);
                break;
            case 4:
            default:
                msg = "Saxon/local3";
                sum += testLocalFpp3(NUMBERS, REPS);
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
        
    private int testJdk(double[] input, int reps)
    {
        int result = 0;
        while (--reps >= 0) {
            StringBuilder sb = new StringBuilder(BUFLEN);
            int ix = 0;
            for (int i = 0; i < 200; ++i) {
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

    private int testSaxon(double[] input, int reps)
    {
        int result = 0;
        while (--reps >= 0) {
            FastStringBuffer sb = new FastStringBuffer(BUFLEN);
            int ix = 0;
            for (int i = 0; i < 200; ++i) {
                if (--ix < 0) {
                    ix = input.length-1;
                }
                FloatingPointConverter.appendDouble(sb, input[ix]);
            }
            result += sb.length();

            if (sb.length() > BUFLEN) throw new Error("Internal error buflen = "+sb.length()+" > "+BUFLEN);
        }

        return result;
    }

    private int testLocalFpp(double[] input, int reps)
    {
        int result = 0;
        while (--reps >= 0) {
            StringBuilder sb = new StringBuilder(BUFLEN);
            int ix = 0;
            for (int i = 0; i < 200; ++i) {
                if (--ix < 0) {
                    ix = input.length-1;
                }
                fppfpp(sb, input[ix]);
            }
            result += sb.length();

            if (sb.length() > BUFLEN) throw new Error("Internal error buflen = "+sb.length()+" > "+BUFLEN);
        }

        return result;
    }

    private int testLocalFpp2(double[] input, int reps)
    {
        int result = 0;
        while (--reps >= 0) {
            StringBuilder sb = new StringBuilder(BUFLEN);
            int ix = 0;
            for (int i = 0; i < 200; ++i) {
                if (--ix < 0) {
                    ix = input.length-1;
                }
                fppfpp2(sb, input[ix]);
            }
            result += sb.length();

            if (sb.length() > BUFLEN) throw new Error("Internal error buflen = "+sb.length()+" > "+BUFLEN);
        }

        return result;
    }

    private int testLocalFpp3(double[] input, int reps)
    {
        int result = 0;
        while (--reps >= 0) {
            StringBuilder sb = new StringBuilder(BUFLEN);
            int ix = 0;
            for (int i = 0; i < 200; ++i) {
                if (--ix < 0) {
                    ix = input.length-1;
                }
                fppfpp3(sb, input[ix]);
            }
            result += sb.length();

            if (sb.length() > BUFLEN) throw new Error("Internal error buflen = "+sb.length()+" > "+BUFLEN);
        }

        return result;
    }

    private static final char[] charForDigit = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
    };

    private static final long doubleSignMask = 0x8000000000000000L;
    private static final long doubleExpMask = 0x7ff0000000000000L;
    private static final int doubleExpShift = 52;
    private static final int doubleExpBias = 1023;
    private static final long doubleFractMask = 0xfffffffffffffL;

    private static void fppfpp(StringBuilder sb, double d)
    {
        if (d < 0) {
            sb.append('-');
            d = -d;
        }
        //boolean exponential = (d >= 1000000 || d < 0.000001);
        boolean exponential = (d >= 1000000 || d < 0.01);
        long bits = Double.doubleToLongBits(d);
        long fraction = (1L<<52) | (bits & doubleFractMask);
        long rawExp = (bits & doubleExpMask) >> doubleExpShift;
        if (rawExp == 0) {
            throw new Error();
        }
        if (exponential) {
            sb.append(d);
        } else {
            int exp = (int)rawExp - doubleExpBias;
            fppfpp_(sb, exp, fraction, 52);
        }
    }

    private static void fppfpp_(StringBuilder sb, int exp, long fraction, int prec)
    {
        long R = fraction << Math.max(exp-prec, 0);
        long S = 1L << Math.max(0, -(exp-prec));
        long Mminus = 1L << Math.max(exp-prec, 0);
        long Mplus = Mminus;
        boolean initial = true;

        // simpleFixup

        if (fraction == 1L << (prec-1)) {
            Mplus = Mplus << 1;
            R = R << 1;
            S = S << 1;
        }
        int k = 0;
        while (R < (S+9)/10) {  // (S+9)/10 == ceiling(S/10)
            k--;
            R = R*10;
            Mminus = Mminus * 10;
            Mplus = Mplus * 10;
        }
        while (2*R + Mplus >= 2*S) {
            S = S*10;
            k++;
        }

        for (int z=k; z<0; z++) {
            if (initial) {
                sb.append("0.");
            }
            initial = false;
            sb.append('0');
        }

        // end simpleFixup

        //int H = k-1;

        boolean low;
        boolean high;
        int U;
        while (true) {
            k--;
            U = (int)(R*10 / S);
            R = R*10 % S;
            Mminus = Mminus * 10;
            Mplus = Mplus * 10;
            low = 2*R < Mminus;
            high = 2*R > 2*S - Mplus;
            if (low || high) break;
            if (k == -1) {
                if (initial) {
                    sb.append('0');
                }
                sb.append('.');
            }
            sb.append(charForDigit[U]);
            initial = false;
        }
        if (high && (!low || 2*R > S)) {
            U++;
        }
        if (k == -1) {
            if (initial) {
                sb.append('0');
            }
            sb.append('.');
        }
        sb.append(charForDigit[U]);
        for (int z=0; z<k; z++) {
            sb.append('0');
        }
    }

    private static void fppfpp2(StringBuilder sb, double d)
    {
        if (d < 0) {
            sb.append('-');
            d = -d;
        }
        //boolean exponential = (d >= 1000000 || d < 0.000001);
        boolean exponential = (d >= 1000000 || d < 0.01);
        long bits = Double.doubleToLongBits(d);
        long fraction = (1L<<52) | (bits & doubleFractMask);
        long rawExp = (bits & doubleExpMask) >> doubleExpShift;
        if (rawExp == 0) {
            throw new Error();
        }
        if (exponential) {
            sb.append(d);
        } else {
            int exp = (int)rawExp - doubleExpBias;
            fppfpp2_(sb, exp, fraction, 52);
        }
    }

    private static void fppfpp2_(StringBuilder sb, int exp, long fraction, int prec)
    {
        long R = fraction << Math.max(exp-prec, 0);
        long S = 1L << Math.max(0, -(exp-prec));
        long Mminus = 1L << Math.max(exp-prec, 0);
        long Mplus = Mminus;
        boolean initial = true;

        // simpleFixup

        if (fraction == 1L << (prec-1)) {
            Mplus = Mplus << 1;
            R = R << 1;
            S = S << 1;
        }
        int k = 0;
        while (R < (S+9)/10) {  // (S+9)/10 == ceiling(S/10)
            k--;
            R = R*10;
            Mminus = Mminus * 10;
            Mplus = Mplus * 10;
        }
        while (2*R + Mplus >= 2*S) {
            S = S*10;
            k++;
        }

        for (int z=k; z<0; z++) {
            if (initial) {
                sb.append("0.");
            }
            initial = false;
            sb.append('0');
        }

        // end simpleFixup

        //int H = k-1;

        boolean low;
        boolean high;
        int U;
        while (true) {
            k--;
            long R10 = R*10;
            U = (int)(R10 / S);
            R = R10 - (U * S);
            Mminus = Mminus * 10;
            Mplus = Mplus * 10;
            low = 2*R < Mminus;
            high = 2*R > 2*S - Mplus;
            if (low || high) break;
            if (k == -1) {
                if (initial) {
                    sb.append('0');
                }
                sb.append('.');
            }
            sb.append(charForDigit[U]);
            initial = false;
        }
        if (high && (!low || 2*R > S)) {
            U++;
        }
        if (k == -1) {
            if (initial) {
                sb.append('0');
            }
            sb.append('.');
        }
        sb.append(charForDigit[U]);
        for (int z=0; z<k; z++) {
            sb.append('0');
        }
    }

    private static void fppfpp3(StringBuilder sb, double d)
    {
        if (d > 0.0) {
            if (d == Double.POSITIVE_INFINITY) {
                sb.append("INF");
                return;
            }
            if (d == Double.MAX_VALUE) {
                sb.append("1.7976931348623157E308");
                return;
            }
            if (d == Double.MIN_VALUE) {
                sb.append("4.9E-324");
                return;
            }
        } else {
            if (d != d) {
                sb.append("NaN");
                return;
            }
            if (d == 0.0) {
                if ((Double.doubleToLongBits(d) & doubleSignMask) != 0) {
                    sb.append('-');
                }
                sb.append('0');
                return;
            }
            if (d == Double.NEGATIVE_INFINITY) {
                sb.append("-INF");
                return;
            }
            if (d == -Double.MAX_VALUE) {
                sb.append("-1.7976931348623157E308");
                return;
            }
            if (d == -Double.MIN_VALUE) {
                sb.append("-4.9E-324");
                return;
            }
            sb.append('-');
            d = -d;
        }

        boolean exponential = (d >= 1000000 || d < 0.01);
        long bits = Double.doubleToLongBits(d);
        long fraction = (1L<<52) | (bits & doubleFractMask);
        long rawExp = (bits & doubleExpMask) >> doubleExpShift;
        if (rawExp == 0) {
            throw new Error();
        }
        int exp = (int)rawExp - doubleExpBias;
        //fppfpp3_(sb, exp, fraction, 52);
        fppfpp3b_(sb, exp, fraction, 52);
    }

    private static void fppfpp3_(StringBuilder sb, int exp, long fraction, int prec)
    {
        long R = fraction << Math.max(exp-prec, 0);
        long S = 1L << Math.max(0, -(exp-prec));
        long Mminus = 1L << Math.max(exp-prec, 0);
        long Mplus = Mminus;
        boolean initial = true;

        // simpleFixup

        if (fraction == 1L << (prec-1)) {
            Mplus = Mplus << 1;
            R = R << 1;
            S = S << 1;
        }
        int k = 0;
        while (R < (S+9)/10) {  // (S+9)/10 == ceiling(S/10)
            k--;
            R = R*10;
            Mminus = Mminus * 10;
            Mplus = Mplus * 10;
        }
        while (2*R + Mplus >= 2*S) {
            S = S*10;
            k++;
        }

        for (int z=k; z<0; z++) {
            if (initial) {
                sb.append("0.");
            }
            initial = false;
            sb.append('0');
        }

        // end simpleFixup

        //int H = k-1;

        boolean low;
        boolean high;
        int U;
        while (true) {
            k--;
            long R10 = R*10;
            U = (int)(R10 / S);
            R = R10 - (U * S);
            Mminus = Mminus * 10;
            Mplus = Mplus * 10;
            low = 2*R < Mminus;
            high = 2*R > 2*S - Mplus;
            if (low || high) break;
            if (k == -1) {
                if (initial) {
                    sb.append('0');
                }
                sb.append('.');
            }
            sb.append(charForDigit[U]);
            initial = false;
        }
        if (high && (!low || 2*R > S)) {
            U++;
        }
        if (k == -1) {
            if (initial) {
                sb.append('0');
            }
            sb.append('.');
        }
        sb.append(charForDigit[U]);
        for (int z=0; z<k; z++) {
            sb.append('0');
        }
    }

    private static final BigInteger TEN = BigInteger.valueOf(10);
    private static final BigInteger NINE = BigInteger.valueOf(9);

    private static void fppfpp3b_(StringBuilder sb, int e, long f, int p)
    {
        //long R = f << Math.max(e-p, 0);
        MutableBigInteger R = new MutableBigInteger(f);
        R.shiftLeft(Math.max(e-p, 0));

        //long S = 1L << Math.max(0, -(e-p));
        MutableBigInteger S = MutableBigInteger.createLeftShifted(Math.max(0, -(e-p)));
        //long Mminus = 1 << Math.max(e-p, 0);
        MutableBigInteger Mminus = MutableBigInteger.createLeftShifted(Math.max(e-p, 0));

        //long Mplus = Mminus;
        MutableBigInteger Mplus = Mminus;

        boolean initial = true;

        // simpleFixup

        if (f == 1L << (p-1)) {
            Mplus.shiftLeftOne();
            R.shiftLeftOne();
            S.shiftLeftOne();
        }
        int k = 0;

        {
            MutableBigInteger S9_10 = S.dup().add(9).div(10);
            while (R.compareTo(S9_10) < 0) {  // (S+9)/10 == ceiling(S/10)
                k--;
                R.mult(10);
                Mminus.mult(10);
                Mplus.mult(10);
            }
        }

        {
            MutableBigInteger S_shifted = S.dup().shiftLeftOne();
            MutableBigInteger R_shift_add = R.dup().shiftLeftOne().add(Mplus);
            while (R_shift_add.compareTo(S_shifted) >= 0) {
                S.mult(10);
                S_shifted = S.dup().shiftLeftOne();
                k++;
            }
        }

        for (int z=k; z<0; z++) {
            if (initial) {
                sb.append("0.");
            }
            initial = false;
            sb.append('0');
        }

        // end simpleFixup

        //int H = k-1;

        boolean low;
        boolean high;
        int U;
        while (true) {
            k--;
            MutableBigInteger R10 = R.dup().mult(10);
            U = R10.dup().div(S).intValue();
            R = R10.mod(S);
            Mminus.mult(10);
            Mplus.mult(10);
            MutableBigInteger R2 = R.dup().shiftLeftOne();
            low = R2.compareTo(Mminus) < 0;
            MutableBigInteger S_shift_sub = S.dup().shiftLeftOne();
            S.sub(Mplus);
            high = R2.compareTo(S_shift_sub) > 0;
            if (low || high) break;
            if (k == -1) {
                if (initial) {
                    sb.append('0');
                }
                sb.append('.');
            }
            sb.append(charForDigit[U]);
            initial = false;
        }
        if (high && (!low || R.shiftLeftOne().compareTo(S) > 0)) {
            U++;
        }
        if (k == -1) {
            if (initial) {
                sb.append('0');
            }
            sb.append('.');
        }
        sb.append(charForDigit[U]);
        for (int z=0; z<k; z++) {
            sb.append('0');
        }
    }

    public static void main(String[] args)
        throws Exception
    {
        //verify();

        new TestDoublePrint().test(args);
    }

    static void verify()
    {
        /*
        System.out.println("NaN < 0" + (Double.NaN < 0.0));
        System.out.println("NaN > 0" + (Double.NaN > 0.0));

        System.out.println("-inf < 0" + (Double.NEGATIVE_INFINITY < 0.0));
        System.out.println("-max < 0" + (-Double.MAX_VALUE < 0.0));
        System.out.println("-min < 0" + (-Double.MIN_VALUE < 0.0));
        System.out.println("+inf > 0" + (Double.POSITIVE_INFINITY > 0.0));
        System.out.println("+max > 0" + (Double.MAX_VALUE > 0.0));
        System.out.println("+min > 0" + (Double.MIN_VALUE > 0.0));
        */

        //final  int START = 10000;
        final int START = 2000;

        TestDoublePrint inst = new TestDoublePrint();
        System.out.println("Start...");
        //for (int i = START; i <= 999999; ++i) {
        for (int i = 999999; i >= START; --i) {
            double d1 = (double) i / 1000000.0;
            String exp = String.valueOf(d1);
            String act;

            StringBuilder sb = new StringBuilder();
            try {
                inst.fppfpp2(sb, d1);
            } catch (Exception ex) {
                throw new Error("Problem when exp "+exp+", got "+sb+" (i "+i+"): "+ex);
            }


            act = sb.toString();
            /*
            {
                FastStringBuffer sb = new FastStringBuffer(20);
                FloatingPointConverter.appendDouble(sb, d1);
                act = sb.toString();
            }
            */

            double d2 = Double.parseDouble(act);
            if (d1 != d2) {
                throw new Error("Exp "+exp+", got "+act+" (i "+i+")");
            }
        }
        System.out.println("Done!");
    }

    /**
     * Optimized alternative to {@link java.lang.BigInteger}
     */
    final static class MutableBigInteger
    {
        final static int INT_SIGN = (1 << 31);

        int signum;
        int[] mag;

        private MutableBigInteger(int s, int[] m)
        {
            signum = s;
            mag = m;
        }

        public MutableBigInteger(long val)
        {
            if (val < 0) {
                signum = -1;
                val = -val;
            } else {
                signum = 1;
            }

            int highWord = (int)(val >>> 32);
            if (highWord==0) {
                mag = new int[1];
                mag[0] = (int)val;
            } else {
                mag = new int[2];
                mag[0] = highWord;
                mag[1] = (int)val;
            }
        }

        public MutableBigInteger dup()
        {
            int len = mag.length;
            int[] newMag = new int[len];
            System.arraycopy(mag, 0, newMag, 0, len);
            return new MutableBigInteger(signum, newMag);
        }

        public static MutableBigInteger createLeftShifted(int shift)
        {
            if (shift < 63) { // 1<<63 would be negative
                return new MutableBigInteger(1L << shift);
            }
            MutableBigInteger i = new MutableBigInteger(1L);
            i.shiftLeft(shift);
            return i;
        }

        public MutableBigInteger shiftLeftOne()
        {
            // First, let's shift in place
            boolean carry = false;
            for (int i = mag.length; --i >= 0; ) {
                int value = mag[i];
                if (carry) {
                    mag[i] = (value << 1) | INT_SIGN;
                } else {
                    mag[i] = (value << 1);
                }
                carry = (value < 0);
            }
            // But do we need to expand?
            if (carry) {
                expandByOne(1);
            }
            return this;
        }
 
        public MutableBigInteger shiftLeft(int n)
        {
            if (n < 1) {
                if (n < 0) {
                    throw new IllegalArgumentException("Negative shift "+n);
                }
            }
            int nInts = n >>> 5;
            int nBits = n & 0x1f;
            int magLen = mag.length;
            int newMag[];

            if (nBits == 0) {
                newMag = new int[magLen + nInts];
                for (int i=0; i<magLen; i++) {
                    newMag[i] = mag[i];
                }
            } else {
                int i = 0;
                int nBits2 = 32 - nBits;
                int highBits = mag[0] >>> nBits2;
                if (highBits != 0) {
                    newMag = new int[magLen + nInts + 1];
                    newMag[i++] = highBits;
                } else {
                    newMag = new int[magLen + nInts];
                }
                int j=0;
                while (j < magLen-1) {
                    newMag[i++] = mag[j++] << nBits | mag[j] >>> nBits2;
                }
                newMag[i] = mag[j] << nBits;
            }
            mag = newMag;
            return this;
        }

        public MutableBigInteger add(int amount)
        {
            // !!! TBI
            return this;
        }

        public MutableBigInteger add(MutableBigInteger i)
        {
            // !!! TBI
            return this;
        }

        public MutableBigInteger sub(MutableBigInteger i)
        {
            // !!! TBI
            return this;
        }

        public MutableBigInteger mult(int amount)
        {
            // !!! TBI
            return this;
        }

        public MutableBigInteger mod(MutableBigInteger i)
        {
            // !!! TBI
            return this;
        }

        public MutableBigInteger div(int amount)
        {
            // !!! TBI
            return this;
        }

        public MutableBigInteger div(MutableBigInteger i)
        {
            // !!! TBI
            return this;
        }

        public int compareTo(MutableBigInteger other)
        {
            // !!! TBI
            return 0;
        }

        public int intValue()
        {
            if (mag.length > 1) {
                throw new IllegalStateException("Overflow");
            }
            return mag[0];
        }

        private void expandByOne(int firstWord)
        {
            int[] oldMag = mag;
            int len = oldMag.length;
            mag = new int[len+1];
            System.arraycopy(oldMag, 0, mag, 1, len);
            mag[0] = firstWord;
        }
   }
}
