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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.*;

import javax.xml.stream.*;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import com.ctc.wstx.cfg.InputConfigFlags;
import com.ctc.wstx.sr.ReaderConfig;
import com.ctc.wstx.dtd.DTDId;
import com.ctc.wstx.dtd.DTDReaderProxy;
import com.ctc.wstx.dtd.DTDSubset;
import com.ctc.wstx.dtd.MinimalDTDReaderProxy;
import com.ctc.wstx.exc.WstxIOException;
import com.ctc.wstx.io.*;
import com.ctc.wstx.util.DefaultXmlSymbolTable;
import com.ctc.wstx.util.SimpleCache;
import com.ctc.wstx.util.SymbolTable;
import com.ctc.wstx.util.TextBuilder;
import com.ctc.wstx.util.URLUtil;
import com.ctc.wstx.sr.WstxStreamReader;
import com.ctc.wstx.sr.ReaderCreator;
import com.ctc.wstx.sr.ReaderConfig;

/**
 * Minimalistic input factory, which implements just the basic XML
 * parsing functionality, including namespace handling. It does not
 * support any DTD handling beyond simple skipping of internal subset;
 * it also does not implement the Event API part of StAX specs.
 * It is intended as the smallest valid (J2ME) subset of StAX as
 * suggested by the specs.
 *<p>
 * Unfortunately, the way StAX 1.0 is defined, this class can NOT be
 * the base class of the full input factory, without getting references
 * to most of StAX event classes. It does however have lots of shared
 * (cut'n pasted code) with {@link com.ctc.wstx.stax.WstxInputFactory}.
 * Hopefully in future this problem can be resolved.
 *<p>
 * Implementation note: since entity objects are built directly on top
 * of StAX events Objects, couple of event classes (specifically,
 * {@link javax.xml.stream.events.EntityDeclaration} and the generic
 * base class, {@link javax.xml.stream.events.XMLEvent}, and Woodstox
 * classes that implement them) will still need to be included in the
 * subset.
 */
public class MinimalInputFactory
    // can not implement it as of now... would add too many dependencies
    //extends XMLInputFactory
    implements ReaderCreator, InputConfigFlags
{
    /**
     * Flag used to distinguish "real" minimal implementations and
     * extending non-minimal ones
     */
    protected final boolean mIsMinimal;

    /*
    /////////////////////////////////////////////////////
    // Actual storage of configuration settings
    /////////////////////////////////////////////////////
     */

    protected final ReaderConfig mConfig;

    /*
    /////////////////////////////////////////////////////
    // Objects shared by actual parsers
    /////////////////////////////////////////////////////
     */

    /**
     * 'Root' symbol table, passed to instances. Will be updated by
     * instances when they are done with using the table (after parsing
     * the document)
     */
    final static SymbolTable mRootSymbols = DefaultXmlSymbolTable.getInstance();
    static {
        /* By default, let's enable intern()ing of names (element, attribute,
         * prefixes) added to symbol table. This is likely to make some
         * access (attr by QName) and comparison of element/attr names
         * more efficient. However, it will add some overhead on adding
         * new symbols to symbol table.
         */
        mRootSymbols.setInternStrings(true);
    }

    /**
     * Actual current 'parent' symbol table; concrete instances will be
     * created from this instance using <code>makeChild</code> method
     */
    SymbolTable mSymbols = mRootSymbols;

    /*
    /////////////////////////////////////////////////////
    // Life-cycle:
    /////////////////////////////////////////////////////
     */

    public MinimalInputFactory() {
        this(MinimalDTDReaderProxy.getInstance(), true);
    }

    protected MinimalInputFactory(DTDReaderProxy dtdReader, boolean minimal) {
        mConfig = ReaderConfig.createJ2MEDefaults(null, dtdReader);
        mIsMinimal = minimal;
    }

    /**
     * Need to add this method, since we have no base class to do it...
     */
    public static MinimalInputFactory newMinimalInstance() {
        return new MinimalInputFactory();
    }

    /*
    /////////////////////////////////////////////////////
    // ReaderCreator implementation
    /////////////////////////////////////////////////////
     */

    // // // Configuration access methods:

    /**
     * Method readers created by this factory call, if DTD caching is
     * enabled, to see if an external DTD (subset) has been parsed
     * and cached earlier.
     */
    public DTDSubset findCachedDTD(DTDId id)
    {
        // Could throw an exception, too...
        return null;
    }

    // // // Callbacks for updating shared information

    /**
     * Method individual parsers call to pass back symbol table that
     * they updated, which may be useful for other parser to reuse, instead
     * of previous base symbol table.
     *<p>
     * Note: parser is only to call this method, if passed-in symbol
     * table was modified, ie new entry/ies were added in addition to
     * whatever was in root table.
     */
    public synchronized void updateSymbolTable(SymbolTable t)
    {
        SymbolTable curr = mRootSymbols;
        /* Let's only add if table was direct descendant; this prevents
         * siblings from keeping overwriting settings (multiple direct
         * children have additional symbols added)
         */
        if (t.isDirectChildOf(curr)) {
            mSymbols = t;
        }
    }

    public synchronized void addCachedDTD(DTDId id, DTDSubset extSubset)
    {
        // Could throw an exception, too...
    }

    /*
    /////////////////////////////////////////////////////
    // Subset of XMLInputFactory API, factory methods
    /////////////////////////////////////////////////////
     */

    //public XMLEventReader createXMLEventReader(...)

    public XMLStreamReader createXMLStreamReader(InputStream in)
        throws XMLStreamException
    {
        return createSR(null, StreamBootstrapper.getInstance(in, null, null, getInputBufferLength()));
    }

    public XMLStreamReader createXMLStreamReader(InputStream in, String enc)
        throws XMLStreamException
    {
        if (enc == null || enc.length() == 0) {
            return createXMLStreamReader(in);
        }

        try {
            return createSR(null, ReaderBootstrapper.getInstance
                            (new InputStreamReader(in, enc), null, null,
                             getInputBufferLength(), enc));
        } catch (UnsupportedEncodingException ex) {
            throw new XMLStreamException(ex);
        }
    }

    public XMLStreamReader createXMLStreamReader(Reader r)
        throws XMLStreamException
    {
        return createXMLStreamReader(null, r);
    }

    public XMLStreamReader createXMLStreamReader(javax.xml.transform.Source source)
        throws XMLStreamException
    {
        return createSR(source);
    }

    public XMLStreamReader createXMLStreamReader(String systemId, InputStream in)
        throws XMLStreamException
    {
        return createSR(systemId, StreamBootstrapper.getInstance(in, null, systemId, getInputBufferLength()));
    }

    public XMLStreamReader createXMLStreamReader(String systemId, Reader r)
        throws XMLStreamException
    {
        return createSR(systemId,
                        ReaderBootstrapper.getInstance(r, null, systemId, getInputBufferLength(), null));
    }

    /*
    /////////////////////////////////////////////////////
    // Subset of XMLInputFactory API, accessors/mutators:
    /////////////////////////////////////////////////////
     */

    public Object getProperty(String name) {
        int id = mConfig.getPropertyId(name);

        // Event allocator not available via J2ME subset...
        if (id == ReaderConfig.PROP_EVENT_ALLOCATOR) {
            throw new IllegalArgumentException("Event allocator not usable with J2ME subset.");
        }
        return mConfig.getProperty(id);
    }

    //public XMLEventAllocator getEventAllocator();
    
    public XMLReporter getXMLReporter() {
        return mConfig.getXMLReporter();
    }

    public XMLResolver getXMLResolver() {
        return mConfig.getXMLResolver();
    }

    public boolean isPropertySupported(String name) {
        return mConfig.isPropertySupported(name);
    }

    //public void setEventAllocator(XMLEventAllocator allocator);

    public void setProperty(String propName, Object value)
    {
        int id = mConfig.getPropertyId(propName);

        // Event allocator not available via J2ME subset...
        if (id == ReaderConfig.PROP_EVENT_ALLOCATOR) {
            throw new IllegalArgumentException("Event allocator not usable with J2ME subset.");
        }

        mConfig.setProperty(propName, id, value);
    } 

    /*
    /////////////////////////////////////////
    // Type-safe configuration access:
    /////////////////////////////////////////
     */

    // // // Accessors:

    // Standard properties
    // (except ones for which StAX has specific getter)

    public boolean willCoalesceText() {
        return mConfig.willCoalesceText();
    }

    public boolean willSupportNamespaces() {
        return mConfig.willSupportNamespaces();
    }

    public boolean willReplaceEntityRefs() {
        return mConfig.willReplaceEntityRefs();
    }

    public boolean willSupportExternalEntities() {
        return mConfig.willSupportExternalEntities();
    }

    public boolean willSupportDTDs() {
        return mConfig.willSupportDTDs();
    }

    public boolean willValidateWithDTD() {
        return mConfig.willValidateWithDTD();
    }

    // Wstx-properties:

    public boolean willNormalizeLFs() {
        return mConfig.willNormalizeLFs();
    }

    public boolean willNormalizeAttrValues() {
        return mConfig.willNormalizeAttrValues();
    }

    public boolean willInternNsURIs() {
        return mConfig.willInternNsURIs();
    }

    public boolean willReportAllTextAsCharacters() {
        return mConfig.willReportAllTextAsCharacters();
    }

    public boolean willReportPrologWhitespace() {
        return mConfig.willReportPrologWhitespace();
    }

    public boolean willCacheDTDs() {
        return mConfig.willCacheDTDs();
    }

    public boolean willParseLazily() {
        return mConfig.willParseLazily();
    }

    /* No point in implementing this method -- it only affects Event
     * API, which is NOT implemented by this factory.
     */
    /*
    public boolean willPreserveLocation() {
        return mConfig.willPreserveLocation();
    }
    */

    // Real DTD-handling not supported by this factory...
    /*
    public boolean willSupportDTDPP() {
        return mConfig.willSupportDTDPP();
    }
    */

    public int getInputBufferLength() {
        return mConfig.getInputBufferLength();
    }

    public int getTextBufferLength() {
        return mConfig.getTextBufferLength();
    }

    public int getShortestReportedTextSegment() {
        return mConfig.getShortestReportedTextSegment();
    }

    public Map getCustomInternalEntities() {
        return mConfig.getCustomInternalEntities();
    }

    public URL getBaseURL() {
        return mConfig.getBaseURL();
    }

    public WstxInputResolver getDtdResolver() {
        return null;
    }

    public WstxInputResolver getEntityResolver() {
        return mConfig.getEntityResolver();
    }

    // // // Mutators:

    public void doCoalesceText(boolean state) {
        mConfig.doCoalesceText(state);
    }

    public void doSupportNamespaces(boolean state) {
        mConfig.doSupportNamespaces(state);
    }

    public void doReplaceEntityRefs(boolean state) {
        mConfig.doReplaceEntityRefs(state);
    }

    public void doSupportExternalEntities(boolean state) {
        mConfig.doSupportExternalEntities(state);
    }

    public void doSupportDTDs(boolean state) {
        if (state && mIsMinimal) {
            throwUnsupported("doSupportDTDs(true)");
        }
        mConfig.doSupportDTDs(state);
    }

    public void doValidateWithDTD(boolean state) {
        if (state && mIsMinimal) {
            throwUnsupported("doValidateWithDTD(true)");
        }
        mConfig.doValidateWithDTD(state);
    }

    public void setXMLReporter(XMLReporter r) {
        mConfig.setXMLReporter(r);
    }

    /**
     * Note: it's preferable to use Wstx-specific {@link #setEntityResolver}
     * instead, if possible, since this just wraps passed in resolver.
     */
    public void setXMLResolver(XMLResolver r)
    {
        mConfig.setXMLResolver(r);
    }

    // Wstx-properties:

    public void doNormalizeLFs(boolean state) {
        mConfig.doNormalizeLFs(state);
    }

    public void doNormalizeAttrValues(boolean state) {
        mConfig.doNormalizeAttrValues(state);
    }

    public void doInternNsURIs(boolean state) {
        mConfig.doInternNsURIs(state);
    }

    public void doReportPrologWhitespace(boolean state) {
        mConfig.doReportPrologWhitespace(state);
    }

    public void doCacheDTDs(boolean state) {
        mConfig.doCacheDTDs(state);
    }

    public void doParseLazily(boolean state) {
        mConfig.doParseLazily(state);
    }

    /* No point in implementing this method -- it only affects Event
     * API, which is NOT implemented by this factory.
     */
    /*
    public void doPreserveLocation(boolean state) {
        mConfig.doPreserveLocation(state);
    }
    */

    // Real DTD-handling not supported by this factory...
    /*
    public void doSupportDTDPP(boolean state) {
        mConfig.doSupportDTDPP(state);
    }
    */

    public void doReportAllTextAsCharacters(boolean state) {
        mConfig.doReportAllTextAsCharacters(state);
    }

    public void setInputBufferLength(int value)
    {
        mConfig.setInputBufferLength(value);
    }

    public void setTextBufferLength(int value) {
        mConfig.setTextBufferLength(value);
    }

    public void setShortestReportedTextSegment(int value) {
        mConfig.setShortestReportedTextSegment(value);
    }

    public void setBaseURL(URL baseURL) {
        mConfig.setBaseURL(baseURL);
    }

    public void setCustomInternalEntities(Map in)
    {
        mConfig.setCustomInternalEntities(in);
    }

    public void setDtdResolver(WstxInputResolver r) {
        if (r != null && mIsMinimal) {
            throwUnsupported("setDtdResolver(...)");
        }
        mConfig.setDtdResolver(r);
    }

    public void setEntityResolver(WstxInputResolver r)
    {
        mConfig.setEntityResolver(r);
    }

    /*
    /////////////////////////////////////////////////////
    // "Profile" mutators
    /////////////////////////////////////////////////////
     */

    public void configureForMaxConformance() {
        mConfig.configureForMaxConformance();
    }

    public void configureForMaxConvenience() {
        mConfig.configureForMaxConvenience();
    }

    public void configureForMaxSpeed() {
        mConfig.configureForMaxSpeed();
    }

    public void configureForMinMemUsage() {
        mConfig.configureForMinMemUsage();
    }

    public void configureForRoundTripping() {
        mConfig.configureForRoundTripping();
    }

    /*
    /////////////////////////////////////////////////////
    // Overridable methods:
    /////////////////////////////////////////////////////
     */

    protected XMLStreamReader doCreateSR(BranchingReaderSource input,
                                         ReaderConfig cfg,
                                         InputBootstrapper bs)
        throws IOException, XMLStreamException
    {
        return WstxStreamReader.createBasicStreamReader(input, this, cfg, bs);
    }

    /*
    /////////////////////////////////////////////////////
    // Internal methods:
    /////////////////////////////////////////////////////
     */

    private XMLStreamReader createSR(String systemId, InputBootstrapper bs)
        throws XMLStreamException
    {
        // 16-Aug-2004, TSa: Maybe we have a context?
        URL src = mConfig.getBaseURL();

        // If not, maybe we can derive it from system id?
        if ((src == null) && (systemId != null && systemId.length() > 0)) {
            try {
                src = URLUtil.urlFromSystemId(systemId);
            } catch (IOException ie) {
                throw new WstxIOException(ie);
            }
        }

        Reader r;
        try {
            r = bs.bootstrapInput(true, getXMLReporter());
        } catch (IOException ie) {
            throw new WstxIOException(ie);
        }

        /* null -> no parent
         * null -> not expanded from an entity
         * null -> no public id available
         * false -> don't close the reader when scope is closed.
         */
        ReaderConfig cfg = mConfig.createNonShared(mSymbols.makeChild());
        BranchingReaderSource input = InputSourceFactory.constructBranchingSource
            (null, null, bs, // no parent, not from entity
             null, systemId, src, r, false, cfg.getInputBufferLength());

      
        try {
            return doCreateSR(input, cfg, bs);
        } catch (IOException ie) {
            throw new XMLStreamException(ie);
        }
    }

    /**
     * Helper factory method that will create and return a
     * {@link MinimalStreamReader} configured according to current
     * settings of this factory.
     */
    private XMLStreamReader createSR(Source src)
        throws XMLStreamException
    {
        if (src instanceof StreamSource) {
            StreamSource ss = (StreamSource) src;
            InputBootstrapper bs;

            Reader r = ss.getReader();
            if (r == null) {
                InputStream in = ss.getInputStream();
                if (in == null) {
                    throw new XMLStreamException("Can not create StAX reader for a StreamSource -- neither reader nor input stream was set.");
                }
                bs = StreamBootstrapper.getInstance
                    (in, ss.getPublicId(), ss.getSystemId(),
                     getInputBufferLength());
            } else {
                bs = ReaderBootstrapper.getInstance
                    (r, ss.getPublicId(), ss.getSystemId(),
                     getInputBufferLength(), null);
            }
            return createSR(src.getSystemId(), bs);
        }

        if (src instanceof SAXSource) {
            SAXSource sr = (SAXSource) src;
            // !!! TBI
            throw new XMLStreamException("Can not create a STaX reader for a SAXSource -- not (yet) implemented.");
        }

        if (src instanceof DOMSource) {
            DOMSource sr = (DOMSource) src;
            // !!! TBI
            throw new XMLStreamException("Can not create a STaX reader for a DOMSource -- not (yet) implemented.");
        }

        throw new IllegalArgumentException("Can not instantiate StAX reader for XML source type "+src.getClass()+" (unknown type)");
    }
    
    private void throwUnsupported(String msg) {
        throw new IllegalArgumentException("MinimalInputFactory has no DTD support: can not call "+msg);
    }

    /*
    /////////////////////////////////////////////////////
    // Trivial test driver, to check loading of the
    // class and instance creation work
    /////////////////////////////////////////////////////
     */

    public static void main(String[] args)
        throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java "+MinimalInputFactory.class+" [input file]");
            System.exit(1);
        }
        MinimalInputFactory f = new MinimalInputFactory();

        System.out.println("Creating J2ME stream reader for file '"+args[0]+"'.");
        XMLStreamReader r = f.createXMLStreamReader(new java.io.FileInputStream(args[0]));
        r.close();
        System.out.println("Reader created and closed ok, exiting.");
    }
}

