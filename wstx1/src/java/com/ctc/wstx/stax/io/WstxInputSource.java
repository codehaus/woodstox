package com.ctc.wstx.stax.io;

import java.io.IOException;
import java.net.URL;

import javax.xml.stream.Location;

import com.ctc.wstx.util.TextBuffer;

/**
 * Interface that defines API actual parsers (or, "readers"
 * in StAX lingo) can use to read input from various input sources. Needed to
 * abstract out details of getting input from primary input files, secondary
 * (potentially cached) referenced documents, and from parsed entities, as
 * well as for allowing hierarchic location information for error reporting.
 */
public abstract class WstxInputSource
{
    /**
     * Parent in input source stack
     */
    protected final WstxInputSource mParent;

    /**
     * Nesting level of this input source; 0 is the parent level, increases
     * by one with each expansion.
     *<p>
     * Mostly used to help to catch cyclic references, but also potentially
     * to limit amount of recursion if necessary.
     */
    //final int mLevel;

    /**
     * Name/id of the entity that was expanded to produce this input source;
     * null if not entity-originated. Used for catching recursive expansions
     * of entities.
     */
    final String mFromEntity;

    protected WstxInputSource(WstxInputSource parent, String fromEntity)
    {
        mParent = parent;
        mFromEntity = fromEntity;
        //mLevel = (parent == null) ? 0 : (parent.getLevel() + 1);
    }
    
    /*
    //////////////////////////////////////////////////////////
    // Basic accessors:
    //////////////////////////////////////////////////////////
     */

    /*
    public final int getLevel() {
        return mLevel;
    }
    */

    /**
     * @return Length of suggested input buffer (if source needs one); used
     *   for passing default buffer size down the input source line.
     */
    public abstract int getInputBufferLength();

    public final WstxInputSource getParent() {
        return mParent;
    }

    /**
     * Method that recursively checks if this input source has been
     * expanded -- directly or indirectly -- from specified entity.
     * Note that entity ids are expected to have been interned (using
     * whatever uniqueness mechanism used), and thus can be simply
     * equality checked.
     */
    public boolean hasRecursion()
    {
        String entityId = mFromEntity;
        if (entityId != null) { // should always be true
            WstxInputSource input = mParent;
            while (input != null) {
                if (input.mFromEntity == entityId) {
                    return true;
                }
                input = input.mParent;
            }
        }
        return false;
    }

    /*
    //////////////////////////////////////////////////////////
    // Location info:
    //////////////////////////////////////////////////////////
     */

    public abstract URL getSource();

    public abstract String getPublicId();

    public abstract String getSystemId();

    /**
     * Method usually called to get a parent location for another input
     * source. Works since at this point context (line, row, chars) information
     * has already been saved to this object.
     */
    public abstract WstxInputLocation getLocation();

    public abstract WstxInputLocation getLocation(int total, int row, int col);

    /*
    //////////////////////////////////////////////////////////
    // Actual input handling
    //////////////////////////////////////////////////////////
     */

    /**
     * Method called by Reader when current input has changed to come
     * from this input source. Should reset/initialize input location
     * information Reader keeps, for error messages to work ok.
     */
    public abstract void initInputLocation(WstxInputData reader);

    /**
     * Method called to read at least one more char from input source, and
     * update input data appropriately.
     *
     * @return Number of characters read from the input source (at least 1),
     *   if it had any input; -1 if input source has no more input.
     */
    public abstract int readInto(WstxInputData reader)
        throws IOException;
    
    /**
     * Method called by reader when it has to have at least specified number
     * of consequtive input characters in its buffer, and it currently does
     * not have. If so, it asks input source to do whatever it has to do
     * to try to get more data, if possible (including moving stuff in
     * input buffer if necessary and possible).
     *
     * @return True if input source was able to provide specific number of
     *   characters or more; false if not. In latter case, source is free
     *   to return zero or more characters any way.
     */
    public abstract boolean readMore(WstxInputData reader, int minAmount)
        throws IOException;

    /**
     * Method Reader calls when this input source is being stored, when
     * a nested input source gets used instead (due to entity expansion).
     * Needs to get location info from Reader and store it in this Object.
     */
    public abstract void saveContext(WstxInputData reader);

    /**
     * Method Reader calls when this input source is resumed as the
     * current source. Needs to update Reader's input location data
     * used for error messages etc.
     */
    public abstract void restoreContext(WstxInputData reader);

    public abstract void close() throws IOException;

    /*
    //////////////////////////////////////////////////////////
    // Overridden standard methods:
    //////////////////////////////////////////////////////////
     */

    public String toString() {
        StringBuffer sb = new StringBuffer(80);
        sb.append("<WstxInputSource [class ");
        sb.append(getClass().toString());
        sb.append("]; systemId: ");
        sb.append(getSystemId());
        sb.append(", source: ");
        sb.append(getSource());
        sb.append('>');
        return sb.toString();
    }
}
