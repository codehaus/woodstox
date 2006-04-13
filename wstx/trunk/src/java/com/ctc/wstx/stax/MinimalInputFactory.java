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

import com.ctc.wstx.api.ReaderConfig;
import com.ctc.wstx.cfg.InputConfigFlags;
import com.ctc.wstx.cfg.XmlConsts;
import com.ctc.wstx.dtd.DTDId;
import com.ctc.wstx.dtd.DTDSubset;
import com.ctc.wstx.exc.WstxIOException;
import com.ctc.wstx.io.*;
import com.ctc.wstx.util.DefaultXmlSymbolTable;
import com.ctc.wstx.util.SymbolTable;
import com.ctc.wstx.util.TextBuilder;
import com.ctc.wstx.util.URLUtil;
import com.ctc.wstx.sr.BasicStreamReader;
import com.ctc.wstx.sr.ReaderCreator;

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
 * Regarding Stax2 extensions: they are not included either (since
 * just like stax 1.0, it does have event API extensions) in this
 * minimal subset.
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
        this(true);
    }

    protected MinimalInputFactory(boolean minimal) {
        mConfig = ReaderConfig.createJ2MEDefaults();
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
        return createSR(null, StreamBootstrapper.getInstance(in, null, null));
    }

    public XMLStreamReader createXMLStreamReader(InputStream in, String enc)
        throws XMLStreamException
    {
        if (enc == null || enc.length() == 0) {
            return createXMLStreamReader(in);
        }

        try {
            return createSR(null, ReaderBootstrapper.getInstance
                            (new InputStreamReader(in, enc), null, null, enc));
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
        return createSR(systemId, StreamBootstrapper.getInstance(in, null, systemId));
    }

    public XMLStreamReader createXMLStreamReader(String systemId, Reader r)
        throws XMLStreamException
    {
        return createSR(systemId,
                        ReaderBootstrapper.getInstance(r, null, systemId, null));
    }

    /*
    /////////////////////////////////////////////////////
    // Subset of XMLInputFactory API, accessors/mutators:
    /////////////////////////////////////////////////////
     */

    public Object getProperty(String name)
    {
        Object ob = mConfig.getProperty(name);

        if (ob == null) {
            if (name.equals(XMLInputFactory.ALLOCATOR)) {
                throw new IllegalArgumentException("Event allocator not usable with J2ME subset.");
            }
        }
        return ob;
    }

    public void setProperty(String propName, Object value)
    {
        if (!mConfig.setProperty(propName, value)) {
            if (XMLInputFactory.ALLOCATOR.equals(propName)) {
                throw new IllegalArgumentException("Event allocator not usable with J2ME subset.");
            }
        }
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

    /* Neither minimal factory, nor validating factory implement
     * the event interface...
     */

    //public void setEventAllocator(XMLEventAllocator allocator);

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
    // Overridable methods:
    /////////////////////////////////////////////////////
     */

    protected XMLStreamReader doCreateSR(BranchingReaderSource input,
                                         ReaderConfig cfg,
                                         InputBootstrapper bs)
        throws IOException, XMLStreamException
    {
	// false -> stream reader never (directly) used by an event reader
        return BasicStreamReader.createBasicStreamReader
            (input, this, cfg, bs, false);
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

        ReaderConfig cfg = mConfig.createNonShared(mSymbols.makeChild());
        Reader r;
        try {
            r = bs.bootstrapInput(cfg, true, XmlConsts.XML_V_UNKNOWN);
        } catch (IOException ie) {
            throw new WstxIOException(ie);
        }

        /* null -> no public id available
         * false -> don't close the reader when scope is closed.
         */
        BranchingReaderSource input = InputSourceFactory.constructDocumentSource
            (cfg, bs, // no parent, not from entity
             null, systemId, src, r, false);

      
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
                    (in, ss.getPublicId(), ss.getSystemId());
            } else {
                bs = ReaderBootstrapper.getInstance
                    (r, ss.getPublicId(), ss.getSystemId(), null);
            }
            return createSR(src.getSystemId(), bs);
        }

        if (src instanceof SAXSource) {
            //SAXSource sr = (SAXSource) src;
            // !!! TBI
            throw new XMLStreamException("Can not create a STaX reader for a SAXSource -- not (yet) implemented.");
        }

        if (src instanceof DOMSource) {
            //DOMSource sr = (DOMSource) src;
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

    /*
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
    */
}

