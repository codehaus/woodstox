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

import com.ctc.wstx.api.ReaderConfig;
import com.ctc.wstx.api.WstxInputProperties;
import com.ctc.wstx.api.XMLInputFactory2;
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
                                                             mConfig.getInputBufferLength()));
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
			     mConfig.getInputBufferLength(), enc));
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
        return createSR(systemId, StreamBootstrapper.getInstance(in, null, systemId,
								 mConfig.getInputBufferLength()));
    }

    public XMLStreamReader createXMLStreamReader(String systemId, Reader r)
        throws XMLStreamException
    {
        return createSR(systemId,
                        ReaderBootstrapper.getInstance(r, null, systemId,
						       mConfig.getInputBufferLength(), null));
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
                     mConfig.getInputBufferLength());
            } else {
                bs = ReaderBootstrapper.getInstance
                    (r, ss.getPublicId(), ss.getSystemId(),
                     mConfig.getInputBufferLength(), null);
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

