package org.codehaus.staxmate.sr;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.XMLStreamReader2;

/**
 * Default implementation of generic nested (scoped) iterator; iterator
 * that only traverses direct children of a single start element.
 *
 * @author Tatu Saloranta
 */
public class SMNestedIterator
    extends SMIterator
{
    /*
    ////////////////////////////////////////////
    // Life cycle
    ////////////////////////////////////////////
     */

    public SMNestedIterator(SMIterator parent, XMLStreamReader2 sr, SMFilter f)
    {
        super(parent, sr, f);
    }

    /*
    /////////////////////////////////////////w//////////
    // Public API, accessing iterator state information
    ///////////////////////////////////////////////////
     */

    public int getDepth() {
        return mParentCount;
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
            // If we had a start element, need to skip the subtree...
            if (mCurrEvent == SMEvent.START_ELEMENT) {
                skipSubTree();
            }
        }

        while (true) {
            int type;

            // Root level has no end element...
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
               break;
            }
            if (type == XMLStreamConstants.START_ELEMENT) {
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
                /* Nope, let's just skip over; but we may still need to
                 * create the tracked element?
                 */
                if (type == XMLStreamConstants.START_ELEMENT) { 
                    if (mElemTracking == Tracking.ALL_SIBLINGS) {
                        mTrackedElement = constructElementInfo
                            (mParentTrackedElement, mTrackedElement);
                    }
                }
                /* Note: level skipping will be done in the beginning of
                 * the loop
                 */
                continue;
            }

            // Need to update tracked element?
            if (type == XMLStreamConstants.START_ELEMENT
                && mElemTracking != Tracking.NONE) {
                SMElementInfo prev = (mElemTracking == Tracking.PARENTS) ?
                    null : mTrackedElement;
                mTrackedElement = constructElementInfo(mParentTrackedElement, prev);
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

    protected void skipTree()
        throws XMLStreamException
    {
        if (mState == State.CLOSED) { // already finished?
            return;
        }
        mCurrEvent = null;
        mState = State.CLOSED;

        // child iterator(s) to delegate skipping to?
        if (mState == State.HAS_CHILD) {
            mChildIterator.skipTree();
            mChildIterator = null;
        }
        skipSubTree();
    }

    /*
    ////////////////////////////////////////////
    // Internal/package methods
    ////////////////////////////////////////////
     */

    protected void skipSubTree()
        throws XMLStreamException
    {
        int depth = 0;

        while (true) {
            int type = mStreamReader.next();
            if (type == XMLStreamConstants.START_ELEMENT) {
                ++depth;
            } else if (type == XMLStreamConstants.END_ELEMENT) {
                if (--depth < 0) {
                    break;
                }
            } else if (type == XMLStreamConstants.END_DOCUMENT) {
                // An error...
                throw new IllegalStateException("Unexpected END_DOCUMENT encountered when skipping a sub-tree.");
            }
        }
    }
}
