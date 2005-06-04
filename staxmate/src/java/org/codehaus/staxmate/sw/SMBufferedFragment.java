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
    /* These are the distinct states:
     *
     * BUFFERED_AND_BLOCKED: initial state, where output is blocked both
     *   by this fragment being buffered, AND parent being blocked by
     *   some other buffering.
     * BUFFERED: initial state, where output is only blocked due to the
     *   fragment itself being buffered.
     * BLOCKED: optional state; will be moved here if fragment is unbuffered
     *   but parent has not yet indicate it is ready to be unblocked
     * OPEN: state in which we can freely output children
     * CLOSED: state during which no children can be added any more
     */
    protected final static int STATE_BUFFERED_AND_BLOCKED = 1;
    protected final static int STATE_BUFFERED = 2;
    protected final static int STATE_BLOCKED = 3;
    protected final static int STATE_OPEN = 4;
    protected final static int STATE_CLOSED = 5;

    protected final static int LAST_BUFFERED = STATE_BUFFERED;
    protected final static int LAST_BLOCKED = STATE_BLOCKED;

    /**
     * All instances are initially buffered; state will be changed when
     * instance is released.
     */
    protected int mState = STATE_BUFFERED;

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
        return (mState <= LAST_BUFFERED);
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
        //mIsBuffered = false;
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
        if (mState <= LAST_BLOCKED) {
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
        if (mState <= LAST_BLOCKED) {
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
        //mIsBuffered = false;
        forceChildOutput();
    }

    public boolean canOutputNewChild()
        throws XMLStreamException
    {
        // Can not just output if we are buffered...
        if (mState <= LAST_BLOCKED) {
            return false;
        }
        return closeAndOutputChildren();
    }
}
