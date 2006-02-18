/* Woodstox XML processor
 *
 * Copyright (c) 2004- Tatu Saloranta, tatu.saloranta@iki.fi
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

import java.io.*;
import java.net.URL;
import java.util.*;

import javax.xml.stream.*;
import javax.xml.stream.util.XMLEventAllocator;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.InputSource;

import org.codehaus.stax2.XMLEventReader2;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamReader2;

import com.ctc.wstx.api.ReaderConfig;
import com.ctc.wstx.api.WstxInputProperties;
import com.ctc.wstx.cfg.InputConfigFlags;
import com.ctc.wstx.dtd.DTDId;
import com.ctc.wstx.dtd.DTDSubset;
import com.ctc.wstx.evt.DefaultEventAllocator;
import com.ctc.wstx.evt.FilteredEventReader;
import com.ctc.wstx.evt.WstxEventReader;
import com.ctc.wstx.exc.WstxIOException;
import com.ctc.wstx.io.*;
import com.ctc.wstx.sr.ValidatingStreamReader;
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
    extends XMLInputFactory2
    implements ReaderCreator,
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
        mConfig = ReaderConfig.createFullDefaults();
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

    // // // Filtered reader factory methods

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
        // false for auto-close, since caller has access to the input stream
        return new WstxEventReader(createEventAllocator(),
                                   createSR(null, in, null, true, false));
    }

    public XMLEventReader createXMLEventReader(InputStream in, String enc)
        throws XMLStreamException
    {
        // false for auto-close, since caller has access to the input stream
        return new WstxEventReader(createEventAllocator(),
                                   createSR(null, in, enc, true, false));
    }

    public XMLEventReader createXMLEventReader(Reader r)
        throws XMLStreamException
    {
        // false for auto-close, since caller has access to the input stream
        return new WstxEventReader(createEventAllocator(),
                                   createSR(null, r, true, false));
    }

    public XMLEventReader createXMLEventReader(javax.xml.transform.Source source)
        throws XMLStreamException
    {
        /* true for auto-close, since caller has no (guaranteed) access to
         * the underlying input stream/reader (source object may hand
         * different readers for each call)
         */
        return new WstxEventReader(createEventAllocator(),
                                   createSR(source, true, true));
    }

    public XMLEventReader createXMLEventReader(String systemId, InputStream in)
        throws XMLStreamException
    {
        // false for auto-close, since caller has access to the input stream
        return new WstxEventReader(createEventAllocator(),
                                   createSR(systemId, in, null, true, false));
    }

    public XMLEventReader createXMLEventReader(String systemId, Reader r)
        throws XMLStreamException
    {
        // false for auto-close, since caller has access to the reader
        return new WstxEventReader(createEventAllocator(),
                                   createSR(systemId, r, true, false));
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
        // false for auto-close, since caller has access to the input stream
        return createSR(null, in, null, false, false);
    }
    
    public XMLStreamReader createXMLStreamReader(InputStream in, String enc)
        throws XMLStreamException
    {
        // false for auto-close, since caller has access to the input stream
        return createSR(null, in, enc, false, false);
    }

    public XMLStreamReader createXMLStreamReader(Reader r)
        throws XMLStreamException
    {
        // false for auto-close, since caller has access to the reader
        return createSR(null, r, false, false);
    }

    public XMLStreamReader createXMLStreamReader(javax.xml.transform.Source src)
        throws XMLStreamException
    {
        /* true for auto-close, since caller has no (guaranteed) access to
         * the underlying input stream/reader (source object may hand
         * different readers for each call)
         */
        return createSR(src, false, true);
    }

    public XMLStreamReader createXMLStreamReader(String systemId, InputStream in)
        throws XMLStreamException
    {
        // false for auto-close, since caller has access to the input stream
        return createSR(systemId, in, null, false, false);
    }

    public XMLStreamReader createXMLStreamReader(String systemId, Reader r)
        throws XMLStreamException
    {
        // false for auto-close, since caller has access to the Reader
        return createSR(systemId, r, false, false);
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

    public void setXMLReporter(XMLReporter r) {
        mConfig.setXMLReporter(r);
    }

    /**
     * Note: it's preferable to use Wstx-specific
     * {@link ReaderConfig#setEntityResolver}
     * instead, if possible, since this just wraps passed in resolver.
     */
    public void setXMLResolver(XMLResolver r)
    {
        mConfig.setXMLResolver(r);
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
    /////////////////////////////////////////////////////
    // StAX2 implementation
    /////////////////////////////////////////////////////
     */

    // // // StAX2, additional factory methods:

    public XMLEventReader2 createXMLEventReader(URL src)
        throws XMLStreamException
    {
        /* true for auto-close, since caller has no access to the underlying
         * input stream created from the URL
         */
        return new WstxEventReader(createEventAllocator(),
                                   createSR(src, true, true));
    }

    public XMLEventReader2 createXMLEventReader(File f)
        throws XMLStreamException
    {
        /* true for auto-close, since caller has no access to the underlying
         * input stream created from the File
         */
        return new WstxEventReader(createEventAllocator(),
                                   createSR(f, true, true));
    }

    public XMLStreamReader2 createXMLStreamReader(URL src)
        throws XMLStreamException
    {
        /* true for auto-close, since caller has no access to the underlying
         * input stream created from the URL
         */
        return createSR(src, false, true);
    }

    /**
     * Convenience factory method that allows for parsing a document
     * stored in the specified file.
     */
    public XMLStreamReader2 createXMLStreamReader(File f)
        throws XMLStreamException
    {
        /* true for auto-close, since caller has no access to the underlying
         * input stream created from the File
         */
        return createSR(f, false, true);
    }

    // // // StAX2 "Profile" mutators

    public void configureForXmlConformance()
    {
        mConfig.configureForXmlConformance();
    }

    public void configureForConvenience()
    {
        mConfig.configureForConvenience();
    }

    public void configureForSpeed()
    {
        mConfig.configureForSpeed();
    }

    public void configureForLowMemUsage()
    {
        mConfig.configureForLowMemUsage();
    }

    public void configureForRoundTripping()
    {
        mConfig.configureForRoundTripping();
    }

    /*
    /////////////////////////////////////////
    // Woodstox-specific configuration access
    /////////////////////////////////////////
     */

    public ReaderConfig getConfig() {
        return mConfig;
    }

    /*
    /////////////////////////////////////////////////////
    // Internal methods:
    /////////////////////////////////////////////////////
     */

    /**
     * Bottleneck method used for creating ALL full stream reader instances
     * (via other createSR() methods and directly)
     *
     * @param forER True, if the reader is being constructed to be used
     *   by an event reader; false if it is not (or the purpose is not
     *   known)
     * @param autoCloseInput Whether the underlying input source should be
     *   actually closed when encountering EOF, or when <code>close()</code>
     *   is called. Will be true for input sources that are automatically
     *   managed by stream reader (input streams created for
     *   {@link java.net.URL} and {@link java.io.File} arguments, or when
     *   configuration settings indicate auto-closing is to be enabled
     *   (the default value is false as per Stax 1.0 specs).
     */
    private ValidatingStreamReader createSR(String systemId, InputBootstrapper bs, 
                                            URL src, boolean forER,
                                            boolean autoCloseInput)
        throws XMLStreamException
    {
        ReaderConfig cfg = mConfig.createNonShared(mSymbols.makeChild());

        /* Automatic closing of input: will happen always for some input
         * types (ones application has no direct access to; but can also
         * be explicitly enabled.
         */
        if (!autoCloseInput) {
            autoCloseInput = cfg.willAutoCloseInput();
        }

        Reader r;
        try {
            r = bs.bootstrapInput(true, getXMLReporter(), null);
        } catch (IOException ie) {
            throw new WstxIOException(ie);
        }

        /* null -> no public id available
         * false -> don't close the reader when scope is closed.
         */
        BranchingReaderSource input = InputSourceFactory.constructDocumentSource
            (bs, null, systemId, src, r, autoCloseInput, cfg.getInputBufferLength());

      
        try {
            ValidatingStreamReader sr = ValidatingStreamReader.createValidatingStreamReader
                (input, this, cfg, bs, forER);
            return sr;
        } catch (IOException ie) {
            throw new WstxIOException(ie);
        }
    }

    /**
     * Method that is eventually called to create a (full) stream read
     * instance.
     *
     * @param systemId System id used for this reader (if any)
     * @param bs Bootstrapper to use for creating actual underlying
     *    physical reader
     * @param forER Flag to indicate whether it will be used via
     *    Event API (will affect some configuration settings), true if it
     *    will be, false if not (or not known)
     * @param autoCloseInput Whether the underlying input source should be
     *   actually closed when encountering EOF, or when <code>close()</code>
     *   is called. Will be true for input sources that are automatically
     *   managed by stream reader (input streams created for
     *   {@link java.net.URL} and {@link java.io.File} arguments, or when
     *   configuration settings indicate auto-closing is to be enabled
     *   (the default value is false as per Stax 1.0 specs).
     */
    private ValidatingStreamReader createSR(String systemId, InputBootstrapper bs,
                                            boolean forER,
                                            boolean autoCloseInput)
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
        return createSR(systemId, bs, src, forER, autoCloseInput);
    }

    protected ValidatingStreamReader createSR(String systemId, InputStream in, String enc,
                                              boolean forER,
                                              boolean autoCloseInput)
        throws XMLStreamException
    {
        // sanity check:
        if (in == null) {
            throw new IllegalArgumentException("Null InputStream is not a valid argument");
        }

        if (enc == null || enc.length() == 0) {
            return createSR(systemId, StreamBootstrapper.getInstance
                            (in, null, systemId, mConfig.getInputBufferLength()),
                            forER, autoCloseInput);
        }

        int inputBufLen = mConfig.getInputBufferLength();
        /* !!! 17-Feb-2006, TSa: We don't yet know if it's xml 1.0 or 1.1;
         *   so have to specify 1.0 (which is less restrictive WRT input
         *   streams). Would be better to let bootstrapper deal with it
         *   though:
         */
        Reader r = DefaultInputResolver.constructOptimizedReader(in, false, enc, inputBufLen);
        return createSR(systemId, ReaderBootstrapper.getInstance
                        (r, null, systemId, enc), forER, autoCloseInput);
    }

    protected ValidatingStreamReader createSR(URL src, boolean forER,
                                              boolean autoCloseInput)
        throws XMLStreamException
    {
        try {
            return createSR(src, URLUtil.optimizedStreamFromURL(src),
                            forER, autoCloseInput);
        } catch (IOException ie) {
            throw new WstxIOException(ie);
        }
    }

    private ValidatingStreamReader createSR(URL src, InputStream in,
                                            boolean forER,
                                            boolean autoCloseInput)
        throws XMLStreamException
    {
        String sysId = src.toExternalForm();
        return createSR(sysId,
                        StreamBootstrapper.getInstance(in, null, sysId, mConfig.getInputBufferLength()),
                        src,
                        forER, autoCloseInput);
    }

    protected ValidatingStreamReader createSR(String systemId, Reader r,
                                              boolean forER,
                                              boolean autoCloseInput)
        throws XMLStreamException
    {
        return createSR(systemId,
                        ReaderBootstrapper.getInstance
                        (r, null, systemId, null), forER, autoCloseInput);
    }

    protected ValidatingStreamReader createSR(File f, boolean forER,
                                              boolean autoCloseInput)
        throws XMLStreamException
    {
        try {
            return createSR(f.toURL(), new FileInputStream(f),
                            forER, autoCloseInput);
        } catch (IOException ie) {
            throw new WstxIOException(ie);
        }
    }

    protected ValidatingStreamReader createSR(javax.xml.transform.Source src,
                                              boolean forER,
                                              boolean autoCloseInput)
        throws XMLStreamException
    {
        if (src instanceof StreamSource) {
            StreamSource ss = (StreamSource) src;
            InputBootstrapper bs;
            Reader r = ss.getReader();
            String sysId = ss.getSystemId();

            if (r == null) {
                InputStream in = ss.getInputStream();
                if (in == null) { // can try just resolving the system id then
                    if (sysId == null) {
                        throw new XMLStreamException("Can not create StAX reader for a StreamSource -- neither reader, input stream nor system id was set.");
                    }
                    try {
                        return createSR(URLUtil.urlFromSystemId(sysId),
                                        forER, autoCloseInput);
                    } catch (IOException ioe) {
                        throw new WstxIOException(ioe);
                    }
                }
                bs = StreamBootstrapper.getInstance(in, ss.getPublicId(), sysId, mConfig.getInputBufferLength());
            } else {
                bs = ReaderBootstrapper.getInstance
                    (r, ss.getPublicId(), sysId, null);
            }
            return createSR(sysId, bs, forER, autoCloseInput);
        }

        if (src instanceof SAXSource) {
            SAXSource ss = (SAXSource) src;
            /* 28-Jan-2006, TSa: Not a complete implementation, but maybe
             *   even this might help...
             */
            InputSource isrc = ss.getInputSource();
            if (isrc != null) {
                InputBootstrapper bs = null;
                Reader r = isrc.getCharacterStream();
                String sysId = isrc.getSystemId();
                if (r != null) {
                    bs = ReaderBootstrapper.getInstance
                        (r, isrc.getPublicId(), sysId, null);
                } else {
                    InputStream in = isrc.getByteStream();
                    if (in != null) {
                        bs = StreamBootstrapper.getInstance(in, isrc.getPublicId(), sysId, mConfig.getInputBufferLength());
                    } else if (sysId != null) { // can try just resolving the system id then
                        try {
                            return createSR(URLUtil.urlFromSystemId(sysId),
                                            forER, autoCloseInput);
                        } catch (IOException ioe) {
                            throw new WstxIOException(ioe);
                        }
                    }
                }
                if (bs != null) {
                    return createSR(sysId, bs, forER, autoCloseInput);
                }
            }
            throw new XMLStreamException("Can only create STaX reader for a SAXSource if Reader or InputStream exposed via getSource(); can not use -- not implemented.");
        }

        if (src instanceof DOMSource) {
            // !!! TBI
            //DOMSource sr = (DOMSource) src;
            throw new XMLStreamException("Can not create a STaX reader for a DOMSource -- not (yet) implemented.");
        }

        throw new IllegalArgumentException("Can not instantiate StAX reader for XML source type "+src.getClass()+" (unknown type)");
    }

    protected XMLEventAllocator createEventAllocator() 
    {
        // Explicitly set allocate?
        if (mAllocator != null) {
            return mAllocator.newInstance();
        }

        /* Complete or fast one? Note: standard allocator is designed
         * in such a way that newInstance() need not be called (calling
         * it wouldn't do anything, anyway)
         */
        return mConfig.willPreserveLocation() ?
            DefaultEventAllocator.getDefaultInstance()
            : DefaultEventAllocator.getFastInstance();
    }
}

