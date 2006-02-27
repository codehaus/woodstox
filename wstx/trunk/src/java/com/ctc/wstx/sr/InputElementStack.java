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

import java.util.Iterator;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.AttributeInfo;
import org.codehaus.stax2.validation.ValidationContext;
import org.codehaus.stax2.validation.XMLValidator;
import org.codehaus.stax2.validation.XMLValidationException;
import org.codehaus.stax2.validation.XMLValidationProblem;

import com.ctc.wstx.cfg.ErrorConsts;
import com.ctc.wstx.cfg.XmlConsts;
import com.ctc.wstx.exc.WstxException;
import com.ctc.wstx.exc.WstxValidationException;
import com.ctc.wstx.util.BaseNsContext;
import com.ctc.wstx.util.SingletonIterator;
import com.ctc.wstx.util.StringVector;
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
    implements AttributeInfo, NamespaceContext, ValidationContext
{
    protected InputProblemReporter mReporter = null;

    protected final boolean mInternNsURIs;

    /**
     * State flag that indicates whether we are processing an xml 1.1
     * document or not.
     */
    protected final boolean mXml11;

    /*
    //////////////////////////////////////////////////
    // Element validation (optional)
    //////////////////////////////////////////////////
    */

    /**
     * Optional validator object that will get called if set,
     * and that can validate xml content. Note that it is possible
     * that this is set to a proxy object that calls multiple
     * validators in sequence.
     */
    protected XMLValidator mValidator = null;

    /*
    //////////////////////////////////////////////////
    // Life-cycle (create, update state)
    //////////////////////////////////////////////////
     */

    protected InputElementStack(boolean internNsURIs, boolean xml11)
    {
        mInternNsURIs = internNsURIs;
        mXml11 = xml11;
    }

    protected void connectReporter(InputProblemReporter rep)
    {
        mReporter = rep;
    }

    public void setValidator(XMLValidator validator) {
        mValidator = validator;
    }

    /**
     * Method called by the (validating) stream reader, to indicate that
     * this input element stack needs to co-operate with an object
     * that can provide default namespace declarations. While messy, this
     * is needed to let DTDs work well with legacy handling of namespace
     * declarations for some standard DTDs.
     */
    protected abstract void connectNsDefaultProvider(NsDefaultProvider provider);

    protected boolean hasDTDValidator() {
        /* !!! 26-Nov-2005, TSa: Should be fixed once other pluggable
         *   validators are allowed
         */
        return (mValidator != null);
    }

    /**
     * Method called by {@link BasicStreamReader}, to retrieve the
     * attribute collector it needs for some direct access.
     */
    protected abstract AttributeCollector getAttrCollector();

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
        throws XMLStreamException;

    /**
     * Method called to resolve element and attribute namespaces (in
     * namespace-aware mode), and do optional validation using pluggable
     * validator object.
     *
     * @return Text content validation state that should be effective
     *   for the fully resolved element context
     */
    public abstract int resolveAndValidateElement()
        throws XMLStreamException;

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
    public abstract int getIdAttributeIndex();

    /**
     * Default implementation just indicates it does not know of such
     * attributes; this because that requires DTD information that only
     * some implementations have.
     */
    public abstract int getNotationAttributeIndex();

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
    // ValidationContext
    ///////////////////////////////////////////////////
     */

    public final String getXmlVersion() {
        return mXml11 ? XmlConsts.XML_V_11_STR : XmlConsts.XML_V_10_STR;
    }

    // Part of Stax2, see above:
    //public int getAttributeCount();

    public String getAttributeLocalName(int index) {
        return getAttrCollector().getLocalName(index);
    }

    public String getAttributeNamespace(int index) {
        return getAttrCollector().getURI(index);
    }

    public String getAttributePrefix(int index) {
        return getAttrCollector().getPrefix(index);
    }

    public String getAttributeValue(int index) {
        return getAttrCollector().getValue(index);
    }

    public String getAttributeValue(String nsURI, String localName)
    {
        int ix = findAttributeIndex(nsURI, localName);
        return (ix < 0) ? null : getAttributeValue(ix);
    }

    // Part of Stax2, see above:
    //public int findAttributeIndex(String nsURI, String localName);

    public boolean isNotationDeclared(String name)
    {
        // !!! TBI
        return false;
    }

    public boolean isUnparsedEntityDeclared(String name)
    {
        // !!! TBI
        return false;
    }

    public String getBaseUri()
    {
        // !!! TBI
        return null;
    }

    public abstract QName getCurrentElementName();

    // This was defined above for NamespaceContext
    //public String getNamespaceURI(String prefix);

    public Location getValidationLocation()
    {
        return mReporter.getLocation();
    }

    public void reportProblem(XMLValidationProblem problem)
        throws XMLValidationException
    {
        mReporter.reportValidationProblem(problem);
    }

    /**
     * Method called by actual validator instances when attributes with
     * default values have no explicit values for the element; if so,
     * default value needs to be added as if it was parsed from the
     * element.
     */
    public abstract int addDefaultAttribute(String localName, String uri, String prefix,
                                            String value);
    /*
    ///////////////////////////////////////////////////
    // Support for NsDefaultProvider
    ///////////////////////////////////////////////////
     */

    public abstract String getLocalNsURI(String internedPrefix);

    public abstract void addNsBinding(String prefix, String uri);

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
    public abstract String getAttributeType(int index);
}
