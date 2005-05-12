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

package com.ctc.wstx.sw;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.HashMap;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import com.ctc.wstx.util.StringVector;

/**
 * Class that encapsulates information a specific element in virtual
 * output stack, for writers that fully support namespace handling.
 */
public final class OutputElement
    implements NamespaceContext
{
    public final static int PREFIX_UNBOUND = 0;
    public final static int PREFIX_OK = 1;
    public final static int PREFIX_MISBOUND = 2;

    /**
     * Shared instance used as the virtual root for all output XML documents
     */
    final static OutputElement sRootInstance = new OutputElement();

    final static String sXmlNsPrefix = XMLConstants.XML_NS_PREFIX;
    final static String sXmlNsURI = XMLConstants.XML_NS_URI;
    
    /*
    ////////////////////////////////////////////
    // Information about element itself:
    ////////////////////////////////////////////
     */

    final OutputElement mParent;

    /**
     *<p>
     * Note: unfortunately prefix can NOT be final, since it may get changed
     * after object is instantiated, if there are conflicts.
     */
    String mPrefix;

    final String mLocalName;

    /* Note: no need to really store namespace information of element,
     * since it has already been used for finding the appropriate prefix
     * (and thus reverse transformation could be done as well).
     */

    /*
    ////////////////////////////////////////////
    // Default namespace information
    ////////////////////////////////////////////
     */

    /**
     * URI of the default namespace for this element; either inherited
     * from parent, or locally declared
     */
    String mDefaultNsUri;

    /**
     * Whether there is a local default namespace declaration or not
     */
    boolean mDefaultNsDeclared;

    boolean mDefaultNsOutput;

    /*
    ////////////////////////////////////////////
    // Other namespace declarations
    ////////////////////////////////////////////
     */

    StringVector mNamespaces;

    /**
     * Whether {@link mNamespaces} instance is shared (ie. using same
     * as parent) or not; if it is, and modifications are needed, a local
     * copy (copy-on-write) needs to be taken before modifications.
     */
    boolean mNsShared;

    /**
     * Set of flags for all locally declared non-default namespaces (if
     * any). Used to verify (if necessary), that all declared prefixes
     * are output for the element.
     */
    BitSet mNsOutput;

    int mLocalNsStart;

    int mLocalNsEnd;

    int mNextAutomaticNsId = 1;

    /*
    ////////////////////////////////////////////
    // Attribute information
    ////////////////////////////////////////////
     */

    HashMap mAttr = null;

    /*
    //////////////////////////////////////////////////
    // Life-cycle (create, update state)
    //////////////////////////////////////////////////
     */

    /**
     * @param parent Parent element; element that encloses this element
     *   in XML output
     * @param decl (optional) Namespace declarations that are new for
     *   this element; null if nothing declared.
     * @param checkNs If true, caller will be doing further checks for
     *   namespace validity; if false, it will not.
     */
    public OutputElement(OutputElement parent, String localName,
                         Declarations decl, boolean checkNs)
        throws XMLStreamException
    {
        mParent = parent;
        mPrefix = ""; // will be initialized later on!
        mLocalName = localName;

        mDefaultNsOutput = false;
        mNextAutomaticNsId = parent.mNextAutomaticNsId;

        String defaultNsUri;
        StringVector otherNs; // namespaces declared for this element

        if (decl == null) { // will inherit everything from parent
            defaultNsUri = null;
            otherNs = null;
        } else { // need to mix'n match:
            defaultNsUri = decl.getDefaultNsUri();
            otherNs = decl.getNamespaces();
        }

        if (defaultNsUri == null) {
            mDefaultNsUri = parent.mDefaultNsUri;
            mDefaultNsDeclared = false;
        } else {
            mDefaultNsUri = defaultNsUri;
            mDefaultNsDeclared = true;
        }

        if (otherNs == null || otherNs.isEmpty()) {
            // Can share parent's namespaces... at least initially
            mNamespaces = parent.mNamespaces;
            mNsShared = true;
            mLocalNsStart = mLocalNsEnd = 0;
            mNsOutput = null;
        } else {
            StringVector orig = parent.mNamespaces;
            /* Otherwise need to make a copy; let's also ensure there's room
             * for few more namespace declarations (4) without resize.
             */
            int otherSize = otherNs.size();
            int extraSize = otherSize + 8;
            if (extraSize < 8) {
                extraSize = 8;
            }
            mNamespaces = orig.makeCopy(extraSize);
            mNsShared = false;
            mLocalNsStart = orig.size();
            mLocalNsEnd = mLocalNsStart + otherSize;
            mNsOutput = checkNs ? new BitSet(otherSize >> 1) : null;

            for (int i = 0, len = otherNs.size(); i < len; i += 2) {
                String prefix = otherNs.getString(i);
                String nsURI = otherNs.getString(i+1);

                // Do we need to override something?
                int ix = mNamespaces.findLastIndexNonInterned(prefix);
                if (ix >= 0) { // masks an earlier declaration!
                    /* ... need to clear that earlier one, then; since it
                     * won't be visible at this scope, and should not be
                     * used for anything any more
                     */
                    mNamespaces.setString(ix, null);
                    mNamespaces.setString(ix+1, null);
                }
                // Either way, have to add it too
                mNamespaces.addStrings(prefix, nsURI);
            }
        }
    }

    /**
     * Constructor that is only used to create the singleton 'root' instance
     * that is used as the parent of the actual document specific root
     * element.
     */
    private OutputElement()
    {
        mParent = null;
        mPrefix = ""; // shouldn't be used ever
        mLocalName = ""; // - "" -

        mDefaultNsUri = ""; // so it'll be inherited
        mDefaultNsDeclared = true; // doesn't really matter
        mDefaultNsOutput = true; // - "" -
        mNamespaces = new StringVector(2); // No namespaces to declare
        mNsShared = false; // shouldn't matter
        mNsOutput = null;

        mLocalNsStart = mLocalNsEnd = 0; // shouldn't matter
    }

    public final static OutputElement getRootInstance() {
        return sRootInstance;
    }

    /**
     * Initialization method that may need to be called, since real prefix
     * to use may not be known when element object is created.
     */
    public void setPrefix(String prefix) {
        mPrefix = prefix;
    }

    /*
    //////////////////////////////////////////////////
    // NamespaceContext implementation
    //////////////////////////////////////////////////
     */

    public String getNamespaceURI(String prefix) {
        if (prefix.length() == 0) { //default NS
            return mDefaultNsUri;
        }
        return mNamespaces.findLastNonInterned(prefix);
    }

    public String getDefaultNsUri() { return mDefaultNsUri; }

    public String getPrefix(String nsURI) {
        if (mDefaultNsUri.equals(nsURI)) {
            return "";
        }
        return mNamespaces.findLastByValueNonInterned(nsURI);
    }

    public Iterator getPrefixes(String nsURI) {
        return getPrefixes(nsURI, new ArrayList(), false);
    }

    /**
     * Called by {@link Declarations}, to get all prefixes bound.
     */
    protected Iterator getPrefixes(String nsURI, ArrayList l,
                                   boolean defaultMatched)
    {
        if (!defaultMatched) {
            if (mDefaultNsUri.equals(nsURI)) {
                l.add("");
            }
        }
        String[] strs = mNamespaces.getInternalArray();
        for (int i = mNamespaces.size(); (i -= 2) >= 0; ) {
            String uri = strs[i+1];
            if (uri.equals(nsURI)) {
                l.add(strs[i]);
            }
        }
        return l.iterator();
    }

    /*
    //////////////////////////////////////////////////
    // Public API, accessors
    //////////////////////////////////////////////////
     */

    public OutputElement getParent() { return mParent; }
    public String getLocalName() { return mLocalName; }
    public String getPrefix() { return mPrefix; }

    public boolean isRoot() { return this == sRootInstance; }

    /**
     * Method that will try to find a prefix that maps to specific namespace
     * URI.
     */
    public String findPrefix(String nsURI, boolean defaultNsOk)
        throws XMLStreamException
    {
        if (defaultNsOk) {
            if (nsURI == mDefaultNsUri) {
                return "";
            }
            if (nsURI == null) {
                if (mDefaultNsUri.length() == 0) {
                    return "";
                }
            } else if (nsURI.equals(mDefaultNsUri)) {
                return "";
            }
        }
        /* 26-Sep-2004, TSa: Need to handle 'xml' prefix and its associated
         *   URI; they are always declared by default
         */
        if (sXmlNsURI.equals(nsURI)) {
            return sXmlNsPrefix;
        }
        return mNamespaces.findLastByValueNonInterned(nsURI);
    }

    /*
    //////////////////////////////////////////////////
    // Public API, validation
    //////////////////////////////////////////////////
     */

    /**
     * Method that verifies that write attempt of the default namespace
     * matches with earlier declaration of the default namespace.
     */
    public void checkDefaultNsWrite(String nsURI)
        throws XMLStreamException
    {
        if (!mDefaultNsDeclared) {
            throw new XMLStreamException("Default namespace not declared for element '"+getElementName()+"'.");
        }
        if (!nsURI.equals(mDefaultNsUri)) {
            throw new XMLStreamException("Default namespace declared as '"+mDefaultNsUri+"'; trying to output it as '"+nsURI+"'.");
        }

        // Let's mark it as having been output, then
        mDefaultNsOutput = true;
    }

    /**
     * Method that verifies that a write attempt of a namespace corresponds
     * to an earlier declaration of the namespace; if not, throws an
     * exception.
     */
    public void checkNsWrite(NamespaceContext root, String prefix, String nsURI)
        throws XMLStreamException
    {
        /* We could just call checkPrefixValidity first, but that would
         * also check parent elements... and we don't want to do that; should
         * actually have closer declaration.
         */
        for (int i = mLocalNsStart; i < mLocalNsEnd; i += 2) {
            String currPrefix = mNamespaces.getString(i);
            if (prefix != currPrefix && !prefix.equals(currPrefix)) {
                continue;
            }
            String currURI = mNamespaces.getString(i+1);
            if (currURI == nsURI || currURI.equals(nsURI)) {
                int ix = (i - mLocalNsStart) >> 1;
                mNsOutput.set(ix);
                return;
            }
        }

        /* Now, alternate is that the root namespace context (if any) has
         * the mapping...
         */
        if (root != null) {
            String uri = root.getNamespaceURI(prefix);
            if (uri != null && uri.equals(nsURI)) {
                return;
            }
        }

        throw new XMLStreamException("Trying to write undeclared namespace (prefix '"+prefix+"', URI '"+nsURI+"'.");
    }

    /**
     * Method that verifies that passed-in prefix indeed maps to the specified
     * namespace URI; and depending on how it goes returns a status for
     * caller.
     *
     * @param isElement If true, rules for the default NS are those of elements
     *   (ie. empty prefix can map to non-default namespace); if false,
     *   rules are those of attributes (only non-default prefix can map to
     *   a non-default namespace).
     *
     * @return PREFIX_OK, if passed-in prefix matches matched-in namespace URI
     *    in current scope; PREFIX_UNBOUND if it's not bound to anything, 
     *    and PREFIX_MISBOUND if it's bound to another URI.
     *
     * @throws XMLStreamException True if default (no) prefix is allowed to
     *    match a non-default URI (elements); false if not (attributes)
     */
    public int isPrefixValid(String prefix, String nsURI,
                             boolean checkNS, boolean isElement)
        throws XMLStreamException
    {
        // Hmmm.... caller shouldn't really pass null.
        if (nsURI == null) {
            nsURI = "";
        }

        /* First thing is to see if specified prefix is bound to a namespace;
         * and if so, verify it matches with data passed in:
         */

        // Checking default namespace?
        if (prefix == null || prefix.length() == 0) {
            /* This basically means caller wants to use "no namespace" for
             * an attribute... which is fine.
             */
            if (!isElement) {
                return PREFIX_OK;
            }
            // It's fine for elements only if the URI actually matches:
            if (nsURI == mDefaultNsUri || nsURI.equals(mDefaultNsUri)) {
                return PREFIX_OK;
            }
            return PREFIX_MISBOUND;
        }

        /* 26-Sep-2004, TSa: Need to handle 'xml' prefix and its associated
         *   URI; they are always declared by default
         */
        if (prefix.equals(sXmlNsPrefix)) {
            // Should we thoroughly verify its namespace matches...?
            if (checkNS) {
                if (!nsURI.equals(sXmlNsURI)) {
                    throwOutputError("Namespace prefix '"+sXmlNsPrefix
                                     +"' can not be bound to non-default namespace ('"+nsURI+"'); has to be the default '"
                                     +sXmlNsURI+"'");
                }
            }
            return PREFIX_OK;
        }

        // Nope checking some other namespace
        String act = mNamespaces.findLastNonInterned(prefix);
        
        // Just fine as is?
        if (act == null) {
            return PREFIX_UNBOUND;
        }
        return (act == nsURI || act.equals(nsURI)) ?
            PREFIX_OK : PREFIX_MISBOUND;
    }

    public void checkAttrWrite(String nsURI, String localName, String value)
        throws XMLStreamException
    {
        AttrName an = new AttrName(nsURI, localName);
        if (mAttr == null) {
            mAttr = new HashMap();
            mAttr.put(an, value);
        } else {
            Object old = mAttr.put(an, value);
            if (old != null) {
                throw new XMLStreamException("Duplicate attribute write for attribute '"+an+"' (previous value '"+old+"', new value '"+value+"').");
            }
        }
    }

    /**
     * Method called by writer when start/empty element is closed, and
     * no more attributes/namespaces can be added. If so, we better check
     * that all declared namespaces were indeed written out.
     */
    public void checkAllNsWrittenOk()
        throws XMLStreamException
    {
        // We can also help GC a bit, by discarding any attribute values
        // we may have collected so far... no new ones can be output for
        // this element.
        mAttr = null;

        // Ok, first; has default namespace been written?
        if (mDefaultNsDeclared) {
            if (!mDefaultNsOutput) {
                throw new XMLStreamException("Default namespace declared but not written, for element '"+getElementName()+"'.");
            }
        }

        // Then how about other declared namespaces?
        int count = (mLocalNsEnd - mLocalNsStart) >> 1;
        for (int i = 0; i < count; ++i) {
            if (!mNsOutput.get(i)) {
                throw new XMLStreamException("Namespace with prefix '"
                                             +(mNamespaces.getString(mLocalNsStart + i + i))
                                             +"' declared but not output in element '"+
                                             getElementName()+"'.");
            }
        }
    }

    /**
     * Method called by the writer to write all bound namespace prefixes;
     * done if namespace-repairing is enabled.
     */
    public void outputDeclaredNamespaces(Writer w)
        throws IOException, XMLStreamException
    {
        /* First, do we need to output the default namespace?
         */
        if (mDefaultNsDeclared) {
            mDefaultNsOutput = true;
            w.write(' ');
            w.write(XMLConstants.XMLNS_ATTRIBUTE);
            w.write("=\"");
            w.write(mDefaultNsUri);
            w.write('"');
        }

        // Then how about other declared namespaces?
        int count = (mLocalNsEnd - mLocalNsStart) >> 1;

        if (count > 0) {
            BitSet done = mNsOutput;
            if (done == null) { // if we started as shared
                mNsOutput = done = new BitSet();
            }

            for (int i = 0; i < count; ++i) {
                if (!done.get(i)) { // not yet output:
                    done.set(i);
                    w.write(' ');
                    w.write(XMLConstants.XMLNS_ATTRIBUTE);
                    int ix = mLocalNsStart + i + i;
                    String prefix = mNamespaces.getString(ix);
                    w.write(':');
                    w.write(prefix);
                    w.write("=\"");
                    String uri = mNamespaces.getString(ix+1);
                    w.write(uri);
                    w.write('"');
                }
            }
        }
    }

    /*
    //////////////////////////////////////////////////
    // Public API, state changes
    //////////////////////////////////////////////////
     */

    /**
     * Method called to 'declare' namespace for this element; called
     * when an automatic namespace is generated.
     */
    public void addPrefix(String prefix, String nsURI)
        throws XMLStreamException
    {
        /* 21-Sep-2004, TSa: If NS list was shared, need to first unshare
         *   it..
         */
        if (mNsShared) {
            StringVector orig = mNamespaces;
            mNamespaces = orig.makeCopy(8);
            mLocalNsStart = orig.size();
            mNsOutput = new BitSet();
        }
        mNamespaces.addStrings(prefix, nsURI);
        mLocalNsEnd = mNamespaces.size();
    }

    public void setDefaultNs(String defNsUri)
    {
        mDefaultNsDeclared = true;
        mDefaultNsUri = defNsUri;
    }

    public String generatePrefix(NamespaceContext ctxt, String prefixBase)
    {
        String prefix;
        while (true) {
            int nr = mNextAutomaticNsId++;
            // We better intern the resulting prefix...
            prefix = (prefixBase + nr).intern();
            if (mNamespaces.findLastNonInterned(prefix) != null) {
                continue;
            }
            if (ctxt != null && ctxt.getNamespaceURI(prefix) != null) {
                continue;
            }
            break;
        }

        return prefix;
    }

    /**
     * Method mostly used for error/debug messages.
     */
    public String getElementName() {
        if (mPrefix == null || mPrefix.length() == 0) {
            return mLocalName;
        }
        return mPrefix + ":" + mLocalName;
    }

    /*
    //////////////////////////////////////////////////
    // Internal methods:
    //////////////////////////////////////////////////
     */

    private void throwOutputError(String msg)
        throws XMLStreamException
    {
        throw new XMLStreamException(msg);
    }

    /*
    //////////////////////////////////////////////////
    // Helper classes:
    //////////////////////////////////////////////////
     */

    /**
     * Simple key class used to represent two-piece (attribute) names;
     * first part being optional (URI), and second non-optional (local name).
     */
    final static class AttrName
        implements Comparable
    {
        final String mNsURI;
        final String mLocalName;

        /**
         * Let's cache the hash code, since although hash calculation is
         * fast, hash code is needed a lot as this is always used as a 
         * HashMap/TreeMap key.
         */
        final int mHashCode;

        public AttrName(String nsURI, String localName) {
            mNsURI = (nsURI == null) ? "" : nsURI;
            mLocalName = localName;
            mHashCode = mNsURI.hashCode() * 31 ^ mLocalName.hashCode();
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof AttrName)) {
                return false;
            }
            AttrName other = (AttrName) o;
            String otherLN = other.mLocalName;
            // Local names are shorter, more varying:
            if (otherLN != mLocalName && !otherLN.equals(mLocalName)) {
                return false;
            }
            String otherURI = other.mNsURI;
            return (otherURI == mNsURI || otherURI.equals(mNsURI));
        }

        public String toString() {
            if (mNsURI.length() > 0) {
                return "{"+mNsURI + "} " +mLocalName;
            }
            return mLocalName;
        }

        public int hashCode() {
            return mHashCode;
        }

        public int compareTo(Object o) {
            AttrName other = (AttrName) o;
            // Let's first order by namespace:
            int result = mNsURI.compareTo(other.mNsURI);
            if (result == 0) {
                result = mLocalName.compareTo(other.mLocalName);
            }
            return result;
        }
    }

    /**
     * Container class used to store namespace prefix declarations (by
     * stream writer), passed to OutputElement's constructor.
     *<p>
     * Note that this class also implements {@link NamespaceContext}, so
     * that writers can easily get access to such instance.
     */
    public final static class Declarations
        implements NamespaceContext
    {
        /**
         * No need to use a huge upper limit; let's guess that most often
         * there won't be more than 8 namespaces for an element
         */
        final static int TYPICAL_MAX_NS_COUNT = 8;

        final OutputElement mParent;

        private String mDefaultNsUri = null;

        private StringVector mNamespaces = null;

        public Declarations(OutputElement parent) {
            mParent = parent;
        }

        /*
        //////////////////////////////////////////////////
        // NamespaceContext implementation
        //////////////////////////////////////////////////
        */
        
        public String getNamespaceURI(String prefix)
        {
            if (prefix.length() == 0) { //default NS
                if (mDefaultNsUri != null) {
                    return mDefaultNsUri;
                }
            }
            if (mNamespaces != null) {
                String str = mNamespaces.findLastNonInterned(prefix);
                if (str != null) {
                    return str;
                }
            }
            return mParent.getNamespaceURI(prefix);
        }
        
        public String getPrefix(String nsURI) {
            if (mDefaultNsUri != null && mDefaultNsUri.equals(nsURI)) {
                return "";
            }
            if (mNamespaces != null) {
                String str = mNamespaces.findLastByValueNonInterned(nsURI);
                if (str != null) {
                    return str;
                }
            }
            return mParent.getPrefix(nsURI);
        }

        public Iterator getPrefixes(String nsURI)
        {
            boolean defaultMatches = false;
            ArrayList l = new ArrayList();
            if (mDefaultNsUri != null) {
                if (mDefaultNsUri.equals(nsURI)) {
                    l.add("");
                    defaultMatches = true;
                }
            }
            if (mNamespaces != null) {
                String[] strs = mNamespaces.getInternalArray();
                for (int i = mNamespaces.size(); (i -= 2) >= 0; ) {
                    String uri = strs[i+1];
                    if (uri.equals(nsURI)) {
                        l.add(strs[i]);
                    }
                }
            }
            return mParent.getPrefixes(nsURI, l, defaultMatches);
        }

        /*
        //////////////////////////////////////////////////
        // Other methods
        //////////////////////////////////////////////////
        */

        public String getDefaultNsUri() { return mDefaultNsUri; }

        public StringVector getNamespaces() { return mNamespaces; }

        public void setDefaultNsUri(String uri)
            throws XMLStreamException
        {
            if (uri == null) {
                uri = "";
            }
            /* ??? 09-May-2004, TSa: Should it be an error to re-declare
             *   the default namespace? At least if it's to be bound with
             *   a different URI?
             */
            /* 07-Mar-2005, TSa: Actually, no; let's not check that, since
             *   there may be cases where it needs to be changed (esp.
             *   for repairing writer, which may need override the suggested
             *   mapping)
             */
            /*
            if (mDefaultNsUri != null && !mDefaultNsUri.equals(uri)) {
                throw new XMLStreamException("Trying to re-declare default namespace from '"+mDefaultNsUri+"' to '"+uri+"'");
            }
            */
            mDefaultNsUri = uri;
        }

        public void addNamespace(String prefix, String uri)
            throws XMLStreamException
        {
            if (prefix.length() == 0) {
                setDefaultNsUri(uri);
                return;
            }

            if (mNamespaces == null) {
                mNamespaces = new StringVector(TYPICAL_MAX_NS_COUNT * 2);
                mNamespaces.addStrings(prefix, uri);
                return;
            }

            // Do we have declaration already?
            String old = mNamespaces.findLastNonInterned(prefix);
            if (old != null) {
                // Let's allow it, but only if the old URI was the same:
                if (!old.equals(uri)) {
                    throw new XMLStreamException("Trying to reset namespace with prefix '"+prefix+"'; previous URI was '"+old+"', trying to reset to '"
                                                 +uri+"'.");
                }
            } else {
                mNamespaces.addStrings(prefix, uri);
            }
        }
    }


}
