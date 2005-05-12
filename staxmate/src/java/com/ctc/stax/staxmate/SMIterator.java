package com.ctc.stax.staxmate;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Base class for iterators for StaxMate.
 *
 * @author Tatu Saloranta
 */
public abstract class SMIterator
{
    /*
    ////////////////////////////////////////////
    // Constants
    ////////////////////////////////////////////
     */

    // // // Additional dummy event value(s)

    /**
     * Constant that indicates that iterator has no more nodes to iterate
     * over
     */
    protected final static int SM_NODE_NONE = -1;

    // // // Constants for element tracking:

    /**
     * Value that indicates that no element state information should
     * be tracked. This means that {@link #getTrackedElement} will always
     * return null for this element, as well as that if immediate child
     * iterators do have tracking enabled, element states it saves have
     * no parent element information available.
     */
    public final static int TRACK_ELEM_NONE = 0;

    /**
     * Value that indicates that basic element state information should
     * be tracked, including linkage to the parent element (but only
     * if the parent iterator was tracking elements).
     * This means that {@link #getTrackedElement} will return non-null
     * values, as soon as this iterator has been advanced over its first
     * element node. However, element will return null from its
     * {@link SMElementInfo#getPreviousSibling} since sibling information
     * is not tracked.
     */
    public final static int TRACK_ELEM_PARENTS = 1;

    /**
     * Value that indicates full element state information should
     * be tracked for all "visible" elements: visible meaning that element
     * node was accepted by the filter this iterator uses.
     * This means that {@link #getTrackedElement} will return non-null
     * values, as soon as this iterator has been advanced over its first
     * element node, and that element will return non-null from its
     * {@link SMElementInfo#getPreviousSibling} unless it's the first element
     * iterated by this iterator.
     */
    public final static int TRACK_ELEM_VISIBLE_SIBLINGS = 2;

    /**
     * Value that indicates full element state information should
     * be tracked for ALL elements (including ones not visible to the
     * caller via {@link #getNext} method).
     * This means that {@link #getTrackedElement} will return non-null
     * values, as soon as this iterator has been advanced over its first
     * element node, and that element will return non-null from its
     * {@link SMElementInfo#getPreviousSibling} unless it's the first element
     * iterated by this iterator.
     */
    public final static int TRACK_ELEM_ALL_SIBLINGS = 3;


    // // // Constants for the iterator state

    /**
     * Initial means that the iterator has been constructed, but hasn't
     * yet been advanced. No data can be accessed yet, but the iterator
     * can be advanced.
     */
    protected final static int STATE_INITIAL = 0;

    /**
     * Active means that iterator's data is valid and can be accessed;
     * plus it can be advanced as well.
     */
    protected final static int STATE_ACTIVE = 1;

    /**
     * Status that indicates that although iterator would be active, there
     * is a child iterator active which means that this iterator can not
     * be used to access data: only the innermost child iterator can.
     * It can still be advanced, however.
     */
    protected final static int STATE_HAS_CHILD = 2;

    /**
     * Closed iterators are ones that do not point to accessible data, nor
     * can be advanced any further.
     */
    protected final static int STATE_CLOSED = 3;

    protected final static String[] STATE_DESCS = new String[] {
        "[INITIAL]", "[ACTIVE]", "[HAS_CHILD]", "[CLOSED]"
    };

    /*
    ////////////////////////////////////////////
    // Configuration
    ////////////////////////////////////////////
     */

    protected final XMLStreamReader mStreamReader;

    /**
     * Optional filter object that can be used to filter out events of
     * types caller is not interested in.
     */
    protected SMFilter mFilter = null;

    /**
     * Whether element information is to be tracked or not, and if it is,
     * how much of it will be stored. See <code>TRACK_ELEM_xxx</code>
     * constants (like {@link #TRACK_ELEM_NONE}
     * {@link #TRACK_ELEM_PARENTS}.
     */
    protected int mElemTracking;

    /**
     * Optional factory instance that is used to create element info
     * objects if element tracking is enabled. If null, will use default
     * generation mechanism, implemented by SMIterator itself.
     *<p>
     * Note that by default, this factory will be passed down to child
     * and descendant iterators this iterator creates, so usually one
     * only needs to set the factory of the root iterator.
     */
    protected ElementInfoFactory mElemInfoFactory;

    /*
    ////////////////////////////////////////////
    // Iteration state
    ////////////////////////////////////////////
     */

    protected int mCurrEvent = SM_NODE_NONE;

    protected int mState = STATE_INITIAL;

    /**
     * Number of nodes iterated over by this iterator, including the
     * current one.
     */
    protected int mNodeCount = 0;

    /**
     * Number of start elements iterated over by this iterator, including the
     * current one.
     */
    protected int mElemCount = 0;

    /**
     * Number of enclosing start elements for this iterator; 0 for root
     * iterators
     */
    protected final int mParentCount;

    /**
     * Element that was last "tracked"; element over which iterator was
     * moved, and of which state has been saved for further use. At this
     * point, it can be null if no elements have yet been iterater over.
     * Alternatively, if it's not null, it may be currently pointed to
     * or not; if it's not, either child iterator is active, or this
     * iterator points to a non-start-element node.
     */
    protected SMElementInfo mTrackedElement = null;

    /**
     * Element parent iterator tracked when this iterator was created,
     * if any.
     */
    protected SMElementInfo mParentTrackedElement = null;

    /**
     * Iterator that has been opened for iterating child nodes of the
     * start element node this iterator points to. Needed to keep
     * iterator hierarchy synchronized, independent of which ones are
     * traversed.
     */
    protected SMIterator mChildIterator = null;

    /*
    ////////////////////////////////////////////
    // Additional data
    ////////////////////////////////////////////
     */

    /**
     * Non-typesafe payload data that applications can use, to pass
     * an extra argument along with iterators. Not used by the framework
     * itself for anything.
     */
    protected Object mData;

    /*
    ////////////////////////////////////////////
    // Life cycle, configuration
    ////////////////////////////////////////////
     */

    public SMIterator(SMIterator parent, XMLStreamReader sr, SMFilter filter)
    {
        mStreamReader = sr;
        mFilter = filter;
        /* By default, we use parent iterator's element tracking setting;
         * or "no tracking" if we have no parent
         */
        if (parent == null) {
            mElemTracking = TRACK_ELEM_NONE;
            mParentTrackedElement = null;
            mParentCount = 0;
            mElemInfoFactory = null;
        } else {
            mElemTracking = parent.getElementTracking();
            mParentTrackedElement = parent.getTrackedElement();
            mParentCount = parent.getDepth() + 1;
            mElemInfoFactory = parent.getElementInfoFactory();
        }
    }

    public final void setFilter(SMFilter f) {
        mFilter = f;
    }

    /**
     * Changes tracking mode of this iterator to the new specified
     * mode. Default mode for iterators is the one their parent uses;
     * {@link #TRACK_ELEM_NONE} for root iterators with no parent.
     */
    public final void setElementTracking(int tracking) {
        mElemTracking = tracking;
    }

    public final int getElementTracking() {
        return mElemTracking;
    }

    public final void setElementInfoFactory(ElementInfoFactory f) {
        mElemInfoFactory = f;
    }

    public final ElementInfoFactory getElementInfoFactory() {
        return mElemInfoFactory;
    }

    /*
    ///////////////////////////////////////////////////
    // Public API, accessing iterator state information
    ///////////////////////////////////////////////////
     */

    /**
     * @return Number of nodes iterator has traversed (including ones
     *   filtered out). Starts with 0, and is incremented each time
     *   underlying stream reader's {@link XMLStreamReader#next} method
     *   is called, but not counting child iterators' node counts.
     */
    public int getNodeCount() {
        return mNodeCount;
    }

    /**
     * @return Number of start elements iterator has traversed (including ones
     *   filtered out). Starts with 0, and is incremented each time
     *   underlying stream reader's {@link XMLStreamReader#next} method
     *   is called and has moved over a start element, but not counting
     *   child iterators' element counts.
     */
    public int getElementCount() {
        return mNodeCount;
    }

    /**
     * Returns number of parent elements current node (or, the last
     * node iterator pointed to, or in absence of one [before iterator
     * has been advanced for the first time], it would point if advanced)
     * has.
     *<p>
     * For example, here are expected results for <code>getDepth()</code>
     * for an example XML document:
     *<pre>
     *  &lt;!-- Comment outside tree --> [0]
     *  &lt;root> [0]
     *    Text [1]
     *    &lt;branch> [1]
     *      Inner text [2]
     *      &lt;child /> [2]
     *    &lt;/branch> [1]
     *  &lt;/root> [0]
     *</pre>
     * Numbers in bracket are depths that would be returned when the
     * iterator points to the node.
     *<p>
     * Note: depths are different from what some APIs (such as XmlPull)
     * return.
     *
     * @return Number of enclosing nesting levels, ie. number of parent
     *   start elements for the node that iterator currently points to (or,
     *   in case of initial state, that it will point to if scope has
     *   node(s)).
     */
    public abstract int getDepth();

    /**
     * @return Type of event this iterator points to (if it currently points
     *   to one), or last pointed to (if not).
     */
    public int getCurrentEventType() {
        return mCurrEvent;
    }

    /*
    ////////////////////////////////////////////
    // Public API, accessing tracked elements
    ////////////////////////////////////////////
     */

    /**
     * @return Information about last "tracked" element; element we have
     *    last iterated over when tracking has been enabled.
     */
    public SMElementInfo getTrackedElement() {
        return mTrackedElement;
    }

    /**
     * @return Information about the tracked element the parent iterator
     *    had, if parent iterator existed and was tracking element
     *    information.
     */
    public SMElementInfo getParentTrackedElement() {
        return mParentTrackedElement;
    }

    /*
    ////////////////////////////////////////////
    // Public API, accessing data via reader
    ////////////////////////////////////////////
     */

    public boolean readerAccessible() {
        return (mState == STATE_ACTIVE);
    }

    public boolean isCurrentText()
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            return false;
        }
        return mStreamReader.hasText();
    }

    public boolean hasCurrentName()
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            return false;
        }
        return mStreamReader.hasName();
    }

    /**
     * Method that can be used to get direct access to the underlying
     * stream reader. This is usually needed to access some of less
     * often needed accessors for which there is no convenience method
     * in StaxMate API.
     *
     * @return Stream reader the iterator uses for getting XML events
     */
    public XMLStreamReader getStreamReader() {
        return mStreamReader;
    }

    public String getCurrentText()
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            return notAccessible("getCurrentText");
        }
        return mStreamReader.getText();
    }

    public QName getCurrentName()
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            notAccessible("getCurrentName");
            return null; // probably never gets here
        }
        return mStreamReader.getName();
    }

    public String getCurrentLocalName()
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            return notAccessible("getCurrentLocalName");
        }
        return mStreamReader.getLocalName();
    }

    public String getCurrentPrefix()
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            return notAccessible("getCurrentPrefix");
        }
        return mStreamReader.getPrefix();
    }

    public String getCurrentNamespaceURI()
        throws XMLStreamException
    {
        if (!readerAccessible()) {
            return notAccessible("getCurrentNamespaceURI");
        }
        return mStreamReader.getNamespaceURI();
    }

    /*
    ////////////////////////////////////////////////
    // Public API, accessing extra application data
    ////////////////////////////////////////////////
     */

    public Object getData() {
        return mData;
    }

    public void setData(Object o) {
        mData = o;
    }

    /*
    ////////////////////////////////////////////
    // Public API, iteration
    ////////////////////////////////////////////
     */

    /**
     * Main iterating method.
     *
     * @return Type of event (from {@link XMLStreamConstants}, such as
     *   {@link XMLStreamConstants#START_ELEMENT}, if a new node was
     *   iterated over; {@link #SM_NODE_NONE} when there are no more
     *   nodes this iterator can iterate over.
     */
    public abstract int getNext()
        throws XMLStreamException;

    /**
     * Method that will create a new nested iterator for iterating
     * over all (immediate) child nodes of the start element this iterator
     * currently points to.
     * If iterator does not point to a start element,
     * it will throw {@link IllegalStateException}; if it does not support
     * concept of child iterators, it will throw
     * {@link UnsupportedOperationException}
     *
     * @param f Filter child iterator is to use for filtering out
     *    'unwanted' nodes; may be null for no filtering
     *
     * @throws IllegalStateException If iterator can not be created due
     *   to the state iterator is in.
     * @throws UnsupportedOperationException If iterator does not allow
     *   creation of child iterators.
     */
    public SMIterator childIterator(SMFilter f)
        throws XMLStreamException
    {
        if (mState != STATE_ACTIVE) {
            if (mState == STATE_HAS_CHILD) {
                throw new IllegalStateException("Child iterator already requested.");
            }
            throw new IllegalStateException("Can not iterate children: iterator does not point to a start element (state "+getStateDesc()+")");
        }
        if (mCurrEvent != XMLStreamConstants.START_ELEMENT) {
            throw new IllegalStateException("Can not iterate children: iterator does not point to a start element (pointing to "+mCurrEvent+")");
        }

        mChildIterator = constructChildIterator(f);
        mState = STATE_HAS_CHILD;
        return mChildIterator;
    }

    public final SMIterator childIterator()
        throws XMLStreamException
    {
        return childIterator(null);
    }

    /**
     * Method that will create a new nested iterator for iterating
     * over all the descendant (children and grandchildren) nodes of
     * the start element this iterator currently points to.
     * If iterator does not point to a start element,
     * it will throw {@link IllegalStateException}; if it does not support
     * concept of descendant iterators, it will throw
     * {@link UnsupportedOperationException}
     *
     * @throws IllegalStateException If iterator can not be created due
     *   to the state iterator is in (or for some iterators, if they never
     *   allow creating such iterators)
     * @throws UnsupportedOperationException If iterator does not allow
     *   creation of descendant iterators.
     */
    public SMIterator descendantIterator(SMFilter f)
        throws XMLStreamException
    {
        if (mState != STATE_ACTIVE) {
            if (mState == STATE_HAS_CHILD) {
                throw new IllegalStateException("Child iterator already requested.");
            }
            throw new IllegalStateException("Can not iterate children: iterator does not point to a start element (state "+getStateDesc()+")");
        }
        if (mCurrEvent != XMLStreamConstants.START_ELEMENT) {
            throw new IllegalStateException("Can not iterate children: iterator does not point to a start element (pointing to "+mCurrEvent+")");
        }

        mChildIterator = constructDescendantIterator(f);
        mState = STATE_HAS_CHILD;
        return mChildIterator;
    }

    public final SMIterator descendantIterator()
        throws XMLStreamException
    {
        return descendantIterator(null);
    }

    /**
     * Convenience method; equivalent to 
     *<code>childIterator(SMFilterFactory.getElementOnlyFilter());</code>
     */
    public final SMIterator childElementIterator()
        throws XMLStreamException
    {
        return childIterator(SMFilterFactory.getElementOnlyFilter());
    }

    /**
     * Convenience method; equivalent to 
     *<code>descendantIterator(SMFilterFactory.getElementOnlyFilter());</code>
     */
    public final SMIterator descendantElementIterator()
        throws XMLStreamException
    {
        return descendantIterator(SMFilterFactory.getElementOnlyFilter());
    }

    /**
     * Convenience method; equivalent to 
     *<code>childIterator(SMFilterFactory.getMixedFilter());</code>
     */
    public final SMIterator childMixedIterator()
        throws XMLStreamException
    {
        return childIterator(SMFilterFactory.getMixedFilter());
    }

    /**
     * Convenience method; equivalent to 
     *<code>descendantIterator(SMFilterFactory.getMixedFilter());</code>
     */
    public final SMIterator descendantMixedIterator()
        throws XMLStreamException
    {
        return descendantIterator(SMFilterFactory.getMixedFilter());
    }

    /*
    ////////////////////////////////////////////
    // Methods sub-classes need or can override
    // to customize behaviour:
    ////////////////////////////////////////////
     */

    /**
     * Method called by the parent iterator, to skip over the scope
     * this iterator iterates, and of its sub-scopes if any.
     */
    protected abstract void skipTree()
        throws XMLStreamException;

    /**
     * Method iterator calls when it needs to track element state information;
     * if so, it calls this method to take a snapshot of the element.
     *<p>
     * Note caller already suppresses calls so that this method is only
     * called when information needs to be preserved. Further, previous
     * element is only passed if such linkage is to be preserved (reason
     * for not always doing it is the increased memory usage).
     *<p>
     * Finally, note that this method does NOT implement
     * {@link ElementInfoFactory}, as its signature does not include the
     * iterator argument, as that's passed as this pointer already.
     */
    protected SMElementInfo constructElementInfo(SMElementInfo parent,
                                                 SMElementInfo prevSibling)
        throws XMLStreamException
    {
        if (mElemInfoFactory != null) {
            return mElemInfoFactory.constructElementInfo(this, parent, prevSibling);
        }
        XMLStreamReader sr = mStreamReader;
        return new DefaultElementInfo(parent, prevSibling,
                                      sr.getPrefix(), sr.getNamespaceURI(), sr.getLocalName(),
                                      mNodeCount-1, mElemCount-1, getDepth());
    }

    protected abstract SMIterator constructChildIterator(SMFilter f)
        throws XMLStreamException;

    protected abstract SMIterator constructDescendantIterator(SMFilter f)
        throws XMLStreamException;

    /*
    ////////////////////////////////////////////
    // Package methods
    ////////////////////////////////////////////
     */

    protected final boolean isRootIterator() {
        return (mParentCount == 0);
    }

    protected String notAccessible(String method)
        throws XMLStreamException
    {
        if (mChildIterator != null) {
            throw new XMLStreamException("Can not call '"+method+"(): iterator does not point to a valid node, as it has an active open child iterator.");
        }
        throw new XMLStreamException("Can not call '"+method+"(): iterator does not point to a valid node (type "+getCurrentEventType()+"; iterator state "
                                     +getStateDesc());
    }

    protected String getStateDesc() {
        if (mState < 0 || mState >= STATE_DESCS.length) {
            return "[Unknown]";
        }
        return STATE_DESCS[mState];
    }
}
