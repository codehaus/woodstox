/* Woodstox XML processor
 *
 * Copyright (c) 2004 Tatu Saloranta, tatu.saloranta@iki.fi
 *
 * Licensed under the License specified in file LICENSE, included with
 * the source code.
 * You may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ctc.wstx.sr;

import javax.xml.XMLConstants;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.namespace.QName;

import com.ctc.wstx.exc.WstxException;
import com.ctc.wstx.util.StringVector;
import com.ctc.wstx.util.TextBuilder;
import com.ctc.wstx.util.WordResolver;

/**
 * Shared base class that defines API stream reader uses to communicate
 * with the attribute collector implementation, independent of whether it's
 * operating in namespace-aware or non-namespace modes.
 * Collector class is used to build up attribute lists; for the most part
 * will just hold references to few specialized {@link TextBuilder}s that
 * are used to create efficient semi-shared value Strings.
 *<p>
 * In addition 
 */
public abstract class AttributeCollector
{
    final static char CHAR_SPACE = (char) 0x0020;

    /**
     * Threshold value that indicates minimum length for lists instances
     * that need a Map structure, for fast attribute access by fully-qualified
     * name.
     */
    protected final static int LONG_ATTR_LIST_LEN = 4;

    /**
     * Expected typical maximum number of attributes for any element;
     * chosen to minimize need to resize, while trying not to waste space.
     */
    protected final static int EXP_ATTR_COUNT = 32;
    
    /*
    //////////////////////////////////////////
    // Configuration
    //////////////////////////////////////////
     */

    protected final boolean mNormAttrs;

    /*
    //////////////////////////////////////////
    // Collected attribute information:
    //////////////////////////////////////////
     */

    /**
     * Actual number of attributes collected, including attributes
     * added via default values.
     */
    protected int mAttrCount;

    /**
     * Number of attribute values actually parsed, not including
     * ones created via default value expansion. Equal to or less than
     * {@link #mAttrCount}.
     */
    protected int mNonDefCount;

    /**
     * TextBuilder into which values of all attributes are appended
     * to, including default valued ones (defaults are added after
     * explicit ones)
     */
    protected final TextBuilder mValueBuffer = new TextBuilder(EXP_ATTR_COUNT);

    /**
     * Vector in which attribute names are added; exact number of elements
     * per attribute depends on whether namespace support is enabled or
     * not (non-namespace mode only needs one entry; namespace mode two,
     * one for prefix, one for local name). Contains
     */
    protected final StringVector mAttrNames = new StringVector(EXP_ATTR_COUNT);

    /*
    //////////////////////////////////////////
    // Resolved (derived) attribute information:
    //////////////////////////////////////////
     */

    /**
     * Array in which attribute value Strings are added, first time they
     * are requested. Values are first added to <code>mValueBuffer</code>,
     * from which a String is created, and finally substring created as
     * needed and added to this array.
     */
    protected String[] mAttrValues = null;

    /*
    //////////////////////////////////////////////////////////////
    // Information that defines "Map-like" data structure used for
    // quick access to attribute values by fully-qualified name
    //////////////////////////////////////////////////////////////
     */

    /**
     * Encoding of a data structure that contains mapping from
     * attribute names to attribute index in main attribute name arrays.
     *<p>
     * Data structure contains two separate areas; main hash area (with
     * size <code>mAttrHashSize</code>), and remaining spillover area
     * that follows hash area up until (but not including)
     * <code>mAttrSpillEnd</code> index.
     * Main hash area only contains indexes (index+1; 0 signifying empty slot)
     * to actual attributes; spillover area has both hash and index for
     * any spilled entry. Spilled entries are simply stored in order
     * added, and need to be searched using linear search. In case of both
     * primary hash hits and spills, eventual comparison with the local
     * name needs to be done with actual name array.
     */
    protected int[] mAttrMap = null;

    /**
     * Size of hash area in <code>mAttrMap</code>; generally at least 20%
     * more than number of attributes (<code>mAttrCount</code>).
     */
    protected int mAttrHashSize;

    /**
     * Pointer to int slot right after last spill entr, in
     * <code>mAttrMap</code> array.
     */
    protected int mAttrSpillEnd;

    /*
    ///////////////////////////////////////////////
    // Life-cycle:
    ///////////////////////////////////////////////
     */

    protected AttributeCollector(boolean normAttrs) {
        mNormAttrs = normAttrs;
    }

    /**
     * Method called to allow reusing of collector, usually right before
     * starting collecting attributes for a new start tag.
     */
    protected abstract void reset();

    /*
    ///////////////////////////////////////////////
    // Public accesors (for stream reader)
    ///////////////////////////////////////////////
     */

    /**
     * @return Number of namespace declarations collected, not including
     *   possible default namespace declaration
     */
    public abstract int getNsCount();

    public abstract String getNsPrefix(int index);

    public abstract String getNsURI(int index);

    // // // Direct access to attribute/NS prefixes/localnames/URI

    public final int getCount() {
        return mAttrCount;
    }

    public abstract String getPrefix(int index);

    public abstract String getLocalName(int index);

    public abstract String getURI(int index);

    public abstract QName getQName(int index);

    public final String getValue(int index)
    {
        if (index < 0 || index >= mAttrCount) {
            throwIndex(index);
        }
        /* Note: array has been properly (re)sized by sub-classes
         * resolveXxx() method, so it's either null or properly sized
         * by now
         */
        if (mAttrValues == null) {
            mAttrValues = new String[mAttrCount];
        }
        String str = mAttrValues[index];
        if (str == null) {
            str = mValueBuffer.getEntry(index);
            mAttrValues[index] = str;
        }
        return str;
    }

    public abstract String getValue(String nsURI, String localName);

    public boolean isSpecified(int index) {
        return (index < mNonDefCount);
    }

    /**
     * Method called by the stream reader (and/or other Woodstox classes)
     * to get information about all the attributes of the current
     * start element, via callback Object passed in as the argument.
     * This is potentially more efficient than calling separate accessors.
     */
    public abstract void iterateAttributes(ElemIterCallback cb)
        throws XMLStreamException;

    /*
    ///////////////////////////////////////////////
    // Accessors for accessing helper objects
    ///////////////////////////////////////////////
     */

    public abstract TextBuilder getDefaultNsBuilder();

    public abstract TextBuilder getNsBuilder(String localName);

    public abstract TextBuilder getAttrBuilder(String attrPrefix, String attrLocalName);

    /**
     * Method needed by event builder code; called to build a non-transient
     * attribute container to use by a start element event.
     */
    public abstract ElemAttrs buildAttrOb();

    /*
    ///////////////////////////////////////////////
    // Validation methods:
    ///////////////////////////////////////////////
     */

    /**
     * Method called by validation/normalization code, to normalize
     * specified attribute value and return it as (non-interned) String,
     * if normalization was done. May return null if no changes were done.
     */
    public void normalizeValue(int index)
    {
        String val = mValueBuffer.normalizeSpaces(index);

        if (val == null) {
            if (mAttrValues == null) {
                mAttrValues = new String[mAttrCount];
            }
            mAttrValues[index] = val;
        }
    }

    /**
     * Method called by validation/normalization code for enumeration-valued
     * attributes, to trim
     * specified attribute value (full normalization not needed -- called
     * for values that CAN NOT have spaces inside; such values can not
     * be legal), and then check whether it is included
     * in set of words (tokens) passed in. If actual value was included,
     * will return the normalized word (as well as store shared String
     * locally); otherwise will return null.
     */
    public String checkEnumValue(int index, WordResolver res)
    {
        /* Better NOT to build temporary Strings quite yet; can resolve
         * matches via resolver more efficiently.
         */

        TextBuilder b = mValueBuffer;
        int start = b.getOffset(index);
        int end = b.getOffset(index+1)-1;
        char[] buf = b.getCharBuffer();

        if (mNormAttrs) {
            while (start <= end && buf[start] == CHAR_SPACE) {
                ++start;
            }
            while (end > start && buf[end] == CHAR_SPACE) {
                --end;
            }
        }

        // Empty String is never legal for enums:
        if (start > end) {
            return null;
        }
        String result = res.find(buf, start, end+1);
        if (result != null) {
            if (mAttrValues == null) {
                mAttrValues = new String[mAttrCount];
            }
            mAttrValues[index] = result;
        }
        return result;
    }

    /**
     * Method called by validator to insert an attribute that has a default
     * value and wasn't yet included in collector's attribute set.
     */
    public abstract void addDefaultAttr(InputProblemReporter rep, StringVector ns,
                                        String prefix, String localName, String value)
        throws WstxException;

    /**
     * Low-level accessor method that attribute validation code may call
     * for certain types of attributes; generally only for id and idref/idrefs
     * attributes. It returns the underlying 'raw' attribute value buffer
     * for direct access.
     */
    public final TextBuilder getAttrBuilder() {
        return mValueBuffer;
    }

    /**
     * Low-level mutator method that attribute validation code may call
     * for certain types of attributes, when it wants to handle the whole
     * validation and normalization process by itself. It is generally
     * only called for id and idref/idrefs attributes, as those values
     * are usually normalized.
     */
    public final void setNormalizedValue(int index, String value) {
        if (mAttrValues == null) {
            mAttrValues = new String[mAttrCount];
        }
        mAttrValues[index] = value;
    }

    /*
    ///////////////////////////////////////////////
    // Package methods:
    ///////////////////////////////////////////////
     */

    protected void throwIndex(int index) {
        throw new IllegalArgumentException("Invalid index "+index+"; current element has only "+getCount()+" attributes");
    }

    /**
     * Method called by {@link InputElementStack} instance that "owns" this
     * attribute collector; 
     */
    public StringVector getNameList() {
        return mAttrNames;
    }

    /*
    ///////////////////////////////////////////////
    // Internal methods:
    ///////////////////////////////////////////////
     */

    protected static String[] resize(String[] old) {
        int len = old.length;
        String[] result = new String[len];
        System.arraycopy(old, 0, result, 0, len);
        return result;
    }

    protected void throwDupAttr(InputProblemReporter rep, int index)
        throws WstxException
    {
        rep.throwParseError("Duplicate attribute '"+getQName(index)+"'.");
    }
}
