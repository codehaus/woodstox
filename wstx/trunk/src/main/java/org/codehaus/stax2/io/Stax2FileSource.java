package org.codehaus.stax2.io;

import java.io.*;
import java.net.URL;

/**
 * Simple implementation of {@link Stax2ReferentialSource}, which refers
 * to the specific file.
 */
public class Stax2FileSource
    extends Stax2ReferentialSource
{
    final File mFile;

    public Stax2FileSource(File f) {
        mFile = f;
    }

    /*
    /////////////////////////////////////////
    // Implementation of the Public API
    /////////////////////////////////////////
     */

    /**
     * @return URL that refers to the reference resource, for the purposes
     *   of resolving a relative reference from content read from the
     *   resource.
     */
    public URL getReference()
    {
        /* 01-Feb-2009, TSa: Changed between Stax2 v3 and v4
         *   to first convert to URI, then to URL (needed JDK 1.5)
         *   to ensure that characters in the
         *   filename are properly quoted
         */
        try {
            return mFile.toURI().toURL();
        } catch (java.net.MalformedURLException e) {
            /* Hmmh. Signature doesn't allow IOException to be thrown. So,
             * let's use something close enough; this should not occur
             * often in practice.
             */
            throw new IllegalArgumentException("(was "+e.getClass()+") Could not convert File '"+mFile.getPath()+"' to URL: "+e);
        }
    }

    public Reader constructReader()
        throws IOException
    {
        String enc = getEncoding();
        if (enc != null && enc.length() > 0) {
            return new InputStreamReader(constructInputStream(), enc);
        }
        // Sub-optimal; really shouldn't use the platform default encoding
        return new FileReader(mFile);
    }

    public InputStream constructInputStream()
        throws IOException
    {
        return new FileInputStream(mFile);
    }

    /*
    /////////////////////////////////////////
    // Additional API for this source
    /////////////////////////////////////////
     */

    public File getFile() {
        return mFile;
    }
}
