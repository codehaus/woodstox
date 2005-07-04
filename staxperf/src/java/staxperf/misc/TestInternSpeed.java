package staxperf.misc;

import java.util.*;

public class TestInternSpeed
{
    final int REPS = 100;

    private TestInternSpeed() { }

    private int testInternNative(String[] strs)
    {
        int len = strs.length;
        int sum = 0;

        for (int i = 0; i < REPS; ++i) {
            for (int j = 0; j < len; ++j) {
                String orig = strs[i];
                String intern = orig.intern();
                if (orig != intern) {
                    ++sum;
                }
            }
        }
        return sum;
    }

    final static class Interner
    {
        final static HashMap mMap = new HashMap();

        public static String intern(String str) {
            HashMap m = mMap;
            synchronized (m) {
                String interned = (String) m.get(str);
                if (interned == null) {
                    interned = str.intern();
                    /* Just for testing; let's make sure Map won't be using
                     * cheap equality comparison... either can create new
                     * String or intern key
                     */
                    m.put(""+interned, interned);
                }
                return interned;
            }
        }
    }

    private int testInternEmulated(String[] strs)
    {
        int len = strs.length;
        int sum = 0;

        for (int i = 0; i < REPS; ++i) {
            for (int j = 0; j < len; ++j) {
                String orig = strs[i];
                String intern = Interner.intern(orig);
                if (orig != intern) {
                    ++sum;
                }
            }
        }
        return sum;
    }

    public void test(String[] strs1)
    {
        // First, let's create base String prefixes we use, unless
        // some were passed explicitly
        if (strs1 == null || strs1.length == 0) {
            strs1 = new String[] {
                "a", "foo", "bar", "123",
                "    xyz foobar ", "-12", "@@@!@!", "aaaaaa",
                "String", "", "WARMUP_ROUNDS", "@$#^(_^%_*)@",
                "9824", "214u89ujgikljlrtkjgh2eko34ktp43iy", "x", "yza"
            };
        }

        System.out.println("Got "+strs1.length+" input Strings.");

        // Then create 16 mutations for each (for total of 256)
        int len = strs1.length;
        String[] strs2 = new String[len * 16];
        for (int i = 0, ix = 0; i < len; ++i) {
            for (int j = 0; j < 16; ++j) {
                String nonInterned = strs1[i]+i;
                strs2[ix++] = nonInterned;
                String interned = nonInterned.intern();
                if (nonInterned == interned) {
                    throw new Error("Weird: interned and non-interned instances equal: should never happen!");
                }
            }
        }

        System.out.println("... which gets "+strs2.length+" test Strings.");

        int i = 0;
        int sum = 0;

        while (true) {
            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
            System.gc();

            long curr = System.currentTimeMillis();
            String msg;

            if ((++i & 1) == 0) {
                msg = "Intern, native";
                sum += testInternNative(strs2);
            } else {
                msg = "Intern, emulated";
                sum += testInternEmulated(strs2);
            }

            curr = System.currentTimeMillis() - curr;
            System.out.println("Test '"+msg+"' -> "+curr+" msecs ("
                               +(sum & 0xFF)+").");
        }
    }

    public static void main(String[] args) {
        new TestInternSpeed().test(args);
    }
}
