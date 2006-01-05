/* Woodstox XML processor
 *
 * Copyright (c) 2004- Tatu Saloranta, tatu.saloranta@iki.fi
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

import java.util.*;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.validation.XMLValidator;

import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.util.*;

/**
 * Sub-class of {@link InputElementStack} used when operating in
 * namespace-aware mode.
 *<p>
 * Implementation note: this class reuses {@link NamespaceContext}
 * instances, so that consequtive accesses just return the same instance,
 * as long as nothing in bindings change. As a result, only those instances
 * that explicitly add new bindings create distinct non-shareable context
 * instances. Although it would also be possible to share underlying String
 * array to further improve object sharing, it seems like marginal gain
 * with more complexity: as such the current simple scheme should work
 * just fine (and is measure to be very close to optimal for most common
 * namespace-heavey document types like Soap messages).
 */
public final class NsInputElementStack
    extends InputElementStack
{
    /**
     * Topmost namespace URI assigned for root element, if not specifically
     * defined. Could either be null, or empty String; seems like most
     * xml processors prefer latter.
     */
    final static String DEFAULT_NAMESPACE_URI = "";

    final static int IX_PREFIX = 0;
    final static int IX_LOCALNAME = 1;
    final static int IX_URI = 2;
    final static int IX_DEFAULT_NS = 3;

    final static int ENTRY_SIZE = 4;

    /**
     * Canonicalized String 'xml'; used to verify that 'xml' namespace
     * prefix won't be redefined from its default URI.
     */
    protected final String mPrefixXml;

    /**
     * Canonicalized String 'xmlns'; used to verify that 'xmlns' namespace
     * prefix won't be redefined from its default URI.
     */
    protected final String mPrefixXmlns;

    final static InternCache sInternCache = InternCache.getInstance();

    /*
    //////////////////////////////////////////////////
    // Configuration
    //////////////////////////////////////////////////
     */

    protected final NsAttributeCollector mAttrCollector;

    /*
    //////////////////////////////////////////////////
    // Element stack state information
    //////////////////////////////////////////////////
     */

    /**
     * Vector that contains all currently active namespaces; one String for
     * prefix, another for matching URI. Does also include default name
     * spaces (at most one per level).
     */
    protected final StringVector mNamespaces = new StringVector(64);

    /**
     * Array that contains path of open elements from root; for each there
     * are 4 Strings; prefix, localname, URI, and default name space URI.
     */
    protected String[] mElements;

    /**
     * Number of Strings in {@link #mElements} that are valid (ie depth
     * multiplied by 4)
     */
    protected int mSize;

    /**
     * Array that contains namespace offsets for each element; that is,
     * index of first 'local' name space entry, entry declared for
     * current element. Number of such local entries is
     * <code>mCurrNsCount - mNsCounts[mSize-1]</code>
     */
    protected int[] mNsCounts;

    /*
    //////////////////////////////////////////////////
    // Simple 1-slot QName cache; used for improving
    // efficiency of code that uses QNames extensively
    // (like StAX Event API implementation)
    //////////////////////////////////////////////////
     */

    protected String mLastLocalName = null;
    protected String mLastPrefix = null;
    protected String mLastNsURI = null;

    protected QName mLastName = null;

    /*
    /////////////////////////////////////////////////////
    // Simple caching for non-transient NamespaceContext
    // instance - mostly for event API as well
    /////////////////////////////////////////////////////
     */

    /**
     * Last potentially shareable NamespaceContext created by
     * this stack. This reference is cleared each time bindings
     * change (either due to a start element with new bindings, or due
     * to the matching end element that closes scope of such binding(s)).
     */
    protected BaseNsContext mLastNsContext = null;

    /*
    //////////////////////////////////////////////////
    // Life-cycle (create, update state)
    //////////////////////////////////////////////////
     */

    public NsInputElementStack(int initialSize,
                               boolean normAttrs, boolean internNsURIs,
                               String prefixXml, String prefixXmlns)
    {
        super(internNsURIs);
        mPrefixXml = prefixXml;
        mPrefixXmlns = prefixXmlns;
        mSize = 0;
        if (initialSize < 4) {
            initialSize = 4;
        }
        mElements = new String[initialSize << 2];
        mNsCounts = new int[initialSize];
        mAttrCollector = new NsAttributeCollector(normAttrs, prefixXml);
    }

    public final void push(String prefix, String localName)
    {
        int index = mSize;
        if (index == mElements.length) {
            String[] old = mElements;
            mElements = new String[old.length + 64];
            System.arraycopy(old, 0, mElements, 0, old.length);
        }
        mElements[index] = prefix;
        mElements[index+1] = localName;

        if (index == 0) { // root element
            mElements[IX_DEFAULT_NS] = DEFAULT_NAMESPACE_URI;
        } else {
            // Let's just duplicate parent's default NS URI as baseline:
            mElements[index + IX_DEFAULT_NS] = mElements[index - (ENTRY_SIZE - IX_DEFAULT_NS)];
         }
        mSize = index+4;

        // Also need to update namespace stack:
        index >>= 2;
        if (index == mNsCounts.length) {
            int[] old = mNsCounts;
            mNsCounts = new int[old.length + 16];
            System.arraycopy(old, 0, mNsCounts, 0, old.length);
        }
        mNsCounts[index] = mNamespaces.size();
        mAttrCollector.reset();
    }

    public final void push(String fullName) {
        throw new Error("Internal error: push(fullName) shouldn't be called for namespace aware element stack.");
    }

    /**
     * @return Validation state that should be effective for the parent
     *   element state
     */
    public int pop()
        throws XMLStreamException
    {
        int index = mSize;
        if (index == 0) {
            throw new IllegalStateException("Popping from empty stack.");
        }

        int result;

        /* Can and should not shrink (or clear) the stack before calling
         * the validator, so let's just update the local count
         */
        index -= 4;

        if (mValidator == null) {
            /* Let's allow GCing (not likely to matter, as Strings are very
             * likely interned... but it's a good habit
             */
            result = XMLValidator.CONTENT_ALLOW_ANY_TEXT;
        } else {
            result = mValidator.validateElementEnd(mElements[index+IX_LOCALNAME],
                                                   mElements[index+IX_URI],
                                                   mElements[index+IX_PREFIX]);

        }

        // Now we can shrink the stack:
        mSize = index;
        mElements[index] = null;
        mElements[index+1] = null;
        mElements[index+2] = null;
        mElements[index+3] = null;
            
        // Need to purge namespaces?
        int nsCount = mNamespaces.size() - mNsCounts[index >> 2];
        if (nsCount > 0) { // 2 entries for each NS mapping:
            mLastNsContext = null; // let's invalidate ns ctxt too, if we had one
            mNamespaces.removeLast(nsCount);
        }
        return result;
    }

    /**
     * Method called to update information about top of the stack, with
     * attribute information passed in. Will resolve namespace references,
     * and update namespace stack with information.
     *
     * @return Validation state that should be effective for the fully
     *   resolved element context
     */
    public int resolveAndValidateElement()
        throws XMLStreamException
    {
        if (mSize == 0) { // just a simple sanity check
            throw new IllegalStateException("Calling validate() on empty stack.");
        }
        NsAttributeCollector ac = mAttrCollector;

        // Any namespace declarations?
        {
            int nsCount = ac.getNsCount();
            if (nsCount > 0) {
                /* let's first invalidate old (possibly) shared ns ctxt too,
                 * if we had one; new one can be created at a later point
                 */
                mLastNsContext = null;

                String [] nsPrefixes = ac.getNsPrefixes();
                TextBuilder nsURIs = ac.getNsURIs();
                for (int i = 0; i < nsCount; ++i) {
                    String nsUri = nsURIs.getEntry(i);
                    if (mInternNsURIs && nsUri.length() > 0) {
                        nsUri = sInternCache.intern(nsUri);
                    }
                    /* 28-Jul-2004, TSa: Now we will have default namespaces
                     *   in there too; they have empty String as prefix
                     */
                    String prefix = nsPrefixes[i];
                    if (prefix == null) {
                        prefix = "";
                        mElements[mSize-(ENTRY_SIZE - IX_DEFAULT_NS)] = nsUri;

                    /* 18-Jul-2004, TSa: Need to check that 'xml' and 'xmlns'
                     *   prefixes are not re-defined.
                     * !!! Should probably also check that matching URIs are
                     *   never bound to other prefixes, since that's what
                     *   Xerces does?
                     */
                    } else if (prefix == mPrefixXml) {
                        if (!nsUri.equals(XMLConstants.XML_NS_URI)) {
                            mReporter.throwParseError(ErrorConsts.ERR_NS_REDECL_XML,
                                                      nsUri);
                        }
                    } else if (prefix == mPrefixXmlns) {
                        if (!nsUri.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
                            mReporter.throwParseError(ErrorConsts.ERR_NS_REDECL_XMLNS,
                                                      nsUri);
                        }
                    }
                    mNamespaces.addStrings(prefix, nsUri);
                }
            }
        }

        // Then, let's set element's namespace, if any:
        String prefix = mElements[mSize-(ENTRY_SIZE - IX_PREFIX)];
        String ns;

        if (prefix == null || prefix.length() == 0) { // use default NS, if any
            ns = mElements[mSize-(ENTRY_SIZE - IX_DEFAULT_NS)];
        } else if (prefix == mPrefixXml) {
            ns = XMLConstants.XML_NS_URI;
        } else {
            // Need to find namespace with the prefix:
            ns = mNamespaces.findLastFromMap(prefix);
            if (ns == null) {
                mReporter.throwParseError(ErrorConsts.ERR_NS_UNDECLARED, prefix);
                ns = ""; // will never get here, for now
            }
        }
        mElements[mSize-(ENTRY_SIZE - IX_URI)] = ns;

        // And finally, resolve attributes' namespaces too:
        ac.resolveNamespaces(mReporter, mNamespaces);

        // If we have no validator(s), nothing more to do:
        XMLValidator vld = mValidator;
        if (vld == null) { // no DTD in use
            return XMLValidator.CONTENT_ALLOW_ANY_TEXT;
        }

        // Otherwise need to call relevant validation methods.

        /* First, a call to check if the element itself may be acceptable
         * within structure:
         */
        vld.validateElementStart
            (mElements[mSize-(ENTRY_SIZE - IX_LOCALNAME)],
             mElements[mSize-(ENTRY_SIZE - IX_URI)],
             prefix);

        // Then attributes, if any:
        StringVector attrNames = ac.getNameList();
        int attrLen = attrNames.size();
        if (attrLen > 0) {
            String[] attrURIs = ac.getAttrURIs();
            String[] nameData = attrNames.getInternalArray();
            TextBuilder attrBuilder = ac.getAttrBuilder();
            char[] attrCB = attrBuilder.getCharBuffer();
            for (int i = 0, nr = 0; i < attrLen; i += 2, ++nr) {
                prefix = nameData[i];
                String ln = nameData[i+1];
                String normValue = mValidator.validateAttribute
                    (ln, attrURIs[nr], prefix,
                     attrCB, attrBuilder.getOffset(nr),
                     attrBuilder.getOffset(nr+1));
                if (normValue != null) {
                    ac.setNormalizedValue(nr, normValue);
                }
            }
        }

        /* And finally let's wrap things up to see what textual content
         * is allowed as child content, if any:
         */
        return mValidator.validateElementAndAttributes();
    }

    /*
    ///////////////////////////////////////////////////
    // Public methods
    ///////////////////////////////////////////////////
     */

    public final boolean isNamespaceAware() {
        return true;
    }

    /**
     * @return Number of open elements in the stack; 0 when parser is in
     *  prolog/epilog, 1 inside root element and so on.
     */
    public final int getDepth() {
        return (mSize >> 2);
    }

    public final AttributeCollector getAttrCollector() {
        return mAttrCollector;
    }

    /**
     * Method called to construct a non-transient NamespaceContext instance;
     * generally needed when creating events to return from event-based
     * iterators.
     */
    public final BaseNsContext createNonTransientNsContext(Location loc)
    {
        // Have an instance we can reuse? Great!
        if (mLastNsContext != null) {
            return mLastNsContext;
        }

        // No namespaces declared at this point? Easy, as well:
        int totalNsSize = mNamespaces.size();
        if (totalNsSize < 1) {
            return (mLastNsContext = EmptyNamespaceContext.getInstance());
        }

        // Otherwise, we need to create a new non-empty context:
        int localCount = getCurrentNsCount() << 1;
        BaseNsContext nsCtxt = new CompactNsContext
            (loc, getDefaultNsURI(),
             mNamespaces.asArray(), totalNsSize,
             totalNsSize - localCount);
        /* And it can be shared if there are no new ('local', ie. included
         * within this start element) bindings -- if there are, underlying
         * array might be shareable, but offsets wouldn't be)
         */
        if (localCount == 0) {
            mLastNsContext = nsCtxt;
        }
        return nsCtxt;
    }

    /*
    ///////////////////////////////////////////////////
    // Implementation of NamespaceContext:
    ///////////////////////////////////////////////////
     */

    public final String getNamespaceURI(String prefix)
    {
        if (prefix == null) {
            throw new IllegalArgumentException(ErrorConsts.ERR_NULL_ARG);
        }
        if (prefix.length() == 0) {
            if (mSize == 0) { // could signal an error too
                return null;
            }
            return mElements[mSize-(ENTRY_SIZE - IX_DEFAULT_NS)];
        }
        if (prefix.equals(XMLConstants.XML_NS_PREFIX)) {
            return XMLConstants.XML_NS_URI;
        }
        if (prefix.equals(XMLConstants.XMLNS_ATTRIBUTE)) {
            return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
        }
        /* Ok, need to find the match, if any; starting from end of the
         * list of active namespaces. Note that we can not count on prefix
         * being interned/canonicalized.
         */
        return mNamespaces.findLastNonInterned(prefix);
    }

    public final String getPrefix(String nsURI) {
        if (nsURI == null || nsURI.length() == 0) {
            throw new IllegalArgumentException("Illegal to pass null/empty prefix as argument.");
        }
        if (nsURI.equals(XMLConstants.XML_NS_URI)) {
            return XMLConstants.XML_NS_PREFIX;
        }
        if (nsURI.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
            return XMLConstants.XMLNS_ATTRIBUTE;
        }

        /* Ok, need to find the match, if any; starting from end of the
         * list of active namespaces. Note that we can not count on prefix
         * being interned/canonicalized.
         */
        //String prefix = mNamespaces.findLastByValueNonInterned(nsURI);
        String prefix = null;

        /* 29-Sep-2004, TSa: Need to check for namespace masking, too...
         */
        String[] strs = mNamespaces.getInternalArray();
        int len = mNamespaces.size();

        main_loop:
        for (int index = len-1; index > 0; index -= 2) {
            if (nsURI.equals(strs[index])) {
                // Ok, is prefix masked?
                prefix = strs[index-1];
                for (int j = index+1; j < len; j += 2) {
                    if (strs[j] == prefix) { // masked!
                        prefix = null;
                        continue main_loop;
                    }
                }
                // nah, it's good
                break main_loop;
            }
        }

        return prefix;
    }

    public final Iterator getPrefixes(String nsURI)
    {
        if (nsURI == null || nsURI.length() == 0) {
            throw new IllegalArgumentException("Illegal to pass null/empty prefix as argument.");
        }
        if (nsURI.equals(XMLConstants.XML_NS_URI)) {
            return new SingletonIterator(XMLConstants.XML_NS_PREFIX);
        }
        if (nsURI.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
            return new SingletonIterator(XMLConstants.XMLNS_ATTRIBUTE);
        }

        //return mNamespaces.findAllByValueNonInterned(nsURI);

        /* 29-Sep-2004, TSa: Need to check for namespace masking, too...
         */
        String[] strs = mNamespaces.getInternalArray();
        int len = mNamespaces.size();
        ArrayList l = null;

        main_loop:
        for (int index = len-1; index > 0; index -= 2) {
            if (nsURI.equals(strs[index])) {
                // Ok, is prefix masked?
                String prefix = strs[index-1];
                for (int j = index+1; j < len; j += 2) {
                    if (strs[j] == prefix) { // masked!
                        continue main_loop;
                    }
                }
                // nah, it's good!
                if (l == null) {
                    l = new ArrayList();
                }
                l.add(prefix);
            }
        }

        return (l == null) ? EmptyIterator.getInstance() : l.iterator();
    }

    /*
    ///////////////////////////////////////////////////
    // AttributeInfo methods (StAX2)
    ///////////////////////////////////////////////////
     */

    public final int getAttributeCount()
    {
        return mAttrCollector.getCount();
    }

    public final int findAttributeIndex(String nsURI, String localName)
    {
        return mAttrCollector.findIndex(nsURI, localName);
    }

    public String getAttributeType(int index) {
        return (mValidator == null) ? UNKNOWN_ATTR_TYPE : 
            mValidator.getAttributeType(index);
    }

    public int getIdAttributeIndex() {
        return (mValidator == null) ? -1 : 
            mValidator.getIdAttrIndex();
    }

    public int getNotationAttributeIndex() {
        return (mValidator == null) ? -1 :
            mValidator.getNotationAttrIndex();
    }

    /*
    ///////////////////////////////////////////////////
    // ValidationContext methods
    ///////////////////////////////////////////////////
     */

    public final QName getCurrentElementName()
    {
        if (mSize == 0) {
            return null;
        }
        String prefix = mElements[mSize-(ENTRY_SIZE - IX_PREFIX)];
        if (prefix == null) {
            prefix = "";
        }
        /* 03-Dec-2004, TSa: Maybe we can just reuse the last QName
         *    object created, if we have same data? (happens if
         *    state hasn't changed, or we got end element for a leaf
         *    element, or repeating leaf elements)
         */
        String nsURI = mElements[mSize-(ENTRY_SIZE - IX_URI)];
        String ln = mElements[mSize-(ENTRY_SIZE - IX_LOCALNAME)];

        /* Since we generally intern most Strings, can do identity
         * comparisons here:
         */
        if (ln != mLastLocalName) {
            mLastLocalName = ln;
            mLastPrefix = prefix;
            mLastNsURI = nsURI;
        } else if (prefix != mLastPrefix) {
            mLastPrefix = prefix;
            mLastNsURI = nsURI;
        } else if (nsURI != mLastNsURI) {
            mLastNsURI = nsURI;
        } else {
            return mLastName;
        }
        QName n = new QName(nsURI, ln, prefix);
        mLastName = n;
        return n;
    }

    public int addDefaultAttribute(String localName, String uri, String prefix,
                                   String value)
    {
        return mAttrCollector.addDefaultAttribute(localName, uri, prefix, value);
    }

    /*
    ///////////////////////////////////////////////////
    // Accessors:
    ///////////////////////////////////////////////////
     */

    // // // Generic stack information:

    public final boolean isEmpty() {
        return mSize == 0;
    }

    // // // Information about element at top of stack:

    public final String getDefaultNsURI() {
        if (mSize == 0) {
            throw new IllegalStateException("Illegal access, empty stack.");
        }
        return mElements[mSize-(ENTRY_SIZE - IX_DEFAULT_NS)];
    }

    public final String getNsURI() {
        if (mSize == 0) {
            throw new IllegalStateException("Illegal access, empty stack.");
        }
        return mElements[mSize-(ENTRY_SIZE - IX_URI)];
    }

    public final String getPrefix() {
        if (mSize == 0) {
            throw new IllegalStateException("Illegal access, empty stack.");
        }
        return mElements[mSize-(ENTRY_SIZE - IX_PREFIX)];
    }

    public final String getLocalName() {
        if (mSize == 0) {
            throw new IllegalStateException("Illegal access, empty stack.");
        }
        return mElements[mSize-(ENTRY_SIZE - IX_LOCALNAME)];
    }

    public final boolean matches(String prefix, String localName) {
        if (mSize == 0) {
            throw new IllegalStateException("Illegal access, empty stack.");
        }
        String thisPrefix = mElements[mSize-(ENTRY_SIZE - IX_PREFIX)];
        if (prefix == null || prefix.length() == 0) { // no name space
            if (thisPrefix != null && thisPrefix.length() > 0) {
                return false;
            }
        } else {
            if (thisPrefix != prefix && !thisPrefix.equals(prefix)) {
                return false;
            }
        }

        String thisName = mElements[mSize-3];
        return (thisName == localName) || thisName.equals(localName);
    }

    public final String getTopElementDesc() {
        if (mSize == 0) {
            throw new IllegalStateException("Illegal access, empty stack.");
        }
        String name = mElements[mSize-3];
        String prefix = mElements[mSize-4];
        if (prefix == null || prefix.length() == 0) { // no name space
            return name;
        }
        return prefix + ":" + name;
    }

    // // // Namespace information:

    /**
     * @return Number of active prefix/namespace mappings for current scope,
     *   including mappings from enclosing elements.
     */
    public final int getTotalNsCount() {
        return mNamespaces.size() >> 1;
    }

    /**
     * @return Number of active prefix/namespace mappings for current scope,
     *   NOT including mappings from enclosing elements.
     */
    public final int getCurrentNsCount() {
        // Need not check for empty stack; should return 0 properly
        return (mNamespaces.size() - mNsCounts[(mSize-1) >> 2]) >> 1;
    }

    public final String getLocalNsPrefix(int index) {
        int offset = mNsCounts[(mSize-1) >> 2];
        int localCount = (mNamespaces.size() - offset);
        index <<= 1; // 2 entries, prefix/URI for each NS
        if (index < 0 || index >= localCount) {
            throw new IllegalArgumentException("Illegal namespace index "
                                           +(index >> 1)
                                           +"; current scope only has "
                                           +(localCount >> 1)
                                           +" namespace declarations.");
        }
        return mNamespaces.getString(offset + index);
    }

    public final String getLocalNsURI(int index) {
        int offset = mNsCounts[(mSize-1) >> 2];
        int localCount = (mNamespaces.size() - offset);
        index <<= 1; // 2 entries, prefix/URI for each NS
        if (index < 0 || index >= localCount) {
            throw new IllegalArgumentException("Illegal namespace index "
                                           +(index >> 1)
                                           +"; current scope only has "
                                           +(localCount >> 1)
                                           +" namespace declarations.");
        }
        return mNamespaces.getString(offset + index + 1);
    }
}
