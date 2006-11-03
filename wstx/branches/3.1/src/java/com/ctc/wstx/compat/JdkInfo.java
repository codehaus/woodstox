package com.ctc.wstx.compat;

import java.lang.reflect.*;

/**
 * Simple accessor class that can access JDK version information. Separate
 * from JdkFeatures so that former can call static methods defined in this
 * class, from its own static initializers.
 */
public class JdkInfo
{
    final static int JDK_INFO_UNKNOWN = 0;

    /**
     * @return Simple int value for version, from 101 to 999 representing
     *   theoretical version numbers from 1.0.1 to 99.9.9; or 0 to indicate
     *   no version information could be gathered.
     */
    public static int getJDKVersion()
    {
        int id = JDK_INFO_UNKNOWN;
        try {
            // Do we have a specs?
            id = findVersion("java.specification.version");
            if (id == JDK_INFO_UNKNOWN) {
                // If not, maybe VM version is known:
                findVersion("java.vm.version");
                if (id == JDK_INFO_UNKNOWN) {
                    // If not, maybe generic Java version is?
                    findVersion("java.version");
                }
            }
            return id;
        } catch (Throwable t) {
            // Where's logging when you need it...
            System.err.println("Problems trying to access System properties: "+t);
        }
        return id;
    }

    /**
     * Really crude class-loading functionality; just tries to create
     * JDK wrapper instance for given version.
     */
    public static JdkImpl constructImpl(int version)
        throws Exception
    {
        Class cls = Class.forName("com.ctc.wstx.compat.Jdk"+version+"Impl");
        return (JdkImpl) cls.newInstance();
    }

    /*
    /////////////////////////////////////////////////////////
    // Internal methods
    /////////////////////////////////////////////////////////
     */

    private static int findVersion(String propId)
    {
        String str = System.getProperty(propId);
        if (str == null || str.length() < 3) {
            return JDK_INFO_UNKNOWN;
        }
        int ix = str.indexOf('.');
        if (ix < 1) {
            return JDK_INFO_UNKNOWN;
        }
        int major;
        try {
            major = Integer.parseInt(str.substring(0, ix));
        } catch (NumberFormatException nex) {
            return JDK_INFO_UNKNOWN;
        }
        if (major < 1 || major > 99) {
            return JDK_INFO_UNKNOWN;
        }
        // Ok how about minor versions?
        str = str.substring(ix+1);
        int len = str.length();
        // Need to have at least one more digit
        if (len < 1 || !(str.charAt(0) >= '0' && str.charAt(0) <= '9')) {
            return JDK_INFO_UNKNOWN;
        }
        int med = 0;
        int i = 0;

        for (; i < len; ++i) {
            char c = str.charAt(i);
            if (c >= '0' && c <= '9') {
                med = (med * 10) + (c - '0');
            }
        }

        /* !!! For now, let's only worry about next number; minor revision
         *   does not really affect API as of now. If necessary, can be
         *   improved to provide more accuracy.
         */

        if (med > 9) { // sanity check; should never really happen...
            return JDK_INFO_UNKNOWN;
        }

        // Ok, think we got it?
        return (major * 100) + med;
    }
}

    
