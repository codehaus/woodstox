package com.ctc.wstx.util;

public final class StringUtil
{
    static String sLF = null;

    public static String getLF()
    {
        String lf = sLF;
        if (lf == null) {
            try {
                lf = (String) System.getProperty("line.separator");
                sLF = (lf == null) ? "\n" : lf;
            } catch (Throwable t) {
                // Doh.... whatever; most likely SecurityException
                sLF = lf = "\n";
            }
        }
        return lf;
    }

    public static void appendLF(StringBuffer sb) {
        sb.append(getLF());
    }
}
