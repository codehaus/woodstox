package org.codehaus.staxmate.sw;

import javax.xml.stream.XMLStreamException;

/**
 * Buffered version of {@link SMOutputElement}; starts its life buffered,
 * so that it, its attributes and content are not automatically written to the
 * underlying stream, but only when buffered instance is released.
 */
public final class SMBufferedElement
    extends SMOutputElement
    implements SMBufferable
{
    /**
     * All instances are initially buffered; state will be changed when
     * instance is released.
     */
    protected boolean mIsBuffered = true;

    /**
     *<p>
     * Note: although signature indicates we could throw an exception,
     * this sub-class never does. But since super-class in itself could,
     * we have to declare it as potentially being thrown.
     */
    protected SMBufferedElement(SMOutputContext ctxt,
                                String localName, SMNamespace ns)
	throws XMLStreamException
    {
        super(ctxt, null, localName, ns, true);
    }

    /*
    ///////////////////////////////////////////////////////////
    // SMBufferable implementation
    ///////////////////////////////////////////////////////////
     */

    public boolean isBuffered() {
        return mIsBuffered;
    }

    public void linkParent(SMOutputContainer parent, boolean blocked)
    {
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
	// !!! TBI
    }

    protected boolean doOutput(boolean canClose)
	throws XMLStreamException
    {
	// Let's first do a sanity check:
	if (mIsBuffered) {
	    throw new IllegalStateException("Shouldn't call doOutput() for still buffered element");
	}

	// !!! TBI
	return false;
    }
}
