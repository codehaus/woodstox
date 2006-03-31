package org.codehaus.staxmate.sr;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.XMLStreamReader2;

/**
 * Default implementation of generic flat (non-scoped) iterator; iterator
 * that traverse all descendants (children and grandchildren) of a start
 * element.
 *<p>
 * Differences to {@link SMNestedIterator} are:
 * <ul>
 *  <li>Flat iterators return {@link XMLStreamConstants#END_ELEMENT} nodes (except
 *    for the one that closes the outermost level), unless
 *    filtered out by the filter, whereas the nested iterator automatically
 *    leaves those out.
 *   </li>
 *  <li>Flat iterators can not have child/descendant iterators
 *   </li>
 * </ul> 
 *
 * @author Tatu Saloranta
 */
public class SMFlatIterator
    extends SMIterator
{
    /*
    ////////////////////////////////////////////
    // Iteration state
    ////////////////////////////////////////////
     */

    /**
     * Number of nested start elements this element has iterated over,
     * including current one (if pointing to one)
     */
    protected int mNesting;

    /*
    ////////////////////////////////////////////
    // Life cycle, configuration
    ////////////////////////////////////////////
     */

    public SMFlatIterator(SMIterator parent, XMLStreamReader2 sr, SMFilter f)
    {
        super(parent, sr, f);
        mNesting = 0;
    }

    /*
    ///////////////////////////////////////////////////
    // Public API, accessing iterator state information
    ///////////////////////////////////////////////////
     */

    public int getDepth()
    {
        return mParentCount + mNesting;
    }

    /*
    ////////////////////////////////////////////
    // Public API, iterating
    ////////////////////////////////////////////
     */

    public SMEvent getNext()
        throws XMLStreamException
    {
        if (mState == State.CLOSED) {
            return null;
        }

        /* If there is a child iterator, it has to be traversed
         * through
         */
        if (mState == State.HAS_CHILD) {
            mChildIterator.skipTree();
            mChildIterator = null;
            mState = State.ACTIVE;
        } else if (mState == State.INITIAL) {
            mState = State.ACTIVE;
        } else { // active
            /* Start element parent count can only be updated AFTER the node
             * has been observed; whereas end element has to be updated
             * when (AFTER) observed.
             */
            if (mCurrEvent == SMEvent.START_ELEMENT) {
                ++mNesting;
            }
        }

        while (true) {
            int type;

            /* Root level has no end element; should always get END_DOCUMENT,
             * but let's be extra careful... (maybe there's need for fragment
             * iterators later on)
             */
            if (isRootIterator()) {
                if (!mStreamReader.hasNext()) {
                    break;
                }
                type = mStreamReader.next();
                /* Document end marker at root level is same as end
                 * element at inner levels...
                 */
                if (type == XMLStreamConstants.END_DOCUMENT) {
                    break;
                }
            } else {
                type = mStreamReader.next();
            }

            ++mNodeCount;

            if (type == XMLStreamConstants.END_ELEMENT) {
                if (--mNesting < 0) { // starts at 0
                    break;
                }
            } else if (type == XMLStreamConstants.START_ELEMENT) {
                ++mElemCount;
            }
            // !!! only here temporarily, shouldn't be needed
            else if (type == XMLStreamConstants.END_DOCUMENT) {
                throw new IllegalStateException("Unexpected END_DOCUMENT encountered (root = "+isRootIterator()+")");
            }

            SMEvent evt = sEventsByIds[type];
            mCurrEvent = evt;

            // Ok, are we interested in this event?
            if (mFilter != null && !mFilter.accept(evt, this)) {
                // Nope, let's just skip over

                // May still need to create the tracked element?
                if (type == XMLStreamConstants.START_ELEMENT) { 
                    if (mElemTracking == Tracking.ALL_SIBLINGS) {
                        mTrackedElement = constructElementInfo
                            (mParentTrackedElement, mTrackedElement);
                    }
                }
                continue;
            }

            // Need to update tracked element?
            if (type == XMLStreamConstants.START_ELEMENT
                && mElemTracking != Tracking.NONE) {
                SMElementInfo prev = (mElemTracking == Tracking.PARENTS) ?
                    null : mTrackedElement;
                mTrackedElement = constructElementInfo
                    (mParentTrackedElement, prev);
            }
            return evt;
        }

        // Ok, no more events
        mState = State.CLOSED;
        mCurrEvent = null;
        return null;
    }

    public SMIterator constructChildIterator(SMFilter f) {
        return new SMNestedIterator(this, mStreamReader, f);
    }

    public SMIterator constructDescendantIterator(SMFilter f) {
        return new SMFlatIterator(this, mStreamReader, f);
    }

    /**
     * Method called by the parent iterator, to skip over the scope
     * this iterator iterates, and of its sub-scopes if any.
     */
    protected void skipTree()
        throws XMLStreamException
    {
        if (mState == State.CLOSED) { // already finished?
            return;
        }
        mState = State.CLOSED;

        // child iterator(s) to delegate skipping to?
        if (mState == State.HAS_CHILD) {
            mChildIterator.skipTree();
            mChildIterator = null;
        }

        int depth = mNesting;
        while (true) {
            int type = mStreamReader.next();
            if (type == XMLStreamConstants.START_ELEMENT) {
                ++depth;
            } else if (type == XMLStreamConstants.END_ELEMENT) {
                if (--depth < 0) {
                    break;
                }
            } else if (type == XMLStreamConstants.END_DOCUMENT) { // sanity check
                // An error...
                throw new IllegalStateException("Unexpected END_DOCUMENT encountered when skipping a sub-tree.");
            }
        }
    }

    /*
    ////////////////////////////////////////////
    // Internal/package methods
    ////////////////////////////////////////////
     */

}
