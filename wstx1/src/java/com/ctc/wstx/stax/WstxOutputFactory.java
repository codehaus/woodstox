/* Woodstox XML processor.
 *<p>
 * Copyright (c) 2004 Tatu Saloranta, tatu.saloranta@iki.fi
 *<p>
 * You can redistribute this work and/or modify it under the terms of
 * LGPL (Lesser Gnu Public License), as published by
 * Free Software Foundation (http://www.fsf.org). No warranty is
 * implied. See LICENSE for details about licensing.
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

import com.ctc.wstx.stax.cfg.OutputConfigFlags;
import com.ctc.wstx.stax.cfg.WriterConfig;
import com.ctc.wstx.stax.stream.BaseStreamWriter;
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
    extends XMLOutputFactory
    implements OutputConfigFlags
{
    // // // Constants for standard StAX properties:

    protected final String DEFAULT_AUTOMATIC_NS_PREFIX = "wstxns";

    // Simple flags:
    final static int PROP_AUTOMATIC_NS = 1;

    // // // Constants for additional properties:

    final static int PROP_ENABLE_NS = 2;
    final static int PROP_OUTPUT_EMPTY_ELEMS = 3;
    final static int PROP_AUTOMATIC_NS_PREFIX = 4;
    final static int PROP_VALIDATE_NS = 5;
    final static int PROP_VALIDATE_STRUCTURE = 6;
    final static int PROP_VALIDATE_CONTENT = 7;
    final static int PROP_VALIDATE_ATTR = 8;


    // // // Default settings for additional properties:

    final static boolean DEFAULT_ENABLE_NS = true;
    final static boolean DEFAULT_OUTPUT_EMPTY_ELEMS = false;

    /* How about validation? Let's turn them mostly off by default, since
     * there are some performance hits when enabling them.
     */

    final static boolean DEFAULT_VALIDATE_NS = false;
    // Structural checks are easy, cheap and useful...
    final static boolean DEFAULT_VALIDATE_STRUCTURE = true;
    final static boolean DEFAULT_VALIDATE_CONTENT = false;
    final static boolean DEFAULT_VALIDATE_ATTR = false;

    /**
     * Default config flags are converted from individual settings,
     * to conform to StAX 1.0 specifications.
     */
    final static int DEFAULT_CONFIG_FLAGS =
        0 // | CFG_AUTOMATIC_NS
        | (DEFAULT_ENABLE_NS ? CFG_ENABLE_NS : 0)
        | (DEFAULT_OUTPUT_EMPTY_ELEMS ? CFG_OUTPUT_EMPTY_ELEMS : 0)
        | (DEFAULT_VALIDATE_NS ? CFG_VALIDATE_NS : 0)
        | (DEFAULT_VALIDATE_STRUCTURE ? CFG_VALIDATE_STRUCTURE : 0)
        | (DEFAULT_VALIDATE_CONTENT ? CFG_VALIDATE_CONTENT : 0)
        | (DEFAULT_VALIDATE_ATTR ? CFG_VALIDATE_ATTR : 0)
        ;

    // // // 

    /**
     * Map to use for converting from String property ids to ints
     * described above; useful to allow use of switch later on.
     */
    final static HashMap sProperties = new HashMap(8);
    static {
        // Standard ones; support for features
        sProperties.put(XMLOutputFactory.IS_REPAIRING_NAMESPACES,
                        new Integer(PROP_AUTOMATIC_NS));

        // Non-standard ones:
        sProperties.put(WstxOutputProperties.P_OUTPUT_ENABLE_NS,
                        new Integer(PROP_ENABLE_NS));
        sProperties.put(WstxOutputProperties.P_OUTPUT_EMPTY_ELEMENTS,
                        new Integer(PROP_OUTPUT_EMPTY_ELEMS));
        sProperties.put(WstxOutputProperties.P_OUTPUT_VALIDATE_NS,
                        new Integer(PROP_VALIDATE_NS));
        sProperties.put(WstxOutputProperties.P_OUTPUT_VALIDATE_STRUCTURE,
                        new Integer(PROP_VALIDATE_STRUCTURE));
        sProperties.put(WstxOutputProperties.P_OUTPUT_VALIDATE_CONTENT,
                        new Integer(PROP_VALIDATE_CONTENT));
        sProperties.put(WstxOutputProperties.P_OUTPUT_VALIDATE_ATTR,
                        new Integer(PROP_VALIDATE_ATTR));
    }

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
            return new WstxNsStreamWriter(w, mConfig);
        }
        return new WstxNonNsStreamWriter(w, mConfig);
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
