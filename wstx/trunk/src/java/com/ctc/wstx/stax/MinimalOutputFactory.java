/* Woodstox XML processor
 *
 * Copyright (c) 2004 Tatu Saloranta, tatu.saloranta@iki.fi
 *
 * Licensed under the License specified in the file LICENSE which is
 * included with the source code.
 * You may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ctc.wstx.stax;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.HashMap;

import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;

import javax.xml.stream.*;

import com.ctc.wstx.api.WstxOutputFactoryConfig;
import com.ctc.wstx.cfg.OutputConfigFlags;
import com.ctc.wstx.sw.WriterConfig;
import com.ctc.wstx.sw.WstxStreamWriter;
import com.ctc.wstx.sw.WstxNonNsStreamWriter;
import com.ctc.wstx.sw.WstxNsStreamWriter;
import com.ctc.wstx.util.ArgUtil;

/**
 * Minimalistic input factory, which implements the suggested J2ME
 * subset of {@link javax.xml.stream.XMLOutputFactory} API: basically
 * just the cursor-based iteration, and classes it needs.
 *<p>
 * Unfortunately, the way StAX 1.0 is defined, this class can NOT be
 * the base class of the full input factory, without getting references
 * to most of StAX event classes. It does however have lots of shared
 * (cut'n pasted code) with {@link com.ctc.wstx.stax.WstxOutputFactory}.
 * Hopefully in future this problem can be resolved.
 */
public final class MinimalOutputFactory
    //extends XMLOutputFactory
    implements OutputConfigFlags
{
    /**
     * Flag used to distinguish "real" minimal implementations and
     * extending non-minimal ones (currently there's such distinction
     * for input factories, for minimal <= validating <= event-based,
     * but not for ouput)
     */
    protected final boolean mIsMinimal;

    /*
    /////////////////////////////////////////////////////
    // Actual storage of configuration settings
    /////////////////////////////////////////////////////
     */

    protected final WriterConfig mConfig;

    /*
    /////////////////////////////////////////////////////
    // Life-cycle
    /////////////////////////////////////////////////////
     */

    protected MinimalOutputFactory(boolean isMinimal) {
        mIsMinimal = isMinimal;
        mConfig = WriterConfig.createJ2MEDefaults();
    }

    /**
     * Need to add this method, since we have no base class to do it...
     */
    public static MinimalOutputFactory newMinimalInstance() {
        return new MinimalOutputFactory(true);
    }

    /*
    /////////////////////////////////////////////////////
    // XMLOutputFactory API
    /////////////////////////////////////////////////////
     */

    //public XMLEventWriter createXMLEventWriter(OutputStream out);

    public XMLStreamWriter createXMLStreamWriter(OutputStream out)
    {
        return createXMLStreamWriter(new OutputStreamWriter(out));
    }

    public XMLStreamWriter createXMLStreamWriter(OutputStream out, String enc)
        throws XMLStreamException
    {
        try {
            return createXMLStreamWriter(new OutputStreamWriter(out, enc));
        } catch (UnsupportedEncodingException ex) {
            throw new XMLStreamException(ex);
        }
    }

    public XMLStreamWriter createXMLStreamWriter(javax.xml.transform.Result result)
        throws XMLStreamException
    {
        return createWstxStreamWriter(result);
    }

    public XMLStreamWriter createXMLStreamWriter(Writer w) {
        return createWstxStreamWriter(w);    
    }
    
    public Object getProperty(String name)
    {
        return mConfig.getProperty(name);
    }
    
    public boolean isPropertySupported(String name) {
        return mConfig.isPropertySupported(name);
    }
    
    public void setProperty(String name, Object value)
    {
        mConfig.setProperty(name, value);
    }

    /*
    /////////////////////////////////////////
    // Type-safe configuration access:
    /////////////////////////////////////////
     */

    // // // Accessors:

    // Standard properies:

    public boolean automaticNamespacesEnabled() {
        return mConfig.automaticNamespacesEnabled();
    }

    // Wstx properies:

    public boolean willSupportNamespaces() {
        return mConfig.willSupportNamespaces();
    }

    public boolean willOutputEmptyElements() {
        return mConfig.willOutputEmptyElements();
    }

    /**
     * @return Prefix to use as the base for automatically generated
     *   namespace prefixes ("namespace prefix prefix", so to speak).
     *   Defaults to "wstxns".
     */
    public String getAutomaticNsPrefix() {
        return mConfig.getAutomaticNsPrefix();
    }

    public boolean willValidateNamespaces() {
        return mConfig.willValidateNamespaces();
    }

    public boolean willValidateStructure() {
        return mConfig.willValidateStructure();
    }

    public boolean willValidateContent() {
        return mConfig.willValidateContent();
    }

    public boolean willValidateAttributes() {
        return mConfig.willValidateAttributes();
    }

    // // // Mutators:

    // Standard properies:

    public void enableAutomaticNamespaces(boolean state) {
        mConfig.enableAutomaticNamespaces(state);
    }

    // Wstx properies:

    public void doSupportNamespaces(boolean state) {
        mConfig.doSupportNamespaces(state);
    }

    public void doOutputEmptyElements(boolean state) {
        mConfig.doOutputEmptyElements(state);
    }

    /**
     * @return Prefix to use as the base for automatically generated
     *   namespace prefixes ("namespace prefix prefix", so to speak).
     *   Defaults to "wstxns".
     */
    public void setAutomaticNsPrefix(String prefix) {
        mConfig.setAutomaticNsPrefix(prefix);
    }

    public void doValidateNamespaces(boolean state) {
        mConfig.doValidateNamespaces(state);
    }

    public void doValidateStructure(boolean state) {
        mConfig.doValidateStructure(state);
    }

    public void doValidateContent(boolean state) {
        mConfig.doValidateContent(state);
    }

    public void doValidateAttributes(boolean state) {
        mConfig.doValidateAttributes(state);
    }

    // // // Convenience mutators for config info:

    /**
     * Method call to make writer be as strict (anal) with output as possible,
     * ie maximize validation it does to try to catch any well-formedness
     * or validity problems. In a way, reverse of calling
     * {@link #configureForMinValidation}.
     */
    public void configureForMaxValidation()
    {
        mConfig.configureForMaxValidation();
    }

    /**
     * Method call to make writer be as lenient with output as possible,
     * ie minimize validation it does. In a way, reverse of calling
     * {@link #configureForMaxValidation}.
     */
    public void configureForMinValidation()
    {
        mConfig.configureForMinValidation();
    }

    /*
    /////////////////////////////////////////
    // Internal methods:
    /////////////////////////////////////////
     */

    /**
     * Factory method used internally; needs to take care of passing
     * proper settings to stream writer.
     */
    private WstxStreamWriter createWstxStreamWriter(Writer w) {
        if (mConfig.willSupportNamespaces()) {
            return new WstxNsStreamWriter(w, mConfig);
        }
        return new WstxNonNsStreamWriter(w, mConfig);
    }

    private WstxStreamWriter createWstxStreamWriter(Result res)
        throws XMLStreamException
    {
        if (res instanceof StreamResult) {
            StreamResult sr = (StreamResult) res;
            Writer w = sr.getWriter();
            if (w == null) {
                OutputStream out = sr.getOutputStream();
                if (out == null) {
                    throw new XMLStreamException("Can not create StAX writer for a StreamResult -- neither writer nor output stream was set.");
                }
                // ... any way to define encoding?
                w = new OutputStreamWriter(out);
            }
            return createWstxStreamWriter(w);
        }

        if (res instanceof SAXResult) {
            SAXResult sr = (SAXResult) res;
            // !!! TBI
            throw new XMLStreamException("Can not create a STaX writer for a SAXResult -- not (yet) implemented.");
        }

        if (res instanceof DOMResult) {
            DOMResult sr = (DOMResult) res;
            // !!! TBI
            throw new XMLStreamException("Can not create a STaX writer for a DOMResult -- not (yet) implemented.");
        }

        throw new IllegalArgumentException("Can not instantiate a writer for XML result type "+res.getClass()+" (unknown type)");
    }
}
