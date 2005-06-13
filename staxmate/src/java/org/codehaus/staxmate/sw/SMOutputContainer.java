package org.codehaus.staxmate.sw;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Intermediate abstract output class for StaxMate, which is used as the base
 * for all output nodes that can contain other nodes.
 * Both buffered and unbuffered classes exists, as well as root-level
 * and branch containers. All output by sub-classes is using by the underlying
 * {@link javax.xml.stream.XMLStreamWriter}, using the context
 * ({@link SMOutputContext}).
 *<p>
 * Whether writes are buffered or not generally depends on buffering states
 * of preceding nodes (elements, fragments), in document order: if an ancestor
 * (parent, grand-parent) or a preceding sibling is buffered, so is this
 * fragment, until all such nodes have been released.
 */
public abstract class SMOutputContainer
    extends SMLinkedOutput
{
    /**
     * Context of this node; defines things like the underlying stream
     * writer and known namespaces.
     */
    final SMOutputContext mContext;

    /**
     * Parent of this container; null for root-level entities, as well
     * as not-yet-linked buffered containers.
     */
    SMOutputContainer mParent = null;

    /**
     * First child node that has not yet been completely output to the
     * underlying stream. This may be due to 
     a blocking condition (parent blocked, children blocked,
     * or the child itself being buffered). May be null if no children
     * have been added, or if all have been completely output.
     */
    SMLinkedOutput mFirstChild = null;

    /**
     * Last child node that has not been output to the underlying stream.
     */
    SMLinkedOutput mLastChild = null;

    protected SMOutputContainer(SMOutputContext ctxt)
    {
        super();
        mContext = ctxt;
    }

    /*
    ///////////////////////////////////////////////////////////
    // Simple accessors/mutators
    ///////////////////////////////////////////////////////////
    */

    public final SMOutputContainer getParent() {
        return mParent;
    }


    public final SMOutputContext getContext() {
        return mContext;
    }

    /*
    /////////////////////////////////////////////////////
    // Properties/state
    /////////////////////////////////////////////////////
     */

    /**
     * Convenience method for getting namespace instance that
     * uniquely represents the specified URI (uniquely meaning
     * that for a given output context there are never more than
     * one instances for a given URI; which means that identity
     * comparison is enough to check for equality of two namespaces).
     * Calls {@link SMOutputContext} to find the actual namespace
     * instance.
     */
    public final SMNamespace getNamespace(String uri) {
        return mContext.getNamespace(uri);
    }

    /*
    ///////////////////////////////////////////////////////////
    // Output methods for simple nodes (no elements, attributes
    // or buffering)
    ///////////////////////////////////////////////////////////
    */

    public void addCharacters(String text)
        throws XMLStreamException
    {
        if (canOutputNewChild()) {
            mContext.writeCharacters(text);
        } else {
            linkNewChild(mContext.createCharacters(text));
        }
    }

    public void addCharacters(char[] buf, int offset, int len)
        throws XMLStreamException
    {
        if (canOutputNewChild()) {
            mContext.writeCharacters(buf, offset, len);
        } else {
            linkNewChild(mContext.createCharacters(buf, offset, len));
        }
    }

    public void addCData(String text)
        throws XMLStreamException
    {
        if (canOutputNewChild()) {
            mContext.writeCData(text);
        } else {
            linkNewChild(mContext.createCData(text));
        }
    }

    public void addCData(char[] buf, int offset, int len)
        throws XMLStreamException
    {
        if (canOutputNewChild()) {
            mContext.writeCData(buf, offset, len);
        } else {
            linkNewChild(mContext.createCData(buf, offset, len));
        }
    }

    public void addComment(String text)
        throws XMLStreamException
    {
        if (canOutputNewChild()) {
            mContext.writeComment(text);
        } else {
            linkNewChild(mContext.createComment(text));
        }
    }

    public void addEntityRef(String name)
        throws XMLStreamException
    {
        if (canOutputNewChild()) {
            mContext.writeEntityRef(name);
        } else {
            linkNewChild(mContext.createEntityRef(name));
        }
    }

    public void addProcessingInstruction(String target, String data)
        throws XMLStreamException
    {
        if (canOutputNewChild()) {
            mContext.writeProcessingInstruction(target, data);
        } else {
            linkNewChild(mContext.createProcessingInstruction(target, data));
        }
    }

    /*
    ////////////////////////////////////////////////////////
    // Output methods for Elements, attributes, buffered
    // fragments
    ////////////////////////////////////////////////////////
    */

    public SMOutputElement addElement(String localName, SMNamespace ns)
        throws XMLStreamException
    {
        final SMOutputContext ctxt = mContext;

        /* First, need to make sure namespace declaration is appropriate
         * for this context
         */
        if (ns == null) {
            ns = SMOutputContext.getEmptyNamespace();
            /* Hmmh. Callers should know better than to share namespace
             * instances... but then again, we can easily fix the problem
             * even if they are shared:
             */
        } else if (!ns.isValidIn(ctxt)) {
            /* Let's find instance from our current context, instead of the
             * one from some other context
             */
            ns = getNamespace(ns.getURI());
        }

        // Ok, let's see if we are blocked already
        boolean blocked = !canOutputNewChild();
        SMOutputElement newElem = new SMOutputElement(ctxt, localName, ns);
        linkNewChild(newElem);
        newElem.linkParent(this, blocked);

        return newElem;
    }
    
    public SMBufferable addBuffered(SMBufferable buffered)
        throws XMLStreamException
    {
        // Ok; first, let's see if we are blocked already
        boolean blocked = !canOutputNewChild();
        linkNewChild((SMLinkedOutput) buffered);
        buffered.linkParent(this, blocked);
        return buffered;
    }

    public SMBufferable addAndReleaseBuffered(SMBufferable buffered)
        throws XMLStreamException
    {
        addBuffered(buffered);
        buffered.release();
        return buffered;
    }

    /*
    ////////////////////////////////////////////////////////
    // Buffered fragment/element construction
    //
    // note: these methods add tight coupling to sub-classes...
    // while not really good, architecturally, these are
    // strongly dependant classes in any case, so let's not
    // get ulcer over such cyclic dependencies (just duly note
    // they are there)
    ////////////////////////////////////////////////////////
    */

    public SMBufferedFragment createBufferedFragment()
    {
        return new SMBufferedFragment(getContext());
    }

    public SMBufferedElement createBufferedElement(String localName, SMNamespace ns)
    {
        return new SMBufferedElement(getContext(), localName, ns);
    }

    /*
    ////////////////////////////////////////////////////////
    // Abstract methods from base classes
    ////////////////////////////////////////////////////////
    */

    protected abstract boolean doOutput(SMOutputContext ctxt, boolean canClose)
        throws XMLStreamException;

    protected abstract void forceOutput(SMOutputContext ctxt)
        throws XMLStreamException;

    /*
    ////////////////////////////////////////////////////////
    // New abstract methods
    ////////////////////////////////////////////////////////
    */

    /**
     * Method called by a child, when it is released and neither is or
     * contains any buffered entities. This should indicate that it
     * can be output unless one of its parents or preceding siblings
     * is buffered.
     * Container is expected to update its own
     * state, and then inform its own parent (if necesary) about release;
     * this may cascade output from parents up the container stack.
     *
     * @param child Child node that now neither is nor contains any buffered
     *    nodes.
     */
    protected abstract void childReleased(SMLinkedOutput child)
        throws XMLStreamException;
   
    /**
     * Method called to figure out if we can just output a newly added
     * child, without any buffering. It will request container to close
     * and output all non-buffered children it has, if any; and indicate
     * whether it was fully succesful or not.
     *
     * @return True if all children (if any) were completely output; false
     *   if there was at least one buffered child that couldn't be output.
     */
    public abstract boolean canOutputNewChild()
        throws XMLStreamException;

    /*
    ////////////////////////////////////////////////////////
    // Internal/package methods
    ////////////////////////////////////////////////////////
    */

    protected void linkNewChild(SMLinkedOutput n)
    {
        SMLinkedOutput last = mLastChild;
        if (last == null) {
            mLastChild = n;
            mFirstChild = n;
        } else {
            last.linkNext(n);
            mLastChild = n;
        }
    }

    /**
     * Method that will try to close and output all child nodes that
     * can be (ones that are not buffered), and returns true if that
     * succeeds; or false if there was at least one buffered descendant.
     *
     * @return True if all descendants (children, recursively) were
     *   succesfully output, possibly closing them first if necessary
     */
    protected final boolean closeAndOutputChildren()
        throws XMLStreamException
    {
        while (mFirstChild != null) {
            if (!doOutput(mContext, true)) {
                // Nope, node was buffered or had buffered child(ren)
                return false;
            }
            mFirstChild = mFirstChild.mNext;
        }
        mLastChild = null;
        return true;
    }

    /**
     * Method that will try to close and output all children except for
     * the last, and if that succeeds, output last child if it need
     * not be closed (true for non-element/simple children).
     */
    protected final boolean closeAllButLastChild()
        throws XMLStreamException
    {
        SMLinkedOutput child = mFirstChild;
        while (child != null) {
            SMLinkedOutput next = child.mNext;
            /* Need/can not force closing of the last child, but all
             * previous can and should be closed:
             */
            boolean notLast = (next != null);
            if (!mFirstChild.doOutput(mContext, notLast)) {
                // Nope, node was buffered or had buffered child(ren)
                return false;
            }
            mFirstChild = child = next;
        }
        mLastChild = null;
        return true;
    }

    protected final void forceChildOutput()
        throws XMLStreamException
    {
        SMLinkedOutput child = mFirstChild;
        mFirstChild = null;
        mLastChild = null;
        for (; child != null; child = child.mNext) {
            child.forceOutput(mContext);
        }
    }

    protected void throwClosed() {
        throw new IllegalStateException("Illegal call when container (of type "
                                        +getClass()+") was closed");
    }

    protected void throwRelinking() {
            throw new IllegalStateException("Can not re-set parent (for instance of "+getClass()+") once it has been set once");
    }

    protected void throwBuffered() {
        throw new IllegalStateException("Illegal call when container (of type "
                                        +getClass()+") is still buffered");
    }
}
