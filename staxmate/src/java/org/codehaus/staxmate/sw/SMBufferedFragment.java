package org.codehaus.staxmate.sw;

import javax.xml.stream.XMLStreamException;

/**
 * Buffered fragment; starts its life buffered,
 * so that its content are not automatically written to the underlying
 * stream, but only when buffered instance is released. Once released,
 * can not be buffered again.
 */
public final class SMBufferedFragment
    extends SMOutputContainer
    implements SMBufferable
{
    /**
     * All instances are initially buffered; state will be changed when
     * instance is released.
     */
    protected boolean mIsBuffered = true;

    protected SMBufferedFragment(SMOutputContext ctxt)
    {
	super(ctxt, null);
    }

    /*
    ///////////////////////////////////////////////////////////
    // SMBufferable implementation
    ///////////////////////////////////////////////////////////
     */

    public boolean isBuffered() {
	return mIsBuffered;
    }

    public void linkParent(SMOutputContainer parent) {
	if (mParent != null) {
	    throw new IllegalStateException("Can not re-set parent once it has been set once");
	}
	mParent = parent;
    }

    public void release()
	throws XMLStreamException
    {
	mIsBuffered = false;
	if (mParent != null) {
	    mParent.childReleased(this);
	}
    }

    /*
    ///////////////////////////////////////////////////////////
    // Abstract method implementations
    ///////////////////////////////////////////////////////////
     */

    protected void childReleased(SMLinkedOutput child)
	throws XMLStreamException
    {
	// First, if we are buffered, no need to do anything more...
	if (mIsBuffered) {
	    return;
	}
	/* Otherwise, the only significant child is the first one, as it's
	 * the only one that may have blocked output:
	 */
	if (child == mFirstChild) {
	    // If so, parent can (and should) deal with it... if we have one
	    if (mParent != null) {
		mParent.childReleased(this);
	    }
	}
    }

    protected boolean doOutput(boolean canClose)
	throws XMLStreamException
    {
	// No outputting if still buffered...
	if (mIsBuffered) {
	    return false;
	}
	if (canClose) {
	    return closeAndOutputChildren();
	}
	return closeAllButLastChild();
    }

    protected void forceOutput()
	throws XMLStreamException
    {
	mIsBuffered = false;
	forceChildOutput();
    }

    public boolean canOutputNewChild()
	throws XMLStreamException
    {
	// Can not just output if we are buffered...
	if (mIsBuffered) {
	    return false;
	}
	return closeAndOutputChildren();
    }
}
