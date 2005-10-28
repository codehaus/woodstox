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

import java.util.Iterator;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.AttributeInfo;

import com.ctc.wstx.exc.WstxException;
import com.ctc.wstx.util.BaseNsContext;
import com.ctc.wstx.util.SingletonIterator;
import com.ctc.wstx.util.StringVector;
import com.ctc.wstx.util.SymbolTable;
import com.ctc.wstx.util.TextBuilder;

/**
 * Shared base class that defines API stream reader uses to communicate
 * with the element stack implementation, independent of whether it's
 * operating in namespace-aware or non-namespace modes.
 * Element stack class is used for storing nesting information about open
 * elements, and for namespace-aware mode, also information about
 * namespaces active (including default namespace), during parsing of
 * XML input.
 *<p>
 * This class also implements {@link NamespaceContext}, since it has all
 * the information necessary, so parser can just return element stack
 * instance as necesary.
 */
public abstract class InputElementStack
    implements AttributeInfo, NamespaceContext
{
    /**
     * Constants used when no DTD handling is done, and we do not know the
     * 'real' type of an attribute. Seems like CDATA is the safe choice.
     */
    protected final static String UNKNOWN_ATTR_TYPE = "CDATA";

    protected InputProblemReporter mReporter = null;

    /*
    //////////////////////////////////////////////////
    // Life-cycle (create, update state)
    //////////////////////////////////////////////////
     */

    protected InputElementStack() {
    }

    public void connectReporter(InputProblemReporter rep) {
        mReporter = rep;
    }

    /**
     * Method called by reader right before the root element is handled.
     * Allows validating instances to check if they have validator settings,
     * and if not, to warn about that.
     *<p>
     * Note: default implementation is for non-validating instances, and
     * does nothing.
     */
    public void beforeRoot() {
        ;
    }

    /**
     * Stub implementation for method that specialized sub-classes use.
     * Default implementation is fine for non-validating stacks.
     */
    public void setElementSpecs(Map elemSpecs, SymbolTable symbols,
                                boolean normAttrs, Map generalEntities)
    {
        ;
    }

    public abstract AttributeCollector getAttrCollector();

    /**
     * Method called to construct a non-transient NamespaceContext instance;
     * generally needed when creating events to return from event-based
     * iterators.
     */
    public abstract BaseNsContext createNonTransientNsContext(Location loc);

    /**
     * Method called by the stream reader to add new (start) element
     * into the stack in namespace-aware mode; called when a start element
     * is encountered during parsing, but only in ns-aware mode.
     */
    public abstract void push(String prefix, String localName);

    /**
     * Method called by the stream reader to add new (start) element
     * into the stack in non-namespace mode; called when a start element
     * is encountered during parsing, but only in non-namespace mode.
     */
    public abstract void push(String fullName);

    /**
     * Method called by the stream reader to remove the topmost (start)
     * element from the stack;
     * called when an end element is encountered during parsing.
     *
     * @return Validation state that should be effective for the parent
     *   element state
     */
    public abstract int pop()
        throws WstxException;

    /**
     * Method called to update information about top of the stack, with
     * attribute information passed in. In namespace aware mode, will resolve
     * namespace mapping.
     *
     * @return Validation state that should be effective for the fully
     *   resolved element context
     */
    public abstract int resolveElem(boolean internNsURIs)
        throws WstxException;

    /*
    ///////////////////////////////////////////////////
    // AttributeInfo methods (StAX2)
    ///////////////////////////////////////////////////
     */

    public abstract int getAttributeCount();

    /**
     * @return Index of the specified attribute, if the current element
     *   has such an attribute (explicit, or one created via default
     *   value expansion); -1 if not.
     */
    public abstract int findAttributeIndex(String nsURI, String localName);

    /**
     * Default implementation just indicates it does not know of such
     * attributes; this because that requires DTD information that only
     * some implementations have.
     */
    public int getIdAttributeIndex() {
        return -1;
    }

    /**
     * Default implementation just indicates it does not know of such
     * attributes; this because that requires DTD information that only
     * some implementations have.
     */
    public int getNotationAttributeIndex() {
        return -1;
    }

    /*
    ///////////////////////////////////////////////////
    // Implementation of NamespaceContext:
    ///////////////////////////////////////////////////
     */

    public abstract String getNamespaceURI(String prefix);

    public abstract String getPrefix(String nsURI);

    public abstract Iterator getPrefixes(String nsURI);

    /*
    ///////////////////////////////////////////////////
    // Accessors:
    ///////////////////////////////////////////////////
     */

    // // // Generic properties:

    public abstract boolean isNamespaceAware();

    // // // Generic stack information:


    /**
     * @return Number of open elements in the stack; 0 when parser is in
     *  prolog/epilog, 1 inside root element and so on.
     */
    public abstract int getDepth();

    public abstract boolean isEmpty();

    // // // Information about element at top of stack:

    public abstract String getDefaultNsURI();

    public abstract String getNsURI();

    public abstract String getPrefix();

    public abstract String getLocalName();

    public abstract QName getQName();

    public abstract boolean matches(String prefix, String localName);

    public abstract String getTopElementDesc();

    // // // Namespace information:

    public abstract int getTotalNsCount();

    /**
     * @return Number of active prefix/namespace mappings for current scope,
     *   NOT including mappings from enclosing elements.
     */
    public abstract int getCurrentNsCount();

    public abstract String getLocalNsPrefix(int index);

    public abstract String getLocalNsURI(int index);

    // // // DTD-derived attribute information:

    /**
     * Default implementation just returns the 'unknown' type; validating
     * sub-classes need to override
     */
    public String getAttributeType(int index)
    {
         return UNKNOWN_ATTR_TYPE;
    }
}
