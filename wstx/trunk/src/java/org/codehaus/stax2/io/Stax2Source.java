package org.codehaus2.stax2.io;

import java.io.*;

import javax.xml.transform.Source;

/**
 * This is the base class for additional input sources (implementations
 * of {@link javax.xml.transform.Source}) that Stax2
 * {@link XMLInputFactory2} implementations should support.
 */
public abstract class Stax2Source
    implements Source
{
    protected String mPublicId;
    protected String mEncoding;

    protected Stax2Source() { }

    /*
    /////////////////////////////////////////
    // Public API, simple accessors/mutators
    /////////////////////////////////////////
     */

    public String getPublicId() {
        return mPublicId;
    }

    public void setPublicId(String id) {
        mPublicId = id;
    }

    public String getEncoding() {
        return mEncoding;
    }

    public void setEncoding(String enc) {
        mEncoding = enc;
    }

    /**
     * @return Absolute URI that can be used to resolve references
     *   originating from the content read via this source; may be
     *   null if not known (which is the case for most non-referential
     *   sources)
     */
    public abstract String getBaseUri();

    /*
    ///////////////////////////////////////////
    // Public API, convenience factory methods
    ///////////////////////////////////////////
     */

    /**
     * This method creates a {@link Reader} via which underlying input
     * source can be accessed. Note that caller is responsible for
     * closing that Reader when it is done reading it.
     */
    public abstract Reader constructReader()
        throws IOException;

    /**
     * This method creates an {@link InputSource} via which underlying input
     * source can be accessed. Note that caller is responsible for
     * closing that InputSource when it is done reading it
     */
    public abstract InputStream constructInputStream()
        throws IOException;
}

