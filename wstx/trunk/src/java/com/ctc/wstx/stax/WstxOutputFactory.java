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
import com.ctc.wstx.api.XMLOutputFactory2;
import com.ctc.wstx.cfg.OutputConfigFlags;
import com.ctc.wstx.evt.WstxEventWriter;
import com.ctc.wstx.sw.WriterConfig;
import com.ctc.wstx.sw.BaseStreamWriter;
import com.ctc.wstx.sw.NonNsStreamWriter;
import com.ctc.wstx.sw.RepairingNsStreamWriter;
import com.ctc.wstx.sw.SimpleNsStreamWriter;
import com.ctc.wstx.util.ArgUtil;

/**
 * Implementation of {@link XMLOutputFactory} for Wstx.
 *<p>
 * TODO:
 *<ul>
 * <li>Implement outputter that creates SAX events
 *  </li>
 * <li>Implement outputter that builds DOM trees
 *  </li>
 *</ul>
 */
public final class WstxOutputFactory
    extends XMLOutputFactory2
    implements WstxOutputFactoryConfig,
               OutputConfigFlags
{
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

    public WstxOutputFactory() {
        mConfig = WriterConfig.createFullDefaults();
    }

    /*
    /////////////////////////////////////////////////////
    // XMLOutputFactory API
    /////////////////////////////////////////////////////
     */

    public XMLEventWriter createXMLEventWriter(OutputStream out) {
        return createXMLEventWriter(new OutputStreamWriter(out));
    }

    public XMLEventWriter createXMLEventWriter(OutputStream out, String enc)
         throws XMLStreamException
   {
        try {
            return createXMLEventWriter(new OutputStreamWriter(out, enc));
        } catch (UnsupportedEncodingException ex) {
            throw new XMLStreamException(ex);
        }
    }

    public XMLEventWriter createXMLEventWriter(javax.xml.transform.Result result)
         throws XMLStreamException
    {
        return new WstxEventWriter(createWstxStreamWriter(result), mConfig);
    }

    public XMLEventWriter createXMLEventWriter(Writer w) {
        return new WstxEventWriter(w, mConfig);
    }

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
    private BaseStreamWriter createWstxStreamWriter(Writer w) {
        if (mConfig.willSupportNamespaces()) {
	    if (mConfig.automaticNamespacesEnabled()) {
		return new RepairingNsStreamWriter(w, mConfig);
	    }
            return new SimpleNsStreamWriter(w, mConfig);
        }
        return new NonNsStreamWriter(w, mConfig);
    }

    private BaseStreamWriter createWstxStreamWriter(Result res)
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

    /*
    /////////////////////////////////////////////////////
    // Trivial test driver, to check loading of the
    // class and instance creation work (mostly to check
    // j2me subset works)
    /////////////////////////////////////////////////////
     */

    public static void main(String[] args)
        throws Exception
    {
        WstxOutputFactory f = new WstxOutputFactory();

        // !!! TODO: Test it somehow?

        System.out.println("Writer factory created ok.");
    }
}
