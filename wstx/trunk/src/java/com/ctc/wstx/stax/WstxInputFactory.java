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
import javax.xml.stream.util.XMLEventAllocator;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import com.ctc.wstx.api.WstxInputFactoryConfig;
import com.ctc.wstx.api.WstxInputProperties;
import com.ctc.wstx.cfg.InputConfigFlags;
import com.ctc.wstx.dtd.DTDId;
import com.ctc.wstx.dtd.DTDSubset;
import com.ctc.wstx.dtd.FullDTDReaderProxy;
import com.ctc.wstx.evt.DefaultEventAllocator;
import com.ctc.wstx.evt.FilteredEventReader;
import com.ctc.wstx.evt.WstxEventReader;
import com.ctc.wstx.exc.WstxIOException;
import com.ctc.wstx.io.*;
import com.ctc.wstx.sr.FullStreamReader;
import com.ctc.wstx.sr.ReaderConfig;
import com.ctc.wstx.sr.ReaderCreator;
import com.ctc.wstx.util.DefaultXmlSymbolTable;
import com.ctc.wstx.util.SimpleCache;
import com.ctc.wstx.util.SymbolTable;
import com.ctc.wstx.util.TextBuilder;
import com.ctc.wstx.util.URLUtil;

/**
 * Factory for creating various StAX objects (stream/event reader,
 * writer).
 *
 *<p>
 * Currently supported configuration options fall into two categories. First,
 * all properties from {@link XMLInputFactory} (such as, say,
 * {@link XMLInputFactory#IS_NAMESPACE_AWARE}) are at least recognized, and
 * most are supported. Second, there are additional properties, defined in
 * constant class {@link WstxInputProperties}, that are supported.
 * See {@link WstxInputProperties} for further explanation of these 'custom'
 * properties.
 *
 *<p>
 * TODO:
 *<ul>
 * <li>Pass XMLResolver for readers
 *  </li>
 * <li>Try to implement reader that takes SAX events as input? (hard to do?)
 *  </li>
 * <li>Implement reader that takes DOM tree as input
 *  </li>
 *</ul>
 */
public final class WstxInputFactory
    extends XMLInputFactory
    implements WstxInputFactoryConfig, ReaderCreator,
               InputConfigFlags
{
    /*
    /////////////////////////////////////////////////////
    // Actual storage of configuration settings
    /////////////////////////////////////////////////////
     */

    protected final ReaderConfig mConfig;

    // // // StAX - mandated objects:

    protected XMLEventAllocator mAllocator = null;

    // // // Other configuration objects:

    protected SimpleCache mDTDCache = null;

    /*
    /////////////////////////////////////////////////////
    // Objects shared by actual parsers
    /////////////////////////////////////////////////////
     */

    /**
     * 'Root' symbol table, used for creating actual symbol table instances,
     * but never as is.
     */
    final static SymbolTable mRootSymbols = DefaultXmlSymbolTable.getInstance();
    static {
        /* By default, let's enable intern()ing of names (element, attribute,
         * prefixes) added to symbol table. This is likely to make some
         * access (attr by QName) and comparison of element/attr names
         * more efficient. Although it will add some overhead on adding
         * new symbols to symbol table that should be rather negligible.
         *
         * Also note that always doing intern()ing allows for more efficient
         * access during DTD validation.
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

    public WstxInputFactory() {
        mConfig = ReaderConfig.createFullDefaults(null,
                                                  FullDTDReaderProxy.getInstance());
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
    public synchronized DTDSubset findCachedDTD(DTDId id)
    {
        return (mDTDCache == null) ?
            null : (DTDSubset) mDTDCache.find(id);
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
        SymbolTable curr = mSymbols;
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
        if (mDTDCache == null) {
            mDTDCache = new SimpleCache(mConfig.getDtdCacheSize());
        }
        mDTDCache.add(id, extSubset);
    }

    /*
    /////////////////////////////////////////////////////
    // StAX, XMLInputFactory; factory methods
    /////////////////////////////////////////////////////
     */

    public XMLEventReader createFilteredReader(XMLEventReader reader, EventFilter filter)
    {
        return new FilteredEventReader(reader, filter);
    }

    public XMLStreamReader createFilteredReader(XMLStreamReader reader, StreamFilter filter)
        throws XMLStreamException
    {
        return new FilteredStreamReader(reader, filter);
    } 

    // // // Event reader factory methods

    public XMLEventReader createXMLEventReader(InputStream in)
        throws XMLStreamException
    {
        return new WstxEventReader(createEventAllocator(),
                                   createXMLStreamReader(in));
    }

    public XMLEventReader createXMLEventReader(InputStream in, String enc)
        throws XMLStreamException
    {
        return new WstxEventReader(createEventAllocator(),
                                   createXMLStreamReader(in, enc));
    }

    public XMLEventReader createXMLEventReader(Reader r)
        throws XMLStreamException
    {
        return new WstxEventReader(createEventAllocator(),
                                   createXMLStreamReader(r));
    }

    public XMLEventReader createXMLEventReader(javax.xml.transform.Source source)
        throws XMLStreamException
    {
        return new WstxEventReader(createEventAllocator(),
                                   createXMLStreamReader(source));
    }

    public XMLEventReader createXMLEventReader(String systemId, InputStream in)
        throws XMLStreamException
    {
        return new WstxEventReader(createEventAllocator(),
                                   createXMLStreamReader(systemId, in));
    }

    public XMLEventReader createXMLEventReader(String systemId, Reader r)
        throws XMLStreamException
    {
        return new WstxEventReader(createEventAllocator(),
                                   createXMLStreamReader(systemId, r));
    }

    public XMLEventReader createXMLEventReader(XMLStreamReader sr)
        throws XMLStreamException
    {
        return new WstxEventReader(createEventAllocator(), sr);
    }

    // // // Stream reader factory methods

    public XMLStreamReader createXMLStreamReader(InputStream in)
        throws XMLStreamException
    {
        return createSR(null, StreamBootstrapper.getInstance(in, null, null,
                                                             getInputBufferLength()));
    }

    public XMLStreamReader createXMLStreamReader(InputStream in, String enc)
        throws XMLStreamException
    {
        if (enc == null || enc.length() == 0) {
            return createXMLStreamReader(in);
        }

        try {
            return createSR(null, ReaderBootstrapper.getInstance
                            (new InputStreamReader(in, enc), null, null, getInputBufferLength(), enc));
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
    // StAX, XMLInputFactory; generic accessors/mutators
    /////////////////////////////////////////////////////
     */

    public Object getProperty(String name)
    {
        int id = mConfig.getPropertyId(name);

        // Event allocator not available via J2ME subset...
        if (id == ReaderConfig.PROP_EVENT_ALLOCATOR) {
            return getEventAllocator();
        }
        return mConfig.getProperty(id);
    }

    public XMLEventAllocator getEventAllocator() {
        return mAllocator;
    }
    
    public XMLReporter getXMLReporter() {
        return mConfig.getXMLReporter();
    }

    public XMLResolver getXMLResolver() {
        return mConfig.getXMLResolver();
    }

    public boolean isPropertySupported(String name) {
        return mConfig.isPropertySupported(name);
    }

    public void setEventAllocator(XMLEventAllocator allocator) {
        mAllocator = allocator;
    }

    public void setProperty(String propName, Object value)
    {
        if (XMLInputFactory.ALLOCATOR.equals(propName)) {
            setEventAllocator((XMLEventAllocator) value);
        } else {
            mConfig.setProperty(propName, value);
        }
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

    public int getInputBufferLength() {
        return mConfig.getInputBufferLength();
    }

    public int getTextBufferLength() {
        return mConfig.getTextBufferLength();
    }

    public int getShortestReportedTextSegment() {
        return mConfig.getShortestReportedTextSegment();
    }

    public Map getCustomInternalEntities()
    {
        return mConfig.getCustomInternalEntities();
    }

    public URL getBaseURL() {
        return mConfig.getBaseURL();
    }

    public WstxInputResolver getDtdResolver() {
        return mConfig.getDtdResolver();
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
        mConfig.doSupportDTDs(state);
    }

    public void doValidateWithDTD(boolean state) {
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

    public void setCustomInternalEntities(Map in) {
        mConfig.setCustomInternalEntities(in);
    }

    public void setDtdResolver(WstxInputResolver r) {
        mConfig.setDtdResolver(r);
    }

    public void setEntityResolver(WstxInputResolver r) {
        mConfig.setEntityResolver(r);
    }

    /*
    /////////////////////////////////////////////////////
    // "Profile" mutators
    /////////////////////////////////////////////////////
     */

    /**
     * Method to call to make Reader created conform as closely to XML
     * standard as possible, doing all checks and transformations mandated
     * (linefeed conversions, attr value normalizations). Note that this
     * does NOT include enabling DTD validation.
     *<p>
     * Currently does following changes to settings:
     *<ul>
     * <li>Enables all XML mandated transformations: will convert linefeeds
     *   to canonical LF, will convert white space in attributes as per
     *   specs
     *  <li>
     *</ul>
     *<p>
     * Notes: Does NOT change
     *<ul>
     *  <li>DTD-settings (validation, enabling)
     * </li>
     *  <li>namespace settings
     * </li>
     *  <li>entity handling
     * </li>
     *  <li>'performance' settings (buffer sizes, DTD caching, coalescing,
     *    interning).
     * </li>
     *</ul>
     */
    public void configureForMaxConformance() {
        mConfig.configureForMaxConformance();
    }

    /**
     * Method to call to make Reader created be as "convenient" to use
     * as possible; ie try to avoid having to deal with some of things
     * like segmented text chunks. This may incure some slight performance
     * penalties, but shouldn't affect conformance.
     *<p>
     * Currently does following changes to settings:
     *<ul>
     * <li>Enables text coalescing.
     *  <li>
     * <li>Forces all non-ignorable text events (Text, CDATA) to be reported
     *    as CHARACTERS event.
     *  <li>
     * <li>Enables automatic entity reference replacement.
     *  <li>
     * <li>Disables reporting of ignorable whitespace in prolog and epilog
     *   (outside element tree)
     *  <li>
     *</ul>
     *<p>
     * Notes: Does NOT change
     *<ul>
     *  <li>Text normalization (whether it's more convenient that linefeeds
     *    are converted or not is an open question).
     *   </li>
     *  <li>DTD-settings (validation, enabling)
     * </li>
     *  <li>Namespace settings
     * </li>
     *  <li>other 'performance' settings (buffer sizes, interning, DTD caching)
     *    than coalescing
     * </li>
     *</ul>
     */
    public void configureForMaxConvenience() {
        mConfig.configureForMaxConvenience();
    }

    /**
     * Method to call to make Reader created be as fast as possible reading
     * documents, especially for long-running processes where caching is
     * likely to help. 
     * Potential trade-offs are somewhat increased memory usage
     * (full-sized input buffers), and reduced XML conformance (will not
     * do some of transformations).
     *<p>
     * Currently does following changes to settings:
     *<ul>
     * <li>Disables text coalescing, sets lowish value for min. reported
     *    text segment length.
     *  </li>
     * <li>Increases input buffer length a bit from default.
     *  </li>
     * <li>Disables text normalization (linefeeds, attribute values)
     *  </li>
     * <li>Enables all interning (ns URIs)
     *  </li>
     * <li>Enables DTD caching
     *  </li>
     *</ul>
     *
     *<p>
     * Notes: Does NOT change
     *<ul>
     *  <li>DTD-settings (validation, enabling)
     * </li>
     *  <li>Namespace settings
     * </li>
     *  <li>Entity replacement settings (automatic, support external)
     * </li>
     *</ul>
     */
    public void configureForMaxSpeed() {
        mConfig.configureForMaxSpeed();
    }

    /**
     * Method to call to make Reader created minimize its memory usage.
     * This generally incurs some performance penalties, due to using
     * smaller input buffers.
     *<p>
     * Currently does following changes to settings:
     *<ul>
     * <li>Turns off coalescing, to prevent having to store long text
     *   segments in consequtive buffer; resets min. reported text segment
     *   to the default value.
     *  <li>
     * <li>Reduces buffer sizes from default
     *  <li>
     * <li>Turns off DTD-caching
     *  <li>
     *</ul>
     * Notes: Does NOT change
     *<ul>
     *  <li>Normalization (linefeed, attribute value)
     * </li>
     *  <li>DTD-settings (validation, enabling)
     * </li>
     *  <li>namespace settings
     * </li>
     *  <li>entity handling
     * </li>
     *  <li>Interning settings (may or may not affect mem usage)
     * </li>
     *</ul>
     */
    public void configureForMinMemUsage() {
        mConfig.configureForMinMemUsage();
    }
    
    /**
     * Method to call to make Reader try to preserve as much of input
     * formatting as possible, so that round-tripping would be as lossless
     * as possible, ie that matching writer could produce output as closely
     * matching input format as possible.
     *<p>
     * Currently does following changes to settings:
     *<ul>
     * <li>Enables reporting of ignorable whitespace in prolog and epilog
     *   (outside element tree)
     *  <li>
     * <li>Disables XML mandated transformations (linefeed, attribute values),
     *   to preserve most of original formatting.
     *  <li>
     * <li>Disables coalescing, to prevent CDATA and Text segments from getting
     *   combined.
     *  <li>
     * <li>Increases minimum report text segment length so that all original
     *    text segment chunks are reported fully
     *  <li>
     * <li>Disables automatic entity replacement, to allow for preserving
     *    such references.
     *  <li>
     * <li>Disables automatic conversion of CDATA to Text events.
     *  <li>
     *</ul>
     * Notes: Does NOT change
     *<ul>
     *  <li>DTD-settings (validation, enabling)
     * </li>
     *  <li>namespace settings (enable/disable)
     * </li>
     *  <li>Some perfomance settings: interning settings, DTD caching
     * </li>
     *</ul>
     */
    public void configureForRoundTripping()
    {
        mConfig.configureForRoundTripping();
    }

    /*
    /////////////////////////////////////////////////////
    // Internal methods:
    /////////////////////////////////////////////////////
     */

    /**
     * Method that is eventually called to create a (full) stream read
     * instance.
     */
    private FullStreamReader createSR(String systemId, InputBootstrapper bs)
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
            (null, null, bs,
             null, systemId, src, r, false, cfg.getInputBufferLength());

      
        try {
            FullStreamReader sr = FullStreamReader.createFullStreamReader
                (input, this, cfg, bs);
            return sr;
        } catch (IOException ie) {
            throw new XMLStreamException(ie);
        }
    }

    private FullStreamReader createSR(Source src)
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


    private XMLEventAllocator createEventAllocator() 
    {
        XMLEventAllocator ea = (mAllocator == null) ?
            DefaultEventAllocator.rootInstance()
            : mAllocator;
        return ea.newInstance();
    }

    /*
    /////////////////////////////////////////////////////
    // Trivial test driver, to check loading of the
    // class and instance creation work.
    /////////////////////////////////////////////////////
     */

    public static void main(String[] args)
        throws Exception
    {
        if (args.length != 1) {
            System.err.println("Usage: java com.ctc.wstx.stax.WstxInputFactory [input file]");
            System.exit(1);
        }
        WstxInputFactory f = new WstxInputFactory();

        System.out.println("Creating stream reader for file '"+args[0]+"'.");
        XMLStreamReader r = f.createXMLStreamReader(new java.io.FileInputStream(args[0]));
        r.close();
        System.out.println("Reader created and closed ok, exiting.");
    }
}

