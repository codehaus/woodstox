package org.codehaus.staxmate.sw;

/**
 * Abstract base class for all namespace objects (local and global ones).
 */
public abstract class SMNamespace
{
    /**
     * URI of the actual namespace this class encapsulates
     */
    protected final String mURI;

    protected SMNamespace(String uri)
    {
	mURI = uri;
    }

    /*
    ///////////////////////////////////
    // Accessors
    ///////////////////////////////////
     */

    public final String getURI() {
	return mURI;
    }

    /**
     * @return Prefix that the caller application has at some point
     *  indicated to be a prefix it'd like to see; StaxMate may try to
     *  use it as the prefix to bind if there are no
     */
    public abstract String getPreferredPrefix();

    /*
    ///////////////////////////////////
    // Internal API
    ///////////////////////////////////
     */

    /**
     * Method used to verify that the namespace is actually valid within
     * the specified output context.
     */
    protected abstract boolean isValidIn(SMOutputContext ctxt);

    /**
     * Method called to update (explicit) prefix used with this
     * namespace in the current output location within the owning
     * output context.
     */
    protected abstract void bindAs(String prefix);

}
