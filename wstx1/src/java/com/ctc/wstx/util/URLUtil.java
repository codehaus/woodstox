package com.ctc.wstx.util;

import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;

import com.ctc.wstx.compat.JdkFeatures;

public final class URLUtil
{
    private URLUtil() { }

    /**
   * Method that tries to figure out how to create valid URL from a system
   * id, without additional contextual information.
   * If we could use URIs this might be easier to do, but they are part
   * of JDK 1.4, and preferably code should only require 1.2 (or maybe 1.3)
   */
    public static URL urlFromSystemId(String sysId)
        throws IOException
    {
        try {
            /* Ok, does it look like a full URL? For one, you need a colon. Also,
             * to reduce likelihood of collision with Windows paths, let's only
             * accept it if there are 3 preceding other chars...
             * Not sure if Mac might be a problem? (it uses ':' as file path
             * separator, alas, at least prior to MacOS X)
             */
            int ix = sysId.indexOf(':');
            /* Also, protocols are generally fairly short, usually 3 or 4
             * chars (http, ftp, urn); so let's put upper limit of 8 chars too
             */
            if (ix >= 3 && ix <= 8) {
                return new URL(sysId);
            }
            // Ok, let's just assume it's local file reference...
            return new java.io.File(sysId).toURL();
        } catch (MalformedURLException e) {
            throwIOException(e, sysId);
            return null; // never gets here
        }
    }

    public static URL urlFromSystemId(String sysId, URL ctxt)
        throws IOException
    {
        if (ctxt == null) {
            return urlFromSystemId(sysId);
        }
        try {
            return new URL(ctxt, sysId);
        } catch (MalformedURLException e) {
            throwIOException(e, sysId);
            return null; // never gets here
        }
    }

    /*
    ///////////////////////////////////////////
    // Private helper methods
    ///////////////////////////////////////////
    */

    /**
     * Helper method that tries to fully convert strange URL-specific exception
     * to more general IO exception. Also, to try to use JDK 1.4 feature without
     * creating requirement, uses reflection to try to set the root cause, if
     * we are running on JDK1.4
     */
    private static void throwIOException(MalformedURLException mex, String sysId)
        throws IOException
    {
        IOException ie = new IOException("[resolving systemId '"+sysId
                                         +"']: "+mex.toString());
        JdkFeatures.getInstance().setInitCause(ie, mex);
        throw ie;
    }
}
