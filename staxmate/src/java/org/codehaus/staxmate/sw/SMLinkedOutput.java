package org.codehaus.staxmate.sw;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Basic building block for all outputtable nodes within StaxMate.
 * Used as the base for both "active" nodes (elements, fragments; both
 * buffered and unbuffered variations, entities that are created for
 * output scoping, ie. as output containers)
 * and those "passive" nodes that are blocked (ones for which instances
 * are only created when they can not be output right away).
 *<p>
 * Note that parent linkage is not included at this level since it is
 * really only needed for active nodes (output containers; all of them
 * since a non-bufferable container may still contain buffered containers).
 */
public abstract class SMLinkedOutput
{
    /**
     * Context of this node; defines things like the underlying stream
     * writer and known namespaces.
     */
    final SMOutputContext mContext;

    protected SMLinkedOutput mNext = null;

    protected SMLinkedOutput(SMOutputContext ctxt)
    {
	mContext = ctxt;
    }

    /*
    /////////////////////////////////////////////////////
    // Link handling
    /////////////////////////////////////////////////////
     */

    protected SMLinkedOutput getNext() {
	return mNext;
    }

    protected void linkNext(SMLinkedOutput next) {
	if (mNext != null) {
	    throw new IllegalStateException("Can not re-set next once it has been set once");
	}
	mNext = next;
    }

    /*
    /////////////////////////////////////////////////////
    // Properties/state
    /////////////////////////////////////////////////////
     */

    public final SMOutputContext getContext() {
	return mContext;
    }

    public final XMLStreamWriter getWriter() {
	/* ... could use accessor, but let's just make this easy for
	 * JIT/HotSpot (we can use package access from the same package)
	 */
	return mContext.mStreamWriter;
    }

    /*
    /////////////////////////////////////////////////////
    // Output handling
    /////////////////////////////////////////////////////
     */

    /**
     * Method called to request that the entity output itself; either
     * as much as it can without closing, or as much as it can if it is to
     * get closed. In both cases output can fail or be only a partial one:
     * buffered nodes will not be output at all, and nodes with buffered
     * children can only be partially output.
     *
     * @param canClose If true, indicates that the node can (and should)
     *   be fully closed if possible. Usually done when a new sibling
     *   is added after a node (element/fragment); if so, current one
     *   should be recursively closed
     *
     * @return True if the whole node could be output, ie. neither it nor
     *   its children are buffered.
     */
    protected abstract boolean doOutput(boolean canClose)
	throws XMLStreamException;

    /**
     * Method similar to {@link #doOutput}, except that this method will
     * always succeed in doing the output. Specifically, it will force all
     * buffered nodes to be unbuffered, and then output.
     */
    protected abstract void forceOutput()
	throws XMLStreamException;    
}
