/* Woodstox XML processor
 *
 * Copyright (c) 2004 Tatu Saloranta, tatu.saloranta@iki.fi
 *
 * Licensed under the License specified in the file LICENSE,
 * included with the source code.
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;

import org.codehaus.stax2.XMLStreamReader2;

import com.ctc.wstx.api.WriterConfig;
import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.sr.AttributeCollector;
import com.ctc.wstx.sr.InputElementStack;
import com.ctc.wstx.sr.StreamReaderImpl;
import com.ctc.wstx.util.DefaultXmlSymbolTable;
import com.ctc.wstx.util.XMLQuoter;

/**
 * Namespace-aware implementation of {@link XMLStreamWriter}, that does
 * namespace repairing, ie resolves possible conflicts between prefixes
 * (add new bindings as necessary), as well as automatically creates
 * namespace declarations as necessary.
 */
public class RepairingNsStreamWriter
    extends BaseNsStreamWriter
{
    /*
    ////////////////////////////////////////////////////
    // Configuration (options, features)
    ////////////////////////////////////////////////////
     */

    // // // Additional specific config flags base class doesn't have

    final String mAutomaticNsPrefix;

    /*
    ////////////////////////////////////////////////////
    // Additional state
    ////////////////////////////////////////////////////
     */

    /**
     * Sequence number used for generating dynamic namespace prefixes.
     * Array used as a wrapper to allow for easy sharing of the sequence
     * number.
     */
    int[] mAutoNsSeq = null;

    String mSuggestedDefNs = null;

    /**
     * Map that contains URI-to-prefix entries that point out suggested
     * prefixes for URIs. These are populated by calls to
     * {@link #setPrefix}, and they are only used as hints for binding;
     * if there are conflicts, repairing writer can just use some other
     * prefix.
     */
    HashMap mSuggestedPrefixes = null;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle (ctors)
    ////////////////////////////////////////////////////
     */

    public RepairingNsStreamWriter(Writer w, WriterConfig cfg)
    {
        super(w, cfg, true);
        mAutomaticNsPrefix = cfg.getAutomaticNsPrefix();
    }

    /*
    ////////////////////////////////////////////////////
    // XMLStreamWriter API
    ////////////////////////////////////////////////////
     */

    //public NamespaceContext getNamespaceContext()
    //public void setNamespaceContext(NamespaceContext context)
    //public String getPrefix(String uri)
    //public void setPrefix(String prefix, String uri)
    //public void setDefaultNamespace(String uri)

    //public void writeAttribute(String localName, String value)

    public void writeAttribute(String nsURI, String localName, String value)
        throws XMLStreamException
    {
        // No need to set mAnyOutput, nor close the element
        if (!mStartElementOpen) {
            throw new IllegalStateException(ErrorConsts.WERR_ATTR_NO_ELEM);
        }
        doWriteAttr(localName, nsURI, findOrCreateAttrPrefix(null, nsURI, mCurrElem),
                    value);
    }

    public void writeAttribute(String prefix, String nsURI,
                               String localName, String value)
        throws XMLStreamException
    {
        if (!mStartElementOpen) {
            throw new IllegalStateException(ErrorConsts.WERR_ATTR_NO_ELEM);
        }

        doWriteAttr(localName, nsURI, findOrCreateAttrPrefix(null, nsURI, mCurrElem),
                    value);
    }

    public void writeDefaultNamespace(String nsURI)
        throws XMLStreamException
    {
        /* 21-Sep-2004, TSa: Should not call this in "namespace repairing"
         *    mode. However, if it is called, what should be done? There
         *    are multiple possibilities; like:
         *   (a) Throw an exception
         *   (b) Ignore the call
         *   (c) Check potential validity; ignore if it matched a declaration,
         *     throw an exception if it didn't.
         *
         *  For now, let's do (b) or (c), depending on whether we are to do
         * more thorough namespace output verification
         */
        // ... still, shouldn't get called in the wrong time, ever:
        if (!mStartElementOpen) {
            throw new IllegalStateException(ERR_NSDECL_WRONG_STATE);
        }

        // ... Ok, should we verify something?

        //throwOutputError("Should not call writeNamespace() for namespace-repairing writers");
    }

    //public void writeEmptyElement(String localName) throws XMLStreamException

    public void writeNamespace(String prefix, String nsURI)
        throws XMLStreamException
    {
        /* 21-Sep-2004, TSa: Shouldn't get called in namespace-repairing
         *    mode; see discussion in 'writeDefaultNamespace()' for details.
         */
        if (!mStartElementOpen) {
            throw new IllegalStateException(ERR_NSDECL_WRONG_STATE);
        }

        // Ok... should we try to do some validation?

        //throwOutputError("Should not call writeNamespace() for namespace-repairing writers");
    }

    /*
    ////////////////////////////////////////////////////
    // Package methods:
    ////////////////////////////////////////////////////
     */

    /**
     * With repairing writer, this is only taken as a suggestion as to how
     * the caller would prefer prefixes to be mapped.
     */
    public void setDefaultNamespace(String uri)
        throws XMLStreamException
    {
	mSuggestedDefNs = (uri == null || uri.length() == 0) ? null : uri;
    }

    public void doSetPrefix(String prefix, String uri)
        throws XMLStreamException
    {
        /* Ok; let's assume that passing in a null or empty String as
         * the URI means that we don't want passed prefix to be preferred
         * for any URI.
         */
        if (uri == null || uri.length() == 0) {
            if (mSuggestedPrefixes != null) {
                for (Iterator it = mSuggestedPrefixes.entrySet().iterator();
                     it.hasNext(); ) {
                    Map.Entry en = (Map.Entry) it.next();
                    String thisP = (String) en.getValue();
                    if (thisP.equals(prefix)) {
                        it.remove();
                    }
                }
            }
        } else {
            if (mSuggestedPrefixes == null) {
                mSuggestedPrefixes = new HashMap(16);
            }
            mSuggestedPrefixes.put(uri, prefix);
        }
    }

    public void writeStartElement(StartElement elem)
        throws XMLStreamException
    {
        /* In repairing mode this is simple: let's just pass info
         * we have, and things should work... a-may-zing!
         */
        QName name = elem.getName();
        writeStartElement(name.getPrefix(), name.getLocalPart(),
                          name.getNamespaceURI());
        Iterator it = elem.getAttributes();
        while (it.hasNext()) {
            Attribute attr = (Attribute) it.next();
            name = attr.getName();
            writeAttribute(name.getPrefix(), name.getNamespaceURI(),
                           name.getLocalPart(), attr.getValue());
        }
    }

    //public void writeEndElement(QName name) throws XMLStreamException

    protected void writeStartOrEmpty(String localName, String nsURI)
        throws XMLStreamException
    {
        checkStartElement(localName);

        // Need a prefix....
        String prefix = findElemPrefix(nsURI, mCurrElem);
        if (prefix != null) { // prefix ok, easy
            mCurrElem = mCurrElem.createChild(prefix, localName);
            doWriteStartElement(prefix, localName);
        } else { // no prefix, more work
            prefix = generateElemPrefix(null, nsURI, mCurrElem);
            mCurrElem = mCurrElem.createChild(prefix, localName);
            mCurrElem.setPrefix(prefix);
            doWriteStartElement(prefix, localName);
            if (prefix == null || prefix.length() == 0) { // def NS
                mCurrElem.setDefaultNsUri(nsURI);
                doWriteNamespace(null, nsURI);
            } else { // explicit NS
                mCurrElem.addPrefix(prefix, nsURI);
                doWriteNamespace(prefix, nsURI);
            }
        }
    }

    protected void writeStartOrEmpty(String suggPrefix, String localName, String nsURI)
        throws XMLStreamException
    {
        checkStartElement(localName);

        // In repairing mode, better ensure validity:
        String actPrefix = validateElemPrefix(suggPrefix, nsURI, mCurrElem);
        if (actPrefix != null) { // fine, an existing binding we can use:
            mCurrElem = mCurrElem.createChild(actPrefix, localName);
            doWriteStartElement(actPrefix, localName);
        } else { // nah, need to create a new binding...
            actPrefix = generateElemPrefix(suggPrefix, nsURI, mCurrElem);
            mCurrElem = mCurrElem.createChild(actPrefix, localName);
            mCurrElem.setPrefix(actPrefix);
            doWriteStartElement(actPrefix, localName);
            if (actPrefix == null || actPrefix.length() == 0) { // def NS
                mCurrElem.setDefaultNsUri(nsURI);
                doWriteNamespace(null, nsURI);
            } else { // explicit NS
                mCurrElem.addPrefix(actPrefix, nsURI);
                doWriteNamespace(actPrefix, nsURI);
            }
        }
    }

    /**
     * Element copier method implementation suitable for use with
     * namespace-aware writers in repairing mode.
     * The trickiest thing is having to properly
     * order calls to <code>setPrefix</code>, <code>writeNamespace</code>
     * and <code>writeStartElement</code>; the order writers expect is
     * bit different from the order in which element information is
     * passed in.
     */
    public final void copyStartElement(InputElementStack elemStack,
                                       AttributeCollector attrCollector)
        throws IOException, XMLStreamException
    {
        /* In case of repairing stream writer, we can actually just
         * go ahead and first output the element: stream writer should
         * be able to resolve namespace mapping for the element
         * automatically, as necessary.
         */
        writeStartElement(elemStack.getPrefix(),
                          elemStack.getLocalName(),
                          elemStack.getNsURI());
        
        // Any namespace declarations/bindings?
        int nsCount = elemStack.getCurrentNsCount();
        
        /* Since repairing writer will deal with namespace bindings
         * and declarations automatically, we don't really to do anything
         * here. Could call setPrefix(), but it really should work out as
         * well by just calling output methods which will generate necessary
         * bindings.
         */
        /* And then let's just output attributes, if any (whether to copy
         * implicit, aka "default" attributes, is configurable)
         */
        AttributeCollector ac = mAttrCollector;
        int attrCount = mCfgCopyDefaultAttrs ? ac.getCount() : 
            ac.getSpecifiedCount();

        /* Unlike in non-ns and simple-ns modes, we can not simply literally
         * copy the attributes here. It is possible that some namespace
         * prefixes have been remapped... so need to be bit more careful.
         */
        for (int i = 0; i < attrCount; ++i) {
            /* First; need to make sure that the prefix-to-ns mapping
             * attribute has is valid... and can not output anything
             * before that's done (since remapping will output a namespace
             * declaration!)
             */
            String uri = attrCollector.getNsURI(i);
            String prefix = attrCollector.getPrefix(i);

            mWriter.write(' ');
            /* With attributes, missing/empty prefix always means 'no
             * namespace', can take a shortcut:
             */
            if (prefix == null || prefix.length() == 0) {
                ;
            } else {
                /* and otherwise we'll always have a prefix as attributes
                 * can not make use of the def. namespace...
                 */
                mWriter.write(findOrCreateAttrPrefix(prefix, uri, mCurrElem));
                mWriter.write(':');
            }
            mWriter.write(attrCollector.getLocalName(i));
            mWriter.write('=');
            mWriter.write(DEFAULT_QUOTE_CHAR);
            attrCollector.writeValue(i, mAttrValueWriter);
            mWriter.write(DEFAULT_QUOTE_CHAR);
        }
    }

    /*
    ////////////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////////////
     */

    /**
     * Method called to find an existing prefix for the given namespace,
     * if any exists in the scope. If one is found, it's returned (including
     * "" for the current default namespace); if not, null is returned.
     *
     * @param nsURI URI of namespace for which we need a prefix
     */
    protected final String findElemPrefix(String nsURI, SimpleOutputElement elem)
        throws XMLStreamException
    {
        /* Special case: empty NS URI can only be bound to the empty
         * prefix... so:
         */
        if (nsURI == null || nsURI.length() == 0) {
            String currDefNsURI = elem.getDefaultNsUri();
            if (currDefNsURI != null && currDefNsURI.length() > 0) {
                // Nope; won't do... has to be re-bound, but not here:
                return null;
            }
            return "";
        }
        return mCurrElem.getPrefix(nsURI);
    }

    /**
     * Method called after {@link #findElemPrefix} has returned null,
     * to create and bind a namespace mapping for specified namespace.
     */
    protected final String generateElemPrefix(String suggPrefix, String nsURI,
                                              SimpleOutputElement elem)
        throws XMLStreamException
    {
        /* Ok... now, since we do not have an existing mapping, let's
         * see if we have a preferred prefix to use.
         */
        /* Except if we need the empty namespace... that can only be
         * bound to the empty prefix:
         */
        if (nsURI == null || nsURI.length() == 0) {
            return "";
        }

        /* Ok; with elements this is easy: the preferred prefix can
         * ALWAYS be used, since it can mask preceding bindings:
         */
        if (suggPrefix == null || suggPrefix.length() == 0) {
	    // caller wants this URI to map as the default namespace?
	    if (mSuggestedDefNs != null && mSuggestedDefNs.equals(nsURI)) {
		suggPrefix = "";
	    } else {
		suggPrefix = (mSuggestedPrefixes == null) ? null:
		    (String) mSuggestedPrefixes.get(nsURI);
		if (suggPrefix == null) {
		    if (mAutoNsSeq == null) {
			mAutoNsSeq = new int[1];
			mAutoNsSeq[0] = 1;
		    }
		    suggPrefix = elem.generateMapping(mAutomaticNsPrefix, nsURI,
						      mAutoNsSeq);
		}
            }
        }

        // Ok; let's let the caller deal with bindings
        return suggPrefix;
    }

    /**
     * Method called to somehow find a prefix for given namespace, to be
     * used for a new start element; either use an existing one, or
     * generate a new one. If a new mapping needs to be generated,
     * it will also be automatically bound, and necessary namespace
     * declaration output.
     *
     * @param suggPrefix Suggested prefix to bind, if any; may be null
     *   to indicate "no preference"
     * @param nsURI URI of namespace for which we need a prefix
     * @param elem Currently open start element, on which the attribute
     *   will be added.
     */
    protected final String findOrCreateAttrPrefix(String suggPrefix, String nsURI,
                                                  SimpleOutputElement elem)
        throws XMLStreamException
    {
        if (nsURI == null || nsURI.length() == 0) {
            /* Attributes never use the default namespace; missing
             * prefix always leads to the empty ns... so nothing
             * special is needed here.
             */
             return null;
        }
        // Maybe the suggested prefix is properly bound?
        if (suggPrefix != null) {
            int status = elem.isPrefixValid(suggPrefix, nsURI, false);
            if (status == SimpleOutputElement.PREFIX_OK) {
                return suggPrefix;
            }
            // Otherwise, 
        }

        // If not, perhaps there's another existing binding available?
        String prefix = elem.getExplicitPrefix(nsURI);
        if (prefix != null) { // already had a mapping for the URI... cool.
            return prefix;
        }

        /* Nope, need to create one. First, let's see if there's a
         * preference...
         */
        if (suggPrefix != null) {
            prefix = suggPrefix;
        } else if (mSuggestedPrefixes != null) {
            prefix = (String) mSuggestedPrefixes.get(nsURI);
	    // note: def ns is never added to suggested prefix map
        }

        if (prefix != null) {
            /* Can not use default namespace for attributes.
             * Also, re-binding is tricky for attributes; can't
             * re-bind anything that's bound on this scope... or
             * used in this scope. So, to simplify life, let's not
             * re-bind anything for attributes.
             */
            if (prefix.length() == 0
                || (elem.getNamespaceURI(prefix) != null)) {
                prefix = null;
            }
        }

        if (prefix == null) {
            if (mAutoNsSeq == null) {
                mAutoNsSeq = new int[1];
                mAutoNsSeq[0] = 1;
            }
            prefix = mCurrElem.generateMapping(mAutomaticNsPrefix, nsURI,
                                               mAutoNsSeq);
        }

        // Ok; so far so good: let's now bind and output the namespace:
        elem.addPrefix(prefix, nsURI);
        doWriteNamespace(prefix, nsURI);
        return prefix;
    }

    private final String validateElemPrefix(String prefix, String nsURI,
                                            SimpleOutputElement elem)
        throws XMLStreamException
    {
        /* 06-Feb-2005, TSa: Special care needs to be taken for the
         *   "empty" (or missing) namespace:
         *   (see comments from findOrCreatePrefix())
         */
        if (nsURI.length() == 0) {
            String currURL = elem.getDefaultNsUri();
            if (currURL != null && currURL.length() > 0) {
                // Ok, good:
                return "";
            }
            // Nope, needs to be re-bound:
            return null;
        }
        
        int status = elem.isPrefixValid(prefix, nsURI, true);
        if (status == SimpleOutputElement.PREFIX_OK) {
            return prefix;
        }

        /* Hmmh... now here's bit of dilemma: that particular prefix is
         * either not bound, or is masked... but it is possible some other
         * prefix would be bound. Should we search for another one, or
         * try to re-define suggested one? Let's do latter, for now;
         * caller can then (try to) bind the preferred prefix:
         */
        return null;
    }
}

