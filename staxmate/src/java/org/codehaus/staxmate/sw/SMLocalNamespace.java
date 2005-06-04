package org.codehaus.staxmate.sw;

/**
 * Namespace that is local to a specific output context
 * ({@link SMOutputContext}), think of it as the document or sub-tree
 * StaxMate will output using a stream writer).
 */
public final class SMLocalNamespace
    extends SMNamespace
{
    /**
     * Output context in which this namespace is to be used (scope of
     * which it is bound)
     */
    protected final SMOutputContext mContext;

    /**
     * Prefererred (or suggested) prefix for the namespace;
     * StaxMate will try to use this prefix if possible when binding
     * namespaces and also passes it to the underlying stream writer.
     */
    protected final String mPrefPrefix;

    /**
     * Prefix
     */
    protected String mCurrPrefix = null;

    /**
     * @param ctxt Output context that "owns" this namespace (within which
     *    namespace will be bound when output)
     * @param uri URI that defines identity of the namespace
     * @param prefPrefix Prefererred (or suggested) prefix for the namespace;
     *   StaxMate will try to use this prefix if possible when binding
     *   namespaces and also passes it to the underlying stream writer.
     */
    protected SMLocalNamespace(SMOutputContext ctxt,
			       String uri, String prefPrefix)
    {
	super(uri);
	mContext = ctxt;
	mPrefPrefix = prefPrefix;
    }

    /*
    ///////////////////////////////////
    // Abstract method implementations
    ///////////////////////////////////
     */

    public String getPreferredPrefix() {
	return mPrefPrefix;
    }
    
    protected boolean isValidIn(SMOutputContext ctxt) {
	return ctxt == mContext;
    }

    /**
     * The only trick with regard to binding/unbinding of local namespaces
     * is that "re-binding" is not allowed (by StaxMate design; XML would
     * allow it). So let's allow transitions to and from null, but not
     * between two non-empty prefixes.
     */
    protected void bindAs(String prefix) {
	if (prefix != null) {
	    if (mCurrPrefix != null) {
		/* Let's not bother checking for equality -- any calls to re-bind
		 * are errors in implementation, and are never called by the
		 * end application
		 */
		throw new IllegalStateException("Trying to re-bind URI '"+mURI
						+"', from prefix '"+mCurrPrefix
						+"' to prefix '"+prefix+"'");
	    }
	}
    }
}
