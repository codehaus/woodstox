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
import com.ctc.wstx.exc.WstxIOException;
import com.ctc.wstx.io.BranchingReaderSource;
import com.ctc.wstx.io.InputBootstrapper;
import com.ctc.wstx.io.WstxInputSource;
import com.ctc.wstx.dtd.DTDId;
import com.ctc.wstx.dtd.DTDSubset;
import com.ctc.wstx.dtd.FullDTDReaderProxy;
import com.ctc.wstx.sr.FullStreamReader;
import com.ctc.wstx.sr.ReaderCreator;
import com.ctc.wstx.util.SimpleCache;

/**
 * Input factory that contains full set of cursor API functionality,
 * including full DTD handling. It does not contain Event API, which
 * means that this is a subset of full StAX specification; subset that
 * is useful if event API is not needed. It builds on top of
 * {@link MinimalInputFactory}, basically just adding the connecting
 * code to use real DTD handling implementation.
 *
 * @see MinimalInputFactory
 */
public final class ValidatingInputFactory
    extends MinimalInputFactory
{
    /*
    /////////////////////////////////////////////////////
    // Actual storage of configuration settings
    /////////////////////////////////////////////////////
     */

    // // // Other configuration objects:

    protected SimpleCache mDTDCache = null;

    /*
    /////////////////////////////////////////////////////
    // Life-cycle:
    /////////////////////////////////////////////////////
     */

    public ValidatingInputFactory() {
        super(FullDTDReaderProxy.getInstance(), false);
    }

    /**
     * Need to add this method, since we have no base class to do it...
     */
    public static ValidatingInputFactory newValidatingInstance() {
        return new ValidatingInputFactory();
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

    // Base class has proper implementation already
    //public synchronized void updateSymbolTable(SymbolTable t)

    public synchronized void addCachedDTD(DTDId id, DTDSubset extSubset)
    {
        if (mDTDCache == null) {
            mDTDCache = new SimpleCache(mConfig.getDtdCacheSize());
        }
        mDTDCache.add(id, extSubset);
    }

    /*
    /////////////////////////////////////////////////////
    // Subset of XMLInputFactory API:
    /////////////////////////////////////////////////////
     */

    // // Base class should be fine for the most part...


    /*
    /////////////////////////////////////////////////////
    // Overridden methods:
    /////////////////////////////////////////////////////
     */

    protected XMLStreamReader doCreateSR(BranchingReaderSource input,
                                         ReaderConfig cfg,
                                         InputBootstrapper bs)
        throws IOException, XMLStreamException
    {
        return FullStreamReader.createFullStreamReader(input, this, cfg, bs);
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
            System.err.println("Usage: java "+ValidatingInputFactory.class+" [input file]");
            System.exit(1);
        }
        ValidatingInputFactory f = new ValidatingInputFactory();

        System.out.println("Creating Val. str. reader for file '"+args[0]+"'.");
        XMLStreamReader r = f.createXMLStreamReader(new java.io.FileInputStream(args[0]));
        r.close();
        System.out.println("Reader created and closed ok, exiting.");
    }
}

