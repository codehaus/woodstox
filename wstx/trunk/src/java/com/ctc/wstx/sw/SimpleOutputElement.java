/* Woodstox XML processor
 *
 * Copyright (c) 2005 Tatu Saloranta, tatu.saloranta@iki.fi
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import com.ctc.wstx.util.BijectiveNsMap;
import com.ctc.wstx.util.EmptyIterator;

/**
 * Class that encapsulates information about a specific element in virtual
 * output stack, for writers that do support namespaces, but do NOT
 * do "repairing", i.e. expect caller to provide full namespace
 * mapping and writing guidance. It does, however, provide rudimentary
 * URI->prefix mappins, for those StAX methods that only take local
 * name and URI arguments.
 */
public final class SimpleOutputElement
    implements NamespaceContext
{
    public final static int PREFIX_UNBOUND = 0;
    public final static int PREFIX_OK = 1;
    public final static int PREFIX_MISBOUND = 2;

    final static String sXmlNsPrefix = XMLConstants.XML_NS_PREFIX;
    final static String sXmlNsURI = XMLConstants.XML_NS_URI;

    /*
    ////////////////////////////////////////////
    // Information about element itself:
    ////////////////////////////////////////////
     */

    final SimpleOutputElement mParent;

    /**
     * Prefix that is used for the element.
     */
    final String mPrefix;

    /**
     *
     */
    final String mLocalName;

    /*
    ////////////////////////////////////////////
    // Namespace binding/mapping information
    ////////////////////////////////////////////
     */

    /**
     * Namespace context end application may have supplied, and that
     * (if given) should be used to augment explicitly defined bindings.
     */
    NamespaceContext mRootNsContext;

    String mDefaultNsURI;

    /**
     * True, if the default namespace URI has been explicitly specified
     * for this element; false if it was inherited from the parent
     * element
     */
    boolean mDefaultNsSet;

    BijectiveNsMap mNsMapping;

    /**
     * True, if {@link #mNsMapping} is a shared copy from the parent;
     * false if a local copy was created (which happens when namespaces
     * get bound etc).
     */
    boolean mNsMapShared;

    /*
    ////////////////////////////////////////////
    // Attribute information
    ////////////////////////////////////////////
     */

    /**
     * Map used to check for duplicate attribute declarations, if
     * feature is enabled.
     */
    HashMap mAttr = null;

    /*
    ////////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////////
     */

    private SimpleOutputElement(SimpleOutputElement parent,
                                String prefix, String localName,
                                BijectiveNsMap ns)
    {
        mParent = parent;
        mPrefix = prefix;
        mLocalName = localName;
        mNsMapping = ns;
        mNsMapShared = (ns != null);
        if (parent == null) {
            mDefaultNsURI = "";
            mRootNsContext = null;
        } else {
            mDefaultNsURI = parent.mDefaultNsURI;
            mRootNsContext = parent.mRootNsContext;
        }
        mDefaultNsSet = false;
    }

    public static SimpleOutputElement createRoot()
     {
        return new SimpleOutputElement(null, "", "", null);
    }

    public SimpleOutputElement createChild(String prefix, String localName)
    {
        SimpleOutputElement elem = new SimpleOutputElement(this, prefix, localName,
                                                           mNsMapping);
        /* At this point we can also discard attribute Map; it is assumed
         * that when a child element has been opened, no more attributes
         * can be output.
         */
        mAttr = null;
        return elem;
    }

    public void setRootNsContext(NamespaceContext ctxt) {
        mRootNsContext = ctxt;
        /* Let's also see if we have an active default ns mapping:
         * (provided it hasn't yet explicitly been set for this element)
         */
        if (!mDefaultNsSet) {
            String defURI = ctxt.getNamespaceURI("");
            if (defURI != null && defURI.length() > 0) {
                mDefaultNsURI = defURI;
            }
        }
    }

    public void addPrefix(String prefix, String uri)
    {
        if (mNsMapping == null) {
            // Didn't have a mapping yet? Need to create one...
            mNsMapping = BijectiveNsMap.createEmpty();
        } else if (mNsMapShared) {
            /* Was shared with parent(s)? Need to create a derivative, to
             * allow for nesting/scoping of new prefix
             */
            mNsMapping = mNsMapping.createChild();
            mNsMapShared = false;
        }
        mNsMapping.addMapping(prefix, uri);
    }

    /*
    ////////////////////////////////////////////
    // Public API, accessors
    ////////////////////////////////////////////
     */

    public SimpleOutputElement getParent() {
        return mParent;
    }

    public boolean isRoot() {
        // (Virtual) Root element has no parent...
        return (mParent == null);
    }

    public String getPrefix() {
        return mPrefix;
    }

    public String getLocalName() {
        return mLocalName;
    }

    public String getDefaultNsUri() {
        return mDefaultNsURI;
    }

    /**
     * Method similar to {@link #getPrefix}, but one that will not accept
     * the default namespace, only an explicit one. Usually used when
     * trying to find a prefix for attributes.
     */
    public String getExplicitPrefix(String uri)
    {
        if (mNsMapping != null) {
            String prefix = mNsMapping.findPrefixByUri(uri);
            if (prefix != null) {
                return prefix;
            }
        }
        if (mRootNsContext != null) {
            String prefix = mRootNsContext.getPrefix(uri);
            if (prefix != null) {
                // Hmmh... still can't use the default NS:
                if (prefix.length() > 0) {
                    return prefix;
                }
                // ... should we try to find an explicit one?
            }
        }
        return null;
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
             * an attribute... which is fine
             */
            if (!isElement) {
                return PREFIX_OK;
            }
            // It's fine for elements only if the URI actually matches:
            if (nsURI == mDefaultNsURI || nsURI.equals(mDefaultNsURI)) {
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
        String act;

        if (mNsMapping != null) {
            act = mNsMapping.findUriByPrefix(prefix);
        } else {
            act = null;
        }

        if (act == null && mRootNsContext != null) {
            act = mRootNsContext.getNamespaceURI(prefix);
        }
 
        // Not (yet) bound...
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

    /*
    ////////////////////////////////////////////
    // Public API, mutators
    ////////////////////////////////////////////
     */

    public void setDefaultNsUri(String uri) {
        mDefaultNsURI = uri;
        mDefaultNsSet = true;
    }

    public String generateMapping(String prefixBase, String uri, int[] seqArr)
    {
        // This is mostly cut'n pasted from addPrefix()...
        if (mNsMapping == null) {
            // Didn't have a mapping yet? Need to create one...
            mNsMapping = BijectiveNsMap.createEmpty();
        } else if (mNsMapShared) {
            /* Was shared with parent(s)? Need to create a derivative, to
             * allow for nesting/scoping of new prefix
             */
            mNsMapping = mNsMapping.createChild();
            mNsMapShared = false;
        }
        return mNsMapping.addGeneratedMapping(prefixBase, mRootNsContext,
                                              uri, seqArr);
    }

    /*
    //////////////////////////////////////////////////
    // NamespaceContext implementation
    //////////////////////////////////////////////////
     */

    public String getNamespaceURI(String prefix)
    {
        if (prefix.length() == 0) { //default NS
            return mDefaultNsURI;
        }
        if (mNsMapping != null) {
            String uri = mNsMapping.findUriByPrefix(prefix);
            if (uri != null) {
                return uri;
            }
        }
        return (mRootNsContext != null) ?
            mRootNsContext.getNamespaceURI(prefix) : null;
    }

    public String getPrefix(String uri)
    {
        if (mDefaultNsURI.equals(uri)) {
            return "";
        }
        if (mNsMapping != null) {
            String prefix = mNsMapping.findPrefixByUri(uri);
            if (prefix != null) {
                return prefix;
            }
        }
        return (mRootNsContext != null) ?
            mRootNsContext.getPrefix(uri) : null;
    }

    public Iterator getPrefixes(String uri)
    {
        List l = null;

        if (mDefaultNsURI.equals(uri)) {
            l = new ArrayList();
            l.add("");
        }
        if (mNsMapping != null) {
            l = mNsMapping.getPrefixesBoundToUri(uri, l);
        }
        // How about the root namespace context? (if any)
        /* Note: it's quite difficult to properly resolve masking, when
         * combining these things (not impossible, just tricky); for now
         * let's do best effort without worrying about masking:
         */
        if (mRootNsContext != null) {
            Iterator it = mRootNsContext.getPrefixes(uri);
            while (it.hasNext()) {
                String prefix = (String) it.next();
                if (prefix.length() == 0) { // default NS already checked
                    continue;
                }
                // slow check... but what the heck
                if (l == null) {
                    l = new ArrayList();
                } else if (l.contains(prefix)) { // double-defined...
                    continue;
                }
                l.add(prefix);
            }
        }
        return (l == null) ? EmptyIterator.getInstance() :
            l.iterator();
    }

    /*
    ////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////
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
}
