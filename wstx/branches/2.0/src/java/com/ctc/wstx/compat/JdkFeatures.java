package com.ctc.wstx.compat;

/**
 * This is the class that is used to access JDK-version dependant
 * features. This is done by asking for instance of
 * {@link JdkImpl}; returned implementation class knows how current JDK
 * implements functionality needed.
 */
public class JdkFeatures
{
    private final static JdkImpl sInstance;

    private final static int sVersion;

    static {
        /* First, let's try to figure out which JDK we have, based on
         * System properties. If that succeeds, we should be able to start
         * with the correct one right away.
         */
        int version = JdkInfo.getJDKVersion();
        sVersion = version;

        /* Ok, maybe we got the version to start looking from, maybe not.
         * In any case, we'd prefer to use latest and greatest implementation.
         */

        JdkImpl x = null;
        StringBuffer sb = null;

        if (version >= 104 || version == 0) { // 1.4+ or unknown
            try {
                x = JdkInfo.constructImpl(14);
            } catch (Throwable t) {
                sb = new StringBuffer(200);
                sb.append("Failed to load 1.4 features: ");
                sb.append(t.toString());
            }
        }
        if (x == null &&
            (version >= 103 || version == 0)) { // 1.4+ or unknown
            try {
                x = JdkInfo.constructImpl(13);
            } catch (Throwable t) {
                sb.append("Failed to load 1.3 features: ");
                sb.append(t.toString());
            }
        }
        /* If all else fails, let's still try loading 1.2, even if version
         * info claims that's not available. Who knows, maybe info is
         * garbage.
         */
        if (x == null) {
            try {
                x = JdkInfo.constructImpl(12);
            } catch (Throwable t) {
                sb.append("Failed to load 1.2 features: ");
                sb.append(t.toString());
            }
        }

        sInstance = x;

        // Would be nice to have logging but...
        if (x == null) {
            System.err.println("Error: Could not load JDK-dependant features (estimated version id "+version+"), problems:\n"+sb+"\n");
        }
    }

    public static JdkImpl getInstance()
    {
        if (sInstance == null) {
            throw new Error("Internal error: No JDK implementation wrapper class available (version "+sVersion+"; need at least 0102 [== JDK 1.2.x]).");
        }
        return sInstance;
    }

    /**
     * Simple test driver; not usable as a unit test, most likely, but can
     * be used for quick diagnostics.
     */
    public static void main(String[] args) {
        System.out.println("java.version: "+System.getProperty("java.version"));
        System.out.println("java.vm.version: "+System.getProperty("java.vm.version"));
        System.out.println("java.specification.version: "+System.getProperty("java.specification.version"));

        System.out.println();
        System.out.println("Version info determined: "+sVersion+".");
        System.out.println("Thus, loaded implementation is: "+sInstance);
    }
}

